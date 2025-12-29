package com.alpha.api.domain.skilldic.service;

import com.alpha.api.domain.skilldic.entity.SkillEmbeddingDic;
import com.alpha.api.domain.skilldic.repository.SkillEmbeddingDicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Skill Normalization Service
 * - Converts skill names to query vector
 * - Core logic for vector similarity search
 * - Follows table_specification.md normalization flow
 *
 * Flow:
 * 1. Input: List<String> skills (e.g., ["Java", "Python", "C"])
 * 2. Lookup each skill in skill_embedding_dic
 * 3. Calculate query vector (average/sum of skill vectors)
 * 4. Return query vector for pgvector search
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillNormalizationService {

    private final SkillEmbeddingDicRepository skillEmbeddingDicRepository;

    /**
     * Normalize skills to query vector
     * - Looks up each skill in skill_embedding_dic
     * - Averages the vectors to create query vector
     * - Returns vector as PostgreSQL-compatible string
     *
     * @param skills List of skill names (e.g., ["Java", "Python"])
     * @return Mono<String> Query vector as string (e.g., "[0.1, 0.2, ...]")
     */
    public Mono<String> normalizeSkillsToQueryVector(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            log.warn("Empty skills list provided for normalization");
            return Mono.error(new IllegalArgumentException("Skills list cannot be empty"));
        }

        log.info("Normalizing {} skills to query vector: {}", skills.size(), skills);

        // Convert skills to lowercase for case-insensitive lookup
        List<String> lowercaseSkills = skills.stream()
                .map(String::toLowerCase)
                .toList();

        // Lookup skills in skill_embedding_dic one by one
        // Note: R2DBC has issues with IN clause for List parameters, so we query individually
        return reactor.core.publisher.Flux.fromIterable(lowercaseSkills)
                .flatMap(skillEmbeddingDicRepository::findBySkill)
                .collectList()
                .flatMap(skillEmbeddings -> {
                    if (skillEmbeddings.isEmpty()) {
                        log.warn("No matching skills found in dictionary for: {}", skills);
                        return Mono.error(new IllegalArgumentException("No matching skills found in dictionary"));
                    }

                    log.info("Found {} matching skills in dictionary", skillEmbeddings.size());

                    // Calculate average vector
                    float[] queryVector = calculateAverageVector(skillEmbeddings);

                    // Convert to PostgreSQL vector format string
                    String vectorString = convertToVectorString(queryVector);

                    log.debug("Generated query vector: {} (length: {})",
                              vectorString.substring(0, Math.min(100, vectorString.length())),
                              queryVector.length);

                    return Mono.just(vectorString);
                });
    }

    /**
     * Calculate average vector from multiple skill vectors
     * - Averages all dimensions across all skill vectors
     * - Normalization strategy: mean (can be changed to sum if needed)
     *
     * @param skillEmbeddings List of SkillEmbeddingDic
     * @return float[] Average vector (384 dimensions)
     */
    private float[] calculateAverageVector(List<SkillEmbeddingDic> skillEmbeddings) {
        int vectorDimension = 384; // from table_specification.md
        float[] sumVector = new float[vectorDimension];
        int count = skillEmbeddings.size();

        // Sum all vectors
        for (SkillEmbeddingDic embedding : skillEmbeddings) {
            List<Float> skillVector = embedding.getSkillVector();
            if (skillVector == null || skillVector.isEmpty()) {
                log.warn("Skill '{}' has null or empty vector", embedding.getSkill());
                continue;
            }
            if (skillVector.size() != vectorDimension) {
                log.warn("Skill '{}' has unexpected vector dimension: {} (expected: {})",
                         embedding.getSkill(), skillVector.size(), vectorDimension);
                continue;
            }

            for (int i = 0; i < vectorDimension; i++) {
                sumVector[i] += skillVector.get(i);
            }
        }

        // Calculate average
        float[] avgVector = new float[vectorDimension];
        for (int i = 0; i < vectorDimension; i++) {
            avgVector[i] = sumVector[i] / count;
        }

        return avgVector;
    }

    /**
     * Convert float array to PostgreSQL vector string
     * - Input: float[] {0.1f, 0.2f, 0.3f, ...}
     * - Output: "[0.1, 0.2, 0.3, ...]"
     *
     * @param vector Vector array
     * @return String PostgreSQL vector string
     */
    private String convertToVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Validate skill name
     * - Checks if skill exists in dictionary
     * - Used for input validation
     *
     * @param skill Skill name
     * @return Mono<Boolean> True if skill exists
     */
    public Mono<Boolean> isValidSkill(String skill) {
        return skillEmbeddingDicRepository.findBySkill(skill)
                .hasElement();
    }

    /**
     * Get available skills by category
     * - Used for Frontend skillCategories query
     *
     * @return Mono<List<SkillEmbeddingDic>> All skills with category info
     */
    public Mono<List<SkillEmbeddingDic>> getAllSkillsWithCategory() {
        return skillEmbeddingDicRepository.findAllWithCategory()
                .collectList();
    }
}
