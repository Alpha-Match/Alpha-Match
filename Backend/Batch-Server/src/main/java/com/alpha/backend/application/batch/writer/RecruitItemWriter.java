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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private final EntityManager entityManager;

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
            // 1. recruit 테이블 Upsert (먼저 저장, PK, 순차)
            log.debug("[Recruit Writer] Upserting {} recruits", recruits.size());
            recruitRepository.upsertAll(recruits);

            // 2. 나머지 3개 테이블 병렬 Upsert (Virtual Thread, FK → recruit)
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // 2-1. recruit_skill 테이블 병렬 Upsert
                Future<?> skillFuture = executor.submit(() -> {
                    if (!allSkills.isEmpty()) {
                        log.debug("[Recruit Writer] Upserting {} skills (parallel)", allSkills.size());
                        recruitSkillRepository.upsertAll(allSkills);
                    } else {
                        log.debug("[Recruit Writer] No skills to upsert");
                    }
                });

                // 2-2. recruit_description 테이블 병렬 Upsert
                Future<?> descFuture = executor.submit(() -> {
                    log.debug("[Recruit Writer] Upserting {} descriptions (parallel)", descriptions.size());
                    recruitDescriptionRepository.upsertAll(descriptions);
                });

                // 2-3. recruit_skills_embedding 테이블 병렬 Upsert
                Future<?> embeddingFuture = executor.submit(() -> {
                    log.debug("[Recruit Writer] Upserting {} embeddings (parallel)", embeddings.size());
                    recruitSkillsEmbeddingRepository.upsertAll(embeddings);
                });

                // 모든 병렬 작업 완료 대기
                skillFuture.get();
                descFuture.get();
                embeddingFuture.get();
            }

            // 3. EntityManager 플러시 및 클리어 (메모리 해제)
            entityManager.flush();
            entityManager.clear();

            log.info("[Recruit Writer] Successfully wrote {} recruits ({} skills, {} descriptions, {} embeddings), EntityManager cleared",
                    recruits.size(), allSkills.size(), descriptions.size(), embeddings.size());

        } catch (Exception e) {
            log.error("[Recruit Writer] Error writing recruits to database", e);
            // TODO: DLQ 처리 (실패한 데이터 저장)
            throw e;
        }
    }
}
