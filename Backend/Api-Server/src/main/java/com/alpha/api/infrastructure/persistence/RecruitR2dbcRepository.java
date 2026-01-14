package com.alpha.api.infrastructure.persistence;

import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.repository.RecruitRepository;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Recruit R2DBC Repository (Adapter - Infrastructure Layer)
 * - Implements RecruitRepository (Port) using Spring Data R2DBC
 * - Extends ReactiveCrudRepository for CRUD operations
 * - Custom queries for business-specific operations
 */
@Repository
public interface RecruitR2dbcRepository extends ReactiveCrudRepository<Recruit, UUID>, RecruitRepository {

    /**
     * Find Recruits by experience years range
     * - Override for custom query
     */
    @Override
    @Query("SELECT * FROM recruit WHERE experience_years BETWEEN :minYears AND :maxYears")
    Flux<Recruit> findByExperienceYearsBetween(Integer minYears, Integer maxYears);

    /**
     * Find Recruits by company name (for search/filter)
     * - Override for custom query
     */
    @Override
    @Query("SELECT * FROM recruit WHERE company_name ILIKE CONCAT('%', :companyName, '%')")
    Flux<Recruit> findByCompanyNameContaining(String companyName);
}
