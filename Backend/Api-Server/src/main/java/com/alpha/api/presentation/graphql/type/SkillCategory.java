package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SkillCategory (GraphQL Type)
 * - Response type for skillCategories query
 * - Contains category name + skills list
 * - Used by Frontend AppInitializer (GET_SKILL_CATEGORIES)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillCategory {

    private String category;

    private List<String> skills;
}
