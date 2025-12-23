package com.alpha.backend.domain.candidate.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Candidate Skills Embedding Entity (candidate_skills_embedding 테이블)
 * 후보자의 기술 스택 벡터 정보를 저장하는 엔티티
 *
 * SQL 매핑:
 * - candidate_id (UUID, PK, FK → candidate)
 * - skills (TEXT[], PostgreSQL 배열)
 * - skills_vector (VECTOR(384))
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "candidate_skills_embedding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkillsEmbeddingEntity {

    public static final int VECTOR_DIMENSION = 384;

    @Id
    @Column(name = "candidate_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID candidateId;

    @Column(name = "skills", columnDefinition = "TEXT[]", nullable = false)
    private String[] skills;

    @Column(name = "skills_vector", columnDefinition = "vector(384)", nullable = false)
    private PGvector skillsVector;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getDomainType() {
        return "candidate";
    }

    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    /**
     * PGvector를 float 배열로 변환
     */
    public float[] toFloatArray() {
        return this.skillsVector != null ? this.skillsVector.toArray() : new float[0];
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
        entity.setCandidateId(candidateId);
        entity.setSkills(skills);
        entity.setSkillsVector(new PGvector(vectorArray));
        return entity;
    }
}
