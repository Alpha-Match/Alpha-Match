package com.alpha.backend.domain.skilldic.repository;

import com.alpha.backend.domain.skilldic.entity.SkillCategoryDicEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SkillCategoryDic (기술 스택 카테고리 사전) Repository - Domain Layer Interface (Port)
 *
 * v2 Schema: skill_category_dic 테이블
 * - category_id (UUID PK) - 자동 생성
 * - category (TEXT UNIQUE) - 카테고리명 (예: "Backend", "Frontend", "Database")
 * - created_at, updated_at
 *
 * Relationship: skill_category_dic (1) ↔ (N) skill_embedding_dic
 */
public interface SkillCategoryDicRepository {

    /**
     * SkillCategoryDic 단건 조회 (ID)
     *
     * @param categoryId 카테고리 ID
     * @return SkillCategoryDic Entity (Optional)
     */
    Optional<SkillCategoryDicEntity> findById(UUID categoryId);

    /**
     * SkillCategoryDic 단건 조회 (카테고리명)
     *
     * @param category 카테고리명
     * @return SkillCategoryDic Entity (Optional)
     */
    Optional<SkillCategoryDicEntity> findByCategory(String category);

    /**
     * SkillCategoryDic 전체 조회
     *
     * @return SkillCategoryDic Entity 리스트
     */
    List<SkillCategoryDicEntity> findAll();

    /**
     * SkillCategoryDic 단건 Upsert (INSERT or UPDATE)
     *
     * @param entity SkillCategoryDic Entity
     */
    void upsert(SkillCategoryDicEntity entity);

    /**
     * SkillCategoryDic 배치 Upsert (Bulk INSERT or UPDATE)
     *
     * Spring Batch Writer에서 사용
     *
     * @param entities SkillCategoryDic Entity 리스트
     */
    void upsertAll(List<SkillCategoryDicEntity> entities);

    /**
     * SkillCategoryDic 삭제
     *
     * @param categoryId 카테고리 ID
     */
    void deleteById(UUID categoryId);

    /**
     * SkillCategoryDic 존재 여부 확인 (ID)
     *
     * @param categoryId 카테고리 ID
     * @return 존재 여부
     */
    boolean existsById(UUID categoryId);

    /**
     * SkillCategoryDic 존재 여부 확인 (카테고리명)
     *
     * @param category 카테고리명
     * @return 존재 여부
     */
    boolean existsByCategory(String category);
}
