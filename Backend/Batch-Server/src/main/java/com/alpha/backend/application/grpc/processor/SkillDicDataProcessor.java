package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.domain.skilldic.entity.SkillCategoryDicEntity;
import com.alpha.backend.domain.skilldic.entity.SkillEmbeddingDicEntity;
import com.alpha.backend.domain.skilldic.repository.SkillCategoryDicRepository;
import com.alpha.backend.domain.skilldic.repository.SkillEmbeddingDicRepository;
import com.alpha.backend.infrastructure.grpc.proto.RowChunk;
import com.alpha.backend.infrastructure.grpc.proto.SkillEmbeddingDicRow;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Skill Dictionary 데이터 처리기
 * <p>
 * Python 서버로부터 전송된 SkillDic RowChunk를 처리하여 2개 테이블에 저장:
 * - skill_category_dic: 카테고리 정보
 * - skill_embedding_dic: 스킬 및 벡터 정보
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillDicDataProcessor implements DataProcessor {

    private final SkillCategoryDicRepository skillCategoryDicRepository;
    private final SkillEmbeddingDicRepository skillEmbeddingDicRepository;

    // 카테고리명 -> UUID 캐시 (동일 chunk 내에서 중복 조회 방지)
    private final Map<String, UUID> categoryCache = new HashMap<>();

    @Override
    public String getDomain() {
        return "skill_dic";
    }

    @Override
    public int processChunk(RowChunk rowChunk) {
        List<SkillEmbeddingDicRow> rows = rowChunk.getSkillDic().getRowsList();
        log.info("Received {} skill_dic rows from RowChunk", rows.size());

        if (rows.isEmpty()) {
            log.warn("Empty row list, skipping save");
            return 0;
        }

        // 3. Row → Entity 변환 (2개 테이블)
        Set<String> categories = new HashSet<>();
        List<SkillEmbeddingDicEntity> embeddingEntities = new ArrayList<>();

        // 모든 카테고리 추출
        for (SkillEmbeddingDicRow row : rows) {
            if (!row.getPositionCategory().isBlank()) {
                categories.add(row.getPositionCategory());
            }
        }

        // 카테고리 저장 및 ID 캐싱
        for (String category : categories) {
            UUID categoryId = getOrCreateCategoryId(category);
            categoryCache.put(category, categoryId);
        }

        // 스킬 임베딩 엔티티 생성
        for (SkillEmbeddingDicRow row : rows) {
            UUID categoryId = categoryCache.get(row.getPositionCategory());
            if (categoryId == null) {
                log.warn("Category not found for skill: {}, skipping", row.getSkill());
                continue;
            }

            embeddingEntities.add(toSkillEmbeddingEntity(row, categoryId));
        }

        // DB 저장
        skillEmbeddingDicRepository.upsertAll(embeddingEntities);
        log.info("Upserted {} skill embedding entities", embeddingEntities.size());

        return rows.size();
    }

    private UUID getOrCreateCategoryId(String category) {
        if (categoryCache.containsKey(category)) {
            return categoryCache.get(category);
        }

        Optional<SkillCategoryDicEntity> existing = skillCategoryDicRepository.findByCategory(category);
        if (existing.isPresent()) {
            UUID categoryId = existing.get().getCategoryId();
            log.debug("Found existing category: {} -> {}", category, categoryId);
            return categoryId;
        }

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

    private SkillEmbeddingDicEntity toSkillEmbeddingEntity(SkillEmbeddingDicRow row, UUID categoryId) {
        List<Float> vectorList = row.getSkillVectorList();
        float[] vector = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            vector[i] = vectorList.get(i);
        }

        return SkillEmbeddingDicEntity.builder()
                .categoryId(categoryId)
                .skill(row.getSkill())
                .skillVector(new PGvector(vector))
                .build();
    }
}
