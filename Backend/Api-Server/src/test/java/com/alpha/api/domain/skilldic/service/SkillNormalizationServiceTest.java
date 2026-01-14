package com.alpha.api.domain.skilldic.service;

import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SkillNormalizationService Test
 * - Tests normalizeSkillsToQueryVector() method
 * - Tests calculateAverageVector() logic
 * - Tests parseVectorString() / convertToVectorString() methods
 * - Uses Mockito for mocking repository
 *
 * NOTE: Service uses findBySkill() for each skill individually (not findBySkillIn)
 */
@ExtendWith(MockitoExtension.class)
class SkillNormalizationServiceTest {

    @Mock
    private SkillEmbeddingDicRepository skillEmbeddingDicRepository;

    @InjectMocks
    private SkillNormalizationService skillNormalizationService;

    private SkillEmbeddingDic javaSkill;
    private SkillEmbeddingDic pythonSkill;

    @BeforeEach
    void setUp() {
        UUID categoryId = UUID.randomUUID();

        // Create Java skill with 1536-dimension vector for accurate testing
        javaSkill = SkillEmbeddingDic.builder()
                .skillId(UUID.randomUUID())
                .categoryId(categoryId)
                .skill("Java")
                .skillVector(generateDummyVectorList(1536, 0.1f))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Create Python skill with different vector values
        pythonSkill = SkillEmbeddingDic.builder()
                .skillId(UUID.randomUUID())
                .categoryId(categoryId)
                .skill("Python")
                .skillVector(generateDummyVectorList(1536, 0.3f))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should normalize skills to query vector (average)")
    void testNormalizeSkillsToQueryVector() {
        // Given
        List<String> skills = Arrays.asList("Java", "Python");

        // Service calls findBySkill for each skill individually
        when(skillEmbeddingDicRepository.findBySkill("java"))
                .thenReturn(Mono.just(javaSkill));
        when(skillEmbeddingDicRepository.findBySkill("python"))
                .thenReturn(Mono.just(pythonSkill));

        // When
        Mono<String> result = skillNormalizationService.normalizeSkillsToQueryVector(skills);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(vector -> {
                    // Expected average: (0.1 + 0.3) / 2 = 0.2
                    return vector.startsWith("[") && vector.endsWith("]") && vector.contains("0.2");
                })
                .verifyComplete();

        verify(skillEmbeddingDicRepository, times(1)).findBySkill("java");
        verify(skillEmbeddingDicRepository, times(1)).findBySkill("python");
    }

    @Test
    @DisplayName("Should convert skills to lowercase for case-insensitive lookup")
    void testNormalizeSkillsCaseInsensitive() {
        // Given
        List<String> skills = Arrays.asList("JAVA", "python"); // Mixed case

        when(skillEmbeddingDicRepository.findBySkill("java"))
                .thenReturn(Mono.just(javaSkill));
        when(skillEmbeddingDicRepository.findBySkill("python"))
                .thenReturn(Mono.just(pythonSkill));

        // When
        Mono<String> result = skillNormalizationService.normalizeSkillsToQueryVector(skills);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(vector -> vector.startsWith("[") && vector.endsWith("]"))
                .verifyComplete();

        // Verify lowercase conversion was applied
        verify(skillEmbeddingDicRepository, times(1)).findBySkill("java");
        verify(skillEmbeddingDicRepository, times(1)).findBySkill("python");
    }

    @Test
    @DisplayName("Should return error for empty skills list")
    void testNormalizeSkillsEmptyList() {
        // Given
        List<String> skills = Arrays.asList();

        // When
        Mono<String> result = skillNormalizationService.normalizeSkillsToQueryVector(skills);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Skills list cannot be empty"))
                .verify();
    }

    @Test
    @DisplayName("Should return error for null skills list")
    void testNormalizeSkillsNullList() {
        // When
        Mono<String> result = skillNormalizationService.normalizeSkillsToQueryVector(null);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Skills list cannot be empty"))
                .verify();
    }

    @Test
    @DisplayName("Should return error when no matching skills found in dictionary")
    void testNormalizeSkillsNoMatchFound() {
        // Given
        List<String> skills = Arrays.asList("NonExistent");

        when(skillEmbeddingDicRepository.findBySkill("nonexistent"))
                .thenReturn(Mono.empty());

        // When
        Mono<String> result = skillNormalizationService.normalizeSkillsToQueryVector(skills);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("No matching skills found in dictionary"))
                .verify();
    }

    @Test
    @DisplayName("Should calculate average vector correctly for single skill")
    void testNormalizeSkillsSingleSkill() {
        // Given
        List<String> skills = Arrays.asList("Java");

        when(skillEmbeddingDicRepository.findBySkill("java"))
                .thenReturn(Mono.just(javaSkill));

        // When
        Mono<String> result = skillNormalizationService.normalizeSkillsToQueryVector(skills);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(vector -> {
                    // Expected: same as original (0.1 for all dimensions)
                    return vector.contains("0.1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should validate skill name")
    void testIsValidSkill() {
        // Given
        String skill = "Java";

        when(skillEmbeddingDicRepository.findBySkill(skill))
                .thenReturn(Mono.just(javaSkill));

        // When
        Mono<Boolean> result = skillNormalizationService.isValidSkill(skill);

        // Then
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return false for invalid skill name")
    void testIsValidSkillNotFound() {
        // Given
        String skill = "NonExistent";

        when(skillEmbeddingDicRepository.findBySkill(skill))
                .thenReturn(Mono.empty());

        // When
        Mono<Boolean> result = skillNormalizationService.isValidSkill(skill);

        // Then
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get all skills with category")
    void testGetAllSkillsWithCategory() {
        // Given
        when(skillEmbeddingDicRepository.findAllWithCategory())
                .thenReturn(Flux.just(javaSkill, pythonSkill));

        // When
        Mono<List<SkillEmbeddingDic>> result = skillNormalizationService.getAllSkillsWithCategory();

        // Then
        StepVerifier.create(result)
                .expectNextMatches(skills -> skills.size() == 2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should format vector string correctly (1536 dimensions)")
    void testVectorStringFormatting() {
        // Given
        List<String> skills = Arrays.asList("Java");

        when(skillEmbeddingDicRepository.findBySkill("java"))
                .thenReturn(Mono.just(javaSkill));

        // When
        Mono<String> result = skillNormalizationService.normalizeSkillsToQueryVector(skills);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(vector -> {
                    // Should start with [ and end with ]
                    // Should have 1536 comma-separated values
                    String[] parts = vector.replace("[", "").replace("]", "").split(",");
                    return vector.startsWith("[") && vector.endsWith("]") && parts.length == 1536;
                })
                .verifyComplete();
    }

    /**
     * Generate dummy vector list for testing
     *
     * @param dimension Vector dimension (1536)
     * @param value Value for each dimension
     * @return List<Float> vector representation
     */
    private List<Float> generateDummyVectorList(int dimension, float value) {
        List<Float> vector = new ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            vector.add(value);
        }
        return vector;
    }
}
