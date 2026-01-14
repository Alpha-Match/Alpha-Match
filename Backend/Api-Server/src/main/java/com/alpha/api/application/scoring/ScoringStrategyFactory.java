package com.alpha.api.application.scoring;

import com.alpha.api.domain.scoring.ScoringStrategy;
import com.alpha.api.presentation.graphql.type.UserMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스코어링 전략 팩토리
 *
 * UserMode에 따라 적절한 ScoringStrategy 구현체를 반환.
 */
@Component
@RequiredArgsConstructor
public class ScoringStrategyFactory {

    private final CandidateViewScoringStrategy candidateViewStrategy;
    private final RecruiterViewScoringStrategy recruiterViewStrategy;

    /**
     * UserMode에 해당하는 스코어링 전략을 반환합니다.
     *
     * @param mode CANDIDATE: 구직자 관점, RECRUITER: 기업 관점
     * @return 해당 모드의 ScoringStrategy 구현체
     */
    public ScoringStrategy getStrategy(UserMode mode) {
        return switch (mode) {
            case CANDIDATE -> candidateViewStrategy;
            case RECRUITER -> recruiterViewStrategy;
        };
    }
}
