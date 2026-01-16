package com.alpha.api.infrastructure.persistence;

import com.alpha.api.application.dto.RecruitSearchResult;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.repository.RecruitSearchRepository;
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
 * Recruit Search Repository Implementation (Adapter - Infrastructure Layer)
 * - Implements RecruitSearchRepository (Port) using R2DBC DatabaseClient
 * - Custom implementation for pgvector similarity search
 * - Maps similarity_score from query result to DTO
 */
@Repository
@RequiredArgsConstructor
public class RecruitCustomRepositoryImpl implements RecruitSearchRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<RecruitSearchResult> findSimilarByVectorWithScore(String queryVector, Double similarityThreshold, Integer limit) {
        String sql = """
            SELECT r.recruit_id, r.position, r.company_name, r.experience_years,
                   r.primary_keyword, r.english_level, r.published_at, r.created_at, r.updated_at,
                   (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
            FROM recruit r
            INNER JOIN recruit_skills_embedding rse ON r.recruit_id = rse.recruit_id
            WHERE (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
              AND rse.skills_vector IS NOT NULL
            ORDER BY rse.skills_vector <=> CAST(:queryVector AS vector)    
            LIMIT :limit
            """;
        return databaseClient.sql(sql)
                .bind("queryVector", queryVector)
                .bind("similarityThreshold", similarityThreshold)
                .bind("limit", limit)
                .map(row -> {
                    // Map Recruit entity
                    Recruit recruit = Recruit.builder()
                            .recruitId(row.get("recruit_id", UUID.class))
                            .position(row.get("position", String.class))
                            .companyName(row.get("company_name", String.class))
                            .experienceYears(row.get("experience_years", Integer.class))
                            .primaryKeyword(row.get("primary_keyword", String.class))
                            .englishLevel(row.get("english_level", String.class))
                            .publishedAt(row.get("published_at", OffsetDateTime.class))
                            .createdAt(row.get("created_at", OffsetDateTime.class))
                            .updatedAt(row.get("updated_at", OffsetDateTime.class))
                            .build();

                    // Map similarity score
                    Double similarityScore = row.get("similarity_score", Double.class);

                    // Build DTO
                    return RecruitSearchResult.builder()
                            .recruit(recruit)
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
            WITH matched_recruits AS (
                SELECT r.recruit_id
                FROM recruit r
                INNER JOIN recruit_skills_embedding rse ON r.recruit_id = rse.recruit_id
                WHERE (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) >= :threshold
                  AND rse.skills_vector IS NOT NULL
            ),
            total AS (
                SELECT COUNT(DISTINCT recruit_id) AS total_count FROM matched_recruits
            ),
            skill_counts AS (
                SELECT rs.skill, COUNT(*) AS count
                FROM matched_recruits mr
                INNER JOIN recruit_skill rs ON mr.recruit_id = rs.recruit_id
                GROUP BY rs.skill
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
     * Find Recruits by similar skills with pagination (offset + limit)
     * - Used for pagination beyond cached results (offset >= 500)
     * - Results sorted by vector similarity at DB level
     * - No upper limit constraint
     *
     * @param queryVector Query vector string
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param offset Number of results to skip
     * @param limit Maximum number of results to return
     * @return Flux of RecruitSearchResult
     */
    @Override
    public Flux<RecruitSearchResult> findSimilarByVectorWithScoreAndOffset(
            String queryVector,
            Double similarityThreshold,
            Integer offset,
            Integer limit
    ) {
        String sql = """
            SELECT r.recruit_id, r.position, r.company_name, r.experience_years,
                   r.primary_keyword, r.english_level, r.published_at, r.created_at, r.updated_at,
                   (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
            FROM recruit r
            INNER JOIN recruit_skills_embedding rse ON r.recruit_id = rse.recruit_id
            WHERE (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
              AND rse.skills_vector IS NOT NULL
            ORDER BY rse.skills_vector <=> CAST(:queryVector AS vector)
            OFFSET :offset
            LIMIT :limit
            """;

        return databaseClient.sql(sql)
                .bind("queryVector", queryVector)
                .bind("similarityThreshold", similarityThreshold)
                .bind("offset", offset)
                .bind("limit", limit)
                .map(row -> {
                    Recruit recruit = Recruit.builder()
                            .recruitId(row.get("recruit_id", UUID.class))
                            .position(row.get("position", String.class))
                            .companyName(row.get("company_name", String.class))
                            .experienceYears(row.get("experience_years", Integer.class))
                            .primaryKeyword(row.get("primary_keyword", String.class))
                            .englishLevel(row.get("english_level", String.class))
                            .publishedAt(row.get("published_at", OffsetDateTime.class))
                            .createdAt(row.get("created_at", OffsetDateTime.class))
                            .updatedAt(row.get("updated_at", OffsetDateTime.class))
                            .build();

                    Double similarityScore = row.get("similarity_score", Double.class);

                    return RecruitSearchResult.builder()
                            .recruit(recruit)
                            .similarityScore(similarityScore)
                            .build();
                })
                .all();
    }
}
