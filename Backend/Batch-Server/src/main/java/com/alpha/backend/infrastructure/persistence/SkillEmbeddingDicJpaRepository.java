package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.skilldic.entity.SkillEmbeddingDicEntity;
import com.alpha.backend.domain.skilldic.repository.SkillEmbeddingDicRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Skill Embedding Dictionary JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 *
 * SQL 매핑:
 * - skill_embedding_dic 테이블 (skill PK, position_category, skill_vector 768d)
 */
@Repository
public interface SkillEmbeddingDicJpaRepository
        extends JpaRepository<SkillEmbeddingDicEntity, String>, SkillEmbeddingDicRepository {

    /**
     * Batch Upsert using Native Query
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     *
     * PK: skill (VARCHAR(50))
     * Vector: VECTOR(384)
     */
    @Override
    @Modifying
    @Query(value = """
        INSERT INTO skill_embedding_dic (skill, position_category, skill_vector, created_at, updated_at)
        VALUES (
            :#{#entity.skill},
            :#{#entity.positionCategory},
            CAST(:#{#entity.skillVector.toString()} AS vector(384)),
            COALESCE(:#{#entity.createdAt}, NOW()),
            NOW()
        )
        ON CONFLICT (skill)
        DO UPDATE SET
            position_category = EXCLUDED.position_category,
            skill_vector = EXCLUDED.skill_vector,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") SkillEmbeddingDicEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    default void upsertAll(List<SkillEmbeddingDicEntity> entities) {
        entities.forEach(this::upsert);
    }
}
