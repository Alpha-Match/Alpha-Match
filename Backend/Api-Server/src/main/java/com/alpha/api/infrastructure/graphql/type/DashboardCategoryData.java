package com.alpha.api.infrastructure.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dashboard Category Data
 * - Category name and skill statistics
 * - Used by Frontend DefaultDashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCategoryData {
    private String category;
    private List<DashboardSkillStat> skills;
}
