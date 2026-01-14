package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CandidateDetail (GraphQL Type)
 * - Full candidate information including resume
 * - Used for DETAIL views (getCandidate query)
 * - Maps to candidate + candidate_description + candidate_skill tables
 * - Note: originalResume from candidate table (not duplicate description)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDetail {

    private String id;

    private String positionCategory;

    private Integer experienceYears;

    private String originalResume;

    private String resumeLang;

    private String moreinfo;

    private String lookingFor;

    private List<String> skills;

    private String createdAt;

    private String updatedAt;
}
