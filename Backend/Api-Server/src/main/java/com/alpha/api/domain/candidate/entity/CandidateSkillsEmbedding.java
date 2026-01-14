package com.alpha.api.domain.candidate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CandidateSkillsEmbedding Entity (Domain Model)
 * - Corresponds to "candidate_skills_embedding" table in table_specification.md
 * - Stores aggregated skill vector (1536 dimensions)
 * - Used for vector similarity search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("candidate_skills_embedding")
public class CandidateSkillsEmbedding {

    public static final int VECTOR_DIMENSION = 1536;

    @Id
    @Column("candidate_id")
    private UUID candidateId;

    @Column("skills")
    private List<String> skills;

    @Column("skills_vector")
    private List<Float> skillsVector;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
