package com.alpha.backend.domain.embedding;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recruit Embedding Entity
 * 채용 공고의 Embedding Vector를 저장하는 엔티티 (pgvector 사용)
 */
@Entity
@Table(name = "recruit_embedding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "vector", nullable = false, columnDefinition = "vector(1536)")
    private PGvector vector;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * float 배열로부터 EmbeddingEntity 생성
     */
    public static EmbeddingEntity fromFloatArray(UUID id, float[] vectorArray) {
        return EmbeddingEntity.builder()
                .id(id)
                .vector(new PGvector(vectorArray))
                .build();
    }

    /**
     * PGvector를 float 배열로 변환
     */
    public float[] toFloatArray() {
        return this.vector != null ? this.vector.toArray() : new float[0];
    }
}
