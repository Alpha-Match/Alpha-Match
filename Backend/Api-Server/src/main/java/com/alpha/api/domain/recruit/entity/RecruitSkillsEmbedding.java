package com.alpha.api.domain.recruit.entity;

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
 * RecruitSkillsEmbedding Entity (Domain Model)
 * - Corresponds to "recruit_skills_embedding" table in table_specification.md
 * - Stores aggregated skill vector (384 dimensions)
 * - Used for vector similarity search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("recruit_skills_embedding")
public class RecruitSkillsEmbedding {

    public static final int VECTOR_DIMENSION = 384;

    @Id
    @Column("recruit_id")
    private UUID recruitId;

    @Column("skills")
    private List<String> skills;

    @Column("skills_vector")
    private List<Float> skillsVector;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
