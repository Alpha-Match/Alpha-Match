package com.alpha.api.domain.skilldic.repository;

import com.alpha.api.domain.skilldic.entity.SkillCategoryDic;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * SkillCategoryDic Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - Skill category dictionary
 * - Parent table of skill_embedding_dic
 * - Used for skillCategories query
 */
public interface SkillCategoryDicRepository extends ReactiveCrudRepository<SkillCategoryDic, UUID> {

    /**
     * Find SkillCategoryDic by category name
     * - Case-insensitive exact match
     *
     * @param category Category name (e.g., "Backend", "Frontend")
     * @return Mono of SkillCategoryDic
     */
    @Query("SELECT * FROM skill_category_dic WHERE LOWER(category) = LOWER(:category)")
    Mono<SkillCategoryDic> findByCategory(String category);

    /**
     * Find all categories ordered by name
     * - For skillCategories query
     *
     * @return Flux of SkillCategoryDic
     */
    @Query("SELECT * FROM skill_category_dic ORDER BY category")
    Flux<SkillCategoryDic> findAllOrderByCategory();

    /**
     * Count total categories
     *
     * @return Mono of Long (total count)
     */
    Mono<Long> count();
}
