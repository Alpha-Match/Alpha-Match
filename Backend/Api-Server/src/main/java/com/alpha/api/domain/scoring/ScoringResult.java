package com.alpha.api.domain.scoring;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * 스코어링 계산 결과 (Value Object)
 *
 * 최종 점수, 세부 점수, 스킬 분류 정보를 포함.
 * GraphQL 응답에서 시각화 및 상세 분석에 활용.
 */
@Value
@Builder
public class ScoringResult {

    /**
     * 최종 하이브리드 점수 (0.0 ~ 100.0)
     */
    double hybridScore;

    /**
     * 벡터 유사도 점수 (0.0 ~ 100.0)
     * - pgvector 코사인 유사도를 100점 만점으로 환산
     */
    double vectorScore;

    /**
     * 검색 스킬 활용도 (0.0 ~ 100.0)
     * - Candidate 모드: 구직자 스킬 중 공고에서 쓰이는 비율
     * - Recruit 모드: 기업 요구 스킬 중 후보자가 보유한 비율
     */
    double overlapRatio;

    /**
     * 대상 스킬 충족도 (0.0 ~ 100.0)
     * - Candidate 모드: 공고 요구 스킬 중 구직자가 보유한 비율
     * - Recruit 모드: 후보자 스킬 중 기업이 원하는 비율
     */
    double coverageRatio;

    /**
     * 추가 스킬 비율 (0.0 ~ 100.0, nullable)
     * - Candidate 모드: 오버스펙 가산점 (양수)
     * - Recruit 모드: 오버스펙 감점 (음수)
     */
    Double extraRatio;

    /**
     * 매칭된 스킬 (교집합)
     * - 검색 스킬과 대상 스킬이 모두 가진 스킬
     */
    Set<String> matchedSkills;

    /**
     * 검색자만 보유한 스킬
     * - Candidate 모드: 구직자의 오버스펙
     * - Recruit 모드: 기업이 원하지만 후보자가 없는 스킬
     */
    Set<String> extraSkills;

    /**
     * 대상만 보유한 스킬
     * - Candidate 모드: 구직자에게 부족한 스킬
     * - Recruit 모드: 후보자의 오버스펙
     */
    Set<String> missingSkills;
}
