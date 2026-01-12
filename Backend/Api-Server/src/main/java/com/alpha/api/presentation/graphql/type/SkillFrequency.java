package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SkillFrequency GraphQL Type
 * - Represents skill name with frequency count and percentage
 * - Used for Dashboard Top Skills in Search Results feature
 * - Maps to dashboard_request.txt requirement #2: "검색된 전체 채용 공고/이력서에 관한 주요 요구 기술 Top 15"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillFrequency {

    /**
     * Skill name
     */
    private String skill;

    /**
     * Number of occurrences across search results
     */
    private Integer count;

    /**
     * Percentage of this skill among all skills in search results (0.0 ~ 100.0)
     */
    private Double percentage;
}
