package com.alpha.backend.application.batch.writer;

import com.alpha.backend.application.batch.dto.CandidateItem;
import com.alpha.backend.domain.candidate.entity.CandidateDescriptionEntity;
import com.alpha.backend.domain.candidate.entity.CandidateEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillsEmbeddingEntity;
import com.alpha.backend.infrastructure.persistence.CandidateDescriptionJpaRepository;
import com.alpha.backend.infrastructure.persistence.CandidateJpaRepository;
import com.alpha.backend.infrastructure.persistence.CandidateSkillJpaRepository;
import com.alpha.backend.infrastructure.persistence.CandidateSkillsEmbeddingJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Candidate ItemWriter (v2)
 *
 * CandidateItem을 4개 테이블에 분산 저장:
 * 1. candidate 테이블 (CandidateEntity)
 * 2. candidate_skill 테이블 (List<CandidateSkillEntity>, 1:N)
 * 3. candidate_description 테이블 (CandidateDescriptionEntity)
 * 4. candidate_skills_embedding 테이블 (CandidateSkillsEmbeddingEntity)
 *
 * 저장 순서 (FK 제약 조건):
 * candidate → candidate_skill, candidate_description, candidate_skills_embedding
 */
@Slf4j
@RequiredArgsConstructor
public class CandidateItemWriter implements ItemWriter<CandidateItem> {

    private final CandidateJpaRepository candidateRepository;
    private final CandidateSkillJpaRepository candidateSkillRepository;
    private final CandidateDescriptionJpaRepository candidateDescriptionRepository;
    private final CandidateSkillsEmbeddingJpaRepository candidateSkillsEmbeddingRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends CandidateItem> chunk) throws Exception {
        List<? extends CandidateItem> items = chunk.getItems();

        if (items.isEmpty()) {
            log.debug("[Candidate Writer] Empty chunk, skipping");
            return;
        }

        log.info("[Candidate Writer] Writing {} candidates to 4 tables", items.size());

        // 각 테이블별로 데이터 수집
        List<CandidateEntity> candidates = new ArrayList<>();
        List<CandidateSkillEntity> allSkills = new ArrayList<>();
        List<CandidateDescriptionEntity> descriptions = new ArrayList<>();
        List<CandidateSkillsEmbeddingEntity> embeddings = new ArrayList<>();

        for (CandidateItem item : items) {
            candidates.add(item.getCandidate());
            allSkills.addAll(item.getSkills());
            descriptions.add(item.getDescription());
            embeddings.add(item.getEmbedding());
        }

        try {
            // 1. candidate 테이블 Upsert (먼저 저장, PK)
            log.debug("[Candidate Writer] Upserting {} candidates", candidates.size());
            candidateRepository.upsertAll(candidates);

            // 2. candidate_skill 테이블 Upsert (FK → candidate)
            log.debug("[Candidate Writer] Upserting {} skills", allSkills.size());
            candidateSkillRepository.upsertAll(allSkills);

            // 3. candidate_description 테이블 Upsert (FK → candidate)
            log.debug("[Candidate Writer] Upserting {} descriptions", descriptions.size());
            candidateDescriptionRepository.upsertAll(descriptions);

            // 4. candidate_skills_embedding 테이블 Upsert (FK → candidate)
            log.debug("[Candidate Writer] Upserting {} embeddings", embeddings.size());
            candidateSkillsEmbeddingRepository.upsertAll(embeddings);

            log.info("[Candidate Writer] Successfully wrote {} candidates ({} skills, {} descriptions, {} embeddings)",
                    candidates.size(), allSkills.size(), descriptions.size(), embeddings.size());

        } catch (Exception e) {
            log.error("[Candidate Writer] Error writing candidates to database", e);
            // TODO: DLQ 처리 (실패한 데이터 저장)
            throw e;
        }
    }
}
