package com.alpha.backend.application;

import com.alpha.backend.config.BatchProperties;
import com.alpha.backend.grpc.EmbeddingGrpcClient;
import com.alpha.backend.grpc.RecruitRow;
import com.alpha.backend.grpc.RowChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * gRPC Streaming Test Service
 * Python AI Server로부터 Embedding 스트리밍 수신 테스트
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GrpcStreamTestService {

    private final EmbeddingGrpcClient embeddingGrpcClient;
    private final BatchProperties batchProperties;

    /**
     * Embedding Stream 테스트
     * Python Server로부터 데이터를 수신하고 로그로 확인
     *
     * @param lastProcessedUuid 마지막 처리된 UUID (null이면 처음부터)
     * @return 수신된 총 row 수
     */
    public int testEmbeddingStream(UUID lastProcessedUuid) {
        log.info("=".repeat(80));
        log.info("Starting gRPC Embedding Stream Test");
        log.info("Last Processed UUID: {}", lastProcessedUuid);
        log.info("Chunk Size: {}", batchProperties.getChunkSize());
        log.info("=".repeat(80));

        AtomicInteger totalRows = new AtomicInteger(0);
        AtomicInteger chunkCount = new AtomicInteger(0);

        try {
            // Embedding Stream 수신
            Flux<RowChunk> stream = embeddingGrpcClient.streamEmbeddings(
                    lastProcessedUuid,
                    batchProperties.getChunkSize()
            );

            // Stream 처리
            stream
                    .doOnNext(rowChunk -> {
                        int currentChunk = chunkCount.incrementAndGet();
                        int rowsInChunk = rowChunk.getRowsCount();
                        totalRows.addAndGet(rowsInChunk);

                        log.info("-".repeat(80));
                        log.info("Chunk #{}: Received {} rows", currentChunk, rowsInChunk);

                        // 첫 번째 row 샘플 출력
                        if (rowsInChunk > 0) {
                            RecruitRow firstRow = rowChunk.getRows(0);
                            log.info("Sample Row - ID: {}", firstRow.getId());
                            log.info("Sample Row - Company: {}", firstRow.getCompanyName());
                            log.info("Sample Row - Experience: {} years", firstRow.getExpYears());
                            log.info("Sample Row - English Level: {}", firstRow.getEnglishLevel());
                            log.info("Sample Row - Primary Keyword: {}", firstRow.getPrimaryKeyword());
                            log.info("Sample Row - Vector Dimension: {}", firstRow.getVectorCount());

                            // Vector 샘플 (첫 5개 값만)
                            if (firstRow.getVectorCount() > 0) {
                                StringBuilder vectorSample = new StringBuilder("Vector Sample (first 5): [");
                                for (int i = 0; i < Math.min(5, firstRow.getVectorCount()); i++) {
                                    vectorSample.append(String.format("%.4f", firstRow.getVector(i)));
                                    if (i < Math.min(4, firstRow.getVectorCount() - 1)) {
                                        vectorSample.append(", ");
                                    }
                                }
                                vectorSample.append(", ...]");
                                log.info(vectorSample.toString());
                            }
                        }
                    })
                    .doOnError(error -> {
                        log.error("Error during stream processing", error);
                    })
                    .doOnComplete(() -> {
                        log.info("=".repeat(80));
                        log.info("Stream Completed Successfully!");
                        log.info("Total Chunks Received: {}", chunkCount.get());
                        log.info("Total Rows Received: {}", totalRows.get());
                        log.info("=".repeat(80));
                    })
                    .blockLast(); // Block until stream completes (for testing purposes)

            return totalRows.get();

        } catch (Exception e) {
            log.error("Failed to test embedding stream", e);
            throw new RuntimeException("gRPC Streaming Test Failed", e);
        }
    }

    /**
     * Checkpoint를 사용한 재개 테스트
     */
    public void testStreamWithCheckpoint(String checkpointUuid) {
        log.info("Testing stream resume from checkpoint: {}", checkpointUuid);

        try {
            UUID lastUuid = UUID.fromString(checkpointUuid);
            int rowsReceived = testEmbeddingStream(lastUuid);
            log.info("Successfully resumed from checkpoint. Received {} rows", rowsReceived);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", checkpointUuid);
        }
    }

    /**
     * 전체 스트리밍 테스트 (처음부터)
     */
    public void testFullStream() {
        log.info("Testing full stream from beginning");
        int rowsReceived = testEmbeddingStream(null);
        log.info("Full stream test completed. Total rows: {}", rowsReceived);
    }

    /**
     * 연결 테스트 (간단한 ping)
     */
    public void testConnection() {
        log.info("Testing gRPC connection to Python Server...");

        try {
            Flux<RowChunk> stream = embeddingGrpcClient.streamEmbeddings(null, 1);

            stream
                    .take(1) // 첫 번째 chunk만 받기
                    .doOnNext(chunk -> {
                        log.info("Connection successful! Received {} rows", chunk.getRowsCount());
                    })
                    .doOnError(error -> {
                        log.error("Connection failed!", error);
                    })
                    .blockLast();

        } catch (Exception e) {
            log.error("Connection test failed", e);
        }
    }
}
