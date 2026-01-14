package com.alpha.api.domain.recruit.repository;

import com.alpha.api.domain.recruit.entity.Recruit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Recruit Repository (Port Interface - Domain Layer)
 * - Pure business interface without technology dependencies
 * - Implementation provided by Infrastructure Layer (R2DBC Adapter)
 * - Read-only operations (Api-Server는 조회만)
 * - Corresponds to table_specification.md: recruit table
 */
public interface RecruitRepository {

    /**
     * Find Recruit by ID
     *
     * @param id Recruit ID
     * @return Mono of Recruit
     */
    Mono<Recruit> findById(UUID id);

    /**
     * Find all Recruits
     *
     * @return Flux of all Recruits
     */
    Flux<Recruit> findAll();

    /**
     * Find Recruits by experience years range
     *
     * @param minYears Minimum experience years (inclusive)
     * @param maxYears Maximum experience years (inclusive)
     * @return Flux of Recruits
     */
    Flux<Recruit> findByExperienceYearsBetween(Integer minYears, Integer maxYears);

    /**
     * Find Recruits by company name (for search/filter)
     *
     * @param companyName Company name (partial match)
     * @return Flux of Recruits
     */
    Flux<Recruit> findByCompanyNameContaining(String companyName);

    /**
     * Count total recruits
     *
     * @return Mono of Long (total count)
     */
    Mono<Long> count();
}
