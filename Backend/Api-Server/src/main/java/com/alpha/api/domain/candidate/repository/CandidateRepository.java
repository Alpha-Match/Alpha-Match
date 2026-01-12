package com.alpha.api.domain.candidate.repository;

import com.alpha.api.domain.candidate.entity.Candidate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Candidate Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - Read-only operations (Api-Server는 조회만)
 * - Corresponds to table_specification.md: candidate table
 */
public interface CandidateRepository extends ReactiveCrudRepository<Candidate, UUID> {

    /**
     * Find Candidates by similar skills (Vector Similarity Search)
     * - Uses pgvector cosine distance operator (<=>)
     * - Joins candidate_skills_embedding table
     * - Returns top N similar candidates
     *
     * @param queryVector Query vector (1536 dimensions)
     * @param similarityThreshold Minimum similarity score (0.7 default)
     * @param limit Maximum number of results
     * @return Flux of Candidates ordered by similarity (descending)
     */
    @Query("""
        SELECT c.candidate_id, c.position_category, c.experience_years, c.original_resume,
               c.created_at, c.updated_at,
               (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
        FROM candidate c
        INNER JOIN candidate_skills_embedding cse ON c.candidate_id = cse.candidate_id
        WHERE (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
        ORDER BY cse.skills_vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """)
    Flux<Candidate> findSimilarByVector(String queryVector, Double similarityThreshold, Integer limit);

    /**
     * Find Candidates by experience years range
     *
     * @param minYears Minimum experience years (inclusive)
     * @param maxYears Maximum experience years (inclusive)
     * @return Flux of Candidates
     */
    @Query("SELECT * FROM candidate WHERE experience_years BETWEEN :minYears AND :maxYears")
    Flux<Candidate> findByExperienceYearsBetween(Integer minYears, Integer maxYears);

    /**
     * Find Candidates by position category
     *
     * @param positionCategory Position category (e.g., "Backend", "Frontend")
     * @return Flux of Candidates
     */
    @Query("SELECT * FROM candidate WHERE position_category ILIKE CONCAT('%', :positionCategory, '%')")
    Flux<Candidate> findByPositionCategoryContaining(String positionCategory);

    /**
     * Count total candidates
     *
     * @return Mono of Long (total count)
     */
    Mono<Long> count();
}
