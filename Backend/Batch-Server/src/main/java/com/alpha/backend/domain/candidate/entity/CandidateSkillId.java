package com.alpha.backend.domain.candidate.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Candidate Skill Composite Primary Key
 * 후보자-스킬 복합 PK (candidate_id + skill)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CandidateSkillId implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID candidateId;
    private String skill;
}
