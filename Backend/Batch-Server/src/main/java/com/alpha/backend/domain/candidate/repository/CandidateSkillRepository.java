package com.alpha.backend.domain.candidate.repository;

import com.alpha.backend.domain.candidate.entity.CandidateSkillEntity;
import com.alpha.backend.domain.candidate.entity.CandidateSkillId;

import java.util.List;

/**
 * Candidate Skill Repository (Domain Interface)
 * 후보자-스킬 매핑 정보에 대한 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface CandidateSkillRepository {

    /**
     * 단일 엔티티 Upsert
     */
    void upsert(CandidateSkillEntity entity);

    /**
     * 다수 엔티티 Upsert
     */
    void upsertAll(List<CandidateSkillEntity> entities);
}
