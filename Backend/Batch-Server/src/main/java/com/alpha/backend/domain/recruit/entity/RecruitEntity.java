package com.alpha.backend.domain.recruit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recruit Entity (recruit 테이블)
 * 채용 공고의 메타데이터를 저장하는 엔티티
 *
 * SQL 매핑:
 * - recruit_id (UUID, PK)
 * - position (TEXT, NOT NULL)
 * - company_name (TEXT, NOT NULL)
 * - experience_years (INTEGER, Nullable, CHECK > 0)
 * - primary_keyword (TEXT, Nullable)
 * - english_level (TEXT, Nullable)
 * - published_at (TIMESTAMPTZ, Nullable)
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "recruit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitEntity {

    @Id
    @Column(name = "recruit_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID recruitId;

    @Column(name = "position", nullable = false, columnDefinition = "TEXT")
    private String position;

    @Column(name = "company_name", nullable = false, columnDefinition = "TEXT")
    private String companyName;

    @Column(name = "experience_years")
    private Integer experienceYears; // NULL = 신입/경력무관

    @Column(name = "primary_keyword", columnDefinition = "TEXT")
    private String primaryKeyword;

    @Column(name = "english_level", columnDefinition = "TEXT")
    private String englishLevel;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getDomainType() {
        return "recruit";
    }
}
