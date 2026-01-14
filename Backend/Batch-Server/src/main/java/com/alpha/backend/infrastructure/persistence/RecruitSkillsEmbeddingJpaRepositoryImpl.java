package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitSkillsEmbeddingEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Custom implementation for RecruitSkillsEmbeddingJpaRepository
 *
 * Handles optimized batch upsert with vector embeddings
 * Important: Vector dimension is configurable (currently 1536, migrating to 1000+)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitSkillsEmbeddingJpaRepositoryImpl implements RecruitSkillsEmbeddingJpaRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void upsertAllOptimized(List<RecruitSkillsEmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        log.debug("[RecruitSkillsEmbedding Repository] Batch upserting {} entities", entities.size());

        // Get vector dimension from first entity (all should have same dimension)
        int vectorDimension = entities.get(0).getSkillsVector() != null ?
                entities.get(0).getSkillsVector().toArray().length : 384;

        StringBuilder sql = new StringBuilder("""
            INSERT INTO recruit_skills_embedding (
                recruit_id, skills, skills_vector, created_at, updated_at
            ) VALUES
            """);

        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) sql.append(", ");
            // Use dynamic vector dimension in CAST
            sql.append("(?, ?, CAST(? AS vector(").append(vectorDimension).append(")), COALESCE(?, NOW()), COALESCE(?, NOW()))");
        }

        sql.append("""

            ON CONFLICT (recruit_id)
            DO UPDATE SET
                skills = EXCLUDED.skills,
                skills_vector = EXCLUDED.skills_vector,
                updated_at = NOW()
            """);

        Object[] params = new Object[entities.size() * 5];
        int idx = 0;
        for (RecruitSkillsEmbeddingEntity entity : entities) {
            params[idx++] = entity.getRecruitId();
            params[idx++] = entity.getSkills();
            params[idx++] = entity.getSkillsVector() != null ? entity.getSkillsVector().toString() : null;
            params[idx++] = entity.getCreatedAt() != null ? Timestamp.from(entity.getCreatedAt().toInstant()) : null;
            params[idx++] = entity.getUpdatedAt() != null ? Timestamp.from(entity.getUpdatedAt().toInstant()) : null;
        }

        int rowsAffected = jdbcTemplate.update(sql.toString(), params);
        log.debug("[RecruitSkillsEmbedding Repository] Batch upsert completed, rows affected: {}, vector dimension: {}",
                rowsAffected, vectorDimension);
    }
}
