package com.alpha.api.application.scoring;

import com.alpha.api.domain.scoring.ScoringContext;
import com.alpha.api.domain.scoring.ScoringResult;
import com.alpha.api.domain.scoring.ScoringStrategy;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 구직자 관점 스코어링 전략 (Candidate 모드)
 *
 * 구직자가 채용공고를 볼 때 적용되는 스코어링.
 * "내가 가진 기술이 이 공고에 얼마나 적합한가?"
 *
 * 가중치: Vector(40%) + Overlap(35%) + Coverage(15%) + Extra(10%)
 * - 오버스펙(구직자가 공고보다 더 많은 기술 보유)에 가산점 부여
 */
@Component
public class CandidateViewScoringStrategy implements ScoringStrategy {

    // 가중치 상수
    private static final double WEIGHT_VECTOR = 0.40;
    private static final double WEIGHT_OVERLAP = 0.30;
    private static final double WEIGHT_COVERAGE = 0.20;
    private static final double WEIGHT_EXTRA = 0.10;

    @Override
    public ScoringResult calculate(ScoringContext context) {
        Set<String> userSkills = context.getSearchSkills();    // 구직자 보유 기술
        Set<String> jobSkills = context.getTargetSkills();      // 채용공고 요구 기술

        // 1. 스킬 집합 계산 (교집합, 차집합)
        Set<String> matchedSkills = new HashSet<>(userSkills);
        matchedSkills.retainAll(jobSkills);                     // 교집합: 일치 기술

        Set<String> userExtraSkills = new HashSet<>(userSkills);
        userExtraSkills.removeAll(jobSkills);                   // 구직자만 보유 (오버스펙)

        Set<String> userMissingSkills = new HashSet<>(jobSkills);
        userMissingSkills.removeAll(userSkills);                // 공고만 요구 (부족 스킬)

        // 2. 지표 산출
        // Overlap: 구직자 기술 활용도 (내 스킬 중 공고에서 쓰이는 비율)
        double overlapRatio = userSkills.isEmpty() ? 0.0 :
                (double) matchedSkills.size() / userSkills.size();

        // Coverage: 기업 요구 충족도 (공고 요구 중 내가 보유한 비율)
        double coverageRatio = jobSkills.isEmpty() ? 0.0 :
                (double) matchedSkills.size() / jobSkills.size();

        // 3. 오버스펙 가산점 (로그 스케일)
        // log1p로 완만하게 증가, 최대 1.0 (100%)으로 제한
        // 1개: ~0.35, 3개: ~0.69, 10개: ~1.0
        double extraRatio = Math.min(1.0, Math.log1p(userExtraSkills.size()) / 2.0);

        // 4. 최종 하이브리드 점수 계산
        double hybridScore = (context.getVectorSimilarity() * WEIGHT_VECTOR)
                + (overlapRatio * WEIGHT_OVERLAP)
                + (coverageRatio * WEIGHT_COVERAGE)
                + (extraRatio * WEIGHT_EXTRA);

        return ScoringResult.builder()
                .hybridScore(round(hybridScore))
                .vectorScore(round(context.getVectorSimilarity()))
                .overlapRatio(round(overlapRatio))
                .coverageRatio(round(coverageRatio))
                .extraRatio(round(extraRatio))
                .matchedSkills(matchedSkills)
                .extraSkills(userExtraSkills)
                .missingSkills(userMissingSkills)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 100.0;
    }
}
