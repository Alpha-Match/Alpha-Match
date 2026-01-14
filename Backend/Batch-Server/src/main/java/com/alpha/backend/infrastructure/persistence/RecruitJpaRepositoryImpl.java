package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Custom implementation for RecruitJpaRepository
 *
 * Provides optimized batch upsert using JdbcTemplate
 * Spring Data JPA will automatically detect this implementation
 * and merge it with the RecruitJpaRepository interface
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitJpaRepositoryImpl implements RecruitJpaRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Optimized batch upsert using JDBC Template
     * Single query for all entities instead of N queries
     *
     * @param entities List of RecruitEntity to upsert
     */
    @Override
    @Transactional
    public void upsertAllOptimized(List<RecruitEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        log.debug("[Recruit Repository] Batch upserting {} entities", entities.size());

        // Build dynamic SQL for batch upsert
        StringBuilder sql = new StringBuilder("""
            INSERT INTO recruit (
                recruit_id, position, company_name, experience_years,
                primary_keyword, english_level, published_at, created_at, updated_at
            ) VALUES
            """);

        // Add VALUES clause for each entity
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?, ?, ?, ?, ?, COALESCE(?, NOW()), COALESCE(?, NOW()))");
        }

        sql.append("""

            ON CONFLICT (recruit_id)
            DO UPDATE SET
                position = EXCLUDED.position,
                company_name = EXCLUDED.company_name,
                experience_years = EXCLUDED.experience_years,
                primary_keyword = EXCLUDED.primary_keyword,
                english_level = EXCLUDED.english_level,
                published_at = EXCLUDED.published_at,
                updated_at = NOW()
            """);

        // Prepare parameters
        Object[] params = new Object[entities.size() * 9];
        int idx = 0;
        for (RecruitEntity entity : entities) {
            params[idx++] = entity.getRecruitId();
            params[idx++] = entity.getPosition();
            params[idx++] = entity.getCompanyName();
            params[idx++] = entity.getExperienceYears();
            params[idx++] = entity.getPrimaryKeyword();
            params[idx++] = entity.getEnglishLevel();
            params[idx++] = entity.getPublishedAt() != null ? Timestamp.from(entity.getPublishedAt().toInstant()) : null;
            params[idx++] = entity.getCreatedAt() != null ? Timestamp.from(entity.getCreatedAt().toInstant()) : null;
            params[idx++] = entity.getUpdatedAt() != null ? Timestamp.from(entity.getUpdatedAt().toInstant()) : null;
        }

        int rowsAffected = jdbcTemplate.update(sql.toString(), params);
        log.debug("[Recruit Repository] Batch upsert completed, rows affected: {}", rowsAffected);
    }
}
