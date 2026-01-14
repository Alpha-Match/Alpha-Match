package com.alpha.backend.domain.candidate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Candidate Description Entity (candidate_description 테이블)
 * 이력서 상세 원문 (Markdown)
 *
 * SQL 매핑:
 * - candidate_id (UUID, PK, FK → candidate)
 * - original_resume (TEXT)
 * - resume_lang (TEXT, Nullable)
 * - moreinfo (TEXT, Nullable, v3 추가)
 * - looking_for (TEXT, Nullable, v3 추가)
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "candidate_description")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDescriptionEntity {

    @Id
    @Column(name = "candidate_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID candidateId;

    @Column(name = "original_resume", nullable = false, columnDefinition = "TEXT")
    private String originalResume;

    @Column(name = "resume_lang", columnDefinition = "TEXT")
    private String resumeLang;

    @Column(name = "moreinfo", columnDefinition = "TEXT")
    private String moreinfo;

    @Column(name = "looking_for", columnDefinition = "TEXT")
    private String lookingFor;

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
        return "candidate";
    }
}
