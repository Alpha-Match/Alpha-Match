package com.alpha.backend.application.grpc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Candidate 도메인 gRPC 전송용 DTO
 * <p>
 * Python 서버에서 JSON으로 직렬화된 데이터를 수신할 때 사용
 */
@Data
@NoArgsConstructor
public class CandidateRowDto {

    @JsonProperty("candidate_id")
    private String candidateId;

    @JsonProperty("position_category")
    private String positionCategory;

    @JsonProperty("experience_years")
    private Integer experienceYears;

    @JsonProperty("original_resume")
    private String originalResume;

    @JsonProperty("resume_lang")
    private String resumeLang;

    @JsonProperty("moreinfo")
    private String moreinfo;

    @JsonProperty("looking_for")
    private String lookingFor;

    @JsonProperty("skills")
    private List<String> skills;

    @JsonProperty("skills_vector")
    private List<Float> skillsVector;
}
