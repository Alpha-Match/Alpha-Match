package com.alpha.backend.domain.skilldic.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Skill Embedding Dictionary Entity (skill_embedding_dic 테이블)
 * 기술 스택 사전 및 벡터 정보를 저장하는 엔티티
 *
 * SQL 매핑:
 * - skill_id (UUID, PK, auto-generated)
 * - category_id (UUID, FK → skill_category_dic)
 * - skill (TEXT, UNIQUE)
 * - skill_vector (VECTOR(1536), NOT NULL)
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "skill_embedding_dic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillEmbeddingDicEntity {

    public static final int VECTOR_DIMENSION = 1536;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "skill_id", updatable = false, nullable = false)
    private UUID skillId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "skill", nullable = false, unique = true, columnDefinition = "TEXT")
    private String skill;

    @Column(name = "skill_vector", columnDefinition = "vector(1536)", nullable = false)
    private PGvector skillVector;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 도메인 타입 반환
     */
    public String getDomainType() {
        return "skill_dic";
    }

    /**
     * Vector 차원 반환
     */
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    /**
     * PGvector를 float 배열로 변환
     */
    public float[] toFloatArray() {
        return this.skillVector != null ? this.skillVector.toArray() : new float[0];
    }

    /**
     * float 배열로부터 SkillEmbeddingDicEntity 생성
     */
    public static SkillEmbeddingDicEntity fromFloatArray(UUID categoryId, String skill, float[] vectorArray) {
        if (vectorArray.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch: expected %d, got %d",
                            VECTOR_DIMENSION, vectorArray.length)
            );
        }

        SkillEmbeddingDicEntity entity = new SkillEmbeddingDicEntity();
        entity.setCategoryId(categoryId);
        entity.setSkill(skill);
        entity.setSkillVector(new PGvector(vectorArray));
        return entity;
    }
}
