package com.alpha.backend.infrastructure.grpc.server;

import com.alpha.backend.application.grpc.processor.DataProcessor;
import com.alpha.backend.application.grpc.processor.DataProcessorFactory;
import com.alpha.backend.infrastructure.grpc.proto.IngestDataRequest;
import com.alpha.backend.infrastructure.grpc.proto.IngestDataResponse;
import com.alpha.backend.infrastructure.grpc.proto.IngestMetadata;
import com.alpha.backend.infrastructure.grpc.proto.EmbeddingStreamServiceGrpc;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

/**
 * gRPC Server: Client Streaming으로 데이터 수신
 * <p>
 * Python 서버가 IngestDataStream RPC로 데이터를 전송하면 수신하여 DB에 저장합니다.
 * <p>
 * 통신 패턴: Client Streaming
 * - 첫 번째 메시지: IngestMetadata (도메인 정보)
 * - 이후 메시지들: data_chunk (JSON 인코딩된 데이터)
 * <p>
 * Port: 50051 (application.yml 설정)
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EmbeddingStreamServiceImpl extends EmbeddingStreamServiceGrpc.EmbeddingStreamServiceImplBase {

    private final DataProcessorFactory processorFactory;

    @Override
    public StreamObserver<IngestDataRequest> ingestDataStream(
            StreamObserver<IngestDataResponse> responseObserver) {

        return new StreamObserver<IngestDataRequest>() {
            private String domain;
            private DataProcessor processor;
            private int receivedChunks = 0;
            private int totalRows = 0;
            private String fileName;
            private int vectorDimension;

            @Override
            public void onNext(IngestDataRequest request) {
                try {
                    if (request.hasMetadata()) {
                        // 첫 번째 메시지: 메타데이터 처리
                        handleMetadata(request.getMetadata());

                    } else if (request.hasDataChunk()) {
                        // 이후 메시지들: 데이터 청크 처리
                        handleDataChunk(request.getDataChunk());
                    }

                } catch (Exception e) {
                    log.error("[STREAM_ERROR] Failed to process request", e);
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("데이터 처리 실패: " + e.getMessage())
                                    .withCause(e)
                                    .asException()
                    );
                }
            }

            /**
             * 메타데이터 처리
             * - 도메인 추출
             * - Factory에서 적절한 Processor 선택
             */
            private void handleMetadata(IngestMetadata metadata) {
                domain = metadata.getDomain();
                fileName = metadata.getFileName();
                vectorDimension = metadata.getVectorDimension();

                log.info("[METADATA] Received metadata - domain: {}, fileName: {}, vectorDim: {}",
                        domain, fileName, vectorDimension);

                // Factory에서 도메인에 맞는 Processor 선택
                processor = processorFactory.getProcessor(domain);

                log.info("[METADATA] Selected processor: {} for domain: {}",
                        processor.getClass().getSimpleName(), domain);
            }

            /**
             * 데이터 청크 처리
             * - JSON bytes → DTO 파싱 → Entity 변환 → DB 저장
             */
            private void handleDataChunk(ByteString dataChunk) {
                if (processor == null) {
                    throw new IllegalStateException("Processor not initialized. Metadata must be sent first.");
                }

                byte[] jsonBytes = dataChunk.toByteArray();
                log.debug("[CHUNK] Received data chunk: {} bytes", jsonBytes.length);

                // Processor에서 파싱 및 저장
                int processedRows = processor.processChunk(jsonBytes);

                receivedChunks++;
                totalRows += processedRows;

                log.info("[CHUNK] Processed chunk #{} - {} rows (total: {} rows)",
                        receivedChunks, processedRows, totalRows);
            }

            @Override
            public void onCompleted() {
                try {
                    log.info("[COMPLETED] Stream completed - domain: {}, chunks: {}, total rows: {}",
                            domain, receivedChunks, totalRows);

                    // 성공 응답 반환
                    IngestDataResponse response = IngestDataResponse.newBuilder()
                            .setSuccess(true)
                            .setReceivedChunks(receivedChunks)
                            .setMessage(String.format("Successfully ingested %d chunks (%d rows) for domain '%s'",
                                    receivedChunks, totalRows, domain))
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                    log.info("[COMPLETED] Response sent successfully");

                } catch (Exception e) {
                    log.error("[COMPLETED_ERROR] Failed to send response", e);
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("응답 전송 실패: " + e.getMessage())
                                    .asException()
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("[STREAM_ERROR] Stream error occurred - domain: {}, chunks processed: {}",
                        domain, receivedChunks, t);

                // 에러 상태 전파
                responseObserver.onError(
                        Status.fromThrowable(t)
                                .withDescription("Stream 처리 중 에러 발생")
                                .asException()
                );
            }
        };
    }
}
