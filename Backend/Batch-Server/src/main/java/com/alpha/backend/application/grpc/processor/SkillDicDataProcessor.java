package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.application.grpc.dto.SkillDicRowDto;
import com.alpha.backend.domain.skilldic.entity.SkillCategoryDicEntity;
import com.alpha.backend.domain.skilldic.entity.SkillEmbeddingDicEntity;
import com.alpha.backend.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.backend.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Skill Dictionary 데이터 처리기
 * <p>
 * Python 서버로부터 전송된 Skill Dictionary 데이터를 JSON에서 파싱하여 2개 테이블에 저장
 * - skill_category_dic: 카테고리 정보
 * - skill_embedding_dic: 스킬 및 벡터 정보
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillDicDataProcessor implements DataProcessor {

    private final ObjectMapper objectMapper;
    private final SkillCategoryDicRepository skillCategoryDicRepository;
    private final SkillEmbeddingDicRepository skillEmbeddingDicRepository;

    // 카테고리명 -> UUID 캐시 (동일 chunk 내에서 중복 조회 방지)
    private final Map<String, UUID> categoryCache = new HashMap<>();

    @Override
    public String getDomain() {
        return "skill_dic";
    }

    @Override
    public int processChunk(byte[] jsonChunk) {
        try {
            // 1. bytes → UTF-8 문자열
            String jsonString = new String(jsonChunk, StandardCharsets.UTF_8);
            log.debug("Received JSON chunk: {} bytes", jsonChunk.length);

            // 2. JSON → DTO 리스트
            List<SkillDicRowDto> dtos = objectMapper.readValue(
                    jsonString,
                    new TypeReference<List<SkillDicRowDto>>() {
                    }
            );
            log.info("Parsed {} skill rows from JSON", dtos.size());

            if (dtos.isEmpty()) {
                log.warn("Empty DTO list, skipping save");
                return 0;
            }

            // 3. DTO → Entity 변환 (2개 테이블)
            Set<String> categories = new HashSet<>();
            List<SkillEmbeddingDicEntity> embeddingEntities = new ArrayList<>();

            // 3-1. 모든 카테고리 추출
            for (SkillDicRowDto dto : dtos) {
                if (dto.getPositionCategory() != null && !dto.getPositionCategory().isBlank()) {
                    categories.add(dto.getPositionCategory());
                }
            }

            // 3-2. 카테고리 저장 및 ID 캐싱
            for (String category : categories) {
                UUID categoryId = getOrCreateCategoryId(category);
                categoryCache.put(category, categoryId);
            }

            // 3-3. 스킬 임베딩 엔티티 생성
            for (SkillDicRowDto dto : dtos) {
                UUID categoryId = categoryCache.get(dto.getPositionCategory());
                if (categoryId == null) {
                    log.warn("Category not found for skill: {}, skipping", dto.getSkill());
                    continue;
                }

                embeddingEntities.add(toSkillEmbeddingEntity(dto, categoryId));
            }

            // 4. DB 저장
            skillEmbeddingDicRepository.upsertAll(embeddingEntities);
            log.info("Upserted {} skill embedding entities", embeddingEntities.size());

            return dtos.size();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON chunk: {}", e.getMessage());
            throw new RuntimeException("데이터 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to process skill_dic chunk: {}", e.getMessage(), e);
            throw new RuntimeException("데이터 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 카테고리 조회 또는 생성
     *
     * @param category 카테고리명
     * @return 카테고리 UUID
     */
    private UUID getOrCreateCategoryId(String category) {
        // 캐시 확인
        if (categoryCache.containsKey(category)) {
            return categoryCache.get(category);
        }

        // DB 조회
        Optional<SkillCategoryDicEntity> existing = skillCategoryDicRepository.findByCategory(category);
        if (existing.isPresent()) {
            UUID categoryId = existing.get().getCategoryId();
            log.debug("Found existing category: {} -> {}", category, categoryId);
            return categoryId;
        }

        // 신규 생성
        SkillCategoryDicEntity newCategory = SkillCategoryDicEntity.builder()
                .category(category)
                .build();

        skillCategoryDicRepository.upsert(newCategory);

        // Native Query는 ID를 반환하지 않으므로 다시 조회
        Optional<SkillCategoryDicEntity> created = skillCategoryDicRepository.findByCategory(category);
        if (created.isEmpty()) {
            throw new RuntimeException("카테고리 생성 실패: " + category);
        }

        UUID categoryId = created.get().getCategoryId();
        log.info("Created new category: {} -> {}", category, categoryId);

        return categoryId;
    }

    /**
     * DTO → SkillEmbeddingDicEntity 변환
     */
    private SkillEmbeddingDicEntity toSkillEmbeddingEntity(SkillDicRowDto dto, UUID categoryId) {
        // List<Double> → float[]
        float[] vector = new float[dto.getSkillVector().size()];
        for (int i = 0; i < dto.getSkillVector().size(); i++) {
            vector[i] = dto.getSkillVector().get(i).floatValue();
        }

        return SkillEmbeddingDicEntity.builder()
                .categoryId(categoryId)
                .skill(dto.getSkill())
                .skillVector(new PGvector(vector))
                .build();
    }
}
