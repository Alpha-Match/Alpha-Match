package com.alpha.backend.application.batch.processor;

import com.alpha.backend.application.batch.dto.CandidateItem;
import com.alpha.backend.domain.candidate.entity.CandidateEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillsEmbeddingEntity;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.grpc.proto.CandidateRow;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Candidate ItemProcessor
 *
 * Candidate 도메인의 Proto 객체(CandidateRow) → CandidateItem 변환
 *
 * 변환 과정:
 * 1. CandidateRow (Flat DTO) → 3개 Entity 분산 생성
 * 2. CandidateEntity - 기본 정보
 * 3. List<CandidateSkillEntity> - skills 배열 분해 (1:N)
 * 4. CandidateSkillsEmbeddingEntity - skills + vector
 */
@Slf4j
@RequiredArgsConstructor
public class CandidateItemProcessor implements ItemProcessor<CandidateRow, CandidateItem> {

    private final BatchProperties batchProperties;

    @Override
    public CandidateItem process(CandidateRow protoRow) throws Exception {
        try {
            UUID candidateId = UUID.fromString(protoRow.getCandidateId());

            log.debug("[Candidate Processor] Processing candidate: {}", candidateId);

            // 1. CandidateEntity 생성
            CandidateEntity candidate = createCandidate(protoRow, candidateId);

            // 2. List<CandidateSkillEntity> 생성 (skills 배열 분해)
            List<CandidateSkillEntity> skills = createSkills(protoRow, candidateId);

            // 3. CandidateSkillsEmbeddingEntity 생성
            CandidateSkillsEmbeddingEntity embedding = createEmbedding(protoRow, candidateId);

            // 벡터 차원 검증
            validateVectorDimension(protoRow.getSkillsVectorList());

            log.debug("[Candidate Processor] Processed candidate: {} ({} skills, vector: {}d)",
                    candidateId, skills.size(), protoRow.getSkillsVectorList().size());

            return CandidateItem.builder()
                    .candidate(candidate)
                    .skills(skills)
                    .embedding(embedding)
                    .build();

        } catch (Exception e) {
            log.error("[Candidate Processor] Error processing candidate: {}", protoRow.getCandidateId(), e);
            throw e;
        }
    }

    /**
     * CandidateEntity 생성
     */
    private CandidateEntity createCandidate(CandidateRow protoRow, UUID candidateId) {
        CandidateEntity candidate = new CandidateEntity();
        candidate.setId(candidateId);
        candidate.setPositionCategory(protoRow.getPositionCategory());
        candidate.setExperienceYears(protoRow.getExperienceYears());
        candidate.setOriginalResume(protoRow.getOriginalResume());
        return candidate;
    }

    /**
     * List<CandidateSkillEntity> 생성 (skills 배열 분해)
     */
    private List<CandidateSkillEntity> createSkills(CandidateRow protoRow, UUID candidateId) {
        return protoRow.getSkillsList().stream()
                .map(skillName -> {
                    CandidateSkillEntity skill = new CandidateSkillEntity();
                    skill.setCandidateId(candidateId);
                    skill.setSkill(skillName);
                    return skill;
                })
                .collect(Collectors.toList());
    }

    /**
     * CandidateSkillsEmbeddingEntity 생성
     */
    private CandidateSkillsEmbeddingEntity createEmbedding(CandidateRow protoRow, UUID candidateId) {
        List<Float> vectorList = protoRow.getSkillsVectorList();
        float[] vectorArray = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            vectorArray[i] = vectorList.get(i);
        }

        CandidateSkillsEmbeddingEntity embedding = new CandidateSkillsEmbeddingEntity();
        embedding.setId(candidateId);
        embedding.setSkills(protoRow.getSkillsList().toArray(new String[0]));
        embedding.setVector(new PGvector(vectorArray));

        return embedding;
    }

    /**
     * 벡터 차원 검증
     */
    private void validateVectorDimension(List<Float> vector) {
        int expectedDimension = batchProperties.getDomainConfig("candidate").getVectorDimension();
        if (vector.size() != expectedDimension) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch for candidate: expected %d, got %d",
                            expectedDimension, vector.size())
            );
        }
    }
}
