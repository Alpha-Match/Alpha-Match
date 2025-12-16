package com.alpha.backend.infrastructure.persistence;

import com.alpha.backend.domain.checkpoint.entity.CheckpointEntity;
import com.alpha.backend.domain.checkpoint.repository.CheckpointRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Checkpoint JPA Repository (Infrastructure Adapter)
 * Domain Repository를 구현하는 JPA Repository
 *
 * Clean Architecture: Infrastructure 계층의 Adapter
 */
@Repository
public interface CheckpointJpaRepository
        extends JpaRepository<CheckpointEntity, String>, CheckpointRepository {

    /**
     * 도메인별 체크포인트 조회 (PK 조회)
     */
    @Override
    Optional<CheckpointEntity> findByDomain(String domain);

    /**
     * 도메인별 마지막 처리 UUID 조회
     */
    @Override
    @Query("SELECT c.lastProcessedUuid FROM CheckpointEntity c WHERE c.domain = :domain")
    Optional<UUID> findLastProcessedUuidByDomain(String domain);

    /**
     * 가장 최근의 체크포인트 조회
     * @deprecated 도메인별 조회 사용 권장
     */
    @Override
    @Deprecated
    @Query("SELECT c FROM CheckpointEntity c ORDER BY c.updatedAt DESC LIMIT 1")
    Optional<CheckpointEntity> findLatest();

    /**
     * 마지막 처리된 UUID 조회
     * @deprecated 도메인별 조회 사용 권장
     */
    @Override
    @Deprecated
    @Query("SELECT c.lastProcessedUuid FROM CheckpointEntity c ORDER BY c.updatedAt DESC LIMIT 1")
    Optional<UUID> findLastProcessedUuid();
}
