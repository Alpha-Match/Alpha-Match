package com.alpha.backend.application.grpc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Recruit 도메인 gRPC 전송용 DTO
 * <p>
 * Python 서버에서 JSON으로 직렬화된 데이터를 수신할 때 사용
 */
@Data
@NoArgsConstructor
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
