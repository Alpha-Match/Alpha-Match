package com.alpha.api.graphql.type;

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

    private String description;
}
