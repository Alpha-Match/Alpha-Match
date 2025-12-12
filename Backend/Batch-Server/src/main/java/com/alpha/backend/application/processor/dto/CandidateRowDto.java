package com.alpha.backend.application.processor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Candidate 데이터 DTO (스켈레톤)
 *
 * Python 서버의 CandidateData 모델과 일치하는 구조
 * 추후 확장 예정
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRowDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("skills")
    private List<String> skills;

    // TODO: 추후 Candidate 도메인 필드 추가
}
