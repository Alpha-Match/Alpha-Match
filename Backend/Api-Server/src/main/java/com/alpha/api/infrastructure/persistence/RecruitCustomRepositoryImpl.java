package com.alpha.api.infrastructure.persistence;

import com.alpha.api.application.dto.RecruitSearchResult;
import com.alpha.api.domain.recruit.entity.Recruit;
import com.alpha.api.domain.recruit.repository.RecruitSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recruit Search Repository Implementation (Adapter - Infrastructure Layer)
 * - Implements RecruitSearchRepository (Port) using R2DBC DatabaseClient
 * - Custom implementation for pgvector similarity search
 * - Maps similarity_score from query result to DTO
 */
@Repository
@RequiredArgsConstructor
public class RecruitCustomRepositoryImpl implements RecruitSearchRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<RecruitSearchResult> findSimilarByVectorWithScore(String queryVector, Double similarityThreshold, Integer limit, Integer minYears, Integer maxYears) {
        String sql = """
            SELECT r.recruit_id, r.position, r.company_name, r.experience_years,
                   r.primary_keyword, r.english_level, r.published_at, r.created_at, r.updated_at,
                   (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
            FROM recruit r
            INNER JOIN recruit_skills_embedding rse ON r.recruit_id = rse.recruit_id
            WHERE (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) >= :similarityThreshold
              AND (r.experience_years IS NULL OR (r.experience_years >= :minYears AND r.experience_years <= :maxYears))
            ORDER BY rse.skills_vector <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """;

        return databaseClient.sql(sql)
                .bind("queryVector", queryVector)
                .bind("similarityThreshold", similarityThreshold)
                .bind("limit", limit)
                .bind("minYears", minYears)
                .bind("maxYears", maxYears)
                .map(row -> {
                    // Map Recruit entity
                    Recruit recruit = Recruit.builder()
                            .recruitId(row.get("recruit_id", UUID.class))
                            .position(row.get("position", String.class))
                            .companyName(row.get("company_name", String.class))
                            .experienceYears(row.get("experience_years", Integer.class))
                            .primaryKeyword(row.get("primary_keyword", String.class))
                            .englishLevel(row.get("english_level", String.class))
                            .publishedAt(row.get("published_at", OffsetDateTime.class))
                            .createdAt(row.get("created_at", OffsetDateTime.class))
                            .updatedAt(row.get("updated_at", OffsetDateTime.class))
                            .build();

                    // Map similarity score
                    Double similarityScore = row.get("similarity_score", Double.class);

                    // Build DTO
                    return RecruitSearchResult.builder()
                            .recruit(recruit)
                            .similarityScore(similarityScore)
                            .build();
                })
                .all();
    }
}
