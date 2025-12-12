package com.alpha.backend.application.processor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Recruit 데이터 DTO
 *
 * Python 서버의 RecruitData 모델과 일치하는 구조
 * JSON 역직렬화를 위한 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruitRowDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("exp_years")
    private Integer expYears;

    @JsonProperty("english_level")
    private String englishLevel;

    @JsonProperty("primary_keyword")
    private String primaryKeyword;

    @JsonProperty("vector")
    private List<Float> vector;
}
