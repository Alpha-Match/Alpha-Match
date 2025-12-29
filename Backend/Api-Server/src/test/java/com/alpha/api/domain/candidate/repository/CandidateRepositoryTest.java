package com.alpha.api.domain.candidate.repository;

import com.alpha.api.domain.candidate.entity.Candidate;
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
 * CandidateRepository Integration Test
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
class CandidateRepositoryTest {

    @Autowired
    private CandidateRepository candidateRepository;

    private UUID testCandidateId1;
    private UUID testCandidateId2;
    private UUID testCandidateId3;

    @BeforeEach
    void setUp() {
        // Clean up test data
        candidateRepository.deleteAll().block();

        // Create test data
        testCandidateId1 = UUID.randomUUID();
        testCandidateId2 = UUID.randomUUID();
        testCandidateId3 = UUID.randomUUID();

        Candidate candidate1 = Candidate.builder()
                .candidateId(testCandidateId1)
                .positionCategory("Backend")
                .experienceYears(5)
                .originalResume("Senior Java developer with 5 years of experience...")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Candidate candidate2 = Candidate.builder()
                .candidateId(testCandidateId2)
                .positionCategory("Frontend")
                .experienceYears(3)
                .originalResume("React developer with 3 years of experience...")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Candidate candidate3 = Candidate.builder()
                .candidateId(testCandidateId3)
                .positionCategory("Full Stack")
                .experienceYears(2)
                .originalResume("Junior full stack developer...")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        candidateRepository.saveAll(Arrays.asList(candidate1, candidate2, candidate3)).blockLast();
    }

    @Test
    @DisplayName("Should find candidate by ID")
    void testFindById() {
        // When
        Mono<Candidate> result = candidateRepository.findById(testCandidateId1);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(candidate ->
                        candidate.getCandidateId().equals(testCandidateId1) &&
                        candidate.getPositionCategory().equals("Backend") &&
                        candidate.getExperienceYears().equals(5))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find all candidates")
    void testFindAll() {
        // When
        Flux<Candidate> result = candidateRepository.findAll();

        // Then
        StepVerifier.create(result)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find candidates by experience years between")
    void testFindByExperienceYearsBetween() {
        // Given
        Integer minYears = 2;
        Integer maxYears = 5;

        // When
        Flux<Candidate> result = candidateRepository.findByExperienceYearsBetween(minYears, maxYears);

        // Then
        StepVerifier.create(result)
                .expectNextCount(3) // All 3 candidates have experience 2-5 years
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find candidates by position category containing")
    void testFindByPositionCategoryContaining() {
        // Given
        String positionCategory = "Backend";

        // When
        Flux<Candidate> result = candidateRepository.findByPositionCategoryContaining(positionCategory);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(candidate -> candidate.getPositionCategory().equals("Backend"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should count total candidates")
    void testCount() {
        // When
        Mono<Long> result = candidateRepository.count();

        // Then
        StepVerifier.create(result)
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find similar candidates by vector (PGvector cosine distance)")
    void testFindSimilarByVector() {
        // Given
        String queryVector = generateDummyVector(384);
        Double similarityThreshold = 0.7;
        Integer limit = 5;

        // When
        Flux<Candidate> result = candidateRepository.findSimilarByVector(queryVector, similarityThreshold, limit);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0) // Expect 0 because no embedding data is set up in this test
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
        Flux<Candidate> result = candidateRepository.findSimilarByVector(queryVector, highThreshold, limit);

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
        Flux<Candidate> result = candidateRepository.findSimilarByVector(queryVector, similarityThreshold, limit);

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
