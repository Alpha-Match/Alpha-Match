package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SkillCompetencyMatch (GraphQL Type)
 * - Response type for getSkillCompetencyMatch query
 * - Compares searched skills vs target (recruit/candidate) skills
 * - Used by Frontend MatchDetailPanel for skill gap analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillCompetencyMatch {

    /**
     * Intersection (common skills between searched and target)
     */
    private List<String> matchedSkills;

    /**
     * Skills only in target (recruit/candidate)
     * These are skills the user doesn't have but the job requires
     */
    private List<String> missingSkills;

    /**
     * Skills only in searched (user's skills)
     * These are extra skills the user has that aren't required
     */
    private List<String> extraSkills;

    /**
     * Match ratio (0-100)
     * Calculated as: (matchedSkills / totalTargetSkills) * 100
     */
    private Double matchingPercentage;

    /**
     * Competency level based on matching percentage
     * - "High": 80%+
     * - "Medium": 50-80%
     * - "Low": <50%
     */
    private String competencyLevel;

    /**
     * Total skills in target (recruit/candidate)
     */
    private Integer totalTargetSkills;

    /**
     * Total skills searched by user
     */
    private Integer totalSearchedSkills;
}
