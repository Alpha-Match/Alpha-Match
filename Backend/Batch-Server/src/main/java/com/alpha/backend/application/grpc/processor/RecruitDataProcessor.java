package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;
import com.alpha.backend.domain.recruit.entity.RecruitEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;
import com.alpha.backend.domain.recruit.repository.RecruitDescriptionRepository;
import com.alpha.backend.domain.recruit.repository.RecruitRepository;
import com.alpha.backend.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.backend.domain.recruit.repository.RecruitSkillsEmbeddingRepository;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.grpc.proto.RecruitRow;
import com.alpha.backend.infrastructure.grpc.proto.RowChunk;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Recruit 도메인 데이터 처리기 (v2)
 * <p>
 * Python 서버로부터 전송된 Recruit RowChunk를 처리하여 4개 테이블에 저장:
 * - recruit, recruit_skill, recruit_description, recruit_skills_embedding
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitDataProcessor implements DataProcessor {

    private final RecruitRepository recruitRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final RecruitDescriptionRepository recruitDescriptionRepository;
    private final RecruitSkillsEmbeddingRepository recruitSkillsEmbeddingRepository;
    private final BatchProperties batchProperties;

    @Override
    public int processChunk(RowChunk rowChunk) {
        List<RecruitRow> rows = rowChunk.getRecruit().getRowsList();
        log.info("Received {} recruit rows from RowChunk", rows.size());

        if (rows.isEmpty()) {
            log.warn("Empty row list, skipping save");
            return 0;
        }

        // Row → Entity 변환 (4개 테이블)
        List<RecruitEntity> recruitEntities = new ArrayList<>();
        List<RecruitSkillEntity> allSkillEntities = new ArrayList<>();
        List<RecruitDescriptionEntity> descriptionEntities = new ArrayList<>();
        List<RecruitSkillsEmbeddingEntity> embeddingEntities = new ArrayList<>();

        for (RecruitRow row : rows) {
            UUID recruitId = UUID.fromString(row.getId());

            recruitEntities.add(toRecruitEntity(row, recruitId));
            allSkillEntities.addAll(toRecruitSkillEntities(row, recruitId));
            descriptionEntities.add(toRecruitDescriptionEntity(row, recruitId));
            embeddingEntities.add(toRecruitSkillsEmbeddingEntity(row, recruitId));
        }

        // DB 저장 (Virtual Thread 병렬 처리)
        // recruit 먼저 저장 (FK 제약조건 - 다른 테이블이 참조)
        long startTime = System.currentTimeMillis();
        recruitRepository.upsertAll(recruitEntities);
        long recruitTime = System.currentTimeMillis() - startTime;
        log.info("Upserted {} recruit entities ({}ms)", recruitEntities.size(), recruitTime);

        // 나머지 3개 테이블 병렬 처리 (Virtual Thread)
        long parallelStartTime = System.currentTimeMillis();

        final List<RecruitSkillEntity> finalSkillEntities = allSkillEntities;
        final List<RecruitDescriptionEntity> finalDescriptionEntities = descriptionEntities;
        final List<RecruitSkillsEmbeddingEntity> finalEmbeddingEntities = embeddingEntities;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> skillFuture = executor.submit(() -> {
                long st = System.currentTimeMillis();
                recruitSkillRepository.upsertAll(finalSkillEntities);
                log.info("Upserted {} recruit skill entities ({}ms)", finalSkillEntities.size(), System.currentTimeMillis() - st);
            });

            Future<?> descFuture = executor.submit(() -> {
                long st = System.currentTimeMillis();
                recruitDescriptionRepository.upsertAll(finalDescriptionEntities);
                log.info("Upserted {} recruit description entities ({}ms)", finalDescriptionEntities.size(), System.currentTimeMillis() - st);
            });

            Future<?> embeddingFuture = executor.submit(() -> {
                long st = System.currentTimeMillis();
                recruitSkillsEmbeddingRepository.upsertAll(finalEmbeddingEntities);
                log.info("Upserted {} recruit embedding entities ({}ms)", finalEmbeddingEntities.size(), System.currentTimeMillis() - st);
            });

            skillFuture.get();
            descFuture.get();
            embeddingFuture.get();
        } catch (Exception e) {
            throw new RuntimeException("병렬 테이블 쓰기 실패: " + e.getMessage(), e);
        }

        long parallelTime = System.currentTimeMillis() - parallelStartTime;
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Parallel write completed: recruit={}ms, parallel(skill+desc+embedding)={}ms, total={}ms",
                recruitTime, parallelTime, totalTime);

        return rows.size();
    }

    @Override
    public String getDomain() {
        return "recruit";
    }

    private RecruitEntity toRecruitEntity(RecruitRow row, UUID recruitId) {
        RecruitEntity entity = new RecruitEntity();
        entity.setRecruitId(recruitId);
        entity.setPosition(row.getPosition());
        entity.setCompanyName(row.getCompanyName());
        entity.setExperienceYears(row.getExperienceYears());
        entity.setPrimaryKeyword(row.getPrimaryKeyword().isEmpty() ? null : row.getPrimaryKeyword());
        entity.setEnglishLevel(row.getEnglishLevel().isEmpty() ? null : row.getEnglishLevel());

        if (!row.getPublishedAt().isEmpty()) {
            entity.setPublishedAt(OffsetDateTime.parse(row.getPublishedAt()));
        } else {
            entity.setPublishedAt(null);
        }

        return entity;
    }

    private List<RecruitSkillEntity> toRecruitSkillEntities(RecruitRow row, UUID recruitId) {
        return row.getSkillsList().stream()
                .map(skillName -> {
                    RecruitSkillEntity entity = new RecruitSkillEntity();
                    entity.setRecruitId(recruitId);
                    entity.setSkill(skillName);
                    return entity;
                })
                .collect(Collectors.toList());
    }

    private RecruitDescriptionEntity toRecruitDescriptionEntity(RecruitRow row, UUID recruitId) {
        RecruitDescriptionEntity entity = new RecruitDescriptionEntity();
        entity.setRecruitId(recruitId);
        entity.setLongDescription(row.getLongDescription().isEmpty() ? null : row.getLongDescription());
        entity.setDescriptionLang(row.getDescriptionLang().isEmpty() ? null : row.getDescriptionLang());
        return entity;
    }

    private RecruitSkillsEmbeddingEntity toRecruitSkillsEmbeddingEntity(RecruitRow row, UUID recruitId) {
        int expectedDim = batchProperties.getDomainConfig("recruit").getVectorDimension();
        List<Float> vectorList = row.getSkillsVectorList();
        if (vectorList.size() != expectedDim) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch for UUID %s: expected=%d, actual=%d",
                            recruitId, expectedDim, vectorList.size())
            );
        }

        float[] vectorArray = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            vectorArray[i] = vectorList.get(i);
        }

        RecruitSkillsEmbeddingEntity entity = new RecruitSkillsEmbeddingEntity();
        entity.setRecruitId(recruitId);
        entity.setSkills(row.getSkillsList().toArray(new String[0]));
        entity.setSkillsVector(new PGvector(vectorArray));

        return entity;
    }
}
