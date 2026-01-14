package com.alpha.backend.domain.common;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base Embedding Entity
 * 모든 도메인의 Embedding Entity가 상속하는 추상 클래스
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEmbeddingEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "vector", nullable = false)
    private PGvector vector;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 도메인 타입 반환 (하위 클래스에서 구현)
     */
    public abstract String getDomainType();

    /**
     * Vector 차원 반환 (하위 클래스에서 구현)
     */
    public abstract int getVectorDimension();

    /**
     * PGvector를 float 배열로 변환
     */
    public float[] toFloatArray() {
        return this.vector != null ? this.vector.toArray() : new float[0];
    }
}
