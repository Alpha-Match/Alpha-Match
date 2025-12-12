package com.alpha.backend.infrastructure;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Checkpoint Entity (Domain-agnostic)
 * 배치 처리의 마지막 처리 시점을 도메인별로 저장하는 엔티티
 *
 * 테이블명이 V3 마이그레이션에서 'checkpoint'로 변경됨
 * PK가 domain으로 변경됨 (도메인당 하나의 체크포인트)
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
    @Column(name = "domain", length = 50, nullable = false)
    private String domain;  // PK: 'recruit', 'candidate', etc.

    @Column(name = "last_processed_uuid", columnDefinition = "UUID")
    private UUID lastProcessedUuid;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 체크포인트 업데이트
     */
    public void updateCheckpoint(UUID uuid) {
        this.lastProcessedUuid = uuid;
    }

    /**
     * 도메인별 체크포인트 생성
     */
    public static CheckpointEntity forDomain(String domain) {
        return CheckpointEntity.builder()
                .domain(domain)
                .lastProcessedUuid(null)
                .build();
    }

    public static CheckpointEntity forRecruit() {
        return forDomain("recruit");
    }

    public static CheckpointEntity forCandidate() {
        return forDomain("candidate");
    }
}
