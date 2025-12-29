package com.alpha.api.graphql.resolver;

import com.alpha.api.domain.dashboard.service.DashboardService;
import com.alpha.api.domain.search.service.SearchService;
import com.alpha.api.graphql.type.DashboardCategoryData;
import com.alpha.api.graphql.type.SearchMatchesResult;
import com.alpha.api.graphql.type.SkillCategory;
import com.alpha.api.graphql.type.UserMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * GraphQL Query Resolver
 * - Handles all Query operations
 * - Frontend compatible queries: searchMatches, skillCategories, dashboardData
 * - Uses Spring for GraphQL annotations
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class QueryResolver {

    private final SearchService searchService;
    private final DashboardService dashboardService;

    /**
     * searchMatches Query (Frontend Compatible)
     * - Matches Frontend SEARCH_MATCHES_QUERY
     * - Parameters:
     *   - mode: CANDIDATE (searches Recruits) or RECRUITER (searches Candidates)
     *   - skills: List of skill names (e.g., ["Java", "Python"])
     *   - experience: Experience level string (e.g., "3-5 Years")
     * - Returns: SearchMatchesResult {matches[], vectorVisualization[]}
     *
     * @param mode UserMode enum
     * @param skills List of skill names
     * @param experience Experience level string
     * @return Mono<SearchMatchesResult>
     */
    @QueryMapping
    public Mono<SearchMatchesResult> searchMatches(
            @Argument UserMode mode,
            @Argument List<String> skills,
            @Argument String experience) {

        log.info("GraphQL Query: searchMatches - mode: {}, skills: {}, experience: {}", mode, skills, experience);

        return searchService.searchMatches(mode, skills, experience)
                .doOnSuccess(result -> log.info("searchMatches returned {} matches", result.getMatches().size()))
                .doOnError(error -> log.error("searchMatches error: {}", error.getMessage(), error));
    }

    /**
     * skillCategories Query (Frontend Compatible)
     * - Matches Frontend GET_SKILL_CATEGORIES
     * - Returns: Array of {category, skills[]}
     * - Used by Frontend AppInitializer to populate skill selector
     *
     * @return Mono<List<SkillCategory>>
     */
    @QueryMapping
    public Mono<List<SkillCategory>> skillCategories() {
        log.info("GraphQL Query: skillCategories");

        return searchService.getSkillCategories()
                .doOnSuccess(categories -> log.info("skillCategories returned {} categories", categories.size()))
                .doOnError(error -> log.error("skillCategories error: {}", error.getMessage(), error));
    }

    /**
     * dashboardData Query (Frontend Compatible)
     * - Matches Frontend GET_DASHBOARD_DATA
     * - Returns: Category-level skill statistics
     * - userMode: CANDIDATE shows recruit statistics, RECRUITER shows candidate statistics
     *
     * @param userMode User mode enum
     * @return Mono<List<DashboardCategoryData>>
     */
    @QueryMapping
    public Mono<List<DashboardCategoryData>> dashboardData(@Argument UserMode userMode) {
        log.info("GraphQL Query: dashboardData - userMode: {}", userMode);

        return dashboardService.getDashboardData(userMode)
                .doOnSuccess(data -> log.info("dashboardData returned {} categories", data.size()))
                .doOnError(error -> log.error("dashboardData error: {}", error.getMessage(), error));
    }
}
