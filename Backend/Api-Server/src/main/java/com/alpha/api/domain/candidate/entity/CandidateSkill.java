package com.alpha.api.domain.candidate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CandidateSkill Entity (Domain Model)
 * - Corresponds to "candidate_skill" table in table_specification.md
 * - Composite Primary Key: (candidate_id, skill)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("candidate_skill")
public class CandidateSkill {

    @Column("candidate_id")
    private UUID candidateId;

    @Column("skill")
    private String skill;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
