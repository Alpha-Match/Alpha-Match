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

            // 4. DB 저장 (순서 중요: recruit → skill, description, embedding)
            recruitRepository.upsertAll(recruitEntities);
            log.info("Upserted {} recruit entities", recruitEntities.size());

            recruitSkillRepository.upsertAll(allSkillEntities);
            log.info("Upserted {} recruit skill entities", allSkillEntities.size());

            recruitDescriptionRepository.upsertAll(descriptionEntities);
            log.info("Upserted {} recruit description entities", descriptionEntities.size());

            recruitSkillsEmbeddingRepository.upsertAll(embeddingEntities);
            log.info("Upserted {} recruit embedding entities", embeddingEntities.size());

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
