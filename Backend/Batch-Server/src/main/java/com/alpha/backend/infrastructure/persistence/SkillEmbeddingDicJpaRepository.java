package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.skilldic.entity.SkillEmbeddingDicEntity;
import com.alpha.backend.domain.skilldic.repository.SkillEmbeddingDicRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Skill Embedding Dictionary JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 *
 * SQL 매핑 (v2 스키마):
 * - skill_embedding_dic 테이블
 * - PK: skill_id (UUID, auto-generated)
 * - UK: skill (TEXT, UNIQUE)
 * - FK: category_id → skill_category_dic
 * - Vector: skill_vector (384d)
 */
@Repository
public interface SkillEmbeddingDicJpaRepository
        extends JpaRepository<SkillEmbeddingDicEntity, UUID>, SkillEmbeddingDicRepository {

    /**
     * Batch Upsert using Native Query (v2)
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     *
     * UK: skill (TEXT UNIQUE) - 비즈니스 키
     * PK: skill_id (UUID) - 자동 생성
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO skill_embedding_dic (skill_id, category_id, skill, skill_vector, created_at, updated_at)
        VALUES (
            COALESCE(:#{#entity.skillId}, gen_random_uuid()),
            :#{#entity.categoryId},
            :#{#entity.skill},
            CAST(:#{#entity.skillVector.toString()} AS vector(384)),
            COALESCE(:#{#entity.createdAt}, NOW()),
            NOW()
        )
        ON CONFLICT (skill)
        DO UPDATE SET
            category_id = EXCLUDED.category_id,
            skill_vector = EXCLUDED.skill_vector,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") SkillEmbeddingDicEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    @Transactional
    default void upsertAll(List<SkillEmbeddingDicEntity> entities) {
        entities.forEach(this::upsert);
    }
}
