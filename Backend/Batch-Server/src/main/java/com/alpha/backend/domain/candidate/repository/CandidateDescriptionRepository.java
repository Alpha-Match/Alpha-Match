package com.alpha.backend.domain.candidate.repository;

import com.alpha.backend.domain.candidate.entity.CandidateDescriptionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CandidateDescription (후보자 상세 설명) Repository - Domain Layer Interface (Port)
 *
 * v2 Schema: candidate_description 테이블
 * - candidate_id (UUID PK/FK) - 1:1 with candidate
 * - original_resume (TEXT) - 원본 이력서 마크다운
 * - resume_lang (TEXT) - 언어 코드
 * - created_at, updated_at
 *
 * Relationship: candidate (1) ↔ (1) candidate_description (CASCADE DELETE)
 */
public interface CandidateDescriptionRepository {

    /**
     * CandidateDescription 단건 조회
     *
     * @param candidateId 후보자 ID
     * @return CandidateDescription Entity (Optional)
     */
    Optional<CandidateDescriptionEntity> findById(UUID candidateId);

    /**
     * CandidateDescription 전체 조회
     *
     * @return CandidateDescription Entity 리스트
     */
    List<CandidateDescriptionEntity> findAll();

    /**
     * CandidateDescription 단건 Upsert (INSERT or UPDATE)
     *
     * @param entity CandidateDescription Entity
     */
    void upsert(CandidateDescriptionEntity entity);

    /**
     * CandidateDescription 배치 Upsert (Bulk INSERT or UPDATE)
     *
     * Spring Batch Writer에서 사용
     *
     * @param entities CandidateDescription Entity 리스트
     */
    void upsertAll(List<CandidateDescriptionEntity> entities);

    /**
     * CandidateDescription 삭제
     *
     * @param candidateId 후보자 ID
     */
    void deleteById(UUID candidateId);

    /**
     * CandidateDescription 존재 여부 확인
     *
     * @param candidateId 후보자 ID
     * @return 존재 여부
     */
    boolean existsById(UUID candidateId);
}
