package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.skilldic.entity.SkillCategoryDicEntity;
import com.alpha.backend.domain.skilldic.repository.SkillCategoryDicRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SkillCategoryDic Repository - Infrastructure Layer (JPA Adapter)
 *
 * v2 Schema: skill_category_dic 테이블
 * - UUID 자동 생성 (DEFAULT gen_random_uuid())
 * - Native Query 기반 Upsert (ON CONFLICT)
 * - Batch Upsert 지원
 */
@Repository
public interface SkillCategoryDicJpaRepository
        extends JpaRepository<SkillCategoryDicEntity, UUID>, SkillCategoryDicRepository {

    /**
     * SkillCategoryDic 단건 조회 (카테고리명)
     *
     * @param category 카테고리명
     * @return SkillCategoryDic Entity (Optional)
     */
    @Override
    Optional<SkillCategoryDicEntity> findByCategory(String category);

    /**
     * SkillCategoryDic 존재 여부 확인 (카테고리명)
     *
     * @param category 카테고리명
     * @return 존재 여부
     */
    @Override
    boolean existsByCategory(String category);

    /**
     * SkillCategoryDic 단건 Upsert (Native Query)
     *
     * category_id가 NULL이면 DB에서 자동 생성
     *
     * @param entity SkillCategoryDic Entity
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO skill_category_dic (
            category_id, category, created_at, updated_at
        )
        VALUES (
            COALESCE(:#{#entity.categoryId}, gen_random_uuid()),
            :#{#entity.category},
            COALESCE(:#{#entity.createdAt}, NOW()),
            COALESCE(:#{#entity.updatedAt}, NOW())
        )
        ON CONFLICT (category)
        DO UPDATE SET
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") SkillCategoryDicEntity entity);

    /**
     * SkillCategoryDic 배치 Upsert (Iterative)
     *
     * Spring Batch에서 Chunk 단위로 호출
     *
     * @param entities SkillCategoryDic Entity 리스트
     */
    @Override
    @Transactional
    default void upsertAll(List<SkillCategoryDicEntity> entities) {
        entities.forEach(this::upsert);
    }
}
