package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;

import java.util.List;

/**
 * Custom Repository interface for RecruitSkill batch operations
 */
public interface RecruitSkillJpaRepositoryCustom {

    /**
     * Optimized batch upsert using JDBC Template
     *
     * @param entities List of RecruitSkillEntity to upsert
     */
    void upsertAllOptimized(List<RecruitSkillEntity> entities);
}
