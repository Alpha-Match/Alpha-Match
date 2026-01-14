package com.alpha.api.domain.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill Count DTO
 * - Used for GROUP BY aggregation results
 * - Maps to: SELECT skill, COUNT(*) FROM {table} GROUP BY skill
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillCount {
    private String skill;
    private Long count;
}
