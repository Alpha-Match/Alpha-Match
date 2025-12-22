package com.alpha.backend.domain.recruit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recruit Skill Entity (recruit_skill 테이블)
 * 채용 공고와 기술 스택의 매핑 정보 (1:N 관계)
 *
 * SQL 매핑:
 * - recruit_id (UUID, PK, FK → recruit)
 * - skill (TEXT, PK)
 * - created_at, updated_at (자동 관리)
 *
 * 복합 PK: (recruit_id, skill)
 */
@Entity
@Table(name = "recruit_skill")
@IdClass(RecruitSkillId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitSkillEntity {

    @Id
    @Column(name = "recruit_id", columnDefinition = "UUID", nullable = false)
    private UUID recruitId;

    @Id
    @Column(name = "skill", columnDefinition = "TEXT", nullable = false)
    private String skill;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
