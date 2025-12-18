package com.alpha.backend.domain.candidate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Candidate Skill Entity (candidate_skill 테이블)
 * 후보자와 기술 스택의 매핑 정보 (1:N 관계)
 *
 * SQL 매핑:
 * - candidate_id (UUID, PK, FK → candidate)
 * - skill (VARCHAR(50), PK, FK → skill_embedding_dic)
 * - created_at, updated_at (자동 관리)
 *
 * 복합 PK: (candidate_id, skill)
 */
@Entity
@Table(name = "candidate_skill")
@IdClass(CandidateSkillId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkillEntity {

    @Id
    @Column(name = "candidate_id", columnDefinition = "UUID", nullable = false)
    private UUID candidateId;

    @Id
    @Column(name = "skill", length = 50, nullable = false)
    private String skill;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
