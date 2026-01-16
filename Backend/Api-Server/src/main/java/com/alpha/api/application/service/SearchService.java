package com.alpha.api.application.service;

import com.alpha.api.application.scoring.ScoringStrategyFactory;
import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.entity.CandidateDescription;
import com.alpha.api.domain.candidate.repository.CandidateDescriptionRepository;
import com.alpha.api.domain.candidate.repository.CandidateRepository;
import com.alpha.api.domain.candidate.repository.CandidateSearchRepository;
import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.repository.RecruitDescriptionRepository;
import com.alpha.api.domain.recruit.repository.RecruitRepository;
import com.alpha.api.domain.recruit.repository.RecruitSearchRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.domain.scoring.ScoringContext;
import com.alpha.api.domain.scoring.ScoringResult;
import com.alpha.api.domain.scoring.ScoringStrategy;
import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.alpha.api.domain.skilldic.service.SkillNormalizationService;
import com.alpha.api.presentation.graphql.type.*;
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
 * - Uses Port interfaces (Domain Layer) instead of Infrastructure implementations
 * - Multi-layer caching (L1: Caffeine, L2: Redis)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SkillNormalizationService skillNormalizationService;
    private final CacheService cacheService;
    private final ScoringStrategyFactory scoringStrategyFactory;
    private final RecruitRepository recruitRepository;
    private final RecruitDescriptionRepository recruitDescriptionRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final RecruitSearchRepository recruitSearchRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateDescriptionRepository candidateDescriptionRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateSearchRepository candidateSearchRepository;
    private final SkillEmbeddingDicRepository skillEmbeddingDicRepository;
    private final SkillCategoryDicRepository skillCategoryDicRepository;

    // Cache limit constant - results beyond this are fetched directly from DB
    private static final int CACHE_LIMIT = 500;

    /**
     * Search matches (Frontend searchMatches query)
     * - mode: CANDIDATE searches Recruits, RECRUITER searches Candidates
     * - experience: String format "0-2 Years", "3-5 Years", etc.
     * - limit: Max number of results (default: 10)
     * - offset: Number of results to skip for pagination (default: 0)
     * - sortBy: Sort order string (e.g., "score DESC, publishedAt DESC") (nullable)
     * - Returns: matches + vectorVisualization
     *
     * Hybrid Pagination Strategy:
     * - offset < 500: Use cached results (hybrid score sorted)
     * - offset >= 500: Fetch directly from DB (vector similarity sorted)
     *
     * @param mode UserMode (CANDIDATE or RECRUITER)
     * @param skills List of skill names
     * @param experience Experience level string
     * @param limit Max number of results (nullable)
     * @param offset Number of results to skip (nullable)
     * @param sortBy Sort order string (nullable)
     * @return Mono<SearchMatchesResult>
     */
    public Mono<SearchMatchesResult> searchMatches(UserMode mode, List<String> skills, String experience, Integer limit, Integer offset, String sortBy) {
        log.info("searchMatches called - mode: {}, skills: {}, experience: {}, limit: {}, offset: {}, sortBy: {}",
                mode, skills, experience, limit, offset, sortBy);

        // Default values
        int finalLimit = (limit != null && limit > 0) ? limit : 10;
        int finalOffset = (offset != null && offset >= 0) ? offset : 0;

        // Sort skills for consistent caching and processing
        List<String> sortedSkills = skills.stream()
                .sorted()
                .collect(Collectors.toList());
        log.debug("Sorted skills: {}", sortedSkills);

        // Determine pagination strategy based on offset
        if (finalOffset < CACHE_LIMIT) {
            // Use cache for first 500 results
            return searchMatchesFromCache(mode, sortedSkills, finalOffset, finalLimit, sortBy);
        } else {
            // Fetch directly from DB for results beyond cache limit
            log.info("Offset {} >= CACHE_LIMIT {}, fetching directly from DB", finalOffset, CACHE_LIMIT);
            return searchMatchesFromDb(mode, sortedSkills, finalOffset, finalLimit, sortBy);
        }
    }

    /**
     * Search matches from cache (offset < 500)
     * - Uses cached results sorted by hybrid score
     */
    private Mono<SearchMatchesResult> searchMatchesFromCache(
            UserMode mode,
            List<String> sortedSkills,
            int finalOffset,
            int finalLimit,
            String sortBy
    ) {
        // Cache key for full search results (hybrid score sorted)
        String cacheKey = CacheService.searchResultsKey(mode.name(), sortedSkills);

        // Get full results from cache or compute
        return cacheService.getOrLoadSearchResults(cacheKey, () ->
                        // Compute full search results if cache miss
                        skillNormalizationService.normalizeSkillsToQueryVector(sortedSkills)
                                .flatMap(queryVector -> {
                                    if (mode == UserMode.CANDIDATE) {
                                        return computeAllRecruits(queryVector, sortedSkills, sortBy);
                                    } else {
                                        return computeAllCandidates(queryVector, sortedSkills, sortBy);
                                    }
                                })
                )
                .flatMap(allMatches -> {
                    // Paginate from cached results
                    int fromIndex = Math.min(finalOffset, allMatches.size());
                    int toIndex = Math.min(finalOffset + finalLimit, allMatches.size());
                    List<MatchItem> paginatedMatches = allMatches.subList(fromIndex, toIndex);

                    log.debug("Cache pagination: total={}, offset={}, limit={}, returned={}",
                            allMatches.size(), finalOffset, finalLimit, paginatedMatches.size());

                    // Generate vector visualization
                    return generateVectorVisualization(sortedSkills)
                            .map(vectorVisualization -> SearchMatchesResult.builder()
                                    .matches(paginatedMatches)
                                    .vectorVisualization(vectorVisualization)
                                    .build());
                });
    }

    /**
     * Search matches directly from DB (offset >= 500)
     * - Bypasses cache, fetches directly with SQL OFFSET/LIMIT
     * - Results sorted by vector similarity at DB level
     * - Hybrid scores calculated on fetched results
     */
    private Mono<SearchMatchesResult> searchMatchesFromDb(
            UserMode mode,
            List<String> sortedSkills,
            int finalOffset,
            int finalLimit,
            String sortBy
    ) {
        Double similarityThreshold = 0.6;

        return skillNormalizationService.normalizeSkillsToQueryVector(sortedSkills)
                .flatMap(queryVector -> {
                    if (mode == UserMode.CANDIDATE) {
                        return fetchRecruitsFromDb(queryVector, sortedSkills, similarityThreshold, finalOffset, finalLimit, sortBy);
                    } else {
                        return fetchCandidatesFromDb(queryVector, sortedSkills, similarityThreshold, finalOffset, finalLimit, sortBy);
                    }
                })
                .flatMap(matches -> {
                    log.debug("DB direct pagination: offset={}, limit={}, returned={}",
                            finalOffset, finalLimit, matches.size());

                    // Generate vector visualization
                    return generateVectorVisualization(sortedSkills)
                            .map(vectorVisualization -> SearchMatchesResult.builder()
                                    .matches(matches)
                                    .vectorVisualization(vectorVisualization)
                                    .build());
                });
    }

    /**
     * Fetch recruits directly from DB with offset/limit
     */
    private Mono<List<MatchItem>> fetchRecruitsFromDb(
            String queryVector,
            List<String> skills,
            Double similarityThreshold,
            int offset,
            int limit,
            String sortBy
    ) {
        ScoringStrategy scoringStrategy = scoringStrategyFactory.getStrategy(UserMode.CANDIDATE);
        Set<String> searchSkillsSet = skills.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        return recruitSearchRepository.findSimilarByVectorWithScoreAndOffset(queryVector, similarityThreshold, offset, limit)
                .flatMap(recruitSearchResult -> {
                    var recruit = recruitSearchResult.getRecruit();
                    Double similarityScore = recruitSearchResult.getSimilarityScore();

                    return recruitSkillRepository.findByRecruitId(recruit.getRecruitId())
                            .map(recruitSkill -> recruitSkill.getSkill())
                            .collectList()
                            .map(recruitSkills -> {
                                Set<String> targetSkillsSet = recruitSkills.stream()
                                        .map(String::toLowerCase)
                                        .map(String::trim)
                                        .collect(Collectors.toSet());

                                ScoringContext context = ScoringContext.builder()
                                        .vectorSimilarity(similarityScore)
                                        .searchSkills(searchSkillsSet)
                                        .targetSkills(targetSkillsSet)
                                        .build();

                                ScoringResult scoringResult = scoringStrategy.calculate(context);

                                return MatchItem.builder()
                                        .id(recruit.getRecruitId().toString())
                                        .title(recruit.getPosition())
                                        .company(recruit.getCompanyName())
                                        .score(scoringResult.getHybridScore())
                                        .skills(recruitSkills)
                                        .experience(recruit.getExperienceYears())
                                        .timestamp(recruit.getPublishedAt() != null ? recruit.getPublishedAt().toString() : null)
                                        .vectorScore(scoringResult.getVectorScore())
                                        .overlapRatio(scoringResult.getOverlapRatio())
                                        .coverageRatio(scoringResult.getCoverageRatio())
                                        .extraRatio(scoringResult.getExtraRatio())
                                        .matchedSkills(new ArrayList<>(scoringResult.getMatchedSkills()))
                                        .extraSkills(new ArrayList<>(scoringResult.getExtraSkills()))
                                        .missingSkills(new ArrayList<>(scoringResult.getMissingSkills()))
                                        .build();
                            });
                })
                .collectList()
                .map(matches -> applySorting(matches, sortBy != null ? sortBy : "score DESC"));
    }

    /**
     * Fetch candidates directly from DB with offset/limit
     */
    private Mono<List<MatchItem>> fetchCandidatesFromDb(
            String queryVector,
            List<String> skills,
            Double similarityThreshold,
            int offset,
            int limit,
            String sortBy
    ) {
        ScoringStrategy scoringStrategy = scoringStrategyFactory.getStrategy(UserMode.RECRUITER);
        Set<String> searchSkillsSet = skills.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        return candidateSearchRepository.findSimilarByVectorWithScoreAndOffset(queryVector, similarityThreshold, offset, limit)
                .flatMap(candidateSearchResult -> {
                    var candidate = candidateSearchResult.getCandidate();
                    Double similarityScore = candidateSearchResult.getSimilarityScore();

                    return candidateSkillRepository.findByCandidateId(candidate.getCandidateId())
                            .map(candidateSkill -> candidateSkill.getSkill())
                            .collectList()
                            .map(candidateSkills -> {
                                Set<String> targetSkillsSet = candidateSkills.stream()
                                        .map(String::toLowerCase)
                                        .map(String::trim)
                                        .collect(Collectors.toSet());

                                ScoringContext context = ScoringContext.builder()
                                        .vectorSimilarity(similarityScore)
                                        .searchSkills(searchSkillsSet)
                                        .targetSkills(targetSkillsSet)
                                        .build();

                                ScoringResult scoringResult = scoringStrategy.calculate(context);

                                return MatchItem.builder()
                                        .id(candidate.getCandidateId().toString())
                                        .title(candidate.getOriginalResume())
                                        .company(candidate.getPositionCategory())
                                        .score(scoringResult.getHybridScore())
                                        .skills(candidateSkills)
                                        .experience(candidate.getExperienceYears())
                                        .timestamp(candidate.getCreatedAt() != null ? candidate.getCreatedAt().toString() : null)
                                        .vectorScore(scoringResult.getVectorScore())
                                        .overlapRatio(scoringResult.getOverlapRatio())
                                        .coverageRatio(scoringResult.getCoverageRatio())
                                        .extraRatio(scoringResult.getExtraRatio())
                                        .matchedSkills(new ArrayList<>(scoringResult.getMatchedSkills()))
                                        .extraSkills(new ArrayList<>(scoringResult.getExtraSkills()))
                                        .missingSkills(new ArrayList<>(scoringResult.getMissingSkills()))
                                        .build();
                            });
                })
                .collectList()
                .map(matches -> applySorting(matches, sortBy != null ? sortBy : "score DESC"));
    }

    /**
     * Compute all recruit matches (for caching)
     * - Fetches ALL results above threshold
     * - Calculates hybrid scores
     * - Sorts by hybrid score
     */
    private Mono<List<MatchItem>> computeAllRecruits(String queryVector, List<String> skills, String sortBy) {
        Double similarityThreshold = 0.6;
        int maxResults = 500; // Maximum results to cache

        ScoringStrategy scoringStrategy = scoringStrategyFactory.getStrategy(UserMode.CANDIDATE);
        Set<String> searchSkillsSet = skills.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        return recruitSearchRepository.findSimilarByVectorWithScore(queryVector, similarityThreshold, maxResults)
                .flatMap(recruitSearchResult -> {
                    Recruit recruit = recruitSearchResult.getRecruit();
                    Double similarityScore = recruitSearchResult.getSimilarityScore();

                    return recruitSkillRepository.findByRecruitId(recruit.getRecruitId())
                            .map(recruitSkill -> recruitSkill.getSkill())
                            .collectList()
                            .map(recruitSkills -> {
                                Set<String> targetSkillsSet = recruitSkills.stream()
                                        .map(String::toLowerCase)
                                        .map(String::trim)
                                        .collect(Collectors.toSet());

                                ScoringContext context = ScoringContext.builder()
                                        .vectorSimilarity(similarityScore)
                                        .searchSkills(searchSkillsSet)
                                        .targetSkills(targetSkillsSet)
                                        .build();

                                ScoringResult scoringResult = scoringStrategy.calculate(context);

                                return MatchItem.builder()
                                        .id(recruit.getRecruitId().toString())
                                        .title(recruit.getPosition())
                                        .company(recruit.getCompanyName())
                                        .score(scoringResult.getHybridScore())
                                        .skills(recruitSkills)
                                        .experience(recruit.getExperienceYears())
                                        .timestamp(recruit.getPublishedAt() != null ? recruit.getPublishedAt().toString() : null)
                                        .vectorScore(scoringResult.getVectorScore())
                                        .overlapRatio(scoringResult.getOverlapRatio())
                                        .coverageRatio(scoringResult.getCoverageRatio())
                                        .extraRatio(scoringResult.getExtraRatio())
                                        .matchedSkills(new ArrayList<>(scoringResult.getMatchedSkills()))
                                        .extraSkills(new ArrayList<>(scoringResult.getExtraSkills()))
                                        .missingSkills(new ArrayList<>(scoringResult.getMissingSkills()))
                                        .build();
                            });
                })
                .collectList()
                .map(matches -> applySorting(matches, sortBy != null ? sortBy : "score DESC"));
    }

    /**
     * Compute all candidate matches (for caching)
     * - Fetches ALL results above threshold
     * - Calculates hybrid scores
     * - Sorts by hybrid score
     */
    private Mono<List<MatchItem>> computeAllCandidates(String queryVector, List<String> skills, String sortBy) {
        Double similarityThreshold = 0.6;
        int maxResults = 500; // Maximum results to cache

        ScoringStrategy scoringStrategy = scoringStrategyFactory.getStrategy(UserMode.RECRUITER);
        Set<String> searchSkillsSet = skills.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        return candidateSearchRepository.findSimilarByVectorWithScore(queryVector, similarityThreshold, maxResults)
                .flatMap(candidateSearchResult -> {
                    Candidate candidate = candidateSearchResult.getCandidate();
                    Double similarityScore = candidateSearchResult.getSimilarityScore();

                    return candidateSkillRepository.findByCandidateId(candidate.getCandidateId())
                            .map(candidateSkill -> candidateSkill.getSkill())
                            .collectList()
                            .map(candidateSkills -> {
                                Set<String> targetSkillsSet = candidateSkills.stream()
                                        .map(String::toLowerCase)
                                        .map(String::trim)
                                        .collect(Collectors.toSet());

                                ScoringContext context = ScoringContext.builder()
                                        .vectorSimilarity(similarityScore)
                                        .searchSkills(searchSkillsSet)
                                        .targetSkills(targetSkillsSet)
                                        .build();

                                ScoringResult scoringResult = scoringStrategy.calculate(context);

                                return MatchItem.builder()
                                        .id(candidate.getCandidateId().toString())
                                        .title(candidate.getOriginalResume())
                                        .company(candidate.getPositionCategory())
                                        .score(scoringResult.getHybridScore())
                                        .skills(candidateSkills)
                                        .experience(candidate.getExperienceYears())
                                        .timestamp(candidate.getCreatedAt() != null ? candidate.getCreatedAt().toString() : null)
                                        .vectorScore(scoringResult.getVectorScore())
                                        .overlapRatio(scoringResult.getOverlapRatio())
                                        .coverageRatio(scoringResult.getCoverageRatio())
                                        .extraRatio(scoringResult.getExtraRatio())
                                        .matchedSkills(new ArrayList<>(scoringResult.getMatchedSkills()))
                                        .extraSkills(new ArrayList<>(scoringResult.getExtraSkills()))
                                        .missingSkills(new ArrayList<>(scoringResult.getMissingSkills()))
                                        .build();
                            });
                })
                .collectList()
                .map(matches -> applySorting(matches, sortBy != null ? sortBy : "score DESC"));
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
     * - Cached (L1: 10s, L2: 10m)
     *
     * @return Mono<List<SkillCategory>>
     */
    public Mono<List<SkillCategory>> getSkillCategories() {
        log.info("getSkillCategories called");

        String cacheKey = CacheService.skillCategoriesKey();

        // Use CacheService.getOrLoad for multi-layer caching
        // Note: We need to use a workaround since List<SkillCategory> cannot be cached directly
        // Alternative: Return from DB if cache miss
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
                .collectList()
                .cache(); // Simple in-memory cache for this Mono (shared across subscribers)
    }

    /**
     * Parse experience string to range
     *
     * [CURRENTLY DISABLED - Experience filtering removed]
     *
     * This method parses experience strings into min/max year ranges:
     * - "0-2 Years" → ExperienceRange(0, 2)
     * - "3-5 Years" → ExperienceRange(3, 5)
     * - "6-9 Years" → ExperienceRange(6, 9)
     * - "10+ Years" → ExperienceRange(10, 999)
     *
     * To re-enable experience filtering:
     * 1. Uncomment this method and ExperienceRange class below
     * 2. In searchMatches() method (line ~81), uncomment:
     *    ExperienceRange expRange = parseExperienceString(experience);
     * 3. Update searchRecruits() and searchCandidates() signatures to accept ExperienceRange
     * 4. Update repository interfaces to accept minYears and maxYears parameters
     * 5. Update SQL WHERE clauses in Repository implementations
     *
     * @param experience Experience string
     * @return ExperienceRange
     */
    /*
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
    */

    /**
     * Calculate Hybrid Score (Vector Similarity + Set-based Similarity)
     *
     * [CURRENTLY DISABLED - Using simple vector similarity instead]
     *
     * This method provides a more sophisticated scoring algorithm that combines:
     * - 40% Vector Similarity (cosine similarity from pgvector)
     * - 30% Overlap Ratio (matched skills / selected skills)
     * - 30% Coverage Ratio (matched skills / target skills)
     *
     * Benefits of Hybrid Scoring:
     * 1. Addresses single-skill match inflation
     *    - Example: Searching for "Java" alone might give 100% vector similarity
     *      even if the recruit/candidate has many other skills
     *    - Hybrid scoring balances vector similarity with set-based metrics
     * 2. Provides more balanced scoring across different skill set sizes
     * 3. Better reflects actual skill matching between user search and target profiles
     *
     * When to use Hybrid Scoring:
     * - When you want more accurate matching that considers skill overlap
     * - When vector-only similarity gives inflated scores for single-skill searches
     * - When you need to balance between semantic similarity and exact matches
     *
     * To activate hybrid scoring:
     * 1. Uncomment this entire method (remove the block comment markers)
     * 2. In searchRecruits() method (line ~124), change:
     *    double score = similarityScore;
     *    to:
     *    double score = calculateHybridScore(similarityScore, skills, recruitSkills);
     * 3. In searchCandidates() method (line ~186), make the same change with candidateSkills
     *
     * Performance considerations:
     * - Hybrid scoring adds minimal overhead (Set operations are O(n))
     * - Vector similarity is already computed in database (no extra cost)
     *
     * @param vectorSimilarity Cosine similarity from database (0.0 ~ 1.0)
     * @param selectedSkills User-selected skills from search
     * @param targetSkills Target entity's skills (recruit or candidate)
     * @return Hybrid score (0.0 ~ 1.0)
     */
    /*
    private double calculateHybridScore(double vectorSimilarity, List<String> selectedSkills, List<String> targetSkills) {
        // 1. Vector similarity (기존 코사인 유사도)
        double vectorSim = vectorSimilarity;

        // 2. Set-based similarity
        Set<String> selectedSet = new HashSet<>(selectedSkills);
        Set<String> targetSet = new HashSet<>(targetSkills);

        // Intersection (교집합)
        Set<String> intersection = new HashSet<>(selectedSet);
        intersection.retainAll(targetSet);

        // Overlap ratio: 선택한 스킬 중 매칭된 비율
        double overlapRatio = selectedSet.isEmpty() ? 0.0 : (double) intersection.size() / selectedSet.size();

        // Coverage ratio: 공고/후보자 스킬 중 커버된 비율
        double coverageRatio = targetSet.isEmpty() ? 0.0 : (double) intersection.size() / targetSet.size();

        // 3. Hybrid Score (가중 평균)
        // 40% vector similarity + 30% overlap ratio + 30% coverage ratio
        double finalScore = (0.4 * vectorSim) + (0.3 * overlapRatio) + (0.3 * coverageRatio);

        log.debug("Hybrid Score - vectorSim: {}, overlapRatio: {}, coverageRatio: {}, finalScore: {}",
                vectorSim, overlapRatio, coverageRatio, finalScore);

        return finalScore;
    }
    */

    /**
     * Get category distribution for selected skills (Pie Chart Data)
     * - Input: List of skill names
     * - Output: Category distribution with percentages
     * - Used by Frontend SearchResultPanel for pie chart visualization
     * - Example: [Java, Spring Boot, MySQL] → Backend 66%, Database 33%
     *
     * @param skills List of skill names
     * @return Mono<List<CategoryMatchDistribution>>
     */
    public Mono<List<CategoryMatchDistribution>> getCategoryDistribution(List<String> skills) {
        log.info("getCategoryDistribution called - skills: {}", skills);

        if (skills == null || skills.isEmpty()) {
            return Mono.just(List.of());
        }

        // Normalize skill names (case-insensitive)
        List<String> normalizedSkills = skills.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());

        // Fetch skill categories from DB
        return Flux.fromIterable(normalizedSkills)
                .flatMap(skill -> skillEmbeddingDicRepository.findBySkill(skill))
                .flatMap(skillEmbedding -> {
                    // Fetch category name for each skill
                    return skillCategoryDicRepository.findById(skillEmbedding.getCategoryId())
                            .map(categoryDic -> new SkillCategoryPair(
                                    categoryDic.getCategory(),
                                    skillEmbedding.getSkill()
                            ));
                })
                .collectList()
                .map(skillCategoryPairs -> {
                    // Group by category
                    Map<String, List<String>> categoryMap = skillCategoryPairs.stream()
                            .collect(Collectors.groupingBy(
                                    SkillCategoryPair::getCategory,
                                    Collectors.mapping(SkillCategoryPair::getSkill, Collectors.toList())
                            ));

                    // Calculate percentages
                    int totalSkills = skillCategoryPairs.size();
                    if (totalSkills == 0) {
                        return List.<CategoryMatchDistribution>of();
                    }

                    return categoryMap.entrySet().stream()
                            .map(entry -> {
                                String category = entry.getKey();
                                List<String> categorySkills = entry.getValue();
                                int count = categorySkills.size();
                                double percentage = (count * 100.0) / totalSkills;

                                return CategoryMatchDistribution.builder()
                                        .category(category)
                                        .percentage(percentage)
                                        .matchedSkills(categorySkills)
                                        .skillCount(count)
                                        .build();
                            })
                            .sorted((d1, d2) -> Double.compare(d2.getPercentage(), d1.getPercentage())) // Descending order
                            .collect(Collectors.toList());
                });
    }

    /**
     * Get skill competency match analysis (Detail Page)
     * - mode: CANDIDATE → analyze recruit, RECRUITER → analyze candidate
     * - targetId: Recruit ID or Candidate ID
     * - searchedSkills: User's selected skills from search
     * - Returns: Matching degree + missing/extra skills
     * - Used by Frontend MatchDetailPanel for skill gap visualization
     *
     * @param mode UserMode (CANDIDATE or RECRUITER)
     * @param targetId Recruit ID or Candidate ID
     * @param searchedSkills User's selected skills
     * @return Mono<SkillCompetencyMatch>
     */
    public Mono<SkillCompetencyMatch> getSkillCompetencyMatch(UserMode mode, String targetId, List<String> searchedSkills) {
        log.info("getSkillCompetencyMatch called - mode: {}, targetId: {}, searchedSkills: {}", mode, targetId, searchedSkills);

        if (searchedSkills == null || searchedSkills.isEmpty()) {
            return Mono.error(new IllegalArgumentException("searchedSkills cannot be empty"));
        }

        // Normalize searched skills (case-insensitive)
        Set<String> searchedSet = searchedSkills.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Fetch target skills based on mode
        Mono<List<String>> targetSkillsMono;
        if (mode == UserMode.CANDIDATE) {
            // Analyze Recruit
            targetSkillsMono = recruitSkillRepository.findByRecruitId(UUID.fromString(targetId))
                    .map(recruitSkill -> recruitSkill.getSkill().toLowerCase())
                    .collectList();
        } else {
            // Analyze Candidate
            targetSkillsMono = candidateSkillRepository.findByCandidateId(UUID.fromString(targetId))
                    .map(candidateSkill -> candidateSkill.getSkill().toLowerCase())
                    .collectList();
        }

        return targetSkillsMono.map(targetSkillsList -> {
            Set<String> targetSet = new HashSet<>(targetSkillsList);

            // Calculate intersections and differences
            Set<String> matched = new HashSet<>(searchedSet);
            matched.retainAll(targetSet); // Intersection

            Set<String> missing = new HashSet<>(targetSet);
            missing.removeAll(searchedSet); // Target only

            Set<String> extra = new HashSet<>(searchedSet);
            extra.removeAll(targetSet); // Searched only

            // Calculate matching percentage
            int totalTarget = targetSet.size();
            int matchedCount = matched.size();
            double matchingPercentage = totalTarget == 0 ? 0.0 : (matchedCount * 100.0) / totalTarget;

            // Determine competency level
            String competencyLevel;
            if (matchingPercentage >= 80.0) {
                competencyLevel = "High";
            } else if (matchingPercentage >= 50.0) {
                competencyLevel = "Medium";
            } else {
                competencyLevel = "Low";
            }

            return SkillCompetencyMatch.builder()
                    .matchedSkills(new ArrayList<>(matched))
                    .missingSkills(new ArrayList<>(missing))
                    .extraSkills(new ArrayList<>(extra))
                    .matchingPercentage(matchingPercentage)
                    .competencyLevel(competencyLevel)
                    .totalTargetSkills(totalTarget)
                    .totalSearchedSkills(searchedSet.size())
                    .build();
        });
    }

    /**
     * Helper class for grouping skills by category
     */
    private static class SkillCategoryPair {
        private final String category;
        private final String skill;

        public SkillCategoryPair(String category, String skill) {
            this.category = category;
            this.skill = skill;
        }

        public String getCategory() {
            return category;
        }

        public String getSkill() {
            return skill;
        }
    }

    /**
     * Apply sorting to match items based on sortBy string
     * - sortBy format: "field1 ORDER1, field2 ORDER2, ..."
     * - Example: "score DESC, publishedAt DESC" or "score DESC, createdAt DESC"
     * - Supported fields: score, timestamp
     * - Supported orders: ASC, DESC
     *
     * @param matches List of MatchItems
     * @param sortBy Sort order string
     * @return Sorted list of MatchItems
     */
    private List<MatchItem> applySorting(List<MatchItem> matches, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            // Default: sort by score descending
            return matches.stream()
                    .sorted((m1, m2) -> Double.compare(m2.getScore(), m1.getScore()))
                    .collect(Collectors.toList());
        }

        // Parse sortBy string: "score DESC, publishedAt DESC" → [(score, DESC), (publishedAt/createdAt, DESC)]
        String[] sortParts = sortBy.split(",");

        Comparator<MatchItem> comparator = null;

        for (String sortPart : sortParts) {
            String[] fieldAndOrder = sortPart.trim().split("\\s+");
            if (fieldAndOrder.length < 1) continue;

            String field = fieldAndOrder[0].toLowerCase();
            boolean ascending = fieldAndOrder.length > 1 && "ASC".equalsIgnoreCase(fieldAndOrder[1]);

            Comparator<MatchItem> currentComparator = null;

            if ("score".equals(field)) {
                currentComparator = ascending
                        ? Comparator.comparing(MatchItem::getScore, Comparator.nullsLast(Double::compareTo))
                        : Comparator.comparing(MatchItem::getScore, Comparator.nullsLast(Double::compareTo)).reversed();
            } else if ("publishedat".equals(field) || "createdat".equals(field) || "timestamp".equals(field)) {
                // Use timestamp field (which contains publishedAt for Recruit, createdAt for Candidate)
                currentComparator = ascending
                        ? Comparator.comparing(MatchItem::getTimestamp, Comparator.nullsLast(String::compareTo))
                        : Comparator.comparing(MatchItem::getTimestamp, Comparator.nullsLast(String::compareTo)).reversed();
            }

            if (currentComparator != null) {
                comparator = (comparator == null) ? currentComparator : comparator.thenComparing(currentComparator);
            }
        }

        if (comparator == null) {
            // Fallback to score DESC if parsing failed
            comparator = Comparator.comparing(MatchItem::getScore, Comparator.nullsLast(Double::compareTo)).reversed();
        }

        return matches.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Experience range helper class
     *
     * [CURRENTLY DISABLED - Experience filtering removed]
     *
     * This helper class stores min/max experience years for filtering search results.
     * See parseExperienceString() method above for usage instructions.
     */
    /*
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
    */
    /**
     * Get Search Statistics (Optimized with CTE + GROUP BY + Caching)
     * - Returns comprehensive search statistics including top skills and total result count
     * - Uses single SQL query with CTE for efficient aggregation (eliminates N+1 problem)
     * - Cached with L1 (30s) and L2 (10min) TTL
     * - Performance: 52,501 queries → 1 query, 30-60s → 200-500ms
     *
     * @param mode UserMode (CANDIDATE searches recruits, RECRUITER searches candidates)
     * @param skills List of skill names for vector search
     * @param limit Maximum number of skills to return in topSkills (default: 15)
     * @return Mono<SearchStatisticsResult>
     */
    public Mono<SearchStatisticsResult> getSearchStatistics(
            UserMode mode,
            List<String> skills,
            Integer limit
    ) {
        int finalLimit = (limit != null && limit > 0) ? limit : 15;

        log.info("getSearchStatistics called - mode: {}, skills: {}, limit: {}", mode, skills, finalLimit);

        // Sort skills for consistent caching and processing
        List<String> sortedSkills = skills.stream()
                .sorted()
                .collect(Collectors.toList());

        // Generate cache key
        String cacheKey = CacheService.searchStatisticsKey(mode.name(), sortedSkills, finalLimit);

        // Use caching with getOrLoad pattern
        return cacheService.getOrLoad(cacheKey, SearchStatisticsResult.class,
                () -> computeSearchStatistics(mode, sortedSkills, finalLimit)
        )
        .doOnSuccess(result -> log.info("getSearchStatistics returned {} top skills, totalCount: {} (cache key: {})",
                result.getTopSkills().size(), result.getTotalCount(), cacheKey))
        .doOnError(error -> log.error("getSearchStatistics error: {}", error.getMessage(), error));
    }

    /**
     * Compute Search Statistics (Internal - called on cache miss)
     * - Uses optimized single SQL query with CTE + GROUP BY
     * - Eliminates N+1 query pattern
     *
     * @param mode UserMode
     * @param sortedSkills Sorted list of skill names
     * @param finalLimit Maximum number of top skills
     * @return Mono<SearchStatisticsResult>
     */
    private Mono<SearchStatisticsResult> computeSearchStatistics(
            UserMode mode,
            List<String> sortedSkills,
            int finalLimit
    ) {
        Double similarityThreshold = 0.6; // Same threshold as searchMatches

        // Normalize skills to query vector
        return skillNormalizationService.normalizeSkillsToQueryVector(sortedSkills)
                .flatMap(queryVector -> {
                    // Use optimized single query with CTE + GROUP BY
                    if (mode == UserMode.CANDIDATE) {
                        return recruitSearchRepository.findSearchStatisticsByVector(
                                queryVector, similarityThreshold, finalLimit);
                    } else {
                        return candidateSearchRepository.findSearchStatisticsByVector(
                                queryVector, similarityThreshold, finalLimit);
                    }
                })
                .doOnSuccess(result -> log.info("computeSearchStatistics completed - topSkills: {}, totalCount: {}",
                        result.getTopSkills().size(), result.getTotalCount()));
    }

    /**
     * Get Recruit Detail (Detail View)
     * - Fetches recruit, description, and skills in parallel
     * - Returns combined RecruitDetail DTO
     * - Moved from QueryResolver to follow Clean Architecture
     *
     * @param id Recruit ID as string
     * @return Mono<RecruitDetail>
     */
    public Mono<RecruitDetail> getRecruitDetail(String id) {
        log.info("getRecruitDetail called - id: {}", id);

        UUID recruitId = UUID.fromString(id);

        // Fetch recruit, description, and skills in parallel
        Mono<Recruit> recruitMono = recruitRepository.findById(recruitId);
        Mono<String> descriptionMono = recruitDescriptionRepository.findByRecruitId(recruitId)
                .map(desc -> desc.getLongDescription())
                .defaultIfEmpty("");
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
                .doOnSuccess(detail -> log.info("getRecruitDetail returned detail for id: {}", id))
                .doOnError(error -> log.error("getRecruitDetail error for id {}: {}", id, error.getMessage(), error));
    }

    /**
     * Get Candidate Detail (Detail View)
     * - Fetches candidate, description, and skills in parallel
     * - Returns combined CandidateDetail DTO
     * - Moved from QueryResolver to follow Clean Architecture
     *
     * @param id Candidate ID as string
     * @return Mono<CandidateDetail>
     */
    public Mono<CandidateDetail> getCandidateDetail(String id) {
        log.info("getCandidateDetail called - id: {}", id);

        UUID candidateId = UUID.fromString(id);

        // Fetch candidate, description (resume_lang, moreinfo, looking_for), and skills in parallel
        Mono<Candidate> candidateMono = candidateRepository.findById(candidateId);
        Mono<CandidateDescription> descriptionMono = candidateDescriptionRepository.findByCandidateId(candidateId)
                .defaultIfEmpty(new CandidateDescription());
        Mono<List<String>> skillsMono = candidateSkillRepository.findByCandidateId(candidateId)
                .map(skill -> skill.getSkill())
                .collectList();

        return Mono.zip(candidateMono, descriptionMono, skillsMono)
                .map(tuple -> {
                    Candidate candidate = tuple.getT1();
                    CandidateDescription description = tuple.getT2();
                    List<String> skills = tuple.getT3();

                    return CandidateDetail.builder()
                            .id(candidate.getCandidateId().toString())
                            .positionCategory(candidate.getPositionCategory())
                            .experienceYears(candidate.getExperienceYears())
                            .originalResume(candidate.getOriginalResume())
                            .resumeLang(description.getResumeLang())
                            .moreinfo(description.getMoreinfo())
                            .lookingFor(description.getLookingFor())
                            .skills(skills)
                            .createdAt(candidate.getCreatedAt() != null ? candidate.getCreatedAt().toString() : null)
                            .updatedAt(candidate.getUpdatedAt() != null ? candidate.getUpdatedAt().toString() : null)
                            .build();
                })
                .doOnSuccess(detail -> log.info("getCandidateDetail returned detail for id: {}", id))
                .doOnError(error -> log.error("getCandidateDetail error for id {}: {}", id, error.getMessage(), error));
    }
}
