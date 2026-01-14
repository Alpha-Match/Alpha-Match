package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitEntity;
import com.alpha.backend.domain.recruit.repository.RecruitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Recruit Repository - Infrastructure Layer (JPA Adapter)
 *
 * v2 Schema: recruit 테이블
 * - Native Query 기반 Upsert (ON CONFLICT)
 * - Batch Upsert 지원
 */
@Repository
public interface RecruitJpaRepository
        extends JpaRepository<RecruitEntity, UUID>, RecruitRepository, RecruitJpaRepositoryCustom {

    /**
     * Recruit 단건 Upsert (Native Query)
     *
     * @param entity Recruit Entity
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO recruit (
            recruit_id, position, company_name, experience_years,
            primary_keyword, english_level, published_at, created_at, updated_at
        )
        VALUES (
            :#{#entity.recruitId},
            :#{#entity.position},
            :#{#entity.companyName},
            :#{#entity.experienceYears},
            :#{#entity.primaryKeyword},
            :#{#entity.englishLevel},
            :#{#entity.publishedAt},
            COALESCE(:#{#entity.createdAt}, NOW()),
            COALESCE(:#{#entity.updatedAt}, NOW())
        )
        ON CONFLICT (recruit_id)
        DO UPDATE SET
            position = EXCLUDED.position,
            company_name = EXCLUDED.company_name,
            experience_years = EXCLUDED.experience_years,
            primary_keyword = EXCLUDED.primary_keyword,
            english_level = EXCLUDED.english_level,
            published_at = EXCLUDED.published_at,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") RecruitEntity entity);

    /**
     * Recruit 배치 Upsert (Delegates to optimized implementation)
     *
     * Spring Batch에서 Chunk 단위로 호출
     * JDBC Template을 사용하여 단일 쿼리로 전체 배치 처리
     *
     * @param entities Recruit Entity 리스트
     */
    @Override
    @Transactional
    default void upsertAll(List<RecruitEntity> entities) {
        upsertAllOptimized(entities);
    }
}
