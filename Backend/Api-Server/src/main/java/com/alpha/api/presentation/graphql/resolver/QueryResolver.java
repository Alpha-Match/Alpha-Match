package com.alpha.api.presentation.graphql.resolver;

import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.repository.CandidateDescriptionRepository;
import com.alpha.api.domain.candidate.repository.CandidateRepository;
import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.application.service.CacheService;
import com.alpha.api.application.service.DashboardService;
import com.alpha.api.application.service.SearchService;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.repository.RecruitDescriptionRepository;
import com.alpha.api.domain.recruit.repository.RecruitRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.presentation.graphql.type.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

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
    private final RecruitRepository recruitRepository;
    private final RecruitDescriptionRepository recruitDescriptionRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateDescriptionRepository candidateDescriptionRepository;
    private final CandidateSkillRepository candidateSkillRepository;

    /**
     * searchMatches Query (Frontend Compatible)
     * - Matches Frontend SEARCH_MATCHES_QUERY
     * - Parameters:
     *   - mode: CANDIDATE (searches Recruits) or RECRUITER (searches Candidates)
     *   - skills: List of skill names (e.g., ["Java", "Python"])
     *   - experience: Experience level string (e.g., "3-5 Years")
     *   - limit: Max number of results (default: 10)
     *   - offset: Number of results to skip for pagination (default: 0)
     * - Returns: SearchMatchesResult {matches[], vectorVisualization[]}
     *
     * @param mode UserMode enum
     * @param skills List of skill names
     * @param experience Experience level string
     * @param limit Max number of results (nullable)
     * @param offset Number of results to skip (nullable)
     * @return Mono<SearchMatchesResult>
     */
    @QueryMapping
    public Mono<SearchMatchesResult> searchMatches(
            @Argument UserMode mode,
            @Argument List<String> skills,
            @Argument String experience,
            @Argument Integer limit,
            @Argument Integer offset) {

        log.info("GraphQL Query: searchMatches - mode: {}, skills: {}, experience: {}, limit: {}, offset: {}",
                mode, skills, experience, limit, offset);

        return searchService.searchMatches(mode, skills, experience, limit, offset)
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
     * - Returns full recruit information including description
     * - Joins recruit + recruit_description + recruit_skill tables
     *
     * @param id Recruit ID
     * @return Mono<RecruitDetail>
     */
    @QueryMapping
    public Mono<RecruitDetail> getRecruit(@Argument String id) {
        log.info("GraphQL Query: getRecruit - id: {}", id);

        UUID recruitId = UUID.fromString(id);

        // Fetch recruit, description, and skills in parallel
        Mono<Recruit> recruitMono = recruitRepository.findById(recruitId);
        Mono<String> descriptionMono = recruitDescriptionRepository.findByRecruitId(recruitId)
                .map(desc -> desc.getLongDescription())
                .defaultIfEmpty(""); // Default to empty string if description not found
        Mono<List<String>> skillsMono = recruitSkillRepository.findByRecruitId(recruitId)
                .map(skill -> skill.getSkill())
                .collectList();

        return Mono.zip(recruitMono, descriptionMono, skillsMono)
                .map(tuple -> {
                    Recruit recruit = tuple.getT1();
                    String description = tuple.getT2();
                    List<String> skills = tuple.getT3();

                    return RecruitDetail.builder()
                            .id(recruit.getRecruitId().toString())
                            .position(recruit.getPosition())
                            .companyName(recruit.getCompanyName())
                            .experienceYears(recruit.getExperienceYears())
                            .primaryKeyword(recruit.getPrimaryKeyword())
                            .englishLevel(recruit.getEnglishLevel())
                            .skills(skills)
                            .description(description)
                            .publishedAt(recruit.getPublishedAt() != null ? recruit.getPublishedAt().toString() : null)
                            .build();
                })
                .doOnSuccess(detail -> log.info("getRecruit returned detail for id: {}", id))
                .doOnError(error -> log.error("getRecruit error for id {}: {}", id, error.getMessage(), error));
    }

    /**
     * getCandidate Query (Detail View)
     * - Returns full candidate information including description
     * - Joins candidate + candidate_description + candidate_skill tables
     *
     * @param id Candidate ID
     * @return Mono<CandidateDetail>
     */
    @QueryMapping
    public Mono<CandidateDetail> getCandidate(@Argument String id) {
        log.info("GraphQL Query: getCandidate - id: {}", id);

        UUID candidateId = UUID.fromString(id);

        // Fetch candidate, description, and skills in parallel
        Mono<Candidate> candidateMono = candidateRepository.findById(candidateId);
        Mono<String> descriptionMono = candidateDescriptionRepository.findByCandidateId(candidateId)
                .map(desc -> desc.getOriginalResume())
                .defaultIfEmpty(""); // Default to empty string if description not found
        Mono<List<String>> skillsMono = candidateSkillRepository.findByCandidateId(candidateId)
                .map(skill -> skill.getSkill())
                .collectList();

        return Mono.zip(candidateMono, descriptionMono, skillsMono)
                .map(tuple -> {
                    Candidate candidate = tuple.getT1();
                    String description = tuple.getT2();
                    List<String> skills = tuple.getT3();

                    return CandidateDetail.builder()
                            .id(candidate.getCandidateId().toString())
                            .positionCategory(candidate.getPositionCategory())
                            .experienceYears(candidate.getExperienceYears())
                            .originalResume(candidate.getOriginalResume())
                            .skills(skills)
                            .description(description)
                            .build();
                })
                .doOnSuccess(detail -> log.info("getCandidate returned detail for id: {}", id))
                .doOnError(error -> log.error("getCandidate error for id {}: {}", id, error.getMessage(), error));
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
}
