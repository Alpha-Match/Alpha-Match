package com.alpha.api.domain.candidate.repository;

import com.alpha.api.domain.candidate.entity.CandidateDescription;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * CandidateDescription Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - 1:1 relationship with Candidate
 * - Stores full resume (original_resume)
 */
public interface CandidateDescriptionRepository extends ReactiveCrudRepository<CandidateDescription, UUID> {

    /**
     * Find CandidateDescription by candidateId
     *
     * @param candidateId Candidate ID
     * @return Mono of CandidateDescription
     */
    Mono<CandidateDescription> findByCandidateId(UUID candidateId);
}
