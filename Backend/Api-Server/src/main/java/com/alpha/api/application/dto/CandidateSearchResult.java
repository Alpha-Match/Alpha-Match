package com.alpha.api.application.dto;

import com.alpha.api.domain.candidate.entity.Candidate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CandidateSearchResult DTO
 * - Contains Candidate entity + similarity score from vector search
 * - Used by SearchService to process findSimilarByVector() results
 * - Avoids adding non-persistent fields to Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSearchResult {

    /**
     * Candidate entity from database
     */
    private Candidate candidate;

    /**
     * Similarity score from vector search (0.0 - 1.0)
     * - Calculated by: 1 - (skills_vector <=> query_vector)
     * - Higher values = more similar
     */
    private Double similarityScore;
}
