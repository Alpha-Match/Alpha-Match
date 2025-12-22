package com.alpha.backend.domain.recruit.repository;

import com.alpha.backend.domain.recruit.entity.RecruitEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recruit (채용 공고 메타데이터) Repository - Domain Layer Interface (Port)
 *
 * v2 Schema: recruit 테이블 (4-table 구조의 Aggregate Root)
 * - recruit_id (UUID PK)
 * - position, company_name, experience_years (메타데이터)
 * - primary_keyword, english_level
 * - published_at, created_at, updated_at
 */
public interface RecruitRepository {

    /**
     * Recruit 단건 조회
     *
     * @param recruitId 채용 공고 ID
     * @return Recruit Entity (Optional)
     */
    Optional<RecruitEntity> findById(UUID recruitId);

    /**
     * Recruit 전체 조회
     *
     * @return Recruit Entity 리스트
     */
    List<RecruitEntity> findAll();

    /**
     * Recruit 단건 Upsert (INSERT or UPDATE)
     *
     * @param entity Recruit Entity
     */
    void upsert(RecruitEntity entity);

    /**
     * Recruit 배치 Upsert (Bulk INSERT or UPDATE)
     *
     * Spring Batch Writer에서 사용
     *
     * @param entities Recruit Entity 리스트
     */
    void upsertAll(List<RecruitEntity> entities);

    /**
     * Recruit 삭제
     *
     * @param recruitId 채용 공고 ID
     */
    void deleteById(UUID recruitId);

    /**
     * Recruit 존재 여부 확인
     *
     * @param recruitId 채용 공고 ID
     * @return 존재 여부
     */
    boolean existsById(UUID recruitId);
}
