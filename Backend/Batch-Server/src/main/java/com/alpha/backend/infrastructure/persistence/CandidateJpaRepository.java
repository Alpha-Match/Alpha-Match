package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.candidate.entity.CandidateEntity;
import com.alpha.backend.domain.candidate.repository.CandidateRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Candidate JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 */
@Repository
public interface CandidateJpaRepository
        extends JpaRepository<CandidateEntity, UUID>, CandidateRepository {

    /**
     * Batch Upsert using Native Query
     * ON CONFLICT 구문을 사용하여 충돌 시 업데이트
     *
     * SQL 매핑:
     * - candidate 테이블 (candidate_id, position_category, experience_years, original_resume)
     */
    @Override
    @Modifying
    @Query(value = """
        INSERT INTO candidate (candidate_id, position_category, experience_years, original_resume, updated_at)
        VALUES (:#{#entity.id}, :#{#entity.positionCategory}, :#{#entity.experienceYears},
                :#{#entity.originalResume}, NOW())
        ON CONFLICT (candidate_id)
        DO UPDATE SET
            position_category = EXCLUDED.position_category,
            experience_years = EXCLUDED.experience_years,
            original_resume = EXCLUDED.original_resume,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") CandidateEntity entity);

    /**
     * Batch Upsert for multiple entities
     */
    @Override
    default void upsertAll(List<CandidateEntity> entities) {
        entities.forEach(this::upsert);
    }
}
