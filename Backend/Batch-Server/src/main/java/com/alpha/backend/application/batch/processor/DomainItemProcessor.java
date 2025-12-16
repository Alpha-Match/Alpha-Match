package com.alpha.backend.application.batch.processor;

import com.alpha.backend.application.batch.dto.DomainItem;
import com.alpha.backend.domain.common.BaseEmbeddingEntity;
import com.alpha.backend.domain.common.BaseMetadataEntity;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.util.List;
import java.util.UUID;

/**
 * Domain ItemProcessor (추상 클래스)
 *
 * Proto 객체 → DomainItem 변환의 공통 로직 제공
 *
 * 공통 기능:
 * - List<Float> → float[] 변환
 * - Vector 차원 검증
 * - PGvector 생성
 *
 * 하위 클래스 구현:
 * - Proto → Metadata Entity 변환
 * - Proto → Embedding Entity 변환
 *
 * @param <I> Input Proto 타입 (RecruitRow, CandidateRow 등)
 * @param <M> Metadata Entity 타입
 * @param <E> Embedding Entity 타입
 */
@Slf4j
@RequiredArgsConstructor
public abstract class DomainItemProcessor<I, M extends BaseMetadataEntity, E extends BaseEmbeddingEntity>
        implements ItemProcessor<I, DomainItem<M, E>> {

    protected final BatchProperties batchProperties;

    /**
     * Proto → DomainItem 변환 (Template Method Pattern)
     */
    @Override
    public DomainItem<M, E> process(I protoRow) throws Exception {
        try {
            // 1. UUID 추출
            UUID id = extractId(protoRow);

            // 2. Metadata Entity 생성 (하위 클래스 구현)
            M metadata = createMetadata(protoRow, id);

            // 3. Vector 추출 및 변환
            List<Float> vectorList = extractVector(protoRow);
            float[] vectorArray = convertToFloatArray(vectorList);
            validateVectorDimension(vectorArray, id);

            // 4. Embedding Entity 생성
            E embedding = createEmbedding(id, vectorArray);

            // 5. DomainItem 생성
            DomainItem<M, E> item = new DomainItem<>(metadata, embedding);

            log.debug("[PROCESSOR] Domain: {} | Processed row: {}", getDomainName(), id);

            return item;

        } catch (Exception e) {
            log.error("[PROCESSOR] Domain: {} | Failed to process row | Error: {}",
                    getDomainName(), e.getMessage());
            throw e;  // Spring Batch가 skip 정책에 따라 처리
        }
    }

    /**
     * Proto Row에서 UUID 추출 (하위 클래스 구현)
     */
    protected abstract UUID extractId(I protoRow);

    /**
     * Proto Row에서 Vector 추출 (하위 클래스 구현)
     */
    protected abstract List<Float> extractVector(I protoRow);

    /**
     * Metadata Entity 생성 (하위 클래스 구현)
     */
    protected abstract M createMetadata(I protoRow, UUID id);

    /**
     * Embedding Entity 생성 (공통 로직)
     */
    protected abstract E createEmbedding(UUID id, float[] vectorArray);

    /**
     * 도메인 이름 반환 (로깅용)
     */
    protected abstract String getDomainName();

    /**
     * List<Float> → float[] 변환 (공통 로직)
     */
    protected float[] convertToFloatArray(List<Float> vectorList) {
        if (vectorList == null || vectorList.isEmpty()) {
            throw new IllegalArgumentException("Vector list is null or empty");
        }

        float[] array = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            array[i] = vectorList.get(i);
        }
        return array;
    }

    /**
     * Vector 차원 검증 (공통 로직)
     */
    protected void validateVectorDimension(float[] vector, UUID id) {
        int expectedDim = batchProperties.getDomainConfig(getDomainName()).getVectorDimension();

        if (vector.length != expectedDim) {
            String errorMsg = String.format(
                    "Vector dimension mismatch for UUID %s (domain=%s): expected=%d, actual=%d",
                    id, getDomainName(), expectedDim, vector.length
            );
            log.error("[PROCESSOR] {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * PGvector 생성 헬퍼 메서드 (공통 로직)
     */
    protected PGvector createPGvector(float[] vectorArray) {
        return new PGvector(vectorArray);
    }
}
