package com.alpha.api.domain.search.service;

import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.repository.CandidateRepository;
import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.repository.RecruitRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.domain.skilldic.entity.SkillCategoryDic;
import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.alpha.api.domain.skilldic.service.SkillNormalizationService;
import com.alpha.api.graphql.type.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Search Service
 * - Unified search service for searchMatches query
 * - Integrates SkillNormalizationService for vector search
 * - Converts entities to Frontend-compatible types
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SkillNormalizationService skillNormalizationService;
    private final RecruitRepository recruitRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillEmbeddingDicRepository skillEmbeddingDicRepository;
    private final SkillCategoryDicRepository skillCategoryDicRepository;

    /**
     * Search matches (Frontend searchMatches query)
     * - mode: CANDIDATE searches Recruits, RECRUITER searches Candidates
     * - experience: String format "0-2 Years", "3-5 Years", etc.
     * - Returns: matches + vectorVisualization
     *
     * @param mode UserMode (CANDIDATE or RECRUITER)
     * @param skills List of skill names
     * @param experience Experience level string
     * @return Mono<SearchMatchesResult>
     */
    public Mono<SearchMatchesResult> searchMatches(UserMode mode, List<String> skills, String experience) {
        log.info("searchMatches called - mode: {}, skills: {}, experience: {}", mode, skills, experience);

        // Normalize skills to query vector
        return skillNormalizationService.normalizeSkillsToQueryVector(skills)
                .flatMap(queryVector -> {
                    // Parse experience range
                    ExperienceRange expRange = parseExperienceString(experience);

                    // Search based on mode
                    if (mode == UserMode.CANDIDATE) {
                        // Job seeker searching for jobs
                        return searchRecruits(queryVector, expRange, skills);
                    } else {
                        // Recruiter searching for candidates
                        return searchCandidates(queryVector, expRange, skills);
                    }
                });
    }

    /**
     * Search Recruits (for CANDIDATE mode)
     *
     * @param queryVector Query vector
     * @param expRange Experience range
     * @param skills Original skill names
     * @return Mono<SearchMatchesResult>
     */
    private Mono<SearchMatchesResult> searchRecruits(String queryVector, ExperienceRange expRange, List<String> skills) {
        Double similarityThreshold = 0.3; // Lowered for testing (original: 0.7)
        Integer limit = 10; // default top 10

        return recruitRepository.findSimilarByVector(queryVector, similarityThreshold, limit)
                .flatMap(recruit -> {
                    // Fetch skills for each recruit
                    return recruitSkillRepository.findByRecruitId(recruit.getRecruitId())
                            .map(recruitSkill -> recruitSkill.getSkill())
                            .collectList()
                            .map(recruitSkills -> {
                                // Convert to MatchItem
                                return MatchItem.builder()
                                        .id(recruit.getRecruitId().toString())
                                        .title(recruit.getPosition())
                                        .company(recruit.getCompanyName())
                                        .score(0.85) // TODO: Extract from similarity_score
                                        .skills(recruitSkills)
                                        .experience(recruit.getExperienceYears())
                                        .description(null) // Will be populated from RecruitDescription if needed
                                        .build();
                            });
                })
                .collectList()
                .flatMap(matches -> {
                    // Generate vector visualization
                    return generateVectorVisualization(skills)
                            .map(vectorVisualization -> SearchMatchesResult.builder()
                                    .matches(matches)
                                    .vectorVisualization(vectorVisualization)
                                    .build());
                });
    }

    /**
     * Search Candidates (for RECRUITER mode)
     *
     * @param queryVector Query vector
     * @param expRange Experience range
     * @param skills Original skill names
     * @return Mono<SearchMatchesResult>
     */
    private Mono<SearchMatchesResult> searchCandidates(String queryVector, ExperienceRange expRange, List<String> skills) {
        Double similarityThreshold = 0.7;
        Integer limit = 10;

        return candidateRepository.findSimilarByVector(queryVector, similarityThreshold, limit)
                .flatMap(candidate -> {
                    // Fetch skills for each candidate
                    return candidateSkillRepository.findByCandidateId(candidate.getCandidateId())
                            .map(candidateSkill -> candidateSkill.getSkill())
                            .collectList()
                            .map(candidateSkills -> {
                                // Convert to MatchItem
                                return MatchItem.builder()
                                        .id(candidate.getCandidateId().toString())
                                        .title(candidate.getOriginalResume()) // TODO: Use name if available
                                        .company(candidate.getPositionCategory())
                                        .score(0.85) // TODO: Extract from similarity_score
                                        .skills(candidateSkills)
                                        .experience(candidate.getExperienceYears())
                                        .description(candidate.getOriginalResume())
                                        .build();
                            });
                })
                .collectList()
                .flatMap(matches -> {
                    // Generate vector visualization
                    return generateVectorVisualization(skills)
                            .map(vectorVisualization -> SearchMatchesResult.builder()
                                    .matches(matches)
                                    .vectorVisualization(vectorVisualization)
                                    .build());
                });
    }

    /**
     * Generate vector visualization data
     * - Creates dummy x, y coordinates for Frontend visualization
     * - TODO: Implement real dimensionality reduction (PCA/t-SNE)
     *
     * @param skills Skill names
     * @return Mono<List<SkillMatch>>
     */
    private Mono<List<SkillMatch>> generateVectorVisualization(List<String> skills) {
        // Dummy visualization data
        Random random = new Random(42); // Fixed seed for consistency
        List<SkillMatch> skillMatches = skills.stream()
                .map(skill -> SkillMatch.builder()
                        .skill(skill)
                        .isCore(true) // TODO: Determine based on vector distance
                        .x(random.nextDouble() * 100)
                        .y(random.nextDouble() * 100)
                        .build())
                .collect(Collectors.toList());

        return Mono.just(skillMatches);
    }

    /**
     * Get skill categories (Frontend GET_SKILL_CATEGORIES)
     * - Returns array of {category, skills[]}
     * - Used by Frontend AppInitializer
     *
     * @return Mono<List<SkillCategory>>
     */
    public Mono<List<SkillCategory>> getSkillCategories() {
        log.info("getSkillCategories called");

        return skillCategoryDicRepository.findAllOrderByCategory()
                .flatMap(category -> {
                    // For each category, fetch skills
                    return skillEmbeddingDicRepository.findByCategoryId(category.getCategoryId())
                            .map(SkillEmbeddingDic::getSkill)
                            .collectList()
                            .map(skills -> SkillCategory.builder()
                                    .category(category.getCategory())
                                    .skills(skills)
                                    .build());
                })
                .collectList();
    }

    /**
     * Parse experience string to range
     * - "0-2 Years" → ExperienceRange(0, 2)
     * - "3-5 Years" → ExperienceRange(3, 5)
     * - "6-9 Years" → ExperienceRange(6, 9)
     * - "10+ Years" → ExperienceRange(10, 999)
     *
     * @param experience Experience string
     * @return ExperienceRange
     */
    private ExperienceRange parseExperienceString(String experience) {
        if (experience == null || experience.isBlank()) {
            return new ExperienceRange(0, 999); // No filter
        }

        String cleaned = experience.replace(" Years", "").trim();

        if (cleaned.contains("+")) {
            // "10+" → 10 to 999
            int minYears = Integer.parseInt(cleaned.replace("+", ""));
            return new ExperienceRange(minYears, 999);
        } else if (cleaned.contains("-")) {
            // "0-2" → 0 to 2
            String[] parts = cleaned.split("-");
            int minYears = Integer.parseInt(parts[0].trim());
            int maxYears = Integer.parseInt(parts[1].trim());
            return new ExperienceRange(minYears, maxYears);
        } else {
            // Default
            return new ExperienceRange(0, 999);
        }
    }

    /**
     * Experience range helper class
     */
    private static class ExperienceRange {
        private final int minYears;
        private final int maxYears;

        public ExperienceRange(int minYears, int maxYears) {
            this.minYears = minYears;
            this.maxYears = maxYears;
        }

        public int getMinYears() {
            return minYears;
        }

        public int getMaxYears() {
            return maxYears;
        }
    }
}
