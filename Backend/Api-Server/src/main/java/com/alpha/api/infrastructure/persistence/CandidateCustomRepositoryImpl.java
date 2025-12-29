package com.alpha.api.infrastructure.persistence;

import com.alpha.api.application.dto.CandidateSearchResult;
import com.alpha.api.domain.candidate.entity.Candidate;
import com.alpha.api.domain.candidate.repository.CandidateSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Candidate Search Repository Implementation (Adapter - Infrastructure Layer)
 * - Implements CandidateSearchRepository (Port) using R2DBC DatabaseClient
 * - Custom implementation for pgvector similarity search
 * - Maps similarity_score from query result to DTO
 */
@Repository
@RequiredArgsConstructor
public class CandidateCustomRepositoryImpl implements CandidateSearchRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<CandidateSearchResult> findSimilarByVectorWithScore(String queryVector, Double similarityThreshold, Integer limit, Integer minYears, Integer maxYears) {
        String sql = """
            SELECT c.candidate_id, c.position_category, c.experience_years, c.original_resume,
                   c.created_at, c.updated_at,
                   (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
            FROM candidate c
            INNER JOIN candidate_skills_embedding cse ON c.candidate_id = cse.candidate_id
            WHERE (1 - (cse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
              AND (c.experience_years IS NULL OR (c.experience_years >= :minYears AND c.experience_years <= :maxYears))
            ORDER BY cse.skills_vector <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """;

        return databaseClient.sql(sql)
                .bind("queryVector", queryVector)
                .bind("similarityThreshold", similarityThreshold)
                .bind("limit", limit)
                .bind("minYears", minYears)
                .bind("maxYears", maxYears)
                .map(row -> {
                    // Map Candidate entity
                    Candidate candidate = Candidate.builder()
                            .candidateId(row.get("candidate_id", UUID.class))
                            .positionCategory(row.get("position_category", String.class))
                            .experienceYears(row.get("experience_years", Integer.class))
                            .originalResume(row.get("original_resume", String.class))
                            .createdAt(row.get("created_at", OffsetDateTime.class))
                            .updatedAt(row.get("updated_at", OffsetDateTime.class))
                            .build();

                    // Map similarity score
                    Double similarityScore = row.get("similarity_score", Double.class);

                    // Build DTO
                    return CandidateSearchResult.builder()
                            .candidate(candidate)
                            .similarityScore(similarityScore)
                            .build();
                })
                .all();
    }
}
