package com.alpha.backend.domain.candidate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Candidate Entity (candidate 테이블)
 * 후보자의 기본 정보를 저장하는 엔티티
 *
 * SQL 매핑:
 * - candidate_id (UUID, PK)
 * - position_category (TEXT, NOT NULL)
 * - experience_years (INTEGER, Nullable, CHECK > 0)
 * - original_resume (TEXT, NOT NULL)
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEntity {

    @Id
    @Column(name = "candidate_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID candidateId;

    @Column(name = "position_category", nullable = false, columnDefinition = "TEXT")
    private String positionCategory;

    @Column(name = "experience_years")
    private Integer experienceYears; // NULL = 신입/경력무관

    @Column(name = "original_resume", nullable = false, columnDefinition = "TEXT")
    private String originalResume;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getDomainType() {
        return "candidate";
    }
}
