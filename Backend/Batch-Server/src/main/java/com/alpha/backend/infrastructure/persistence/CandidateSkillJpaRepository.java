package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.candidate.entity.CandidateSkillEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillId;
import com.alpha.backend.domain.candidate.repository.CandidateSkillRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Candidate Skill JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 *
 * 복합 PK: (candidate_id, skill)
 */
@Repository
public interface CandidateSkillJpaRepository
        extends JpaRepository<CandidateSkillEntity, CandidateSkillId>, CandidateSkillRepository {

    /**
     * Batch Upsert using Native Query
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     *
     * SQL 매핑:
     * - candidate_skill 테이블 (candidate_id, skill) 복합 PK
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO candidate_skill (candidate_id, skill, updated_at)
        VALUES (:#{#entity.candidateId}, :#{#entity.skill}, NOW())
        ON CONFLICT (candidate_id, skill)
        DO UPDATE SET
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") CandidateSkillEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    @Transactional
    default void upsertAll(List<CandidateSkillEntity> entities) {
        entities.forEach(this::upsert);
    }
}
