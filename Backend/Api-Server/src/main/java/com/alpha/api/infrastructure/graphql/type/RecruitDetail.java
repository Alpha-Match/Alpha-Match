package com.alpha.api.infrastructure.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RecruitDetail (GraphQL Type)
 * - Full recruit information including description
 * - Used for DETAIL views (getRecruit query)
 * - Maps to recruit + recruit_description + recruit_skill tables
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitDetail {

    private String id;

    private String position;

    private String companyName;

    private Integer experienceYears;

    private String primaryKeyword;

    private String englishLevel;

    private List<String> skills;

    private String description;

    private String publishedAt;
}
