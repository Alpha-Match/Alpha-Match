package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CategoryMatchDistribution (GraphQL Type)
 * - Response type for getCategoryDistribution query
 * - Shows category distribution of selected skills
 * - Used by Frontend SearchResultPanel for pie chart visualization
 * - Example: [Java, Spring Boot, MySQL] → Backend 66%, Database 33%
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMatchDistribution {

    /**
     * Category name (e.g., "Backend", "Database", "Frontend")
     */
    private String category;

    /**
     * Percentage of skills in this category (0-100)
     */
    private Double percentage;

    /**
     * Skills that belong to this category
     */
    private List<String> matchedSkills;

    /**
     * Number of skills in this category
     */
    private Integer skillCount;
}
