package com.alpha.api.application.dto;

import com.alpha.api.domain.recruit.entity.Recruit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RecruitSearchResult DTO
 * - Contains Recruit entity + similarity score from vector search
 * - Used by SearchService to process findSimilarByVector() results
 * - Avoids adding non-persistent fields to Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitSearchResult {

    /**
     * Recruit entity from database
     */
    private Recruit recruit;

    /**
     * Similarity score from vector search (0.0 - 1.0)
     * - Calculated by: 1 - (skills_vector <=> query_vector)
     * - Higher values = more similar
     */
    private Double similarityScore;
}
