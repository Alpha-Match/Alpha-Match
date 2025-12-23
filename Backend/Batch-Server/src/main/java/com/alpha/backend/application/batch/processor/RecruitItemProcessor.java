package com.alpha.backend.application.batch.processor;

import com.alpha.backend.application.batch.dto.RecruitItem;
import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;
import com.alpha.backend.domain.recruit.entity.RecruitEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.grpc.proto.RecruitRow;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Recruit ItemProcessor (v2)
 *
 * Recruit 도메인의 Proto 객체(RecruitRow) → RecruitItem 변환
 *
 * 변환 과정:
 * 1. RecruitRow (Flat DTO) → 4개 Entity 분산 생성
 * 2. RecruitEntity - 기본 정보 (position, company_name, experience_years, primary_keyword, english_level, published_at)
 * 3. List<RecruitSkillEntity> - 요구 스킬 목록 (1:N, skills 배열 분해)
 * 4. RecruitDescriptionEntity - 채용 공고 원문 (long_description, description_lang)
 * 5. RecruitSkillsEmbeddingEntity - 스킬 벡터 (skills[], skills_vector)
 */
@Slf4j
@RequiredArgsConstructor
public class RecruitItemProcessor implements ItemProcessor<RecruitRow, RecruitItem> {

    private final BatchProperties batchProperties;

    @Override
    public RecruitItem process(RecruitRow protoRow) throws Exception {
        try {
            UUID recruitId = UUID.fromString(protoRow.getId());

            log.debug("[Recruit Processor] Processing recruit: {}", recruitId);

            // 1. RecruitEntity 생성
            RecruitEntity recruit = createRecruit(protoRow, recruitId);

            // 2. List<RecruitSkillEntity> 생성 (skills 배열 분해)
            List<RecruitSkillEntity> skills = createSkills(protoRow, recruitId);

            // 3. RecruitDescriptionEntity 생성
            RecruitDescriptionEntity description = createDescription(protoRow, recruitId);

            // 4. RecruitSkillsEmbeddingEntity 생성
            RecruitSkillsEmbeddingEntity embedding = createEmbedding(protoRow, recruitId);

            // 벡터 차원 검증
            validateVectorDimension(protoRow.getSkillsVectorList());

            log.debug("[Recruit Processor] Processed recruit: {} ({} skills, vector: {}d)",
                    recruitId, skills.size(), protoRow.getSkillsVectorList().size());

            return RecruitItem.builder()
                    .recruit(recruit)
                    .skills(skills)
                    .description(description)
                    .embedding(embedding)
                    .build();

        } catch (Exception e) {
            log.error("[Recruit Processor] Error processing recruit: {}", protoRow.getId(), e);
            throw e;
        }
    }

    /**
     * RecruitEntity 생성
     */
    private RecruitEntity createRecruit(RecruitRow protoRow, UUID recruitId) {
        RecruitEntity recruit = new RecruitEntity();
        recruit.setRecruitId(recruitId);
        recruit.setPosition(protoRow.getPosition());
        recruit.setCompanyName(protoRow.getCompanyName());
        recruit.setExperienceYears(protoRow.getExperienceYears());
        recruit.setPrimaryKeyword(protoRow.getPrimaryKeyword());
        recruit.setEnglishLevel(protoRow.getEnglishLevel());

        // published_at 파싱 (ISO 8601 → OffsetDateTime)
        if (protoRow.getPublishedAt() != null && !protoRow.getPublishedAt().isEmpty()) {
            recruit.setPublishedAt(java.time.OffsetDateTime.parse(protoRow.getPublishedAt()));
        } else {
            recruit.setPublishedAt(null);
        }

        return recruit;
    }

    /**
     * RecruitDescriptionEntity 생성
     */
    private RecruitDescriptionEntity createDescription(RecruitRow protoRow, UUID recruitId) {
        RecruitDescriptionEntity description = new RecruitDescriptionEntity();
        description.setRecruitId(recruitId);
        description.setLongDescription(protoRow.getLongDescription());
        description.setDescriptionLang(protoRow.getDescriptionLang());
        return description;
    }

    /**
     * List<RecruitSkillEntity> 생성 (skills 배열 분해)
     */
    private List<RecruitSkillEntity> createSkills(RecruitRow protoRow, UUID recruitId) {
        return protoRow.getSkillsList().stream()
                .map(skillName -> {
                    RecruitSkillEntity skill = new RecruitSkillEntity();
                    skill.setRecruitId(recruitId);
                    skill.setSkill(skillName);
                    return skill;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * RecruitSkillsEmbeddingEntity 생성
     */
    private RecruitSkillsEmbeddingEntity createEmbedding(RecruitRow protoRow, UUID recruitId) {
        List<Float> vectorList = protoRow.getSkillsVectorList();
        float[] vectorArray = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            vectorArray[i] = vectorList.get(i);
        }

        RecruitSkillsEmbeddingEntity embedding = new RecruitSkillsEmbeddingEntity();
        embedding.setRecruitId(recruitId);
        embedding.setSkills(protoRow.getSkillsList().toArray(new String[0]));
        embedding.setSkillsVector(new PGvector(vectorArray));

        return embedding;
    }

    /**
     * 벡터 차원 검증
     */
    private void validateVectorDimension(List<Float> vector) {
        int expectedDimension = batchProperties.getDomainConfig("recruit").getVectorDimension();
        if (vector.size() != expectedDimension) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch for recruit: expected %d, got %d",
                            expectedDimension, vector.size())
            );
        }
    }
}
