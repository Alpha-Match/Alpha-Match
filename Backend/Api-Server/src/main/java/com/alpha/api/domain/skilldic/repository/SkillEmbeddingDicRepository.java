package com.alpha.api.domain.skilldic.repository;

import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * SkillEmbeddingDic Repository (Domain Interface - Port)
 * - R2DBC based reactive repository
 * - Skill normalization dictionary
 * - Maps skill names to vector embeddings (1536d)
 * - Used to convert user input skills to query vector
 */
public interface SkillEmbeddingDicRepository extends ReactiveCrudRepository<SkillEmbeddingDic, UUID> {

    /**
     * Find SkillEmbeddingDic by skill name
     * - Case-insensitive exact match
     *
     * @param skill Skill name (e.g., "Java", "Python")
     * @return Mono of SkillEmbeddingDic
     */
    @Query("SELECT * FROM skill_embedding_dic WHERE LOWER(skill) = LOWER(:skill)")
    Mono<SkillEmbeddingDic> findBySkill(String skill);

    /**
     * Find multiple skills by names
     * - Used to convert skill list to vector embeddings
     * - For searchMatches query
     *
     * @param skills List of skill names (lowercase)
     * @return Flux of SkillEmbeddingDic
     */
    @Query("SELECT * FROM skill_embedding_dic WHERE LOWER(skill) IN (:skills)")
    Flux<SkillEmbeddingDic> findBySkillIn(List<String> skills);

    /**
     * Find skills by category
     * - For skillCategories query (Frontend GET_SKILL_CATEGORIES)
     *
     * @param categoryId Category ID
     * @return Flux of SkillEmbeddingDic
     */
    @Query("SELECT * FROM skill_embedding_dic WHERE category_id = :categoryId ORDER BY skill")
    Flux<SkillEmbeddingDic> findByCategoryId(UUID categoryId);

    /**
     * Find all skills grouped by category
     * - For skillCategories query
     * - Joins with skill_category_dic
     *
     * @return Flux of SkillEmbeddingDic with category info
     */
    @Query("""
        SELECT sed.skill_id, sed.category_id, sed.skill, sed.skill_vector,
               sed.created_at, sed.updated_at, scd.category
        FROM skill_embedding_dic sed
        INNER JOIN skill_category_dic scd ON sed.category_id = scd.category_id
        ORDER BY scd.category, sed.skill
        """)
    Flux<SkillEmbeddingDic> findAllWithCategory();

    /**
     * Search skills by partial name match
     * - For autocomplete/search functionality
     *
     * @param partialSkill Partial skill name
     * @param limit Maximum number of results
     * @return Flux of SkillEmbeddingDic
     */
    @Query("SELECT * FROM skill_embedding_dic WHERE skill ILIKE CONCAT('%', :partialSkill, '%') LIMIT :limit")
    Flux<SkillEmbeddingDic> searchBySkillContaining(String partialSkill, Integer limit);

    /**
     * Count total skills
     *
     * @return Mono of Long (total count)
     */
    Mono<Long> count();
}
