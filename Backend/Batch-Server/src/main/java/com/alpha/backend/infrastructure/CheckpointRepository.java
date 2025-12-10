package com.alpha.backend.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Checkpoint Repository
 * 배치 처리 체크포인트를 관리하는 레포지토리
 */
@Repository
public interface CheckpointRepository extends JpaRepository<CheckpointEntity, Long> {

    /**
     * 가장 최근의 체크포인트 조회
     */
    @Query("SELECT c FROM CheckpointEntity c ORDER BY c.updatedAt DESC LIMIT 1")
    Optional<CheckpointEntity> findLatest();

    /**
     * 체크포인트 업데이트 (단일 레코드만 유지)
     * @param lastProcessedUuid 마지막 처리된 UUID
     */
    @Modifying
    @Query(value = """
        UPDATE embedding_batch_checkpoint
        SET last_processed_uuid = :lastProcessedUuid, updated_at = NOW()
        WHERE id = (SELECT id FROM embedding_batch_checkpoint ORDER BY updated_at DESC LIMIT 1)
        """, nativeQuery = true)
    void updateLatestCheckpoint(@Param("lastProcessedUuid") UUID lastProcessedUuid);

    /**
     * 마지막 처리된 UUID 조회
     */
    @Query("SELECT c.lastProcessedUuid FROM CheckpointEntity c ORDER BY c.updatedAt DESC LIMIT 1")
    Optional<UUID> findLastProcessedUuid();
}
