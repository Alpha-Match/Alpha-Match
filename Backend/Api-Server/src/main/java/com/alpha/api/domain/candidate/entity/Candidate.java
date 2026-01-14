package com.alpha.api.domain.candidate.entity;

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
 * Candidate Entity (Domain Model)
 * - Corresponds to "candidate" table in table_specification.md
 * - Represents job seeker resume metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("candidate")
public class Candidate {

    @Id
    @Column("candidate_id")
    private UUID candidateId;

    @Column("position_category")
    private String positionCategory;

    @Column("experience_years")
    private Integer experienceYears;

    @Column("original_resume")
    private String originalResume;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
