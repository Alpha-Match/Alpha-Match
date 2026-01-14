package com.alpha.backend.domain.candidate.repository;

import com.alpha.backend.domain.candidate.entity.CandidateEntity;

import java.util.List;
import java.util.UUID;

/**
 * Candidate Repository (Domain Interface)
 * 후보자 기본 정보에 대한 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface CandidateRepository {

    /**
     * 단일 엔티티 Upsert
     */
    void upsert(CandidateEntity entity);

    /**
     * 다수 엔티티 Upsert
     */
    void upsertAll(List<CandidateEntity> entities);
}
