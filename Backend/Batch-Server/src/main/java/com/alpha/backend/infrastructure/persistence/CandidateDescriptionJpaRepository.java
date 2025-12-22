package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.candidate.entity.CandidateDescriptionEntity;
import com.alpha.backend.domain.candidate.repository.CandidateDescriptionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CandidateDescription Repository - Infrastructure Layer (JPA Adapter)
 *
 * v2 Schema: candidate_description 테이블
 * - Native Query 기반 Upsert (ON CONFLICT)
 * - Batch Upsert 지원
 */
@Repository
public interface CandidateDescriptionJpaRepository
        extends JpaRepository<CandidateDescriptionEntity, UUID>, CandidateDescriptionRepository {

    /**
     * CandidateDescription 단건 Upsert (Native Query)
     *
     * @param entity CandidateDescription Entity
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO candidate_description (
            candidate_id, original_resume, resume_lang, created_at, updated_at
        )
        VALUES (
            :#{#entity.candidateId},
            :#{#entity.originalResume},
            :#{#entity.resumeLang},
            COALESCE(:#{#entity.createdAt}, NOW()),
            COALESCE(:#{#entity.updatedAt}, NOW())
        )
        ON CONFLICT (candidate_id)
        DO UPDATE SET
            original_resume = EXCLUDED.original_resume,
            resume_lang = EXCLUDED.resume_lang,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") CandidateDescriptionEntity entity);

    /**
     * CandidateDescription 배치 Upsert (Iterative)
     *
     * Spring Batch에서 Chunk 단위로 호출
     *
     * @param entities CandidateDescription Entity 리스트
     */
    @Override
    @Transactional
    default void upsertAll(List<CandidateDescriptionEntity> entities) {
        entities.forEach(this::upsert);
    }
}
