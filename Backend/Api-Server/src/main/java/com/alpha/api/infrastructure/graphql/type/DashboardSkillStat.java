package com.alpha.api.infrastructure.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard Skill Statistics
 * - Skill name and count
 * - Used by Frontend DefaultDashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSkillStat {
    private String skill;
    private Integer count;
}
