package com.alpha.backend.application;

import com.alpha.backend.config.BatchProperties;
import com.alpha.backend.grpc.EmbeddingGrpcClient;
import com.alpha.backend.grpc.RowChunk;
import com.alpha.backend.infrastructure.CheckpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Embedding Streaming Service
 * gRPC 스트리밍으로 수신한 Embedding 데이터를 처리하는 서비스
 *
 * 핵심 플로우:
 * 1. Python gRPC Stream (Reactive) 수신
 * 2. EmbeddingGrpcClient (Flux<RowChunk>) 변환
 * 3. Virtual Thread Pool로 전환 (Scheduler)
 * 4. ChunkProcessor에서 Blocking JPA 처리
 * 5. Checkpoint 업데이트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingStreamingService {

    private final EmbeddingGrpcClient embeddingGrpcClient;
    private final ChunkProcessor chunkProcessor;
    private final CheckpointRepository checkpointRepository;
    private final BatchProperties batchProperties;

    @Qualifier("jpaScheduler")
    private final Scheduler jpaScheduler;

    /**
     * 전체 데이터 스트리밍 처리 (처음부터 시작)
     *
     * @return 처리 결과 (총 청크 수, 총 Row 수, 마지막 UUID)
     */
    public Mono<StreamingResult> streamAllData() {
        log.info("Starting full streaming from beginning");
        return processStream(null);
    }

    /**
     * Checkpoint부터 재시작하여 스트리밍 처리
     *
     * @return 처리 결과
     */
    public Mono<StreamingResult> streamFromCheckpoint() {
        log.info("Starting streaming from checkpoint");

        return Mono.fromCallable(() -> checkpointRepository.findLastProcessedUuid().orElse(null))
                .doOnNext(lastUuid -> log.info("Retrieved checkpoint: {}", lastUuid))
                .flatMap(this::processStream);
    }

    /**
     * 스트리밍 처리 핵심 로직
     *
     * @param lastProcessedUuid 시작 UUID (null이면 처음부터)
     * @return 처리 결과
     */
    private Mono<StreamingResult> processStream(UUID lastProcessedUuid) {
        long startTime = System.currentTimeMillis();
        int chunkSize = batchProperties.getChunkSize();

        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicInteger rowCount = new AtomicInteger(0);
        AtomicReference<UUID> lastUuid = new AtomicReference<>(lastProcessedUuid);

        log.info("=== Streaming Processing Started ===");
        log.info("Initial UUID: {} | Chunk Size: {}", lastProcessedUuid, chunkSize);

        // gRPC Stream 수신
        Flux<RowChunk> stream = embeddingGrpcClient.streamEmbeddings(lastProcessedUuid, chunkSize);

        return stream
                // Reactive Stream → Virtual Thread로 전환
                .publishOn(jpaScheduler)

                // 각 청크 처리
                .flatMap(chunk -> {
                    int currentChunkNum = chunkCount.incrementAndGet();
                    int chunkRowCount = chunk.getRowsCount();
                    rowCount.addAndGet(chunkRowCount);

                    log.info("Processing chunk #{} with {} rows", currentChunkNum, chunkRowCount);

                    return Mono.fromCallable(() -> {
                        // Blocking JPA 처리 (Virtual Thread에서 실행)
                        UUID processedUuid = chunkProcessor.processChunk(chunk);
                        lastUuid.set(processedUuid);

                        // Checkpoint 업데이트
                        if (processedUuid != null) {
                            updateCheckpoint(processedUuid);
                        }

                        return processedUuid;
                    })
                    .subscribeOn(jpaScheduler)  // Virtual Thread에서 실행
                    .timeout(Duration.ofMinutes(5))  // 타임아웃 설정
                    .doOnError(error -> log.error("Error processing chunk #{}: {}",
                            currentChunkNum, error.getMessage(), error))
                    .retry(batchProperties.getMaxRetry())  // 재시도 정책
                    .onErrorResume(error -> {
                        log.error("Failed to process chunk #{} after {} retries",
                                currentChunkNum, batchProperties.getMaxRetry());
                        return Mono.empty();
                    });
                })

                // 모든 청크 처리 완료 후
                .then(Mono.fromCallable(() -> {
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    StreamingResult result = new StreamingResult(
                            chunkCount.get(),
                            rowCount.get(),
                            lastUuid.get(),
                            elapsedTime
                    );

                    log.info("=== Streaming Processing Completed ===");
                    log.info("Total Chunks: {} | Total Rows: {} | Last UUID: {}",
                            result.totalChunks(), result.totalRows(), result.lastUuid());
                    log.info("Processing Time: {} ms ({} seconds)",
                            result.elapsedTimeMs(), result.elapsedTimeMs() / 1000);

                    return result;
                }));
    }

    /**
     * Checkpoint 업데이트
     */
    private void updateCheckpoint(UUID lastProcessedUuid) {
        try {
            checkpointRepository.updateLatestCheckpoint(lastProcessedUuid);
            log.debug("Checkpoint updated: {}", lastProcessedUuid);
        } catch (Exception e) {
            log.error("Failed to update checkpoint for UUID {}: {}",
                    lastProcessedUuid, e.getMessage(), e);
            // Checkpoint 실패는 치명적이지 않으므로 계속 진행
        }
    }

    /**
     * 병렬 스트리밍 처리 (청크를 더 작은 단위로 분할하여 병렬 처리)
     *
     * @param lastProcessedUuid 시작 UUID
     * @param parallelism 병렬도 (권장: 4-8)
     * @param subChunkSize 서브 청크 크기 (권장: 50-100)
     * @return 처리 결과
     */
    public Mono<StreamingResult> streamWithParallelism(
            UUID lastProcessedUuid,
            int parallelism,
            int subChunkSize) {

        long startTime = System.currentTimeMillis();
        int chunkSize = batchProperties.getChunkSize();

        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicInteger rowCount = new AtomicInteger(0);
        AtomicReference<UUID> lastUuid = new AtomicReference<>(lastProcessedUuid);

        log.info("=== Parallel Streaming Processing Started ===");
        log.info("Initial UUID: {} | Chunk Size: {} | Parallelism: {} | Sub-Chunk Size: {}",
                lastProcessedUuid, chunkSize, parallelism, subChunkSize);

        Flux<RowChunk> stream = embeddingGrpcClient.streamEmbeddings(lastProcessedUuid, chunkSize);

        return stream
                // 큰 청크를 작은 서브 청크로 분할
                .flatMap(largeChunk ->
                    Flux.fromIterable(largeChunk.getRowsList())
                        .buffer(subChunkSize)
                        .map(rows -> RowChunk.newBuilder().addAllRows(rows).build())
                )

                // 병렬 처리
                .parallel(parallelism)
                .runOn(jpaScheduler)  // Virtual Thread 스케줄러

                // 각 서브 청크 처리
                .flatMap(subChunk -> {
                    int currentChunkNum = chunkCount.incrementAndGet();
                    int chunkRowCount = subChunk.getRowsCount();
                    rowCount.addAndGet(chunkRowCount);

                    return Mono.fromCallable(() -> {
                        UUID processedUuid = chunkProcessor.processChunk(subChunk);
                        lastUuid.set(processedUuid);

                        if (processedUuid != null) {
                            updateCheckpoint(processedUuid);
                        }

                        return processedUuid;
                    })
                    .subscribeOn(jpaScheduler)
                    .timeout(Duration.ofMinutes(5))
                    .retry(batchProperties.getMaxRetry())
                    .onErrorResume(error -> {
                        log.error("Failed to process sub-chunk #{}: {}",
                                currentChunkNum, error.getMessage());
                        return Mono.empty();
                    });
                })

                // 병렬 처리를 순차로 다시 변환
                .sequential()

                // 모든 처리 완료
                .then(Mono.fromCallable(() -> {
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    StreamingResult result = new StreamingResult(
                            chunkCount.get(),
                            rowCount.get(),
                            lastUuid.get(),
                            elapsedTime
                    );

                    log.info("=== Parallel Streaming Processing Completed ===");
                    log.info("Total Sub-Chunks: {} | Total Rows: {} | Last UUID: {}",
                            result.totalChunks(), result.totalRows(), result.lastUuid());
                    log.info("Processing Time: {} ms ({} seconds)",
                            result.elapsedTimeMs(), result.elapsedTimeMs() / 1000);

                    return result;
                }));
    }

    /**
     * 스트리밍 처리 결과
     */
    public record StreamingResult(
            int totalChunks,
            int totalRows,
            UUID lastUuid,
            long elapsedTimeMs
    ) {}
}
