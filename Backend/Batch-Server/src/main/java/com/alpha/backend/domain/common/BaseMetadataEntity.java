package com.alpha.backend.domain.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base Metadata Entity
 * 모든 도메인의 Metadata Entity가 상속하는 추상 클래스
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseMetadataEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 도메인 타입 반환 (하위 클래스에서 구현)
     */
    public abstract String getDomainType();
}
