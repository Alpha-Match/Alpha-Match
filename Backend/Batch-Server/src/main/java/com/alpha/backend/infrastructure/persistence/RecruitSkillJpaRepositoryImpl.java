package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Custom implementation for RecruitSkillJpaRepository
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitSkillJpaRepositoryImpl implements RecruitSkillJpaRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void upsertAllOptimized(List<RecruitSkillEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        log.debug("[RecruitSkill Repository] Batch upserting {} entities", entities.size());

        StringBuilder sql = new StringBuilder("""
            INSERT INTO recruit_skill (
                recruit_id, skill, created_at, updated_at
            ) VALUES
            """);

        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, COALESCE(?, NOW()), COALESCE(?, NOW()))");
        }

        sql.append("""

            ON CONFLICT (recruit_id, skill)
            DO UPDATE SET
                updated_at = NOW()
            """);

        Object[] params = new Object[entities.size() * 4];
        int idx = 0;
        for (RecruitSkillEntity entity : entities) {
            params[idx++] = entity.getRecruitId();
            params[idx++] = entity.getSkill();
            params[idx++] = entity.getCreatedAt() != null ? Timestamp.from(entity.getCreatedAt().toInstant()) : null;
            params[idx++] = entity.getUpdatedAt() != null ? Timestamp.from(entity.getUpdatedAt().toInstant()) : null;
        }

        int rowsAffected = jdbcTemplate.update(sql.toString(), params);
        log.debug("[RecruitSkill Repository] Batch upsert completed, rows affected: {}", rowsAffected);
    }
}
