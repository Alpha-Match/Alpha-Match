package com.alpha.api.domain.scoring;

/**
 * 하이브리드 스코어링 전략 인터페이스 (Port)
 *
 * Recruit/Candidate 모드별로 다른 계산식을 적용할 수 있도록 추상화.
 * Strategy Pattern을 통해 스코어링 로직을 교체 가능하게 설계.
 */
public interface ScoringStrategy {

    /**
     * 하이브리드 스코어를 계산합니다.
     *
     * @param context 스코어링에 필요한 입력 데이터 (벡터 유사도, 검색 스킬, 대상 스킬)
     * @return 계산된 스코어링 결과 (최종 점수, 세부 점수, 스킬 분류)
     */
    ScoringResult calculate(ScoringContext context);
}
