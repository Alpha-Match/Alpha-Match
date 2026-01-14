package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;

import java.util.List;

/**
 * Custom Repository interface for RecruitDescription batch operations
 */
public interface RecruitDescriptionJpaRepositoryCustom {

    /**
     * Optimized batch upsert using JDBC Template
     *
     * @param entities List of RecruitDescriptionEntity to upsert
     */
    void upsertAllOptimized(List<RecruitDescriptionEntity> entities);
}
