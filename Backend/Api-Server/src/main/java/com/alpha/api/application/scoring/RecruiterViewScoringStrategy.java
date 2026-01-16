package com.alpha.api.application.scoring;

import com.alpha.api.domain.scoring.ScoringContext;
import com.alpha.api.domain.scoring.ScoringResult;
import com.alpha.api.domain.scoring.ScoringStrategy;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 기업 관점 스코어링 전략 (Recruiter 모드)
 *
 * 기업이 후보자를 볼 때 적용되는 스코어링.
 * "이 후보자가 우리 요구사항을 얼마나 충족하는가?"
 *
 * 가중치: Vector(40%) + Coverage(30%) + Overlap(30%) - Overspec(10%)
 * - 오버스펙(후보자가 요구사항보다 더 많은 기술 보유)에 감점 부여
 * - 이탈 위험, 연봉 미스매치 가능성을 반영
 */
@Component
public class RecruiterViewScoringStrategy implements ScoringStrategy {

    // 가중치 상수
    private static final double WEIGHT_VECTOR = 0.40;
    private static final double WEIGHT_COVERAGE = 0.30;
    private static final double WEIGHT_OVERLAP = 0.30;
    private static final double WEIGHT_EXTRA = 0.10;  // 최대 10% 감점

    @Override
    public ScoringResult calculate(ScoringContext context) {
        Set<String> jobRequirements = context.getSearchSkills();  // 기업 요구 기술
        Set<String> candidateSkills = context.getTargetSkills();  // 후보자 보유 기술

        // 1. 스킬 집합 계산 (교집합, 차집합)
        Set<String> matchedSkills = new HashSet<>(jobRequirements);
        matchedSkills.retainAll(candidateSkills);                  // 교집합: 일치 기술

        Set<String> missingSkills = new HashSet<>(jobRequirements);
        missingSkills.removeAll(candidateSkills);                  // 기업 요구 중 후보자 미보유

        Set<String> candidateExtraSkills = new HashSet<>(candidateSkills);
        candidateExtraSkills.removeAll(jobRequirements);           // 후보자만 보유 (오버스펙)

        // 2. 지표 산출
        // Coverage: 요구사항 충족도 (기업 요구 중 후보자가 보유한 비율)
        double coverageRatio = jobRequirements.isEmpty() ? 0.0 :
                (double) matchedSkills.size() / jobRequirements.size();

        // Overlap: 후보자 기술 활용도 (후보자 스킬 중 기업이 원하는 비율)
        double overlapRatio = candidateSkills.isEmpty() ? 0.0 :
                (double) matchedSkills.size() / candidateSkills.size();

        // 3. 오버스펙 감점 (로그 스케일)
        // log1p로 완만하게 증가, 최대 10%로 제한
        // 분모를 20.0으로 설정하여 가산점보다 완만하게 적용
        double overspecPenalty = Math.min(1.0, Math.log1p(candidateExtraSkills.size()) / 2.0);

        // 4. 최종 하이브리드 점수 계산 (감점 적용)
        double hybridScore = (context.getVectorSimilarity() * WEIGHT_VECTOR)
                + (coverageRatio * WEIGHT_COVERAGE)
                + (overlapRatio * WEIGHT_OVERLAP)
                + (overspecPenalty * WEIGHT_EXTRA);;

        // 점수가 음수가 되지 않도록 보정
        hybridScore = Math.max(0.0, hybridScore);

        return ScoringResult.builder()
                .hybridScore(round(hybridScore))
                .vectorScore(round(context.getVectorSimilarity()))
                .overlapRatio(round(overlapRatio))
                .coverageRatio(round(coverageRatio))
                .extraRatio(round(-overspecPenalty))  // 음수로 표시 (감점)
                .matchedSkills(matchedSkills)
                .extraSkills(missingSkills)          // 기업이 원하지만 후보자 미보유
                .missingSkills(candidateExtraSkills) // 후보자의 오버스펙
                .build();
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 100.0;
    }
}
