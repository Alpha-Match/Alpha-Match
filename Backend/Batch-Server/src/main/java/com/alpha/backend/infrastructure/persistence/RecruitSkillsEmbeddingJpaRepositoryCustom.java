package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;

import java.util.List;

/**
 * Custom Repository interface for RecruitSkillsEmbedding batch operations
 */
public interface RecruitSkillsEmbeddingJpaRepositoryCustom {

    /**
     * Optimized batch upsert using JDBC Template
     *
     * @param entities List of RecruitSkillsEmbeddingEntity to upsert
     */
    void upsertAllOptimized(List<RecruitSkillsEmbeddingEntity> entities);
}
