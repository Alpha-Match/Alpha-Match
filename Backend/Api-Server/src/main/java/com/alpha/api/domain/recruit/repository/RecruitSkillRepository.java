package com.alpha.api.domain.recruit.repository;

import com.alpha.api.domain.common.SkillCount;
import com.alpha.api.domain.recruit.entity.RecruitSkill;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * RecruitSkill Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - 1:N relationship with Recruit
 * - Stores individual skills for each recruit
 */
public interface RecruitSkillRepository extends ReactiveCrudRepository<RecruitSkill, UUID> {

    /**
     * Find all skills for a specific recruit
     *
     * @param recruitId Recruit ID
     * @return Flux of RecruitSkill
     */
    @Query("SELECT * FROM recruit_skill WHERE recruit_id = :recruitId")
    Flux<RecruitSkill> findByRecruitId(UUID recruitId);

    /**
     * Find recruits that have a specific skill
     *
     * @param skill Skill name
     * @return Flux of recruit IDs
     */
    @Query("SELECT DISTINCT recruit_id FROM recruit_skill WHERE skill = :skill")
    Flux<UUID> findRecruitIdsBySkill(String skill);

    /**
     * Count recruits by skill (for dashboard statistics)
     *
     * @param skill Skill name
     * @return Mono<Long> count
     */
    @Query("SELECT COUNT(*) FROM recruit_skill WHERE skill = :skill")
    Mono<Long> countBySkill(String skill);
}
