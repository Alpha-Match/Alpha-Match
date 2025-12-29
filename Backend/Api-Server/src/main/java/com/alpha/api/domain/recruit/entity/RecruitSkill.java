package com.alpha.api.domain.recruit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * RecruitSkill Entity (Domain Model)
 * - Corresponds to "recruit_skill" table in table_specification.md
 * - Composite Primary Key: (recruit_id, skill)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("recruit_skill")
public class RecruitSkill {

    @Column("recruit_id")
    private UUID recruitId;

    @Column("skill")
    private String skill;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
