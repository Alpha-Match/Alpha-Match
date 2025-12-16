package com.alpha.backend.application.batch.processor;

import com.alpha.backend.domain.recruit.entity.RecruitEmbeddingEntity;
import com.alpha.backend.domain.recruit.entity.RecruitMetadataEntity;
import com.alpha.backend.infrastructure.grpc.proto.RecruitRow;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Recruit ItemProcessor
 *
 * Recruit 도메인의 Proto 객체(RecruitRow) → DomainItem 변환
 */
@Slf4j
public class RecruitItemProcessor
        extends DomainItemProcessor<RecruitRow, RecruitMetadataEntity, RecruitEmbeddingEntity> {

    public RecruitItemProcessor(BatchProperties batchProperties) {
        super(batchProperties);
    }

    @Override
    protected UUID extractId(RecruitRow protoRow) {
        return UUID.fromString(protoRow.getId());
    }

    @Override
    protected List<Float> extractVector(RecruitRow protoRow) {
        return protoRow.getVectorList();
    }

    @Override
    protected RecruitMetadataEntity createMetadata(RecruitRow protoRow, UUID id) {
        RecruitMetadataEntity metadata = new RecruitMetadataEntity();
        metadata.setId(id);
        metadata.setCompanyName(protoRow.getCompanyName());
        metadata.setExpYears(protoRow.getExpYears());
        metadata.setEnglishLevel(protoRow.getEnglishLevel());
        metadata.setPrimaryKeyword(protoRow.getPrimaryKeyword());
        return metadata;
    }

    @Override
    protected RecruitEmbeddingEntity createEmbedding(UUID id, float[] vectorArray) {
        RecruitEmbeddingEntity embedding = new RecruitEmbeddingEntity();
        embedding.setId(id);
        embedding.setVector(new PGvector(vectorArray));
        return embedding;
    }

    @Override
    protected String getDomainName() {
        return "recruit";
    }
}
