package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CompanyJobCount GraphQL Type
 * - Represents company name and job posting count
 * - Used for Dashboard Top Companies feature
 * - Maps to dashboard_request.txt requirement #1: "Company_name 기준 공고 많은 기업 Top 10"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyJobCount {

    /**
     * Company name
     */
    private String companyName;

    /**
     * Number of job postings for this company
     */
    private Integer jobCount;
}
