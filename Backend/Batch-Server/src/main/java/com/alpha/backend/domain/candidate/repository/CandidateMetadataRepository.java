package com.alpha.backend.domain.candidate.repository;

import com.alpha.backend.domain.candidate.entity.CandidateMetadataEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Candidate Metadata Repository (Domain Interface)
 * 후보자 메타데이터에 대한 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface CandidateMetadataRepository {

    /**
     * 단일 엔티티 Upsert
     */
    void upsert(CandidateMetadataEntity entity);

    /**
     * 다수 엔티티 Upsert
     */
    void upsertAll(List<CandidateMetadataEntity> entities);
}
