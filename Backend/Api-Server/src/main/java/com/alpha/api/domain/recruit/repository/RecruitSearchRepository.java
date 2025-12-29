package com.alpha.api.domain.recruit.repository;

import com.alpha.api.application.dto.RecruitSearchResult;
import reactor.core.publisher.Flux;

/**
 * Recruit Search Repository (Port Interface - Domain Layer)
 * - Pure business interface for vector-based search operations
 * - Implementation provided by Infrastructure Layer (R2DBC + pgvector)
 * - Returns DTOs containing entity + similarity score
 * - Corresponds to table_specification.md: recruit + recruit_skills_embedding
 */
public interface RecruitSearchRepository {

    /**
     * Find Recruits by similar skills with similarity score
     * - Vector similarity search using pgvector
     * - Returns DTO containing Recruit entity + similarity score
     *
     * @param queryVector Query vector string (384 dimensions, PostgreSQL vector format)
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param limit Maximum number of results
     * @param minYears Minimum experience years (inclusive)
     * @param maxYears Maximum experience years (inclusive)
     * @return Flux of RecruitSearchResult
     */
    Flux<RecruitSearchResult> findSimilarByVectorWithScore(
            String queryVector,
            Double similarityThreshold,
            Integer limit,
            Integer minYears,
            Integer maxYears
    );
}
