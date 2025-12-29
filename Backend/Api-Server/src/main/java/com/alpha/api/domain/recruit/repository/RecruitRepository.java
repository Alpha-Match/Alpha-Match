package com.alpha.api.domain.recruit.repository;

import com.alpha.api.domain.recruit.entity.Recruit;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Recruit Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - Read-only operations (Api-Server는 조회만)
 * - Corresponds to table_specification.md: recruit table
 */
public interface RecruitRepository extends ReactiveCrudRepository<Recruit, UUID> {

    /**
     * Find Recruits by similar skills (Vector Similarity Search)
     * - Uses pgvector cosine distance operator (<=>)
     * - Joins recruit_skills_embedding table
     * - Returns top N similar recruits
     *
     * @param queryVector Query vector (384 dimensions)
     * @param similarityThreshold Minimum similarity score (0.7 default)
     * @param limit Maximum number of results
     * @return Flux of Recruits ordered by similarity (descending)
     */
    @Query("""
        SELECT r.recruit_id, r.position, r.company_name, r.experience_years,
               r.primary_keyword, r.english_level, r.published_at, r.created_at, r.updated_at,
               (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
        FROM recruit r
        INNER JOIN recruit_skills_embedding rse ON r.recruit_id = rse.recruit_id
        WHERE (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
        ORDER BY rse.skills_vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """)
    Flux<Recruit> findSimilarByVector(String queryVector, Double similarityThreshold, Integer limit);

    /**
     * Find Recruits by experience years range
     *
     * @param minYears Minimum experience years (inclusive)
     * @param maxYears Maximum experience years (inclusive)
     * @return Flux of Recruits
     */
    @Query("SELECT * FROM recruit WHERE experience_years BETWEEN :minYears AND :maxYears")
    Flux<Recruit> findByExperienceYearsBetween(Integer minYears, Integer maxYears);

    /**
     * Find Recruits by company name (for search/filter)
     *
     * @param companyName Company name (partial match)
     * @return Flux of Recruits
     */
    @Query("SELECT * FROM recruit WHERE company_name ILIKE CONCAT('%', :companyName, '%')")
    Flux<Recruit> findByCompanyNameContaining(String companyName);

    /**
     * Count total recruits
     *
     * @return Mono of Long (total count)
     */
    Mono<Long> count();
}
