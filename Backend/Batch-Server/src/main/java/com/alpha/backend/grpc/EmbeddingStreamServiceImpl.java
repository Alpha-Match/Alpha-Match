package com.alpha.backend.grpc;

import com.alpha.backend.application.processor.DataProcessor;
import com.alpha.backend.application.processor.DataProcessorFactory;
import com.alpha.backend.config.BatchProperties;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * gRPC 서버 구현 - IngestDataStream RPC
 *
 * Python 서버에서 Client Streaming으로 데이터를 수신하여
 * 도메인별 프로세서로 라우팅하고 DB에 저장
 *
 * 플로우:
 * 1. 첫 메시지에서 메타데이터 수신 (domain, file_name, vector_dimension)
 * 2. 도메인에 따라 적절한 프로세서 선택
 * 3. 이후 메시지들에서 데이터 청크 수신
 * 4. Virtual Thread로 전환하여 Blocking JPA 저장
 * 5. 모든 청크 수신 후 최종 응답 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingStreamServiceImpl extends EmbeddingStreamServiceGrpc.EmbeddingStreamServiceImplBase {

    private final DataProcessorFactory processorFactory;
    private final Executor virtualThreadExecutor;
    private final BatchProperties batchProperties;

    @Override
    public StreamObserver<IngestDataRequest> ingestDataStream(
            StreamObserver<IngestDataResponse> responseObserver) {

        return new StreamObserver<IngestDataRequest>() {

            // 상태 관리
            private String domain;
            private String fileName;
            private int vectorDimension;
            private DataProcessor<?> processor;
            private final AtomicInteger receivedChunks = new AtomicInteger(0);
            private final AtomicInteger processedChunks = new AtomicInteger(0);
            private boolean metadataReceived = false;

            @Override
            public void onNext(IngestDataRequest request) {
                try {
                    // 1. 첫 메시지: 메타데이터 수신
                    if (request.hasMetadata()) {
                        handleMetadata(request.getMetadata());
                        return;
                    }

                    // 2. 이후 메시지: 데이터 청크 수신
                    if (request.hasDataChunk()) {
                        handleDataChunk(request.getDataChunk());
                        return;
                    }

                    log.warn("[INGEST_STREAM] Invalid request type received");

                } catch (Exception e) {
                    log.error("[INGEST_STREAM_ERROR] onNext() error: {}", e.getMessage(), e);
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("[INGEST_STREAM_ERROR] Stream error: {}", t.getMessage(), t);
            }

            @Override
            public void onCompleted() {
                try {
                    log.info("[INGEST_STREAM_COMPLETED] Domain: {} | File: {} | " +
                                    "Total chunks received: {} | Processed: {}",
                            domain, fileName, receivedChunks.get(), processedChunks.get());

                    // 최종 응답 전송
                    IngestDataResponse response = IngestDataResponse.newBuilder()
                            .setSuccess(true)
                            .setReceivedChunks(receivedChunks.get())
                            .setMessage(String.format(
                                    "Successfully ingested %d chunks for domain '%s'",
                                    receivedChunks.get(), domain))
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                } catch (Exception e) {
                    log.error("[INGEST_STREAM_ERROR] onCompleted() error: {}", e.getMessage(), e);
                    responseObserver.onError(e);
                }
            }

            /**
             * 메타데이터 처리
             */
            private void handleMetadata(IngestMetadata metadata) {
                domain = metadata.getDomain();
                fileName = metadata.getFileName();
                vectorDimension = metadata.getVectorDimension();

                log.info("[INGEST_METADATA] Domain: {} | File: {} | Vector Dimension: {}",
                        domain, fileName, vectorDimension);

                // 도메인별 프로세서 선택
                try {
                    processor = processorFactory.getProcessor(domain);
                    metadataReceived = true;

                    log.info("[PROCESSOR_SELECTED] Domain: {} | Processor: {}",
                            domain, processor.getClass().getSimpleName());

                } catch (IllegalArgumentException e) {
                    log.error("[PROCESSOR_NOT_FOUND] Domain: {} | Error: {}",
                            domain, e.getMessage());
                    throw e;
                }

                // Vector 차원 검증
                int expectedDim = batchProperties.getVectorDimension();
                if (vectorDimension != expectedDim) {
                    String errorMsg = String.format(
                            "Vector dimension mismatch: expected %d, got %d",
                            expectedDim, vectorDimension);
                    log.error("[VECTOR_DIM_ERROR] {}", errorMsg);
                    throw new IllegalArgumentException(errorMsg);
                }
            }

            /**
             * 데이터 청크 처리
             * Reactive → Virtual Thread 플로우
             */
            private void handleDataChunk(com.google.protobuf.ByteString dataChunk) {
                if (!metadataReceived) {
                    throw new IllegalStateException(
                            "Metadata must be received before data chunks");
                }

                int chunkNumber = receivedChunks.incrementAndGet();
                byte[] jsonChunk = dataChunk.toByteArray();

                log.debug("[INGEST_CHUNK] Domain: {} | Chunk: {} | Size: {} bytes",
                        domain, chunkNumber, jsonChunk.length);

                // Virtual Thread로 전환하여 Blocking 작업 수행
                CompletableFuture.runAsync(() -> {
                    try {
                        long startTime = System.currentTimeMillis();
                        String threadName = Thread.currentThread().getName();

                        log.debug("[INGEST_PROCESSING] Domain: {} | Chunk: {} | Thread: {}",
                                domain, chunkNumber, threadName);

                        // 1. JSON 파싱
                        long parseStart = System.currentTimeMillis();
                        @SuppressWarnings("unchecked")
                        var parsedData = processor.parseChunk(jsonChunk);
                        long parseTime = System.currentTimeMillis() - parseStart;

                        // 2. DB 저장
                        long saveStart = System.currentTimeMillis();
                        @SuppressWarnings("unchecked")
                        DataProcessor rawProcessor = processor;
                        rawProcessor.saveToDatabase(parsedData);
                        long saveTime = System.currentTimeMillis() - saveStart;

                        processedChunks.incrementAndGet();

                        long totalTime = System.currentTimeMillis() - startTime;
                        log.info("[INGEST] Domain: {} | Chunk: {} | Thread: {} | " +
                                        "Rows: {} | Times: parse={}ms, save={}ms, total={}ms",
                                domain, chunkNumber, threadName,
                                parsedData.size(), parseTime, saveTime, totalTime);

                    } catch (Exception e) {
                        log.error("[INGEST_ERROR] Domain: {} | Chunk: {} | Error: {}",
                                domain, chunkNumber, e.getMessage(), e);
                        // 에러 발생 시 스트림 중단
                        responseObserver.onError(e);
                    }

                }, virtualThreadExecutor);
            }
        };
    }
}
