package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;
import com.alpha.backend.domain.recruit.repository.RecruitDescriptionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * RecruitDescription Repository - Infrastructure Layer (JPA Adapter)
 *
 * v2 Schema: recruit_description 테이블
 * - Native Query 기반 Upsert (ON CONFLICT)
 * - Batch Upsert 지원
 */
@Repository
public interface RecruitDescriptionJpaRepository
        extends JpaRepository<RecruitDescriptionEntity, UUID>, RecruitDescriptionRepository, RecruitDescriptionJpaRepositoryCustom {

    /**
     * RecruitDescription 단건 Upsert (Native Query)
     *
     * @param entity RecruitDescription Entity
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO recruit_description (
            recruit_id, long_description, description_lang, created_at, updated_at
        )
        VALUES (
            :#{#entity.recruitId},
            :#{#entity.longDescription},
            :#{#entity.descriptionLang},
            COALESCE(:#{#entity.createdAt}, NOW()),
            COALESCE(:#{#entity.updatedAt}, NOW())
        )
        ON CONFLICT (recruit_id)
        DO UPDATE SET
            long_description = EXCLUDED.long_description,
            description_lang = EXCLUDED.description_lang,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") RecruitDescriptionEntity entity);

    /**
     * RecruitDescription 배치 Upsert (Delegates to optimized implementation)
     *
     * Spring Batch에서 Chunk 단위로 호출
     *
     * @param entities RecruitDescription Entity 리스트
     */
    @Override
    @Transactional
    default void upsertAll(List<RecruitDescriptionEntity> entities) {
        upsertAllOptimized(entities);
    }
}
