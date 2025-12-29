package com.alpha.api.application.service;

import com.alpha.api.domain.candidate.repository.CandidateSkillRepository;
import com.alpha.api.domain.recruit.repository.RecruitSkillRepository;
import com.alpha.api.domain.skilldic.entity.SkillCategoryDic;
import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.alpha.api.infrastructure.graphql.type.DashboardCategoryData;
import com.alpha.api.infrastructure.graphql.type.DashboardSkillStat;
import com.alpha.api.infrastructure.graphql.type.UserMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Dashboard Service
 * - Provides category-level skill statistics for dashboard visualization
 * - userMode: CANDIDATE shows recruit statistics, RECRUITER shows candidate statistics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SkillCategoryDicRepository skillCategoryDicRepository;
    private final SkillEmbeddingDicRepository skillEmbeddingDicRepository;
    private final RecruitSkillRepository recruitSkillRepository;
    private final CandidateSkillRepository candidateSkillRepository;

    /**
     * Get dashboard data by user mode
     * - Returns category-level skill statistics
     *
     * @param userMode User mode (CANDIDATE or RECRUITER)
     * @return Mono<List<DashboardCategoryData>>
     */
    public Mono<List<DashboardCategoryData>> getDashboardData(UserMode userMode) {
        log.info("getDashboardData called - userMode: {}", userMode);

        // Get all categories with their skills
        return skillCategoryDicRepository.findAllOrderByCategory()
                .flatMap(category -> {
                    // For each category, get skills and their counts
                    return skillEmbeddingDicRepository.findByCategoryId(category.getCategoryId())
                            .flatMap(skillEmbedding -> {
                                // Get count based on user mode
                                Mono<Long> countMono = (userMode == UserMode.CANDIDATE)
                                        ? recruitSkillRepository.countBySkill(skillEmbedding.getSkill())
                                        : candidateSkillRepository.countBySkill(skillEmbedding.getSkill());

                                return countMono.map(count -> DashboardSkillStat.builder()
                                        .skill(skillEmbedding.getSkill())
                                        .count(count.intValue())
                                        .build());
                            })
                            .collectList()
                            .map(skillStats -> DashboardCategoryData.builder()
                                    .category(category.getCategory())
                                    .skills(skillStats)
                                    .build());
                })
                .collectList();
    }
}
