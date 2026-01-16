package com.alpha.api.application.service;

import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.domain.skilldic.entity.SkillCategoryDic;
import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.alpha.api.presentation.graphql.type.CompanyJobCount;
import com.alpha.api.presentation.graphql.type.DashboardCategoryData;
import com.alpha.api.presentation.graphql.type.DashboardSkillStat;
import com.alpha.api.presentation.graphql.type.UserMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Dashboard Service
 * - Provides category-level skill statistics for dashboard visualization
 * - userMode: CANDIDATE shows recruit statistics, RECRUITER shows candidate statistics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SkillCategoryDicRepository skillCategoryDicRepository;
    private final SkillEmbeddingDicRepository skillEmbeddingDicRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final DatabaseClient databaseClient;

    /**
     * Get dashboard data by user mode (Optimized with CTE)
     * - Returns category-level skill statistics
     * - Uses single SQL query with CTE + JOIN for optimal performance
     * - Performance: N+1 queries (200+) → 1 query
     *
     * @param userMode User mode (CANDIDATE or RECRUITER)
     * @return Mono<List<DashboardCategoryData>>
     */
    public Mono<List<DashboardCategoryData>> getDashboardData(UserMode userMode) {
        log.info("getDashboardData called - userMode: {}", userMode);

        // Dynamic table name based on user mode
        // CANDIDATE mode → count from recruit_skill (job market demand)
        // RECRUITER mode → count from candidate_skill (talent pool)
        String skillTable = (userMode == UserMode.CANDIDATE) ? "recruit_skill" : "candidate_skill";

        String sql = String.format("""
            WITH skill_counts AS (
                SELECT
                    sed.skill,
                    sed.category_id,
                    COUNT(DISTINCT s.%s) AS count
                FROM skill_embedding_dic sed
                LEFT JOIN %s s ON LOWER(sed.skill) = LOWER(s.skill)
                GROUP BY sed.skill_id, sed.skill, sed.category_id
            )
            SELECT
                scd.category,
                sc.skill,
                COALESCE(sc.count, 0) AS count
            FROM skill_counts sc
            INNER JOIN skill_category_dic scd ON sc.category_id = scd.category_id
            ORDER BY scd.category, sc.count DESC
            """,
                (userMode == UserMode.CANDIDATE) ? "recruit_id" : "candidate_id",
                skillTable
        );

        return databaseClient.sql(sql)
                .map(row -> new SkillStatRow(
                        row.get("category", String.class),
                        row.get("skill", String.class),
                        row.get("count", Long.class).intValue()
                ))
                .all()
                .collectList()
                .map(this::groupByCategory)
                .doOnSuccess(data -> log.info("getDashboardData returned {} categories", data.size()))
                .doOnError(error -> log.error("getDashboardData error: {}", error.getMessage(), error));
    }

    /**
     * Helper record for row mapping
     */
    private record SkillStatRow(String category, String skill, int count) {}

    /**
     * Group flat skill stats by category
     */
    private List<DashboardCategoryData> groupByCategory(List<SkillStatRow> rows) {
        return rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        SkillStatRow::category,
                        java.util.LinkedHashMap::new, // Preserve order
                        java.util.stream.Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> DashboardCategoryData.builder()
                        .category(entry.getKey())
                        .skills(entry.getValue().stream()
                                .map(row -> DashboardSkillStat.builder()
                                        .skill(row.skill())
                                        .count(row.count())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    /**
     * Get top companies by job posting count
     * - Returns top N companies with most job postings
     * - Used for Dashboard "Company_name 기준 공고 많은 기업 Top 10" (dashboard_request.txt #1)
     *
     * @param limit Maximum number of companies to return (default: 10)
     * @return Mono<List<CompanyJobCount>>
     */
    public Mono<List<CompanyJobCount>> getTopCompanies(Integer limit) {
        int finalLimit = (limit != null && limit > 0) ? limit : 10;

        log.info("getTopCompanies called - limit: {}", finalLimit);

        String sql = """
            SELECT company_name, COUNT(*) as job_count
            FROM recruit
            WHERE company_name IS NOT NULL
            GROUP BY company_name
            ORDER BY job_count DESC
            LIMIT :limit
            """;

        return databaseClient.sql(sql)
                .bind("limit", finalLimit)
                .map(row -> CompanyJobCount.builder()
                        .companyName(row.get("company_name", String.class))
                        .jobCount(row.get("job_count", Integer.class))
                        .build())
                .all()
                .collectList()
                .doOnSuccess(companies -> log.info("getTopCompanies returned {} companies", companies.size()))
                .doOnError(error -> log.error("getTopCompanies error: {}", error.getMessage(), error));
    }
}
