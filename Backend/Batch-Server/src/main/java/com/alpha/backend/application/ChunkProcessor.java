package com.alpha.backend.application;

import com.alpha.backend.config.BatchProperties;
import com.alpha.backend.domain.embedding.EmbeddingEntity;
import com.alpha.backend.domain.embedding.EmbeddingRepository;
import com.alpha.backend.domain.metadata.MetadataEntity;
import com.alpha.backend.domain.metadata.MetadataRepository;
import com.alpha.backend.grpc.RecruitRow;
import com.alpha.backend.grpc.RowChunk;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Chunk Processor
 * gRPC로 수신한 RowChunk를 Metadata와 Embedding으로 분리하여 DB에 저장
 *
 * 핵심 로직:
 * 1. RowChunk를 Metadata와 Embedding으로 분리
 * 2. Metadata 먼저 저장 (FK 제약 조건)
 * 3. Embedding 저장
 * 4. 상세 로깅 (스레드, 청크 사이즈, 마지막 UUID, 마지막 데이터)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkProcessor {

    private final MetadataRepository metadataRepository;
    private final EmbeddingRepository embeddingRepository;
    private final BatchProperties batchProperties;

    /**
     * RowChunk를 처리하여 DB에 저장
     *
     * @param chunk gRPC로 수신한 RowChunk
     * @return 처리된 마지막 UUID
     */
    @Transactional
    public UUID processChunk(RowChunk chunk) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().toString();
        int chunkSize = chunk.getRowsCount();

        log.info("=== Chunk Processing Started ===");
        log.info("Thread: {} | Chunk Size: {}", threadName, chunkSize);

        if (chunkSize == 0) {
            log.warn("Empty chunk received");
            return null;
        }

        try {
            // 1. Metadata와 Embedding 리스트 생성
            List<MetadataEntity> metadataList = new ArrayList<>(chunkSize);
            List<EmbeddingEntity> embeddingList = new ArrayList<>(chunkSize);

            for (RecruitRow row : chunk.getRowsList()) {
                UUID id = UUID.fromString(row.getId());

                // Metadata Entity 생성
                MetadataEntity metadata = MetadataEntity.builder()
                        .id(id)
                        .companyName(row.getCompanyName())
                        .expYears(row.getExpYears())
                        .englishLevel(row.getEnglishLevel())
                        .primaryKeyword(row.getPrimaryKeyword())
                        .build();
                metadataList.add(metadata);

                // Embedding Entity 생성
                float[] vectorArray = convertToFloatArray(row.getVectorList());
                validateVectorDimension(vectorArray, id);

                EmbeddingEntity embedding = EmbeddingEntity.builder()
                        .id(id)
                        .vector(new PGvector(vectorArray))
                        .build();
                embeddingList.add(embedding);
            }

            // 2. Metadata 먼저 저장 (FK 제약 조건)
            long metadataStartTime = System.currentTimeMillis();
            metadataRepository.upsertAll(metadataList);
            long metadataElapsed = System.currentTimeMillis() - metadataStartTime;
            log.debug("Metadata upsert completed in {} ms", metadataElapsed);

            // 3. Embedding 저장
            long embeddingStartTime = System.currentTimeMillis();
            embeddingRepository.upsertAll(embeddingList);
            long embeddingElapsed = System.currentTimeMillis() - embeddingStartTime;
            log.debug("Embedding upsert completed in {} ms", embeddingElapsed);

            // 4. 마지막 데이터 추출
            RecruitRow lastRow = chunk.getRows(chunkSize - 1);
            UUID lastUuid = UUID.fromString(lastRow.getId());
            float[] lastVector = convertToFloatArray(lastRow.getVectorList());

            // 5. 상세 로깅
            long totalElapsed = System.currentTimeMillis() - startTime;
            log.info("=== Chunk Processing Completed ===");
            log.info("Thread: {} | Chunk Size: {} | Last UUID: {}",
                    threadName, chunkSize, lastUuid);
            log.info("Last Data: {{ company: \"{}\", position: \"{}\", exp_years: {}, vector_dim: {} }}",
                    lastRow.getCompanyName(),
                    lastRow.getPrimaryKeyword(),
                    lastRow.getExpYears(),
                    lastVector.length);
            log.info("Processing Time: metadata={}ms, embedding={}ms, total={}ms",
                    metadataElapsed, embeddingElapsed, totalElapsed);

            return lastUuid;

        } catch (Exception e) {
            log.error("Error processing chunk on thread {}: {}", threadName, e.getMessage(), e);
            throw new RuntimeException("Chunk processing failed", e);
        }
    }

    /**
     * List<Float>를 float[]로 변환
     */
    private float[] convertToFloatArray(List<Float> vectorList) {
        float[] array = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            array[i] = vectorList.get(i);
        }
        return array;
    }

    /**
     * Vector 차원 검증
     */
    private void validateVectorDimension(float[] vector, UUID id) {
        int expectedDim = batchProperties.getVectorDimension();
        if (vector.length != expectedDim) {
            String errorMsg = String.format(
                    "Vector dimension mismatch for UUID %s: expected=%d, actual=%d",
                    id, expectedDim, vector.length
            );
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Chunk 통계 정보 반환
     */
    public ChunkStats getChunkStats(RowChunk chunk) {
        if (chunk.getRowsCount() == 0) {
            return new ChunkStats(0, null, 0);
        }

        RecruitRow lastRow = chunk.getRows(chunk.getRowsCount() - 1);
        UUID lastUuid = UUID.fromString(lastRow.getId());
        int vectorDim = lastRow.getVectorCount();

        return new ChunkStats(chunk.getRowsCount(), lastUuid, vectorDim);
    }

    /**
     * Chunk 통계 정보 클래스
     */
    public record ChunkStats(
            int rowCount,
            UUID lastUuid,
            int vectorDimension
    ) {}
}
