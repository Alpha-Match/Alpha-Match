package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SkillMatch (GraphQL Type)
 * - Vector visualization data for Frontend
 * - Maps to Frontend SkillMatch interface
 * - Used in VisualizationPanel component
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMatch {

    private String skill;

    private Boolean isCore;

    private Double x;

    private Double y;
}
