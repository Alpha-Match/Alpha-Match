package com.alpha.backend.domain.candidate.entity;

import com.alpha.backend.domain.common.BaseEmbeddingEntity;
import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Candidate Skills Embedding Entity (candidate_skills_embedding 테이블)
 * 후보자의 기술 스택 벡터 정보를 저장하는 엔티티
 *
 * SQL 매핑:
 * - candidate_id (UUID, PK, FK → candidate)
 * - skills (VARCHAR(50)[], PostgreSQL 배열)
 * - skills_vector (VECTOR(768))
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "candidate_skills_embedding")
@AttributeOverrides({
    @AttributeOverride(name = "id", column = @Column(name = "candidate_id", columnDefinition = "UUID", updatable = false, nullable = false)),
    @AttributeOverride(name = "vector", column = @Column(name = "skills_vector", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkillsEmbeddingEntity extends BaseEmbeddingEntity {

    public static final int VECTOR_DIMENSION = 768;

    @Column(name = "skills", columnDefinition = "VARCHAR(50)[]", nullable = false)
    private String[] skills;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Override
    public String getDomainType() {
        return "candidate";
    }

    @Override
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    /**
     * float 배열과 skills 배열로부터 CandidateSkillsEmbeddingEntity 생성
     */
    public static CandidateSkillsEmbeddingEntity fromFloatArray(UUID candidateId, String[] skills, float[] vectorArray) {
        if (vectorArray.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch: expected %d, got %d",
                            VECTOR_DIMENSION, vectorArray.length)
            );
        }

        CandidateSkillsEmbeddingEntity entity = new CandidateSkillsEmbeddingEntity();
        entity.setId(candidateId);
        entity.setSkills(skills);
        entity.setVector(new PGvector(vectorArray));
        return entity;
    }
}
