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
     * Get dashboard data by user mode
     * - Returns category-level skill statistics
     *
     * @param userMode User mode (CANDIDATE or RECRUITER)
     * @return Mono<List<DashboardCategoryData>>
     */
    public Mono<List<DashboardCategoryData>> getDashboardData(UserMode userMode) {
        log.info("getDashboardData called - userMode: {}", userMode);

        // Get all categories with their skills
        return skillCategoryDicRepository.findAllOrderByCategory()
                .flatMap(category -> {
                    // For each category, get skills and their counts
                    return skillEmbeddingDicRepository.findByCategoryId(category.getCategoryId())
                            .flatMap(skillEmbedding -> {
                                // Get count based on user mode
                                Mono<Long> countMono = (userMode == UserMode.CANDIDATE)
                                        ? recruitSkillRepository.countBySkill(skillEmbedding.getSkill())
                                        : candidateSkillRepository.countBySkill(skillEmbedding.getSkill());

                                return countMono.map(count -> DashboardSkillStat.builder()
                                        .skill(skillEmbedding.getSkill())
                                        .count(count.intValue())
                                        .build());
                            })
                            .collectList()
                            .map(skillStats -> DashboardCategoryData.builder()
                                    .category(category.getCategory())
                                    .skills(skillStats)
                                    .build());
                })
                .collectList();
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
