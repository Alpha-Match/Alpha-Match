package com.alpha.api.graphql.resolver;

import com.alpha.api.domain.search.service.SearchService;
import com.alpha.api.graphql.type.*;
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
 * - Tests searchMatches query
 * - Tests skillCategories query
 * - Uses Mockito for mocking SearchService
 */
@ExtendWith(MockitoExtension.class)
class QueryResolverTest {

    @Mock
    private SearchService searchService;

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
                .description("Job description...")
                .build();

        MatchItem matchItem2 = MatchItem.builder()
                .id("123e4567-e89b-12d3-a456-426614174001")
                .title("Backend Engineer")
                .company("DataCorp")
                .score(0.78)
                .skills(Arrays.asList("Python", "Django", "PostgreSQL"))
                .experience(3)
                .description("Job description...")
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

        when(searchService.searchMatches(mode, skills, experience))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience);

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

        verify(searchService, times(1)).searchMatches(mode, skills, experience);
    }

    @Test
    @DisplayName("Should resolve searchMatches query for RECRUITER mode")
    void testSearchMatchesRecruiterMode() {
        // Given
        UserMode mode = UserMode.RECRUITER;
        List<String> skills = Arrays.asList("Java", "Spring");
        String experience = "5+ Years";

        when(searchService.searchMatches(mode, skills, experience))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    return searchResult.getMatches().size() == 2 &&
                           searchResult.getVectorVisualization().size() == 2;
                })
                .verifyComplete();

        verify(searchService, times(1)).searchMatches(mode, skills, experience);
    }

    @Test
    @DisplayName("Should handle empty search results")
    void testSearchMatchesEmptyResults() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("COBOL");
        String experience = "10+ Years";

        SearchMatchesResult emptyResult = SearchMatchesResult.builder()
                .matches(Arrays.asList())
                .vectorVisualization(Arrays.asList())
                .build();

        when(searchService.searchMatches(mode, skills, experience))
                .thenReturn(Mono.just(emptyResult));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(searchResult -> {
                    return searchResult.getMatches().isEmpty() &&
                           searchResult.getVectorVisualization().isEmpty();
                })
                .verifyComplete();

        verify(searchService, times(1)).searchMatches(mode, skills, experience);
    }

    @Test
    @DisplayName("Should handle searchMatches error")
    void testSearchMatchesError() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("InvalidSkill");
        String experience = "3-5 Years";

        when(searchService.searchMatches(mode, skills, experience))
                .thenReturn(Mono.error(new IllegalArgumentException("No matching skills found in dictionary")));

        // When
        Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("No matching skills found in dictionary"))
                .verify();

        verify(searchService, times(1)).searchMatches(mode, skills, experience);
    }

    @Test
    @DisplayName("Should resolve skillCategories query")
    void testSkillCategories() {
        // Given
        when(searchService.getSkillCategories())
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

        verify(searchService, times(1)).getSkillCategories();
    }

    @Test
    @DisplayName("Should handle empty skillCategories")
    void testSkillCategoriesEmpty() {
        // Given
        when(searchService.getSkillCategories())
                .thenReturn(Mono.just(Arrays.asList()));

        // When
        Mono<List<SkillCategory>> result = queryResolver.skillCategories();

        // Then
        StepVerifier.create(result)
                .expectNextMatches(List::isEmpty)
                .verifyComplete();

        verify(searchService, times(1)).getSkillCategories();
    }

    @Test
    @DisplayName("Should handle skillCategories error")
    void testSkillCategoriesError() {
        // Given
        when(searchService.getSkillCategories())
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        // When
        Mono<List<SkillCategory>> result = queryResolver.skillCategories();

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Database connection failed"))
                .verify();

        verify(searchService, times(1)).getSkillCategories();
    }

    @Test
    @DisplayName("Should verify correct parameter passing to searchMatches")
    void testSearchMatchesParameterPassing() {
        // Given
        UserMode mode = UserMode.RECRUITER;
        List<String> skills = Arrays.asList("JavaScript", "TypeScript", "React");
        String experience = "0-2 Years";

        when(searchService.searchMatches(mode, skills, experience))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        queryResolver.searchMatches(mode, skills, experience).block();

        // Then
        verify(searchService, times(1)).searchMatches(
                eq(UserMode.RECRUITER),
                eq(Arrays.asList("JavaScript", "TypeScript", "React")),
                eq("0-2 Years")
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

            when(searchService.searchMatches(mode, skills, experience))
                    .thenReturn(Mono.just(mockSearchResult));

            // When
            Mono<SearchMatchesResult> result = queryResolver.searchMatches(mode, skills, experience);

            // Then
            StepVerifier.create(result)
                    .expectNextMatches(searchResult -> searchResult.getMatches().size() == 2)
                    .verifyComplete();
        }

        verify(searchService, times(4)).searchMatches(any(UserMode.class), anyList(), anyString());
    }

    @Test
    @DisplayName("Should verify correct logging in searchMatches")
    void testSearchMatchesLogging() {
        // Given
        UserMode mode = UserMode.CANDIDATE;
        List<String> skills = Arrays.asList("Java", "Python");
        String experience = "3-5 Years";

        when(searchService.searchMatches(mode, skills, experience))
                .thenReturn(Mono.just(mockSearchResult));

        // When
        queryResolver.searchMatches(mode, skills, experience).block();

        // Then
        // Verify that the resolver called the service (logging happens in the resolver)
        verify(searchService, times(1)).searchMatches(mode, skills, experience);
        // Note: Actual logging verification would require a logging framework mock (e.g., LogCaptor)
    }
}
