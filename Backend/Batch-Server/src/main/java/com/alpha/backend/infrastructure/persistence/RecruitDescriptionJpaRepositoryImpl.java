package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Custom implementation for RecruitDescriptionJpaRepository
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitDescriptionJpaRepositoryImpl implements RecruitDescriptionJpaRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void upsertAllOptimized(List<RecruitDescriptionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        log.debug("[RecruitDescription Repository] Batch upserting {} entities", entities.size());

        StringBuilder sql = new StringBuilder("""
            INSERT INTO recruit_description (
                recruit_id, long_description, description_lang, created_at, updated_at
            ) VALUES
            """);

        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?, COALESCE(?, NOW()), COALESCE(?, NOW()))");
        }

        sql.append("""

            ON CONFLICT (recruit_id)
            DO UPDATE SET
                long_description = EXCLUDED.long_description,
                description_lang = EXCLUDED.description_lang,
                updated_at = NOW()
            """);

        Object[] params = new Object[entities.size() * 5];
        int idx = 0;
        for (RecruitDescriptionEntity entity : entities) {
            params[idx++] = entity.getRecruitId();
            params[idx++] = entity.getLongDescription();
            params[idx++] = entity.getDescriptionLang();
            params[idx++] = entity.getCreatedAt() != null ? Timestamp.from(entity.getCreatedAt().toInstant()) : null;
            params[idx++] = entity.getUpdatedAt() != null ? Timestamp.from(entity.getUpdatedAt().toInstant()) : null;
        }

        int rowsAffected = jdbcTemplate.update(sql.toString(), params);
        log.debug("[RecruitDescription Repository] Batch upsert completed, rows affected: {}", rowsAffected);
    }
}
