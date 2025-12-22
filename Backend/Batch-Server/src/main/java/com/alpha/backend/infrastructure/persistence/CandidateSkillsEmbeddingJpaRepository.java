package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.candidate.entity.CandidateSkillsEmbeddingEntity;
import com.alpha.backend.domain.candidate.repository.CandidateSkillsEmbeddingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Candidate Skills Embedding JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 *
 * SQL 매핑:
 * - candidate_skills_embedding 테이블 (candidate_id PK, skills[], skills_vector 768d)
 */
@Repository
public interface CandidateSkillsEmbeddingJpaRepository
        extends JpaRepository<CandidateSkillsEmbeddingEntity, UUID>, CandidateSkillsEmbeddingRepository {

    /**
     * Batch Upsert using Native Query
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     *
     * PostgreSQL 배열 타입 처리:
     * - skills: TEXT[]
     * - skills_vector: VECTOR(384)
     */
    @Override
    @Modifying
    @Query(value = """
        INSERT INTO candidate_skills_embedding (
            candidate_id, skills, skills_vector, created_at, updated_at
        )
        VALUES (
            :#{#entity.candidateId},
            :#{#entity.skills},
            CAST(:#{#entity.skillsVector.toString()} AS vector(384)),
            COALESCE(:#{#entity.createdAt}, NOW()),
            NOW()
        )
        ON CONFLICT (candidate_id)
        DO UPDATE SET
            skills = EXCLUDED.skills,
            skills_vector = EXCLUDED.skills_vector,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") CandidateSkillsEmbeddingEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    default void upsertAll(List<CandidateSkillsEmbeddingEntity> entities) {
        entities.forEach(this::upsert);
    }
}
