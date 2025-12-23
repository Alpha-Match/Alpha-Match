package com.alpha.backend.domain.recruit.repository;

import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RecruitSkill (채용 공고 - 기술 스택 매핑) Repository - Domain Layer Interface (Port)
 *
 * v2 Schema: recruit_skill 테이블
 * - recruit_id (UUID PK/FK)
 * - skill (TEXT PK)
 * - created_at, updated_at
 *
 * Composite PK: (recruit_id, skill)
 * Relationship: recruit (1) ↔ (N) recruit_skill (CASCADE DELETE)
 */
public interface RecruitSkillRepository {

    /**
     * RecruitSkill 단건 조회 (Composite PK)
     *
     * @param id RecruitSkillId (composite key)
     * @return RecruitSkill Entity (Optional)
     */
    Optional<RecruitSkillEntity> findById(RecruitSkillId id);

    /**
     * 특정 Recruit의 모든 Skill 조회
     *
     * @param recruitId 채용 공고 ID
     * @return RecruitSkill Entity 리스트
     */
    List<RecruitSkillEntity> findByRecruitId(UUID recruitId);

    /**
     * RecruitSkill 전체 조회
     *
     * @return RecruitSkill Entity 리스트
     */
    List<RecruitSkillEntity> findAll();

    /**
     * RecruitSkill 단건 Upsert (INSERT or UPDATE)
     *
     * @param entity RecruitSkill Entity
     */
    void upsert(RecruitSkillEntity entity);

    /**
     * RecruitSkill 배치 Upsert (Bulk INSERT or UPDATE)
     *
     * Spring Batch Writer에서 사용
     *
     * @param entities RecruitSkill Entity 리스트
     */
    void upsertAll(List<RecruitSkillEntity> entities);

    /**
     * RecruitSkill 삭제 (Composite PK)
     *
     * @param id RecruitSkillId (composite key)
     */
    void deleteById(RecruitSkillId id);

    /**
     * 특정 Recruit의 모든 Skill 삭제
     *
     * @param recruitId 채용 공고 ID
     */
    void deleteByRecruitId(UUID recruitId);

    /**
     * RecruitSkill 존재 여부 확인
     *
     * @param id RecruitSkillId (composite key)
     * @return 존재 여부
     */
    boolean existsById(RecruitSkillId id);
}
