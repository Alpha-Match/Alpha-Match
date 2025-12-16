package com.alpha.backend.domain.recruit.repository;

import com.alpha.backend.domain.recruit.entity.RecruitMetadataEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recruit Metadata Repository (Domain Interface)
 * 채용 공고 메타데이터에 대한 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface RecruitMetadataRepository {

    /**
     * 단일 엔티티 Upsert
     */
    void upsert(RecruitMetadataEntity entity);

    /**
     * 다수 엔티티 Upsert
     */
    void upsertAll(List<RecruitMetadataEntity> entities);
}
