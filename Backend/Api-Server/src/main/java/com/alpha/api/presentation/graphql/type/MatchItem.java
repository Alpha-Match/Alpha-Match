package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MatchItem (GraphQL Type)
 * - Frontend compatible type for search results
 * - Maps to Frontend MatchItem interface
 * - Unified result for both Recruit and Candidate searches
 * - Used for LIST views (searchMatches) - does NOT include description
 * - timestamp field: Internal use only for sorting (not exposed in GraphQL schema)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchItem {

    private String id;

    private String title;

    private String company;

    private Double score;

    private List<String> skills;

    private Integer experience;

    /**
     * Timestamp for sorting (publishedAt for Recruit, createdAt for Candidate)
     * Not exposed in GraphQL schema - internal use only
     */
    private String timestamp;
}
