package com.alpha.backend.application.grpc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Recruit 도메인 gRPC 전송용 DTO (v2)
 * <p>
 * Python 서버에서 JSON으로 직렬화된 데이터를 수신할 때 사용
 *
 * v2 변경사항:
 * - 추가: position, published_at, skills[], long_description, description_lang
 * - 필드명 변경: exp_years → experience_years, vector → skills_vector
 */
@Data
@NoArgsConstructor
public class RecruitRowDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("position")
    private String position;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("experience_years")
    private Integer experienceYears;

    @JsonProperty("primary_keyword")
    private String primaryKeyword;

    @JsonProperty("english_level")
    private String englishLevel;

    @JsonProperty("published_at")
    private String publishedAt;

    @JsonProperty("skills")
    private List<String> skills;

    @JsonProperty("long_description")
    private String longDescription;

    @JsonProperty("description_lang")
    private String descriptionLang;

    @JsonProperty("skills_vector")
    private List<Float> skillsVector;
}
