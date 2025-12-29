package com.alpha.api.infrastructure.graphql.resolver;

import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.repository.CandidateDescriptionRepository;
import com.alpha.api.domain.candidate.repository.CandidateRepository;
import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.application.service.DashboardService;
import com.alpha.api.application.service.SearchService;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.repository.RecruitDescriptionRepository;
import com.alpha.api.domain.recruit.repository.RecruitRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.infrastructure.graphql.type.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

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
}
