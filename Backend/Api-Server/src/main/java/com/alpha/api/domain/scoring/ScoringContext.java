package com.alpha.api.domain.scoring;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * 스코어링 계산에 필요한 입력 데이터 (Value Object)
 *
 * 불변 객체로 설계하여 스레드 안전성 보장.
 */
@Value
@Builder
public class ScoringContext {

    /**
     * pgvector에서 계산된 벡터 코사인 유사도 (0.0 ~ 1.0)
     */
    double vectorSimilarity;

    /**
     * 검색에 사용된 스킬 목록 (소문자 정규화됨)
     * - Candidate 모드: 구직자가 입력한 스킬
     * - Recruit 모드: 기업이 원하는 스킬
     */
    Set<String> searchSkills;

    /**
     * 대상(Recruit/Candidate)의 스킬 목록 (소문자 정규화됨)
     * - Candidate 모드: 채용공고의 요구 스킬
     * - Recruit 모드: 후보자의 보유 스킬
     */
    Set<String> targetSkills;
}
