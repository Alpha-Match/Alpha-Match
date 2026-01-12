package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.recruit.entity.RecruitSkillEntity;
import com.alpha.backend.domain.recruit.entity.RecruitSkillId;
import com.alpha.backend.domain.recruit.repository.RecruitSkillRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * RecruitSkill Repository - Infrastructure Layer (JPA Adapter)
 *
 * v2 Schema: recruit_skill 테이블
 * - Composite PK: (recruit_id, skill)
 * - Native Query 기반 Upsert (ON CONFLICT)
 * - Batch Upsert 지원
 */
@Repository
public interface RecruitSkillJpaRepository
        extends JpaRepository<RecruitSkillEntity, RecruitSkillId>, RecruitSkillRepository, RecruitSkillJpaRepositoryCustom {

    /**
     * 특정 Recruit의 모든 Skill 조회
     *
     * @param recruitId 채용 공고 ID
     * @return RecruitSkill Entity 리스트
     */
    @Override
    List<RecruitSkillEntity> findByRecruitId(UUID recruitId);

    /**
     * 특정 Recruit의 모든 Skill 삭제
     *
     * @param recruitId 채용 공고 ID
     */
    @Override
    @Transactional
    @Modifying
    @Query("DELETE FROM RecruitSkillEntity r WHERE r.recruitId = :recruitId")
    void deleteByRecruitId(@Param("recruitId") UUID recruitId);

    /**
     * RecruitSkill 단건 Upsert (Native Query)
     *
     * @param entity RecruitSkill Entity
     */
    @Override
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO recruit_skill (
            recruit_id, skill, created_at, updated_at
        )
        VALUES (
            :#{#entity.recruitId},
            :#{#entity.skill},
            COALESCE(:#{#entity.createdAt}, NOW()),
            COALESCE(:#{#entity.updatedAt}, NOW())
        )
        ON CONFLICT (recruit_id, skill)
        DO UPDATE SET
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("entity") RecruitSkillEntity entity);

    /**
     * RecruitSkill 배치 Upsert (Delegates to optimized implementation)
     *
     * Spring Batch에서 Chunk 단위로 호출
     *
     * @param entities RecruitSkill Entity 리스트
     */
    @Override
    @Transactional
    default void upsertAll(List<RecruitSkillEntity> entities) {
        upsertAllOptimized(entities);
    }
}
