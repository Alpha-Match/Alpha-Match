package com.alpha.api.domain.candidate.repository;

import com.alpha.api.domain.candidate.entity.CandidateSkill;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * CandidateSkill Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - 1:N relationship with Candidate
 * - Stores individual skills for each candidate
 */
public interface CandidateSkillRepository extends ReactiveCrudRepository<CandidateSkill, UUID> {

    /**
     * Find all skills for a specific candidate
     *
     * @param candidateId Candidate ID
     * @return Flux of CandidateSkill
     */
    @Query("SELECT * FROM candidate_skill WHERE candidate_id = :candidateId")
    Flux<CandidateSkill> findByCandidateId(UUID candidateId);

    /**
     * Find candidates that have a specific skill
     *
     * @param skill Skill name
     * @return Flux of candidate IDs
     */
    @Query("SELECT DISTINCT candidate_id FROM candidate_skill WHERE skill = :skill")
    Flux<UUID> findCandidateIdsBySkill(String skill);

    /**
     * Count candidates by skill (for dashboard statistics)
     *
     * @param skill Skill name
     * @return Mono<Long> count
     */
    @Query("SELECT COUNT(*) FROM candidate_skill WHERE skill = :skill")
    Mono<Long> countBySkill(String skill);
}
