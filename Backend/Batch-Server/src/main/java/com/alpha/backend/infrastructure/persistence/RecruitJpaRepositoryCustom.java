package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitEntity;

import java.util.List;

/**
 * Custom Repository interface for batch operations
 *
 * Spring Data JPA will automatically detect implementations
 * of this interface in RecruitJpaRepositoryImpl
 */
public interface RecruitJpaRepositoryCustom {

    /**
     * Optimized batch upsert using JDBC Template
     *
     * @param entities List of RecruitEntity to upsert
     */
    void upsertAllOptimized(List<RecruitEntity> entities);
}
