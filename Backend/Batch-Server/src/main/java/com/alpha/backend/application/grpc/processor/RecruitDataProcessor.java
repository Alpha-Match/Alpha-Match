package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.application.grpc.dto.RecruitRowDto;
import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;
import com.alpha.backend.domain.recruit.entity.RecruitEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;
import com.alpha.backend.domain.recruit.repository.RecruitDescriptionRepository;
import com.alpha.backend.domain.recruit.repository.RecruitRepository;
import com.alpha.backend.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.backend.domain.recruit.repository.RecruitSkillsEmbeddingRepository;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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
 * Python 서버로부터 전송된 Recruit 데이터를 JSON에서 파싱하여 4개 테이블에 저장
 *
 * v2 변경사항:
 * - 2-table → 4-table 구조 (recruit, recruit_skill, recruit_description, recruit_skills_embedding)
 * - RecruitMetadataEntity → RecruitEntity
 * - RecruitEmbeddingEntity → RecruitSkillsEmbeddingEntity
 * - 신규: RecruitDescriptionEntity, RecruitSkillEntity
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitDataProcessor implements DataProcessor {

    private final ObjectMapper objectMapper;
    private final RecruitRepository recruitRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final RecruitDescriptionRepository recruitDescriptionRepository;
    private final RecruitSkillsEmbeddingRepository recruitSkillsEmbeddingRepository;
    private final BatchProperties batchProperties;

    @Override
    public int processChunk(byte[] jsonChunk) {
        try {
            // 1. bytes → UTF-8 문자열
            String jsonString = new String(jsonChunk, StandardCharsets.UTF_8);
            log.debug("Received JSON chunk: {} bytes", jsonChunk.length);

            // 2. JSON → DTO 리스트
            List<RecruitRowDto> dtos = objectMapper.readValue(
                    jsonString,
                    new TypeReference<List<RecruitRowDto>>() {
                    }
            );
            log.info("Parsed {} recruit rows from JSON", dtos.size());

            if (dtos.isEmpty()) {
                log.warn("Empty DTO list, skipping save");
                return 0;
            }

            // 3. DTO → Entity 변환 (4개 테이블)
            List<RecruitEntity> recruitEntities = new ArrayList<>();
            List<RecruitSkillEntity> allSkillEntities = new ArrayList<>();
            List<RecruitDescriptionEntity> descriptionEntities = new ArrayList<>();
            List<RecruitSkillsEmbeddingEntity> embeddingEntities = new ArrayList<>();

            for (RecruitRowDto dto : dtos) {
                UUID recruitId = UUID.fromString(dto.getId());

                recruitEntities.add(toRecruitEntity(dto, recruitId));
                allSkillEntities.addAll(toRecruitSkillEntities(dto, recruitId));
                descriptionEntities.add(toRecruitDescriptionEntity(dto, recruitId));
                embeddingEntities.add(toRecruitSkillsEmbeddingEntity(dto, recruitId));
            }

            // 4. DB 저장 (Virtual Thread 병렬 처리)
            // 4-1. recruit 먼저 저장 (FK 제약조건 - 다른 테이블이 참조)
            long startTime = System.currentTimeMillis();
            recruitRepository.upsertAll(recruitEntities);
            long recruitTime = System.currentTimeMillis() - startTime;
            log.info("Upserted {} recruit entities ({}ms)", recruitEntities.size(), recruitTime);

            // 4-2. 나머지 3개 테이블 병렬 처리 (Virtual Thread)
            // FK 제약조건이 recruit 테이블만 참조하므로 병렬 실행 가능
            long parallelStartTime = System.currentTimeMillis();

            // 캡처용 final 변수
            final List<RecruitSkillEntity> finalSkillEntities = allSkillEntities;
            final List<RecruitDescriptionEntity> finalDescriptionEntities = descriptionEntities;
            final List<RecruitSkillsEmbeddingEntity> finalEmbeddingEntities = embeddingEntities;

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // 3개 테이블 동시 쓰기
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

                // 모든 병렬 작업 완료 대기
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

            return dtos.size();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON chunk", e);
            throw new RuntimeException("JSON 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to process chunk", e);
            throw new RuntimeException("Chunk 처리 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDomain() {
        return "recruit";
    }

    /**
     * DTO → RecruitEntity 변환
     */
    private RecruitEntity toRecruitEntity(RecruitRowDto dto, UUID recruitId) {
        RecruitEntity entity = new RecruitEntity();
        entity.setRecruitId(recruitId);
        entity.setPosition(dto.getPosition());
        entity.setCompanyName(dto.getCompanyName());
        entity.setExperienceYears(dto.getExperienceYears());
        entity.setPrimaryKeyword(dto.getPrimaryKeyword());
        entity.setEnglishLevel(dto.getEnglishLevel());

        // published_at 파싱 (ISO 8601 → OffsetDateTime)
        if (dto.getPublishedAt() != null && !dto.getPublishedAt().isEmpty()) {
            entity.setPublishedAt(OffsetDateTime.parse(dto.getPublishedAt()));
        } else {
            entity.setPublishedAt(null);
        }

        return entity;
    }

    /**
     * DTO → List<RecruitSkillEntity> 변환
     */
    private List<RecruitSkillEntity> toRecruitSkillEntities(RecruitRowDto dto, UUID recruitId) {
        if (dto.getSkills() == null || dto.getSkills().isEmpty()) {
            return List.of();
        }

        return dto.getSkills().stream()
                .map(skillName -> {
                    RecruitSkillEntity entity = new RecruitSkillEntity();
                    entity.setRecruitId(recruitId);
                    entity.setSkill(skillName);
                    return entity;
                })
                .collect(Collectors.toList());
    }

    /**
     * DTO → RecruitDescriptionEntity 변환
     */
    private RecruitDescriptionEntity toRecruitDescriptionEntity(RecruitRowDto dto, UUID recruitId) {
        RecruitDescriptionEntity entity = new RecruitDescriptionEntity();
        entity.setRecruitId(recruitId);
        entity.setLongDescription(dto.getLongDescription());
        entity.setDescriptionLang(dto.getDescriptionLang());
        return entity;
    }

    /**
     * DTO → RecruitSkillsEmbeddingEntity 변환
     */
    private RecruitSkillsEmbeddingEntity toRecruitSkillsEmbeddingEntity(RecruitRowDto dto, UUID recruitId) {
        // Vector 차원 검증
        int expectedDim = batchProperties.getDomainConfig("recruit").getVectorDimension();
        if (dto.getSkillsVector().size() != expectedDim) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch for UUID %s: expected=%d, actual=%d",
                            recruitId, expectedDim, dto.getSkillsVector().size())
            );
        }

        // List<Float> → float[]
        float[] vectorArray = new float[dto.getSkillsVector().size()];
        for (int i = 0; i < dto.getSkillsVector().size(); i++) {
            vectorArray[i] = dto.getSkillsVector().get(i);
        }

        RecruitSkillsEmbeddingEntity entity = new RecruitSkillsEmbeddingEntity();
        entity.setRecruitId(recruitId);
        entity.setSkills(dto.getSkills() != null ? dto.getSkills().toArray(new String[0]) : new String[0]);
        entity.setSkillsVector(new PGvector(vectorArray));

        return entity;
    }
}
