package com.alpha.backend.grpc;

import com.alpha.backend.grpc.proto.EmbeddingStreamServiceGrpc;
import com.alpha.backend.grpc.proto.RowChunk;
import com.alpha.backend.grpc.proto.StreamEmbeddingRequest;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Embedding gRPC Client
 * Python AI Server로부터 Embedding Stream을 수신하는 클라이언트
 */
@Component
@Slf4j
public class EmbeddingGrpcClient {

    private final ManagedChannel channel;
    private final EmbeddingStreamServiceGrpc.EmbeddingStreamServiceStub asyncStub;

    public EmbeddingGrpcClient(@Qualifier("pythonEmbeddingChannel") ManagedChannel channel) {
        this.channel = channel;
        this.asyncStub = EmbeddingStreamServiceGrpc.newStub(channel);
        log.info("EmbeddingGrpcClient initialized");
    }

    /**
     * Embedding Stream 수신
     * Python Server로부터 RowChunk를 Streaming으로 수신
     *
     * @param lastProcessedUuid 마지막 처리된 UUID (checkpoint)
     * @param chunkSize Chunk 크기
     * @return Flux<RowChunk> Reactive Stream
     */
    public Flux<RowChunk> streamEmbeddings(UUID lastProcessedUuid, int chunkSize) {
        log.info("Starting embedding stream - lastProcessedUuid: {}, chunkSize: {}",
                lastProcessedUuid, chunkSize);

        // Reactive Sink 생성 (backpressure 지원)
        Sinks.Many<RowChunk> sink = Sinks.many().unicast().onBackpressureBuffer();

        // gRPC Request 생성
        StreamEmbeddingRequest.Builder requestBuilder = StreamEmbeddingRequest.newBuilder()
                .setChunkSize(chunkSize);

        if (lastProcessedUuid != null) {
            requestBuilder.setLastProcessedUuid(lastProcessedUuid.toString());
        }

        StreamEmbeddingRequest request = requestBuilder.build();

        // gRPC Streaming 호출
        asyncStub.streamEmbedding(request, new StreamObserver<>() {
            private int chunkCount = 0;

            @Override
            public void onNext(RowChunk rowChunk) {
                chunkCount++;
                log.debug("Received chunk #{} with {} rows", chunkCount, rowChunk.getRowsCount());
                sink.tryEmitNext(rowChunk);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error in embedding stream: {}", throwable.getMessage(), throwable);
                sink.tryEmitError(throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Embedding stream completed. Total chunks received: {}", chunkCount);
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }

    /**
     * Channel 종료
     */
    public void shutdown() {
        try {
            log.info("Shutting down EmbeddingGrpcClient channel");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error shutting down channel", e);
            Thread.currentThread().interrupt();
        }
    }
}
