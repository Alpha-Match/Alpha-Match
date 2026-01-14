package com.alpha.api.domain.recruit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * RecruitDescription Entity (Domain Model)
 * - Corresponds to "recruit_description" table in table_specification.md
 * - Stores detailed job description (Markdown)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("recruit_description")
public class RecruitDescription {

    @Id
    @Column("recruit_id")
    private UUID recruitId;

    @Column("long_description")
    private String longDescription;

    @Column("description_lang")
    private String descriptionLang;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
