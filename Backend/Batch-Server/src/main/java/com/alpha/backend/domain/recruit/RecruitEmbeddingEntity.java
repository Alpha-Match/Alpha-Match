package com.alpha.backend.domain.recruit;

import com.alpha.backend.domain.common.BaseEmbeddingEntity;
import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;

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
public class RecruitEmbeddingEntity extends BaseEmbeddingEntity {

    public static final int VECTOR_DIMENSION = 384;

    @Override
    public String getDomainType() {
        return "recruit";
    }

    @Override
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    /**
     * float 배열로부터 RecruitEmbeddingEntity 생성
     */
    public static RecruitEmbeddingEntity fromFloatArray(UUID id, float[] vectorArray) {
        if (vectorArray.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch: expected %d, got %d",
                            VECTOR_DIMENSION, vectorArray.length)
            );
        }

        RecruitEmbeddingEntity entity = new RecruitEmbeddingEntity();
        entity.setId(id);
        entity.setVector(new PGvector(vectorArray));
        return entity;
    }
}
