package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.domain.candidate.entity.CandidateDescriptionEntity;
import com.alpha.backend.domain.candidate.entity.CandidateEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillsEmbeddingEntity;
import com.alpha.backend.domain.candidate.repository.CandidateDescriptionRepository;
import com.alpha.backend.domain.candidate.repository.CandidateRepository;
import com.alpha.backend.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.backend.domain.candidate.repository.CandidateSkillsEmbeddingRepository;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.grpc.proto.CandidateRow;
import com.alpha.backend.infrastructure.grpc.proto.RowChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Candidate 도메인 데이터 처리기 (v3)
 * <p>
 * Python 서버로부터 전송된 Candidate RowChunk를 처리하여 4개 테이블에 저장:
 * - candidate, candidate_skill, candidate_description, candidate_skills_embedding
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateDataProcessor implements DataProcessor {

    private final CandidateRepository candidateRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateDescriptionRepository candidateDescriptionRepository;
    private final CandidateSkillsEmbeddingRepository candidateSkillsEmbeddingRepository;
    private final BatchProperties batchProperties;

    @Override
    public int processChunk(RowChunk rowChunk) {
        List<CandidateRow> rows = rowChunk.getCandidate().getRowsList();
        log.info("Received {} candidate rows from RowChunk", rows.size());

        if (rows.isEmpty()) {
            log.warn("Empty row list, skipping save");
            return 0;
        }

        // Row → Entity 변환 (4개 테이블)
        List<CandidateEntity> candidateEntities = new ArrayList<>();
        List<CandidateSkillEntity> allSkillEntities = new ArrayList<>();
        List<CandidateDescriptionEntity> descriptionEntities = new ArrayList<>();
        List<CandidateSkillsEmbeddingEntity> embeddingEntities = new ArrayList<>();

        for (CandidateRow row : rows) {
            UUID candidateId = UUID.fromString(row.getCandidateId());

            candidateEntities.add(toCandidateEntity(row, candidateId));
            allSkillEntities.addAll(toCandidateSkillEntities(row, candidateId));
            descriptionEntities.add(toCandidateDescriptionEntity(row, candidateId));
            embeddingEntities.add(toCandidateSkillsEmbeddingEntity(row, candidateId));
        }

        // DB 저장 (Virtual Thread 병렬 처리)
        // candidate 먼저 저장 (FK 제약조건 - 다른 테이블이 참조)
        long startTime = System.currentTimeMillis();
        candidateRepository.upsertAll(candidateEntities);
        long candidateTime = System.currentTimeMillis() - startTime;
        log.info("Upserted {} candidate entities ({}ms)", candidateEntities.size(), candidateTime);

        // 나머지 3개 테이블 병렬 처리 (Virtual Thread)
        long parallelStartTime = System.currentTimeMillis();

        final List<CandidateSkillEntity> finalSkillEntities = allSkillEntities;
        final List<CandidateDescriptionEntity> finalDescriptionEntities = descriptionEntities;
        final List<CandidateSkillsEmbeddingEntity> finalEmbeddingEntities = embeddingEntities;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> skillFuture = executor.submit(() -> {
                long st = System.currentTimeMillis();
                candidateSkillRepository.upsertAll(finalSkillEntities);
                log.info("Upserted {} candidate skill entities ({}ms)", finalSkillEntities.size(), System.currentTimeMillis() - st);
            });

            Future<?> descFuture = executor.submit(() -> {
                long st = System.currentTimeMillis();
                candidateDescriptionRepository.upsertAll(finalDescriptionEntities);
                log.info("Upserted {} candidate description entities ({}ms)", finalDescriptionEntities.size(), System.currentTimeMillis() - st);
            });

            Future<?> embeddingFuture = executor.submit(() -> {
                long st = System.currentTimeMillis();
                candidateSkillsEmbeddingRepository.upsertAll(finalEmbeddingEntities);
                log.info("Upserted {} candidate skills embedding entities ({}ms)", finalEmbeddingEntities.size(), System.currentTimeMillis() - st);
            });

            skillFuture.get();
            descFuture.get();
            embeddingFuture.get();
        } catch (Exception e) {
            throw new RuntimeException("병렬 테이블 쓰기 실패: " + e.getMessage(), e);
        }

        long parallelTime = System.currentTimeMillis() - parallelStartTime;
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Parallel write completed: candidate={}ms, parallel(skill+desc+embedding)={}ms, total={}ms",
                candidateTime, parallelTime, totalTime);

        return rows.size();
    }

    @Override
    public String getDomain() {
        return "candidate";
    }

    private CandidateEntity toCandidateEntity(CandidateRow row, UUID candidateId) {
        CandidateEntity entity = new CandidateEntity();
        entity.setCandidateId(candidateId);
        entity.setPositionCategory(row.getPositionCategory());
        entity.setExperienceYears(row.getExperienceYears());
        entity.setOriginalResume(row.getOriginalResume());
        return entity;
    }

    private List<CandidateSkillEntity> toCandidateSkillEntities(CandidateRow row, UUID candidateId) {
        return row.getSkillsList().stream()
                .map(skill -> {
                    CandidateSkillEntity entity = new CandidateSkillEntity();
                    entity.setCandidateId(candidateId);
                    entity.setSkill(skill);
                    return entity;
                })
                .collect(Collectors.toList());
    }

    private CandidateDescriptionEntity toCandidateDescriptionEntity(CandidateRow row, UUID candidateId) {
        CandidateDescriptionEntity entity = new CandidateDescriptionEntity();
        entity.setCandidateId(candidateId);
        entity.setOriginalResume(row.getOriginalResume());
        entity.setResumeLang(row.hasResumeLang() ? row.getResumeLang() : null);
        entity.setMoreinfo(row.hasMoreinfo() ? row.getMoreinfo() : null);
        entity.setLookingFor(row.hasLookingFor() ? row.getLookingFor() : null);
        return entity;
    }

    private CandidateSkillsEmbeddingEntity toCandidateSkillsEmbeddingEntity(CandidateRow row, UUID candidateId) {
        int expectedDim = batchProperties.getDomainConfig("candidate").getVectorDimension();
        List<Float> vectorList = row.getSkillsVectorList();
        if (vectorList.size() != expectedDim) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch for UUID %s: expected=%d, actual=%d",
                            candidateId, expectedDim, vectorList.size())
            );
        }

        float[] vectorArray = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            vectorArray[i] = vectorList.get(i);
        }

        String[] skillsArray = row.getSkillsList().toArray(new String[0]);
        return CandidateSkillsEmbeddingEntity.fromFloatArray(candidateId, skillsArray, vectorArray);
    }
}
