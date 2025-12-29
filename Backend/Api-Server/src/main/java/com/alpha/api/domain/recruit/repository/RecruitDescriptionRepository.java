package com.alpha.api.domain.recruit.repository;

import com.alpha.api.domain.recruit.entity.RecruitDescription;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * RecruitDescription Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - 1:1 relationship with Recruit
 * - Stores full job description (long_description)
 */
public interface RecruitDescriptionRepository extends ReactiveCrudRepository<RecruitDescription, UUID> {

    /**
     * Find RecruitDescription by recruitId
     *
     * @param recruitId Recruit ID
     * @return Mono of RecruitDescription
     */
    Mono<RecruitDescription> findByRecruitId(UUID recruitId);
}
