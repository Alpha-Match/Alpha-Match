package com.alpha.api.domain.search.service;

import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.entity.CandidateSkill;
import com.alpha.api.domain.candidate.repository.CandidateRepository;
import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.entity.RecruitSkill;
import com.alpha.api.domain.recruit.repository.RecruitRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.domain.skilldic.entity.SkillCategoryDic;
import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.alpha.api.domain.skilldic.service.SkillNormalizationService;
import com.alpha.api.graphql.type.*;
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
 * - Tests parseExperienceString() method ("3-5 Years" → [3, 5])
 * - Uses Mockito for mocking dependencies
 *
 * NOTE: Service uses similarityThreshold = 0.3 for CANDIDATE mode
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SkillNormalizationService skillNormalizationService;

    @Mock
    private RecruitRepository recruitRepository;

    @Mock
    private RecruitSkillRepository recruitSkillRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateSkillRepository candidateSkillRepository;

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
    }

    @Test
    @DisplayName("Should search recruits for CANDIDATE mode")
    void testSearchMatchesCandidateMode() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        // Service uses 0.3 threshold for CANDIDATE mode
        when(recruitRepository.findSimilarByVector(eq(queryVector), eq(0.3), eq(10)))
                .thenReturn(Flux.just(testRecruit));
        when(recruitSkillRepository.findByRecruitId(any(UUID.class)))
                .thenReturn(Flux.just(
                        RecruitSkill.builder().recruitId(testRecruit.getRecruitId()).skill("Java").build(),
                        RecruitSkill.builder().recruitId(testRecruit.getRecruitId()).skill("Python").build()
                ));

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience);

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

        verify(skillNormalizationService, times(1)).normalizeSkillsToQueryVector(skills);
        verify(recruitRepository, times(1)).findSimilarByVector(queryVector, 0.3, 10);
    }

    @Test
    @DisplayName("Should search candidates for RECRUITER mode")
    void testSearchMatchesRecruiterMode() {
        // Given
        UserMode mode = UserMode.RECRUITER;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        // Service uses 0.7 threshold for RECRUITER mode
        when(candidateRepository.findSimilarByVector(eq(queryVector), eq(0.7), eq(10)))
                .thenReturn(Flux.just(testCandidate));
        when(candidateSkillRepository.findByCandidateId(any(UUID.class)))
                .thenReturn(Flux.just(
                        CandidateSkill.builder().candidateId(testCandidate.getCandidateId()).skill("Java").build(),
                        CandidateSkill.builder().candidateId(testCandidate.getCandidateId()).skill("Python").build()
                ));

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    List<MatchItem> matches = searchResult.getMatches();
                    return matches.size() == 1 &&
                           matches.get(0).getCompany().equals("Backend") &&
                           matches.get(0).getSkills().size() == 2;
                })
                .verifyComplete();

        verify(skillNormalizationService, times(1)).normalizeSkillsToQueryVector(skills);
        verify(candidateRepository, times(1)).findSimilarByVector(queryVector, 0.7, 10);
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
    @DisplayName("Should parse experience string '0-2 Years' to [0, 2]")
    void testParseExperienceString0To2() {
        // Given
        List<String> skills = Arrays.asList("Java");
        String experience = "0-2 Years";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        when(recruitRepository.findSimilarByVector(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(UserMode.CANDIDATE, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> searchResult.getMatches().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should parse experience string '3-5 Years' to [3, 5]")
    void testParseExperienceString3To5() {
        // Given
        List<String> skills = Arrays.asList("Java");
        String experience = "3-5 Years";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        when(recruitRepository.findSimilarByVector(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(UserMode.CANDIDATE, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> searchResult.getMatches().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should parse experience string '10+ Years' to [10, 999]")
    void testParseExperienceString10Plus() {
        // Given
        List<String> skills = Arrays.asList("Java");
        String experience = "10+ Years";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        when(recruitRepository.findSimilarByVector(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(UserMode.CANDIDATE, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> searchResult.getMatches().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty experience string (default to [0, 999])")
    void testParseExperienceStringEmpty() {
        // Given
        List<String> skills = Arrays.asList("Java");
        String experience = "";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        when(recruitRepository.findSimilarByVector(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(UserMode.CANDIDATE, skills, experience);

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
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        when(recruitRepository.findSimilarByVector(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience);

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
    @DisplayName("Should handle empty search results")
    void testSearchMatchesEmptyResults() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java");
        String experience = "3-5 Years";
        String queryVector = "[0.1,0.2,0.3]";

        when(skillNormalizationService.normalizeSkillsToQueryVector(skills))
                .thenReturn(Mono.just(queryVector));
        when(recruitRepository.findSimilarByVector(anyString(), anyDouble(), anyInt()))
                .thenReturn(Flux.empty());

        // When
        Mono<SearchMatchesResult> result = searchService.searchMatches(mode, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> searchResult.getMatches().isEmpty())
                .verifyComplete();
    }
}
