package com.alpha.backend.application.batch.dto;

import com.alpha.backend.domain.common.BaseMetadataEntity;
import com.alpha.backend.domain.common.BaseEmbeddingEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain Item DTO
 * ItemProcessor와 ItemWriter 간 데이터 전달을 위한 DTO
 * 하나의 도메인 아이템은 Metadata와 Embedding Entity로 구성됨
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomainItem<M extends BaseMetadataEntity, E extends BaseEmbeddingEntity> {
    
    private M metadata;
    private E embedding;
    
    /**
     * 도메인 타입 반환 (metadata에서 추출)
     */
    public String getDomainType() {
        return metadata != null ? metadata.getDomainType() : null;
    }
}
