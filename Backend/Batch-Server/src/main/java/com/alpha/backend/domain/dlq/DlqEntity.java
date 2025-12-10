package com.alpha.backend.domain.dlq;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dead Letter Queue Entity
 * 처리 실패한 레코드를 저장하는 엔티티
 */
@Entity
@Table(name = "recruit_embedding_dlq")
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

    @Column(name = "recruit_id", columnDefinition = "UUID")
    private UUID recruitId;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "payload", columnDefinition = "JSONB")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * DLQ 엔티티 생성 (payload를 JSON 문자열로 변환)
     */
    public static DlqEntity create(UUID recruitId, String errorMessage, String payloadJson) {
        return DlqEntity.builder()
                .recruitId(recruitId)
                .errorMessage(errorMessage)
                .payload(payloadJson)
                .build();
    }
}
