package com.alpha.backend.domain.recruit.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Recruit Skill Composite Primary Key
 * 채용공고-스킬 복합 PK (recruit_id + skill)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecruitSkillId implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID recruitId;
    private String skill;
}
