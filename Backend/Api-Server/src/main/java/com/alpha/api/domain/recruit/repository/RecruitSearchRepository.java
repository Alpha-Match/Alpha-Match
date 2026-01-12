package com.alpha.api.domain.recruit.repository;

import com.alpha.api.application.dto.RecruitSearchResult;
import com.alpha.api.presentation.graphql.type.SearchStatisticsResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
     * - No experience filtering (experience parameter removed as per requirements)
     *
     * @param queryVector Query vector string (1536 dimensions, PostgreSQL vector format)
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param limit Maximum number of results
     * @return Flux of RecruitSearchResult
     */
    Flux<RecruitSearchResult> findSimilarByVectorWithScore(
            String queryVector,
            Double similarityThreshold,
            Integer limit
    );

    /**
     * Find search statistics by vector similarity (optimized single query)
     * - Uses CTE + GROUP BY for efficient aggregation
     * - Returns topSkills + totalCount in one query
     * - Replaces N+1 query pattern in getSearchStatistics
     *
     * @param queryVector Query vector string (1536 dimensions, PostgreSQL vector format)
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param limit Maximum number of top skills to return
     * @return Mono of SearchStatisticsResult containing topSkills and totalCount
     */
    Mono<SearchStatisticsResult> findSearchStatisticsByVector(
            String queryVector,
            Double similarityThreshold,
            Integer limit
    );
}
