package com.alpha.api.domain.search.service;

import com.alpha.api.application.dto.RecruitSearchResult;
import com.alpha.api.application.dto.CandidateSearchResult;
import com.alpha.api.application.service.CacheService;
import com.alpha.api.application.service.SearchService;
import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.entity.CandidateSkill;
import com.alpha.api.domain.candidate.repository.CandidateDescriptionRepository;
import com.alpha.api.domain.candidate.repository.CandidateRepository;
import com.alpha.api.domain.candidate.repository.CandidateSearchRepository;
import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.entity.RecruitSkill;
import com.alpha.api.domain.recruit.repository.RecruitDescriptionRepository;
import com.alpha.api.domain.recruit.repository.RecruitRepository;
import com.alpha.api.domain.recruit.repository.RecruitSearchRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.domain.skilldic.entity.SkillCategoryDic;
import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.alpha.api.domain.skilldic.service.SkillNormalizationService;
import com.alpha.api.presentation.graphql.type.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SearchService Test
 * - Tests searchMatches() method (integration search)
 * - Tests searchRecruits() and searchCandidates() methods
 * - Tests getSkillCategories() method
 * - Uses Mockito for mocking dependencies
 *
 * NOTE: Service uses similarityThreshold = 0.6 for both modes
 * NOTE: searchMatches now accepts 6 parameters (mode, skills, experience, limit, offset, sortBy)
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SkillNormalizationService skillNormalizationService;

    @Mock
    private CacheService cacheService;

    @Mock
    private RecruitRepository recruitRepository;

    @Mock
    private RecruitDescriptionRepository recruitDescriptionRepository;

    @Mock
    private RecruitSkillRepository recruitSkillRepository;

    @Mock
    private RecruitSearchRepository recruitSearchRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateDescriptionRepository candidateDescriptionRepository;

    @Mock
    private CandidateSkillRepository candidateSkillRepository;

    @Mock
    private CandidateSearchRepository candidateSearchRepository;

    @Mock
    private SkillEmbeddingDicRepository skillEmbeddingDicRepository;

    @Mock
    private SkillCategoryDicRepository skillCategoryDicRepository;

    @InjectMocks
    private SearchService searchService;

    private Recruit testRecruit;
    private Candidate testCandidate;
    private SkillCategoryDic backendCategory;
    private SkillEmbeddingDic javaSkill;
    private RecruitSearchResult testRecruitSearchResult;
    private CandidateSearchResult testCandidateSearchResult;

    @BeforeEach
    void setUp() {
        UUID recruitId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        testRecruit = Recruit.builder()
                .recruitId(recruitId)
                .position("Senior Java Developer")
                .companyName("TechCorp")
                .experienceYears(5)
                .primaryKeyword("Java")
                .englishLevel("fluent")
                .publishedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        testCandidate = Candidate.builder()
                .candidateId(candidateId)
                .positionCategory("Backend")
                .experienceYears(5)
                .originalResume("Senior Java developer with 5 years of experience...")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        backendCategory = SkillCategoryDic.builder()
                .categoryId(categoryId)
                .category("Backend")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        javaSkill = SkillEmbeddingDic.builder()
                .skillId(UUID.randomUUID())
                .categoryId(categoryId)
                .skill("Java")
                .skillVector(Arrays.asList(0.1f, 0.2f, 0.3f))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Create search result DTOs
        testRecruitSearchResult = RecruitSearchResult.builder()
                .recruit(testRecruit)
                .similarityScore(0.85)
                .build();

        testCandidateSearchResult = CandidateSearchResult.builder()
                .candidate(testCandidate)
                .similarityScore(0.85)
                .build();
    }

    @Test
    @DisplayName("Should search recruits for CANDIDATE mode")
    void testSearchMatchesCandidateMode() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(anyList()))
                .thenReturn(Mono.just(queryVector));
        when(recruitSearchRepository.findSimilarByVectorWithScore(eq(queryVector), eq(0.6), eq(10)))
                .thenReturn(Flux.just(testRecruitSearchResult));
        when(recruitSkillRepository.findByRecruitId(any(UUID.class)))
                .thenReturn(Flux.just(
                        RecruitSkill.builder().recruitId(testRecruit.getRecruitId()).skill("Java").build(),
                        RecruitSkill.builder().recruitId(testRecruit.getRecruitId()).skill("Python").build()
                ));

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    List<MatchItem> matches = searchResult.getMatches();
                    return matches.size() == 1 &&
                           matches.get(0).getTitle().equals("Senior Java Developer") &&
                           matches.get(0).getCompany().equals("TechCorp") &&
                           matches.get(0).getSkills().size() == 2;
                })
                .verifyComplete();

        verify(skillNormalizationService, times(1)).normalizeSkillsToQueryVector(anyList());
        verify(recruitSearchRepository, times(1)).findSimilarByVectorWithScore(eq(queryVector), eq(0.6), eq(10));
    }

    @Test
    @DisplayName("Should search candidates for RECRUITER mode")
    void testSearchMatchesRecruiterMode() {
        // Given
        UserMode mode = UserMode.RECRUITER;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(anyList()))
                .thenReturn(Mono.just(queryVector));
        when(candidateSearchRepository.findSimilarByVectorWithScore(eq(queryVector), eq(0.6), eq(10)))
                .thenReturn(Flux.just(testCandidateSearchResult));
        when(candidateSkillRepository.findByCandidateId(any(UUID.class)))
                .thenReturn(Flux.just(
                        CandidateSkill.builder().candidateId(testCandidate.getCandidateId()).skill("Java").build(),
                        CandidateSkill.builder().candidateId(testCandidate.getCandidateId()).skill("Python").build()
                ));

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    List<MatchItem> matches = searchResult.getMatches();
                    return matches.size() == 1 &&
                           matches.get(0).getCompany().equals("Backend") &&
                           matches.get(0).getSkills().size() == 2;
                })
                .verifyComplete();

        verify(skillNormalizationService, times(1)).normalizeSkillsToQueryVector(anyList());
        verify(candidateSearchRepository, times(1)).findSimilarByVectorWithScore(eq(queryVector), eq(0.6), eq(10));
    }

    @Test
    @DisplayName("Should get skill categories with skills")
    void testGetSkillCategories() {
        // Given
        when(skillCategoryDicRepository.findAllOrderByCategory())
                .thenReturn(Flux.just(backendCategory));
        when(skillEmbeddingDicRepository.findByCategoryId(any(UUID.class)))
                .thenReturn(Flux.just(javaSkill));

        // When
        Mono<List<SkillCategory>> result = searchService.getSkillCategories();

        // Then
        StepVerifier.create(result)
                .expectNextMatches(categories -> {
                    return categories.size() == 1 &&
                           categories.get(0).getCategory().equals("Backend") &&
                           categories.get(0).getSkills().contains("Java");
                })
                .verifyComplete();

        verify(skillCategoryDicRepository, times(1)).findAllOrderByCategory();
        verify(skillEmbeddingDicRepository, times(1)).findByCategoryId(any(UUID.class));
    }

    @Test
    @DisplayName("Should handle empty search results")
    void testSearchMatchesEmptyResults() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java");
        String experience = "3-5 Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(anyList()))
                .thenReturn(Mono.just(queryVector));
        when(recruitSearchRepository.findSimilarByVectorWithScore(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> searchResult.getMatches().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return vector visualization with skills")
    void testVectorVisualization() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java", "Python", "React");
        String experience = "3-5 Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(anyList()))
                .thenReturn(Mono.just(queryVector));
        when(recruitSearchRepository.findSimilarByVectorWithScore(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    List<SkillMatch> vectorVisualization = searchResult.getVectorVisualization();
                    return vectorVisualization.size() == 3 &&
                           vectorVisualization.stream().anyMatch(sm -> sm.getSkill().equals("Java")) &&
                           vectorVisualization.stream().anyMatch(sm -> sm.getSkill().equals("Python")) &&
                           vectorVisualization.stream().anyMatch(sm -> sm.getSkill().equals("React"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should apply pagination with offset and limit")
    void testSearchMatchesWithPagination() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java");
        String experience = "3-5 Years";
        Integer limit = 5;
        Integer offset = 10;
        String sortBy = "score DESC";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(anyList()))
                .thenReturn(Mono.just(queryVector));
        // Repository should receive limit + offset for pagination
        when(recruitSearchRepository.findSimilarByVectorWithScore(eq(queryVector), eq(0.6), eq(15)))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> searchResult.getMatches().isEmpty())
                .verifyComplete();

        verify(recruitSearchRepository, times(1)).findSimilarByVectorWithScore(eq(queryVector), eq(0.6), eq(15));
    }
}
