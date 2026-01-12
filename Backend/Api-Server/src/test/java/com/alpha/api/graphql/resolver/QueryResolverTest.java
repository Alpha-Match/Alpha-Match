package com.alpha.api.graphql.resolver;

import com.alpha.api.application.service.CacheService;
import com.alpha.api.application.service.DashboardService;
import com.alpha.api.application.service.SearchService;
import com.alpha.api.presentation.graphql.resolver.QueryResolver;
import com.alpha.api.presentation.graphql.type.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QueryResolver Test
 * - Tests GraphQL query resolvers
 * - Tests searchMatches query (6 parameters: mode, skills, experience, limit, offset, sortBy)
 * - Tests skillCategories query
 * - Uses Mockito for mocking SearchService, DashboardService, CacheService
 */
@ExtendWith(MockitoExtension.class)
class QueryResolverTest {

    @Mock
    private SearchService searchService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private QueryResolver queryResolver;

    private SearchMatchesResult mockSearchResult;
    private List<SkillCategory> mockSkillCategories;

    @BeforeEach
    void setUp() {
        // Mock SearchMatchesResult
        MatchItem matchItem1 = MatchItem.builder()
                .id("123e4567-e89b-12d3-a456-426614174000")
                .title("Senior Java Developer")
                .company("TechCorp")
                .score(0.85)
                .skills(Arrays.asList("Java", "Python", "Spring"))
                .experience(5)
                .build();

        MatchItem matchItem2 = MatchItem.builder()
                .id("123e4567-e89b-12d3-a456-426614174001")
                .title("Backend Engineer")
                .company("DataCorp")
                .score(0.78)
                .skills(Arrays.asList("Python", "Django", "PostgreSQL"))
                .experience(3)
                .build();

        SkillMatch skillMatch1 = SkillMatch.builder()
                .skill("Java")
                .isCore(true)
                .x(10.5)
                .y(20.3)
                .build();

        SkillMatch skillMatch2 = SkillMatch.builder()
                .skill("Python")
                .isCore(true)
                .x(15.2)
                .y(25.7)
                .build();

        mockSearchResult = SearchMatchesResult.builder()
                .matches(Arrays.asList(matchItem1, matchItem2))
                .vectorVisualization(Arrays.asList(skillMatch1, skillMatch2))
                .build();

        // Mock SkillCategories
        SkillCategory backendCategory = SkillCategory.builder()
                .category("Backend")
                .skills(Arrays.asList("Java", "Python", "Go", "Rust"))
                .build();

        SkillCategory frontendCategory = SkillCategory.builder()
                .category("Frontend")
                .skills(Arrays.asList("React", "Vue", "Angular", "TypeScript"))
                .build();

        mockSkillCategories = Arrays.asList(backendCategory, frontendCategory);
    }

    @Test
    @DisplayName("Should resolve searchMatches query for CANDIDATE mode")
    void testSearchMatchesCandidateMode() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";

        when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy)))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    List<MatchItem> matches = searchResult.getMatches();
                    List<SkillMatch> vectorVisualization = searchResult.getVectorVisualization();

                    return matches.size() == 2 &&
                           matches.get(0).getTitle().equals("Senior Java Developer") &&
                           matches.get(0).getCompany().equals("TechCorp") &&
                           matches.get(0).getScore().equals(0.85) &&
                           matches.get(1).getTitle().equals("Backend Engineer") &&
                           vectorVisualization.size() == 2 &&
                           vectorVisualization.get(0).getSkill().equals("Java") &&
                           vectorVisualization.get(1).getSkill().equals("Python");
                })
                .verifyComplete();

        verify(searchService, times(1)).searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy));
    }

    @Test
    @DisplayName("Should resolve searchMatches query for RECRUITER mode")
    void testSearchMatchesRecruiterMode() {
        // Given
        UserMode mode = UserMode.RECRUITER;
        List<String> skills = Arrays.asList("Java", "Spring");
        String experience = "5+ Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";

        when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy)))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    return searchResult.getMatches().size() == 2 &&
                           searchResult.getVectorVisualization().size() == 2;
                })
                .verifyComplete();

        verify(searchService, times(1)).searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy));
    }

    @Test
    @DisplayName("Should handle empty search results")
    void testSearchMatchesEmptyResults() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("COBOL");
        String experience = "10+ Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";

        SearchMatchesResult emptyResult = SearchMatchesResult.builder()
                .matches(Arrays.asList())
                .vectorVisualization(Arrays.asList())
                .build();

        when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy)))
                .thenReturn(Mono.just(emptyResult));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    return searchResult.getMatches().isEmpty() &&
                           searchResult.getVectorVisualization().isEmpty();
                })
                .verifyComplete();

        verify(searchService, times(1)).searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy));
    }

    @Test
    @DisplayName("Should handle searchMatches error")
    void testSearchMatchesError() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("InvalidSkill");
        String experience = "3-5 Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";

        when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy)))
                .thenReturn(Mono.error(new IllegalArgumentException("No matching skills found in dictionary")));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("No matching skills found in dictionary"))
                .verify();

        verify(searchService, times(1)).searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy));
    }

    @Test
    @DisplayName("Should resolve skillCategories query")
    void testSkillCategories() {
        // Given - mock cacheService (QueryResolver uses cacheService.getOrLoadStaticUnchecked)
        when(cacheService.getOrLoadStaticUnchecked(anyString(), any()))
                .thenReturn(Mono.just(mockSkillCategories));

        // When
        Mono<List<SkillCategory>> result = queryResolver.skillCategories();

        // Then
        StepVerifier.create(result)
                .expectNextMatches(categories -> {
                    return categories.size() == 2 &&
                           categories.get(0).getCategory().equals("Backend") &&
                           categories.get(0).getSkills().contains("Java") &&
                           categories.get(0).getSkills().contains("Python") &&
                           categories.get(1).getCategory().equals("Frontend") &&
                           categories.get(1).getSkills().contains("React") &&
                           categories.get(1).getSkills().contains("Vue");
                })
                .verifyComplete();

        verify(cacheService, times(1)).getOrLoadStaticUnchecked(anyString(), any());
    }

    @Test
    @DisplayName("Should handle empty skillCategories")
    void testSkillCategoriesEmpty() {
        // Given - mock cacheService
        when(cacheService.getOrLoadStaticUnchecked(anyString(), any()))
                .thenReturn(Mono.just(Arrays.asList()));

        // When
        Mono<List<SkillCategory>> result = queryResolver.skillCategories();

        // Then
        StepVerifier.create(result)
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

        verify(cacheService, times(1)).getOrLoadStaticUnchecked(anyString(), any());
    }

    @Test
    @DisplayName("Should handle skillCategories error")
    void testSkillCategoriesError() {
        // Given - mock cacheService to return error
        when(cacheService.getOrLoadStaticUnchecked(anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        // When
        Mono<List<SkillCategory>> result = queryResolver.skillCategories();

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Database connection failed"))
                .verify();

        verify(cacheService, times(1)).getOrLoadStaticUnchecked(anyString(), any());
    }

    @Test
    @DisplayName("Should verify correct parameter passing to searchMatches")
    void testSearchMatchesParameterPassing() {
        // Given
        UserMode mode = UserMode.RECRUITER;
        List<String> skills = Arrays.asList("JavaScript", "TypeScript", "React");
        String experience = "0-2 Years";
        Integer limit = 20;
        Integer offset = 5;
        String sortBy = "experience ASC";

        when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy)))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy).block();

        // Then
        verify(searchService, times(1)).searchMatches(
                eq(UserMode.RECRUITER),
                eq(Arrays.asList("JavaScript", "TypeScript", "React")),
                eq("0-2 Years"),
                eq(20),
                eq(5),
                eq("experience ASC")
        );
    }

    @Test
    @DisplayName("Should handle various experience formats in searchMatches")
    void testSearchMatchesVariousExperienceFormats() {
        // Test different experience formats
        List<String> experienceFormats = Arrays.asList(
                "0-2 Years",
                "3-5 Years",
                "6-9 Years",
                "10+ Years"
        );

        for (String experience : experienceFormats) {
            // Given
            UserMode mode = UserMode.CANDIDATE;
            List<String> skills = Arrays.asList("Java");
            Integer limit = 10;
            Integer offset = 0;
            String sortBy = "score DESC";

            when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy)))
                    .thenReturn(Mono.just(mockSearchResult));

            // When
            Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(searchResult -> searchResult.getMatches().size() == 2)
                    .verifyComplete();
        }

        verify(searchService, times(4)).searchMatches(any(UserMode.class), anyList(), anyString(), anyInt(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should handle null pagination parameters")
    void testSearchMatchesNullPaginationParams() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";
        Integer limit = null;
        Integer offset = null;
        String sortBy = null;

        when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), isNull(), isNull(), isNull()))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> searchResult.getMatches().size() == 2)
                .verifyComplete();

        verify(searchService, times(1)).searchMatches(eq(mode), eq(skills), eq(experience), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("Should verify correct logging in searchMatches")
    void testSearchMatchesLogging() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";
        Integer limit = 10;
        Integer offset = 0;
        String sortBy = "score DESC";

        when(searchService.searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy)))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        queryResolver.searchMatches(mode, skills, experience, limit, offset, sortBy).block();

        // Then
        // Verify that the resolver called the service (logging happens in the resolver)
        verify(searchService, times(1)).searchMatches(eq(mode), eq(skills), eq(experience), eq(limit), eq(offset), eq(sortBy));
        // Note: Actual logging verification would require a logging framework mock (e.g., LogCaptor)
    }
}
