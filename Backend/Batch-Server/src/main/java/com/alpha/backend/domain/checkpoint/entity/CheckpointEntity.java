package com.alpha.backend.domain.checkpoint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Checkpoint Entity (Domain-agnostic)
 * 배치 처리의 마지막 처리 시점을 도메인별로 저장하는 엔티티
 *
 * 실제 DB 구조 (V1__init_schema.sql 기준):
 * - id: BIGSERIAL (PK, 자동 증가)
 * - domain: VARCHAR(50) (UNIQUE, 도메인 구분)
 * - last_processed_uuid: UUID (마지막 처리 UUID)
 * - processed_count: BIGINT (처리된 레코드 수)
 * - updated_at: TIMESTAMP (자동 갱신)
 */
@Entity
@Table(name = "checkpoint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;  // PK (자동 증가)

    @Column(name = "domain", length = 50, nullable = false, unique = true)
    private String domain;  // UNIQUE: 'recruit', 'candidate', etc.

    @Column(name = "last_processed_uuid", columnDefinition = "UUID")
    private UUID lastProcessedUuid;

    @Column(name = "processed_count", nullable = false)
    @Builder.Default
    private Long processedCount = 0L;  // 처리된 레코드 수

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 체크포인트 업데이트 (UUID + 처리 개수)
     */
    public void updateCheckpoint(UUID uuid, long count) {
        this.lastProcessedUuid = uuid;
        this.processedCount = count;
    }

    /**
     * 체크포인트 증분 업데이트 (UUID만)
     */
    public void updateCheckpoint(UUID uuid) {
        this.lastProcessedUuid = uuid;
        this.processedCount++;
    }

    /**
     * 처리 개수 증가
     */
    public void incrementProcessedCount() {
        this.processedCount++;
    }

    /**
     * 도메인별 체크포인트 생성
     */
    public static CheckpointEntity forDomain(String domain) {
        return CheckpointEntity.builder()
                .domain(domain)
                .lastProcessedUuid(null)
                .processedCount(0L)
                .build();
    }

    public static CheckpointEntity forRecruit() {
        return forDomain("recruit");
    }

    public static CheckpointEntity forCandidate() {
        return forDomain("candidate");
    }
}
