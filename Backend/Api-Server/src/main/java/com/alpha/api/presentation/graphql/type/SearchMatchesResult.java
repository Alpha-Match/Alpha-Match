package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SearchMatchesResult (GraphQL Type)
 * - Response type for searchMatches query
 * - Contains matches + vectorVisualization
 * - Frontend compatible (SEARCH_MATCHES_QUERY)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchMatchesResult {

    private List<MatchItem> matches;

    private List<SkillMatch> vectorVisualization;
}
