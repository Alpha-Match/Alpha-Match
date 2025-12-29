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
 * CandidateDescription Entity (Domain Model)
 * - Corresponds to "candidate_description" table in table_specification.md
 * - Stores detailed resume description (Markdown)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("candidate_description")
public class CandidateDescription {

    @Id
    @Column("candidate_id")
    private UUID candidateId;

    @Column("original_resume")
    private String originalResume;

    @Column("resume_lang")
    private String resumeLang;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
