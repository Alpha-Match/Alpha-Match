package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Search Statistics Result
 * - Comprehensive statistics for search results
 * - Combines top skills analysis with total result count
 * - Maps to GraphQL type: SearchStatisticsResult
 * - Used by Frontend SearchResultPanel for complete search overview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchStatisticsResult {
    /**
     * Top N most frequently required skills
     * Ordered by frequency (descending)
     */
    private List<SkillFrequency> topSkills;

    /**
     * Total count of search results (unpaginated)
     * Fixed value for consistent UX during infinite scroll
     */
    private Integer totalCount;
}
