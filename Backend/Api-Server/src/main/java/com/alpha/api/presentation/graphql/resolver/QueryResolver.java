package com.alpha.api.presentation.graphql.resolver;

import com.alpha.api.application.service.CacheService;
import com.alpha.api.application.service.DashboardService;
import com.alpha.api.application.service.SearchService;
import com.alpha.api.presentation.graphql.type.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * GraphQL Query Resolver (Presentation Layer)
 * - Input Adapter: Handles GraphQL requests from Frontend
 * - Delegates to Application Services (Use Cases)
 * - Returns GraphQL-specific types
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class QueryResolver {

    private final SearchService searchService;
    private final DashboardService dashboardService;
    private final CacheService cacheService;

    /**
     * searchMatches Query (Frontend Compatible)
     * - Matches Frontend SEARCH_MATCHES_QUERY
     * - Parameters:
     *   - mode: CANDIDATE (searches Recruits) or RECRUITER (searches Candidates)
     *   - skills: List of skill names (e.g., ["Java", "Python"])
     *   - experience: Experience level string (e.g., "3-5 Years")
     *   - limit: Max number of results (default: 10)
     *   - offset: Number of results to skip for pagination (default: 0)
     *   - sortBy: Sort order (e.g., "score DESC, publishedAt DESC") (nullable)
     * - Returns: SearchMatchesResult {matches[], vectorVisualization[]}
     *
     * @param mode UserMode enum
     * @param skills List of skill names
     * @param experience Experience level string
     * @param limit Max number of results (nullable)
     * @param offset Number of results to skip (nullable)
     * @param sortBy Sort order string (nullable)
     * @return Mono<SearchMatchesResult>
     */
    @QueryMapping
    public Mono<SearchMatchesResult> searchMatches(
            @Argument UserMode mode,
            @Argument List<String> skills,
            @Argument String experience,
            @Argument Integer limit,
            @Argument Integer offset,
            @Argument String sortBy) {

        log.info("GraphQL Query: searchMatches - mode: {}, skills: {}, experience: {}, limit: {}, offset: {}, sortBy: {}",
                mode, skills, experience, limit, offset, sortBy);

        return searchService.searchMatches(mode, skills, experience, limit, offset, sortBy)
                .doOnSuccess(result -> log.info("searchMatches returned {} matches", result.getMatches().size()))
                .doOnError(error -> log.error("searchMatches error: {}", error.getMessage(), error));
    }

    /**
     * skillCategories Query (Frontend Compatible)
     * - Matches Frontend GET_SKILL_CATEGORIES
     * - Returns: Array of {category, skills[]}
     * - Used by Frontend AppInitializer to populate skill selector
     * - Cache Warming: Preloaded on startup (24h TTL)
     *
     * @return Mono<List<SkillCategory>>
     */
    @QueryMapping
    public Mono<List<SkillCategory>> skillCategories() {
        log.info("GraphQL Query: skillCategories");

        String key = CacheService.skillCategoriesKey();
        return cacheService.getOrLoadStaticUnchecked(key, () -> searchService.getSkillCategories())
                .doOnSuccess(categories -> log.info("skillCategories returned {} categories", categories.size()))
                .doOnError(error -> log.error("skillCategories error: {}", error.getMessage(), error));
    }

    /**
     * dashboardData Query (Frontend Compatible)
     * - Matches Frontend GET_DASHBOARD_DATA
     * - Returns: Category-level skill statistics
     * - userMode: CANDIDATE shows recruit statistics, RECRUITER shows candidate statistics
     * - Cache Warming: Preloaded on startup for both modes (24h TTL)
     *
     * @param userMode User mode enum
     * @return Mono<List<DashboardCategoryData>>
     */
    @QueryMapping
    public Mono<List<DashboardCategoryData>> dashboardData(@Argument UserMode userMode) {
        log.info("GraphQL Query: dashboardData - userMode: {}", userMode);

        String key = CacheService.dashboardKey(userMode.name());
        return cacheService.getOrLoadStaticUnchecked(key, () -> dashboardService.getDashboardData(userMode))
                .doOnSuccess(data -> log.info("dashboardData returned {} categories", data.size()))
                .doOnError(error -> log.error("dashboardData error: {}", error.getMessage(), error));
    }

    /**
     * getRecruit Query (Detail View)
     * - Delegates to SearchService for Clean Architecture
     * - Returns full recruit information including description
     *
     * @param id Recruit ID
     * @return Mono<RecruitDetail>
     */
    @QueryMapping
    public Mono<RecruitDetail> getRecruit(@Argument String id) {
        log.info("GraphQL Query: getRecruit - id: {}", id);

        return searchService.getRecruitDetail(id);
    }

    /**
     * getCandidate Query (Detail View)
     * - Delegates to SearchService for Clean Architecture
     * - Returns full candidate information including resume
     *
     * @param id Candidate ID
     * @return Mono<CandidateDetail>
     */
    @QueryMapping
    public Mono<CandidateDetail> getCandidate(@Argument String id) {
        log.info("GraphQL Query: getCandidate - id: {}", id);

        return searchService.getCandidateDetail(id);
    }

    /**
     * getCategoryDistribution Query (Pie Chart Data)
     * - Returns category distribution for selected skills
     * - Example: [Java, Spring Boot, MySQL] → Backend 66%, Database 33%
     * - Used by Frontend SearchResultPanel for pie chart visualization
     *
     * @param skills List of skill names
     * @return Mono<List<CategoryMatchDistribution>>
     */
    @QueryMapping
    public Mono<List<CategoryMatchDistribution>> getCategoryDistribution(@Argument List<String> skills) {
        log.info("GraphQL Query: getCategoryDistribution - skills: {}", skills);

        return searchService.getCategoryDistribution(skills)
                .doOnSuccess(distributions -> log.info("getCategoryDistribution returned {} categories", distributions.size()))
                .doOnError(error -> log.error("getCategoryDistribution error: {}", error.getMessage(), error));
    }

    /**
     * getSkillCompetencyMatch Query (Detail Page - Skill Gap Analysis)
     * - Compares searched skills vs target (recruit/candidate) skills
     * - Returns: matched, missing, extra skills + matching percentage
     * - Used by Frontend MatchDetailPanel for skill gap visualization
     *
     * @param mode UserMode (CANDIDATE → analyze recruit, RECRUITER → analyze candidate)
     * @param targetId Recruit ID or Candidate ID
     * @param searchedSkills User's selected skills from search
     * @return Mono<SkillCompetencyMatch>
     */
    @QueryMapping
    public Mono<SkillCompetencyMatch> getSkillCompetencyMatch(
            @Argument UserMode mode,
            @Argument String targetId,
            @Argument List<String> searchedSkills) {

        log.info("GraphQL Query: getSkillCompetencyMatch - mode: {}, targetId: {}, searchedSkills: {}",
                mode, targetId, searchedSkills);

        return searchService.getSkillCompetencyMatch(mode, targetId, searchedSkills)
                .doOnSuccess(match -> log.info("getSkillCompetencyMatch returned - matchingPercentage: {}%, competencyLevel: {}",
                        match.getMatchingPercentage(), match.getCompetencyLevel()))
                .doOnError(error -> log.error("getSkillCompetencyMatch error: {}", error.getMessage(), error));
    }

    /**
     * topCompanies Query (Dashboard - Top 10 Companies)
     * - Returns top N companies with most job postings
     * - Maps to dashboard_request.txt #1: "Company_name 기준 공고 많은 기업 Top 10"
     * - Used by Frontend Dashboard for company statistics
     *
     * @param limit Maximum number of companies to return (default: 10)
     * @return Mono<List<CompanyJobCount>>
     */
    @QueryMapping
    public Mono<List<CompanyJobCount>> topCompanies(@Argument Integer limit) {
        log.info("GraphQL Query: topCompanies - limit: {}", limit);

        return dashboardService.getTopCompanies(limit)
                .doOnSuccess(companies -> log.info("topCompanies returned {} companies", companies.size()))
                .doOnError(error -> log.error("topCompanies error: {}", error.getMessage(), error));
    }

    /**
     * searchStatistics Query (Replaces topSkillsInSearch)
     * - Returns comprehensive search statistics including top skills and total result count
     * - Provides fixed totalCount for consistent UX during infinite scroll
     * - Maps to api_requirements.md: GET_SEARCH_STATISTICS
     *
     * @param mode UserMode (CANDIDATE or RECRUITER)
     * @param skills List of skill names for vector search
     * @param limit Maximum number of skills to return in topSkills (default: 15)
     * @return Mono<SearchStatisticsResult>
     */
    @QueryMapping
    public Mono<SearchStatisticsResult> searchStatistics(
            @Argument UserMode mode,
            @Argument List<String> skills,
            @Argument Integer limit) {

        log.info("GraphQL Query: searchStatistics - mode: {}, skills: {}, limit: {}", mode, skills, limit);

        return searchService.getSearchStatistics(mode, skills, limit)
                .doOnSuccess(result -> log.info("searchStatistics returned {} top skills, totalCount: {}",
                        result.getTopSkills().size(), result.getTotalCount()))
                .doOnError(error -> log.error("searchStatistics error: {}", error.getMessage(), error));
    }
}
