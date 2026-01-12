package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SearchMatchesResult (GraphQL Type)
 * - GraphQL response type for the `searchMatches` query.
 * - Contains a list of matching `MatchItem` objects and `vectorVisualization` data.
 * - `vectorVisualization` provides 2D coordinate data for the skills used in the search,
 *   intended for graphical representation on the frontend (e.g., skill plot).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchMatchesResult {

    private List<MatchItem> matches;

    private List<SkillMatch> vectorVisualization;
}
