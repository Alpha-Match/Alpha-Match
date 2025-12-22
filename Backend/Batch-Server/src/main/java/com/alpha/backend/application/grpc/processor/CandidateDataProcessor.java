package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.application.grpc.dto.CandidateRowDto;
import com.alpha.backend.domain.candidate.entity.CandidateDescriptionEntity;
import com.alpha.backend.domain.candidate.entity.CandidateEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillsEmbeddingEntity;
import com.alpha.backend.domain.candidate.repository.CandidateDescriptionRepository;
import com.alpha.backend.domain.candidate.repository.CandidateRepository;
import com.alpha.backend.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.backend.domain.candidate.repository.CandidateSkillsEmbeddingRepository;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Candidate 도메인 데이터 처리기 (v2)
 * <p>
 * Python 서버로부터 전송된 Candidate 데이터를 JSON에서 파싱하여 4개 테이블에 저장:
 * 1. candidate - 기본 정보
 * 2. candidate_skill - 기술 스택 (1:N)
 * 3. candidate_description - 이력서 원문
 * 4. candidate_skills_embedding - 벡터 정보
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateDataProcessor implements DataProcessor {

    private final ObjectMapper objectMapper;
    private final CandidateRepository candidateRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateDescriptionRepository candidateDescriptionRepository;
    private final CandidateSkillsEmbeddingRepository candidateSkillsEmbeddingRepository;
    private final BatchProperties batchProperties;

    @Override
    public int processChunk(byte[] jsonChunk) {
        try {
            // 1. bytes → UTF-8 문자열
            String jsonString = new String(jsonChunk, StandardCharsets.UTF_8);
            log.debug("Received JSON chunk: {} bytes", jsonChunk.length);

            // 2. JSON → DTO 리스트
            List<CandidateRowDto> dtos = objectMapper.readValue(
                    jsonString,
                    new TypeReference<List<CandidateRowDto>>() {
                    }
            );
            log.info("Parsed {} candidate rows from JSON", dtos.size());

            if (dtos.isEmpty()) {
                log.warn("Empty DTO list, skipping save");
                return 0;
            }

            // 3. DTO → Entity 변환 (4개 테이블)
            List<CandidateEntity> candidateEntities = new ArrayList<>();
            List<CandidateSkillEntity> allSkillEntities = new ArrayList<>();
            List<CandidateDescriptionEntity> descriptionEntities = new ArrayList<>();
            List<CandidateSkillsEmbeddingEntity> embeddingEntities = new ArrayList<>();

            for (CandidateRowDto dto : dtos) {
                UUID candidateId = UUID.fromString(dto.getCandidateId());

                candidateEntities.add(toCandidateEntity(dto, candidateId));
                allSkillEntities.addAll(toCandidateSkillEntities(dto, candidateId));
                descriptionEntities.add(toCandidateDescriptionEntity(dto, candidateId));
                embeddingEntities.add(toCandidateSkillsEmbeddingEntity(dto, candidateId));
            }

            // 4. DB 저장 (순서 중요: candidate → skill, description, embedding)
            candidateRepository.upsertAll(candidateEntities);
            log.info("Upserted {} candidate entities", candidateEntities.size());

            candidateSkillRepository.upsertAll(allSkillEntities);
            log.info("Upserted {} candidate skill entities", allSkillEntities.size());

            candidateDescriptionRepository.upsertAll(descriptionEntities);
            log.info("Upserted {} candidate description entities", descriptionEntities.size());

            candidateSkillsEmbeddingRepository.upsertAll(embeddingEntities);
            log.info("Upserted {} candidate skills embedding entities", embeddingEntities.size());

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
        return "candidate";
    }

    /**
     * DTO → CandidateEntity 변환
     */
    private CandidateEntity toCandidateEntity(CandidateRowDto dto, UUID candidateId) {
        CandidateEntity entity = new CandidateEntity();
        entity.setCandidateId(candidateId);
        entity.setPositionCategory(dto.getPositionCategory());
        entity.setExperienceYears(dto.getExperienceYears());
        entity.setOriginalResume(dto.getOriginalResume());

        return entity;
    }

    /**
     * DTO → List<CandidateSkillEntity> 변환 (1:N)
     */
    private List<CandidateSkillEntity> toCandidateSkillEntities(CandidateRowDto dto, UUID candidateId) {
        if (dto.getSkills() == null || dto.getSkills().isEmpty()) {
            return List.of();
        }

        return dto.getSkills().stream()
                .map(skill -> {
                    CandidateSkillEntity entity = new CandidateSkillEntity();
                    entity.setCandidateId(candidateId);
                    entity.setSkill(skill);
                    return entity;
                })
                .collect(Collectors.toList());
    }

    /**
     * DTO → CandidateDescriptionEntity 변환
     */
    private CandidateDescriptionEntity toCandidateDescriptionEntity(CandidateRowDto dto, UUID candidateId) {
        CandidateDescriptionEntity entity = new CandidateDescriptionEntity();
        entity.setCandidateId(candidateId);
        entity.setOriginalResume(dto.getOriginalResume());
        entity.setResumeLang(null);  // TODO: Proto v2에서 추가 예정
        return entity;
    }

    /**
     * DTO → CandidateSkillsEmbeddingEntity 변환
     */
    private CandidateSkillsEmbeddingEntity toCandidateSkillsEmbeddingEntity(CandidateRowDto dto, UUID candidateId) {

        // Vector 차원 검증
        int expectedDim = batchProperties.getDomainConfig("candidate").getVectorDimension();
        if (dto.getSkillsVector().size() != expectedDim) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch for UUID %s: expected=%d, actual=%d",
                            candidateId, expectedDim, dto.getSkillsVector().size())
            );
        }

        // List<Float> → float[]
        float[] vectorArray = new float[dto.getSkillsVector().size()];
        for (int i = 0; i < dto.getSkillsVector().size(); i++) {
            vectorArray[i] = dto.getSkillsVector().get(i);
        }

        // List<String> → String[]
        String[] skillsArray = dto.getSkills().toArray(new String[0]);

        return CandidateSkillsEmbeddingEntity.fromFloatArray(candidateId, skillsArray, vectorArray);
    }
}
