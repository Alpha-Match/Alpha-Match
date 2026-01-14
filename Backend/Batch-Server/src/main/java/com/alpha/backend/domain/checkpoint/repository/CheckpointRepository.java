package com.alpha.backend.domain.checkpoint.repository;

import com.alpha.backend.domain.checkpoint.entity.CheckpointEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Checkpoint Repository (Domain Interface)
 * 배치 처리 체크포인트를 관리하는 도메인 레포지토리 인터페이스
 *
 * Clean Architecture: Domain 계층의 추상화 (Port)
 * 실제 구현은 Infrastructure 계층에서 담당
 */
public interface CheckpointRepository {

    /**
     * 도메인별 체크포인트 조회
     */
    Optional<CheckpointEntity> findByDomain(String domain);

    /**
     * 도메인별 마지막 처리 UUID 조회
     */
    Optional<UUID> findLastProcessedUuidByDomain(String domain);

    /**
     * 가장 최근의 체크포인트 조회
     * @deprecated 도메인별 조회 사용 권장
     */
    @Deprecated
    Optional<CheckpointEntity> findLatest();

    /**
     * 마지막 처리된 UUID 조회
     * @deprecated 도메인별 조회 사용 권장
     */
    @Deprecated
    Optional<UUID> findLastProcessedUuid();
}
