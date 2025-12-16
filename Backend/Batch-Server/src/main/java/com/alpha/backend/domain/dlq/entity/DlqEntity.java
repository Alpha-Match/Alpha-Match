package com.alpha.backend.domain.dlq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dead Letter Queue Entity (Domain-agnostic)
 * 처리 실패한 레코드를 저장하는 엔티티 (도메인별 구분)
 *
 * 테이블명이 V3 마이그레이션에서 'dlq'로 변경됨
 */
@Entity
@Table(name = "dlq", indexes = {
        @Index(name = "idx_dlq_domain", columnList = "domain"),
        @Index(name = "idx_dlq_entity_id", columnList = "entity_id"),
        @Index(name = "idx_dlq_domain_created_at", columnList = "domain, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlqEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "domain", nullable = false, length = 50)
    private String domain;  // 'recruit', 'candidate', etc.

    @Column(name = "entity_id", columnDefinition = "UUID")
    private UUID entityId;  // 실패한 엔티티의 UUID

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "payload", columnDefinition = "JSONB")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * DLQ 엔티티 생성 (도메인별)
     */
    public static DlqEntity create(String domain, UUID entityId, String errorMessage, String payloadJson) {
        return DlqEntity.builder()
                .domain(domain)
                .entityId(entityId)
                .errorMessage(errorMessage)
                .payload(payloadJson)
                .build();
    }

    /**
     * 도메인별 DLQ 엔티티 생성 (편의 메서드)
     */
    public static DlqEntity forRecruit(UUID recruitId, String errorMessage, String payloadJson) {
        return create("recruit", recruitId, errorMessage, payloadJson);
    }

    public static DlqEntity forCandidate(UUID candidateId, String errorMessage, String payloadJson) {
        return create("candidate", candidateId, errorMessage, payloadJson);
    }
}
