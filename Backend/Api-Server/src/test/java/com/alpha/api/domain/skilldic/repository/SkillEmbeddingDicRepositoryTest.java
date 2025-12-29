package com.alpha.api.domain.skilldic.repository;

import com.alpha.api.domain.skilldic.entity.SkillCategoryDic;
import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * SkillEmbeddingDicRepository Integration Test
 * - Tests skill normalization dictionary queries against real PostgreSQL
 * - Tests findBySkillIn() for skill lookup
 * - Tests case-insensitive skill matching
 *
 * NOTE: This is an INTEGRATION TEST that requires:
 * - PostgreSQL running on localhost:5432
 * - Database 'alpha_match_test' created
 * - pgvector extension enabled
 *
 * Run with: ./gradlew test -Dspring.profiles.active=integration
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:postgresql://localhost:5432/alpha_match_test",
    "spring.r2dbc.username=postgres",
    "spring.r2dbc.password=postgres"
})
@Disabled("Integration test - requires PostgreSQL with alpha_match_test database. Run manually when DB is available.")
class SkillEmbeddingDicRepositoryTest {

    @Autowired
    private SkillEmbeddingDicRepository skillEmbeddingDicRepository;

    @Autowired
    private SkillCategoryDicRepository skillCategoryDicRepository;

    private UUID backendCategoryId;
    private UUID frontendCategoryId;
    private UUID testSkillId1;
    private UUID testSkillId2;
    private UUID testSkillId3;

    @BeforeEach
    void setUp() {
        // Clean up test data
        skillEmbeddingDicRepository.deleteAll().block();
        skillCategoryDicRepository.deleteAll().block();

        // Create test categories
        backendCategoryId = UUID.randomUUID();
        frontendCategoryId = UUID.randomUUID();

        SkillCategoryDic backendCategory = SkillCategoryDic.builder()
                .categoryId(backendCategoryId)
                .category("Backend")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        SkillCategoryDic frontendCategory = SkillCategoryDic.builder()
                .categoryId(frontendCategoryId)
                .category("Frontend")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        skillCategoryDicRepository.saveAll(Arrays.asList(backendCategory, frontendCategory)).blockLast();

        // Create test skills
        testSkillId1 = UUID.randomUUID();
        testSkillId2 = UUID.randomUUID();
        testSkillId3 = UUID.randomUUID();

        SkillEmbeddingDic java = SkillEmbeddingDic.builder()
                .skillId(testSkillId1)
                .categoryId(backendCategoryId)
                .skill("Java")
                .skillVector(generateDummyVector(384))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        SkillEmbeddingDic python = SkillEmbeddingDic.builder()
                .skillId(testSkillId2)
                .categoryId(backendCategoryId)
                .skill("Python")
                .skillVector(generateDummyVector(384))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        SkillEmbeddingDic react = SkillEmbeddingDic.builder()
                .skillId(testSkillId3)
                .categoryId(frontendCategoryId)
                .skill("React")
                .skillVector(generateDummyVector(384))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        skillEmbeddingDicRepository.saveAll(Arrays.asList(java, python, react)).blockLast();
    }

    @Test
    @DisplayName("Should find skill by name (case-insensitive)")
    void testFindBySkill() {
        // When - lowercase
        Mono<SkillEmbeddingDic> result1 = skillEmbeddingDicRepository.findBySkill("java");

        // Then
        StepVerifier.create(result1)
                .expectNextMatches(skill -> skill.getSkill().equals("Java"))
                .verifyComplete();

        // When - uppercase
        Mono<SkillEmbeddingDic> result2 = skillEmbeddingDicRepository.findBySkill("PYTHON");

        // Then
        StepVerifier.create(result2)
                .expectNextMatches(skill -> skill.getSkill().equals("Python"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find multiple skills by names (findBySkillIn)")
    void testFindBySkillIn() {
        // Given
        List<String> skills = Arrays.asList("java", "python");

        // When
        Flux<SkillEmbeddingDic> result = skillEmbeddingDicRepository.findBySkillIn(skills);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(skill -> skill.getSkill().equals("Java"))
                .expectNextMatches(skill -> skill.getSkill().equals("Python"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty for non-existent skill")
    void testFindBySkillNotFound() {
        // When
        Mono<SkillEmbeddingDic> result = skillEmbeddingDicRepository.findBySkill("NonExistent");

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find skills by category ID")
    void testFindByCategoryId() {
        // When
        Flux<SkillEmbeddingDic> result = skillEmbeddingDicRepository.findByCategoryId(backendCategoryId);

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find all skills with category info")
    void testFindAllWithCategory() {
        // When
        Flux<SkillEmbeddingDic> result = skillEmbeddingDicRepository.findAllWithCategory();

        // Then
        StepVerifier.create(result)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should search skills by partial name match")
    void testSearchBySkillContaining() {
        // Given
        String partialSkill = "Jav";
        Integer limit = 10;

        // When
        Flux<SkillEmbeddingDic> result = skillEmbeddingDicRepository.searchBySkillContaining(partialSkill, limit);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(skill -> skill.getSkill().equals("Java"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should count total skills")
    void testCount() {
        // When
        Mono<Long> result = skillEmbeddingDicRepository.count();

        // Then
        StepVerifier.create(result)
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle case-insensitive findBySkillIn")
    void testFindBySkillInCaseInsensitive() {
        // Given
        List<String> skills = Arrays.asList("JAVA", "python", "ReAcT");

        // When
        Flux<SkillEmbeddingDic> result = skillEmbeddingDicRepository.findBySkillIn(skills);

        // Then
        StepVerifier.create(result)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty for empty skill list")
    void testFindBySkillInEmpty() {
        // Given
        List<String> skills = Arrays.asList();

        // When
        Flux<SkillEmbeddingDic> result = skillEmbeddingDicRepository.findBySkillIn(skills);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    private List<Float> generateDummyVector(int dimension) {
        Float[] vector = new Float[dimension];
        Arrays.fill(vector, 0.1f);
        return Arrays.asList(vector);
    }
}
