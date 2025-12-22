package com.alpha.backend.domain.skilldic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Skill Category Dictionary Entity (skill_category_dic 테이블)
 * 직종 카테고리 사전 (Backend, Frontend 등)
 *
 * SQL 매핑:
 * - category_id (UUID, PK, auto-generated)
 * - category (TEXT, UNIQUE)
 * - created_at, updated_at (자동 관리)
 */
@Entity
@Table(name = "skill_category_dic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillCategoryDicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "category_id", updatable = false, nullable = false)
    private UUID categoryId;

    @Column(name = "category", nullable = false, unique = true, columnDefinition = "TEXT")
    private String category;

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
        return "skill_category_dic";
    }
}
