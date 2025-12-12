package com.alpha.backend.domain.candidate;

import com.alpha.backend.domain.common.BaseEmbeddingEntity;
import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Candidate Embedding Entity
 * 후보자의 Embedding Vector를 저장하는 엔티티 (pgvector 사용)
 */
@Entity
@Table(name = "candidate_embedding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEmbeddingEntity extends BaseEmbeddingEntity {

    public static final int VECTOR_DIMENSION = 768;

    @Override
    public String getDomainType() {
        return "candidate";
    }

    @Override
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    /**
     * float 배열로부터 CandidateEmbeddingEntity 생성
     */
    public static CandidateEmbeddingEntity fromFloatArray(UUID id, float[] vectorArray) {
        if (vectorArray.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch: expected %d, got %d",
                            VECTOR_DIMENSION, vectorArray.length)
            );
        }

        CandidateEmbeddingEntity entity = new CandidateEmbeddingEntity();
        entity.setId(id);
        entity.setVector(new PGvector(vectorArray));
        return entity;
    }
}
