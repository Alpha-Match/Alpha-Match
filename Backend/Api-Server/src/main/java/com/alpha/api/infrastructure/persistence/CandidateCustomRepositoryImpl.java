package com.alpha.api.infrastructure.persistence;

import com.alpha.api.application.dto.CandidateSearchResult;
import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.repository.CandidateSearchRepository;
import com.alpha.api.presentation.graphql.type.SearchStatisticsResult;
import com.alpha.api.presentation.graphql.type.SkillFrequency;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Candidate Search Repository Implementation (Adapter - Infrastructure Layer)
 * - Implements CandidateSearchRepository (Port) using R2DBC DatabaseClient
 * - Custom implementation for pgvector similarity search
 * - Maps similarity_score from query result to DTO
 */
@Repository
@RequiredArgsConstructor
public class CandidateCustomRepositoryImpl implements CandidateSearchRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<CandidateSearchResult> findSimilarByVectorWithScore(String queryVector, Double similarityThreshold, Integer limit) {
        String sql = """
            SELECT c.candidate_id, c.position_category, c.experience_years, c.original_resume,
                   c.created_at, c.updated_at,
                   (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
            FROM candidate c
            INNER JOIN candidate_skills_embedding cse ON c.candidate_id = cse.candidate_id
            WHERE (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
              AND cse.skills_vector IS NOT NULL
            ORDER BY cse.skills_vector <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """;

        return databaseClient.sql(sql)
                .bind("queryVector", queryVector)
                .bind("similarityThreshold", similarityThreshold)
                .bind("limit", limit)
                .map(row -> {
                    // Map Candidate entity
                    Candidate candidate = Candidate.builder()
                            .candidateId(row.get("candidate_id", UUID.class))
                            .positionCategory(row.get("position_category", String.class))
                            .experienceYears(row.get("experience_years", Integer.class))
                            .originalResume(row.get("original_resume", String.class))
                            .createdAt(row.get("created_at", OffsetDateTime.class))
                            .updatedAt(row.get("updated_at", OffsetDateTime.class))
                            .build();

                    // Map similarity score
                    Double similarityScore = row.get("similarity_score", Double.class);

                    // Build DTO
                    return CandidateSearchResult.builder()
                            .candidate(candidate)
                            .similarityScore(similarityScore)
                            .build();
                })
                .all();
    }

    /**
     * Find search statistics by vector similarity (optimized single query)
     * - Uses CTE + GROUP BY for efficient aggregation
     * - Returns topSkills + totalCount in one query (eliminates N+1 problem)
     * - Performance: 52,501 queries → 1 query
     *
     * @param queryVector Query vector string
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param limit Maximum number of top skills to return
     * @return Mono of SearchStatisticsResult
     */
    @Override
    public Mono<SearchStatisticsResult> findSearchStatisticsByVector(
            String queryVector,
            Double similarityThreshold,
            Integer limit
    ) {
        String sql = """
            WITH matched_candidates AS (
                SELECT c.candidate_id
                FROM candidate c
                INNER JOIN candidate_skills_embedding cse ON c.candidate_id = cse.candidate_id
                WHERE (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) >= :threshold
                  AND cse.skills_vector IS NOT NULL
            ),
            total AS (
                SELECT COUNT(DISTINCT candidate_id) AS total_count FROM matched_candidates
            ),
            skill_counts AS (
                SELECT cs.skill, COUNT(*) AS count
                FROM matched_candidates mc
                INNER JOIN candidate_skill cs ON mc.candidate_id = cs.candidate_id
                GROUP BY cs.skill
                ORDER BY count DESC
                LIMIT :limit
            ),
            total_skills AS (
                SELECT SUM(count) AS sum_count FROM skill_counts
            )
            SELECT
                sc.skill,
                sc.count,
                CASE WHEN ts.sum_count > 0 THEN (sc.count * 100.0 / ts.sum_count) ELSE 0.0 END AS percentage,
                t.total_count
            FROM skill_counts sc
            CROSS JOIN total t
            CROSS JOIN total_skills ts
            ORDER BY sc.count DESC
            """;

        return databaseClient.sql(sql)
                .bind("queryVector", queryVector)
                .bind("threshold", similarityThreshold)
                .bind("limit", limit)
                .map(row -> {
                    SkillFrequency skillFrequency = SkillFrequency.builder()
                            .skill(row.get("skill", String.class))
                            .count(row.get("count", Long.class).intValue())
                            .percentage(row.get("percentage", Double.class))
                            .build();
                    Integer totalCount = row.get("total_count", Long.class).intValue();
                    return new SkillFrequencyWithTotal(skillFrequency, totalCount);
                })
                .all()
                .collectList()
                .map(results -> {
                    if (results.isEmpty()) {
                        return SearchStatisticsResult.builder()
                                .topSkills(new ArrayList<>())
                                .totalCount(0)
                                .build();
                    }

                    List<SkillFrequency> topSkills = results.stream()
                            .map(SkillFrequencyWithTotal::skillFrequency)
                            .toList();
                    Integer totalCount = results.get(0).totalCount();

                    return SearchStatisticsResult.builder()
                            .topSkills(topSkills)
                            .totalCount(totalCount)
                            .build();
                });
    }

    /**
     * Helper record to hold SkillFrequency with totalCount from single query
     */
    private record SkillFrequencyWithTotal(SkillFrequency skillFrequency, Integer totalCount) {}

    /**
     * Find Candidates by similar skills with pagination (offset + limit)
     * - Used for pagination beyond cached results (offset >= 500)
     * - Results sorted by vector similarity at DB level
     * - No upper limit constraint
     *
     * @param queryVector Query vector string
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param offset Number of results to skip
     * @param limit Maximum number of results to return
     * @return Flux of CandidateSearchResult
     */
    @Override
    public Flux<CandidateSearchResult> findSimilarByVectorWithScoreAndOffset(
            String queryVector,
            Double similarityThreshold,
            Integer offset,
            Integer limit
    ) {
        String sql = """
            SELECT c.candidate_id, c.position_category, c.experience_years, c.original_resume,
                   c.created_at, c.updated_at,
                   (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
            FROM candidate c
            INNER JOIN candidate_skills_embedding cse ON c.candidate_id = cse.candidate_id
            WHERE (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
              AND cse.skills_vector IS NOT NULL
            ORDER BY cse.skills_vector <=> CAST(:queryVector AS vector)
            OFFSET :offset
            LIMIT :limit
            """;

        return databaseClient.sql(sql)
                .bind("queryVector", queryVector)
                .bind("similarityThreshold", similarityThreshold)
                .bind("offset", offset)
                .bind("limit", limit)
                .map(row -> {
                    Candidate candidate = Candidate.builder()
                            .candidateId(row.get("candidate_id", UUID.class))
                            .positionCategory(row.get("position_category", String.class))
                            .experienceYears(row.get("experience_years", Integer.class))
                            .originalResume(row.get("original_resume", String.class))
                            .createdAt(row.get("created_at", OffsetDateTime.class))
                            .updatedAt(row.get("updated_at", OffsetDateTime.class))
                            .build();

                    Double similarityScore = row.get("similarity_score", Double.class);

                    return CandidateSearchResult.builder()
                            .candidate(candidate)
                            .similarityScore(similarityScore)
                            .build();
                })
                .all();
    }
}
