package com.alpha.backend.domain.recruit.repository;

import com.alpha.backend.domain.recruit.entity.RecruitDescriptionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RecruitDescription (채용 공고 상세 설명) Repository - Domain Layer Interface (Port)
 *
 * v2 Schema: recruit_description 테이블
 * - recruit_id (UUID PK/FK) - 1:1 with recruit
 * - long_description (TEXT) - 원본 마크다운
 * - description_lang (TEXT) - 언어 코드
 * - created_at, updated_at
 *
 * Relationship: recruit (1) ↔ (1) recruit_description (CASCADE DELETE)
 */
public interface RecruitDescriptionRepository {

    /**
     * RecruitDescription 단건 조회
     *
     * @param recruitId 채용 공고 ID
     * @return RecruitDescription Entity (Optional)
     */
    Optional<RecruitDescriptionEntity> findById(UUID recruitId);

    /**
     * RecruitDescription 전체 조회
     *
     * @return RecruitDescription Entity 리스트
     */
    List<RecruitDescriptionEntity> findAll();

    /**
     * RecruitDescription 단건 Upsert (INSERT or UPDATE)
     *
     * @param entity RecruitDescription Entity
     */
    void upsert(RecruitDescriptionEntity entity);

    /**
     * RecruitDescription 배치 Upsert (Bulk INSERT or UPDATE)
     *
     * Spring Batch Writer에서 사용
     *
     * @param entities RecruitDescription Entity 리스트
     */
    void upsertAll(List<RecruitDescriptionEntity> entities);

    /**
     * RecruitDescription 삭제
     *
     * @param recruitId 채용 공고 ID
     */
    void deleteById(UUID recruitId);

    /**
     * RecruitDescription 존재 여부 확인
     *
     * @param recruitId 채용 공고 ID
     * @return 존재 여부
     */
    boolean existsById(UUID recruitId);
}
