package com.alpha.api.presentation.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MatchItem (GraphQL Type)
 * - Frontend compatible type for search results
 * - Maps to Frontend MatchItem interface
 * - Unified result for both Recruit and Candidate searches
 * - Used for LIST views (searchMatches) - does NOT include description
 * - timestamp field: Internal use only for sorting (not exposed in GraphQL schema)
 * - Includes hybrid scoring details for visualization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchItem {

    private String id;

    private String title;

    private String company;

    /**
     * 최종 하이브리드 점수 (0.0 ~ 100.0)
     */
    private Double score;

    private List<String> skills;

    private Integer experience;

    /**
     * Timestamp for sorting (publishedAt for Recruit, createdAt for Candidate)
     * Not exposed in GraphQL schema - internal use only
     */
    private String timestamp;

    // ===== Hybrid Scoring Details (for visualization) =====

    /**
     * 벡터 유사도 점수 (0.0 ~ 100.0)
     */
    private Double vectorScore;

    /**
     * 검색 스킬 활용도 (0.0 ~ 100.0)
     */
    private Double overlapRatio;

    /**
     * 대상 스킬 충족도 (0.0 ~ 100.0)
     */
    private Double coverageRatio;

    /**
     * 추가 스킬 비율 (가산점 또는 감점)
     * - Candidate 모드: 양수 (오버스펙 가산점)
     * - Recruit 모드: 음수 (오버스펙 감점)
     */
    private Double extraRatio;

    // ===== Skill Classification (for detail view) =====

    /**
     * 매칭된 스킬 (교집합)
     */
    private List<String> matchedSkills;

    /**
     * 검색자만 보유한 스킬
     */
    private List<String> extraSkills;

    /**
     * 대상만 보유한 스킬
     */
    private List<String> missingSkills;
}
