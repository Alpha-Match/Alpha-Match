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
 * Recruit Entity (Domain Model)
 * - Corresponds to "recruit" table in table_specification.md
 * - Represents job posting metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("recruit")
public class Recruit {

    @Id
    @Column("recruit_id")
    private UUID recruitId;

    @Column("position")
    private String position;

    @Column("company_name")
    private String companyName;

    @Column("experience_years")
    private Integer experienceYears;

    @Column("primary_keyword")
    private String primaryKeyword;

    @Column("english_level")
    private String englishLevel;

    @Column("published_at")
    private OffsetDateTime publishedAt;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
