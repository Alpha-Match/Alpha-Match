package com.alpha.backend.application.grpc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Skill Dictionary Row DTO
 * <p>
 * Python 서버로부터 전송되는 Skill Dictionary 데이터 구조
 * JSON 필드명: skill, position_category, skill_vector
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDicRowDto {

    /**
     * 스킬 이름 (TEXT, UNIQUE)
     * 예: "Java", "Python", "React"
     */
    @JsonProperty("skill")
    private String skill;

    /**
     * 직종 카테고리 (TEXT)
     * 예: "Backend", "Frontend", "Database"
     */
    @JsonProperty("position_category")
    private String positionCategory;

    /**
     * 스킬 벡터 임베딩 (1536차원)
     */
    @JsonProperty("skill_vector")
    private List<Double> skillVector;
}
