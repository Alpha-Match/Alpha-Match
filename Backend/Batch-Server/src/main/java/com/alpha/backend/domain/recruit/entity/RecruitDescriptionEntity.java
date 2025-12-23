package com.alpha.backend.domain.recruit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recruit Description Entity (recruit_description 테이블)
 * 채용 공고 상세 원문 (Markdown)
 *
 * SQL 매핑:
 * - recruit_id (UUID, PK, FK → recruit)
 * - long_description (TEXT)
 * - description_lang (TEXT, Nullable)
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "recruit_description")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitDescriptionEntity {

    @Id
    @Column(name = "recruit_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID recruitId;

    @Column(name = "long_description", nullable = false, columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "description_lang", columnDefinition = "TEXT")
    private String descriptionLang;

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
        return "recruit";
    }
}
