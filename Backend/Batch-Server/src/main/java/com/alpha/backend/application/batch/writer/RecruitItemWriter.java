package com.alpha.backend.application.batch.writer;

import com.alpha.backend.application.batch.dto.RecruitItem;
import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;
import com.alpha.backend.domain.recruit.entity.RecruitEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;
import com.alpha.backend.infrastructure.persistence.RecruitDescriptionJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitSkillJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitSkillsEmbeddingJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Recruit ItemWriter (v2)
 *
 * RecruitItem을 4개 테이블에 분산 저장:
 * 1. recruit 테이블 (RecruitEntity)
 * 2. recruit_skill 테이블 (List<RecruitSkillEntity>, 1:N)
 * 3. recruit_description 테이블 (RecruitDescriptionEntity)
 * 4. recruit_skills_embedding 테이블 (RecruitSkillsEmbeddingEntity)
 *
 * 저장 순서 (FK 제약 조건):
 * recruit → recruit_skill, recruit_description, recruit_skills_embedding
 */
@Slf4j
@RequiredArgsConstructor
public class RecruitItemWriter implements ItemWriter<RecruitItem> {

    private final RecruitJpaRepository recruitRepository;
    private final RecruitSkillJpaRepository recruitSkillRepository;
    private final RecruitDescriptionJpaRepository recruitDescriptionRepository;
    private final RecruitSkillsEmbeddingJpaRepository recruitSkillsEmbeddingRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends RecruitItem> chunk) throws Exception {
        List<? extends RecruitItem> items = chunk.getItems();

        if (items.isEmpty()) {
            log.debug("[Recruit Writer] Empty chunk, skipping");
            return;
        }

        log.info("[Recruit Writer] Writing {} recruits to 4 tables", items.size());

        // 각 테이블별로 데이터 수집
        List<RecruitEntity> recruits = new ArrayList<>();
        List<RecruitSkillEntity> allSkills = new ArrayList<>();
        List<RecruitDescriptionEntity> descriptions = new ArrayList<>();
        List<RecruitSkillsEmbeddingEntity> embeddings = new ArrayList<>();

        for (RecruitItem item : items) {
            recruits.add(item.getRecruit());
            allSkills.addAll(item.getSkills());  // 1:N 관계
            descriptions.add(item.getDescription());
            embeddings.add(item.getEmbedding());
        }

        try {
            // 1. recruit 테이블 Upsert (먼저 저장, PK)
            log.debug("[Recruit Writer] Upserting {} recruits", recruits.size());
            recruitRepository.upsertAll(recruits);

            // 2. recruit_skill 테이블 Upsert (FK → recruit)
            if (!allSkills.isEmpty()) {
                log.debug("[Recruit Writer] Upserting {} skills", allSkills.size());
                recruitSkillRepository.upsertAll(allSkills);
            } else {
                log.debug("[Recruit Writer] No skills to upsert (Proto v1 lacks skills field)");
            }

            // 3. recruit_description 테이블 Upsert (FK → recruit)
            log.debug("[Recruit Writer] Upserting {} descriptions", descriptions.size());
            recruitDescriptionRepository.upsertAll(descriptions);

            // 4. recruit_skills_embedding 테이블 Upsert (FK → recruit)
            log.debug("[Recruit Writer] Upserting {} embeddings", embeddings.size());
            recruitSkillsEmbeddingRepository.upsertAll(embeddings);

            log.info("[Recruit Writer] Successfully wrote {} recruits ({} skills, {} descriptions, {} embeddings)",
                    recruits.size(), allSkills.size(), descriptions.size(), embeddings.size());

        } catch (Exception e) {
            log.error("[Recruit Writer] Error writing recruits to database", e);
            // TODO: DLQ 처리 (실패한 데이터 저장)
            throw e;
        }
    }
}
