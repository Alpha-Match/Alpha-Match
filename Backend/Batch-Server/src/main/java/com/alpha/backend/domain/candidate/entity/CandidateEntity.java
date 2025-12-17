package com.alpha.backend.domain.candidate.entity;

import com.alpha.backend.domain.common.BaseMetadataEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Candidate Entity (candidate 테이블)
 * 후보자의 기본 정보를 저장하는 엔티티
 *
 * SQL 매핑:
 * - candidate_id (UUID, PK) → id (상속받은 필드)
 * - position_category (VARCHAR(50), NOT NULL)
 * - experience_years (INTEGER, NOT NULL, DEFAULT 0)
 * - original_resume (TEXT, NOT NULL)
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "candidate")
@AttributeOverride(name = "id", column = @Column(name = "candidate_id", columnDefinition = "UUID", updatable = false, nullable = false))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEntity extends BaseMetadataEntity {

    @Column(name = "position_category", nullable = false, length = 50)
    private String positionCategory;

    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(name = "original_resume", nullable = false, columnDefinition = "TEXT")
    private String originalResume;

    @Override
    public String getDomainType() {
        return "candidate";
    }
}
