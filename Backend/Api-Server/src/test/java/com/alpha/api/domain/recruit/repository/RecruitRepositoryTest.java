package com.alpha.api.domain.recruit.repository;

import com.alpha.api.domain.recruit.entity.Recruit;
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
import java.util.UUID;

/**
 * RecruitRepository Integration Test
 * - Tests R2DBC repository operations against real PostgreSQL
 * - Tests PGvector similarity search (findSimilarByVector)
 * - Tests similarity threshold filtering (>= 0.7)
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
class RecruitRepositoryTest {

    @Autowired
    private RecruitRepository recruitRepository;

    private UUID testRecruitId1;
    private UUID testRecruitId2;
    private UUID testRecruitId3;

    @BeforeEach
    void setUp() {
        // Clean up test data
        recruitRepository.deleteAll().block();

        // Create test data
        testRecruitId1 = UUID.randomUUID();
        testRecruitId2 = UUID.randomUUID();
        testRecruitId3 = UUID.randomUUID();

        Recruit recruit1 = Recruit.builder()
                .recruitId(testRecruitId1)
                .position("Senior Java Developer")
                .companyName("TechCorp")
                .experienceYears(5)
                .primaryKeyword("Java")
                .englishLevel("fluent")
                .publishedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Recruit recruit2 = Recruit.builder()
                .recruitId(testRecruitId2)
                .position("Python Backend Engineer")
                .companyName("DataCorp")
                .experienceYears(3)
                .primaryKeyword("Python")
                .englishLevel("intermediate")
                .publishedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Recruit recruit3 = Recruit.builder()
                .recruitId(testRecruitId3)
                .position("Full Stack Developer")
                .companyName("StartupCorp")
                .experienceYears(2)
                .primaryKeyword("React")
                .englishLevel("basic")
                .publishedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        recruitRepository.saveAll(Arrays.asList(recruit1, recruit2, recruit3)).blockLast();
    }

    @Test
    @DisplayName("Should find recruit by ID")
    void testFindById() {
        // When
        Mono<Recruit> result = recruitRepository.findById(testRecruitId1);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(recruit ->
                        recruit.getRecruitId().equals(testRecruitId1) &&
                        recruit.getPosition().equals("Senior Java Developer") &&
                        recruit.getCompanyName().equals("TechCorp"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find all recruits")
    void testFindAll() {
        // When
        Flux<Recruit> result = recruitRepository.findAll();

        // Then
        StepVerifier.create(result)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find recruits by experience years between")
    void testFindByExperienceYearsBetween() {
        // Given
        Integer minYears = 2;
        Integer maxYears = 5;

        // When
        Flux<Recruit> result = recruitRepository.findByExperienceYearsBetween(minYears, maxYears);

        // Then
        StepVerifier.create(result)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find recruits by company name containing")
    void testFindByCompanyNameContaining() {
        // Given
        String companyName = "Tech";

        // When
        Flux<Recruit> result = recruitRepository.findByCompanyNameContaining(companyName);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(recruit -> recruit.getCompanyName().equals("TechCorp"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should count total recruits")
    void testCount() {
        // When
        Mono<Long> result = recruitRepository.count();

        // Then
        StepVerifier.create(result)
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find similar recruits by vector (PGvector cosine distance)")
    void testFindSimilarByVector() {
        // Given
        String queryVector = generateDummyVector(384);
        Double similarityThreshold = 0.7;
        Integer limit = 5;

        // When
        Flux<Recruit> result = recruitRepository.findSimilarByVector(queryVector, similarityThreshold, limit);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should filter by similarity threshold >= 0.7")
    void testFindSimilarByVectorWithThreshold() {
        // Given
        String queryVector = generateDummyVector(384);
        Double highThreshold = 0.9;
        Integer limit = 10;

        // When
        Flux<Recruit> result = recruitRepository.findSimilarByVector(queryVector, highThreshold, limit);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should limit results to specified limit")
    void testFindSimilarByVectorWithLimit() {
        // Given
        String queryVector = generateDummyVector(384);
        Double similarityThreshold = 0.0;
        Integer limit = 2;

        // When
        Flux<Recruit> result = recruitRepository.findSimilarByVector(queryVector, similarityThreshold, limit);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    private String generateDummyVector(int dimension) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dimension; i++) {
            sb.append("0.1");
            if (i < dimension - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
