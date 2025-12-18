package com.alpha.backend.application.batch.reader;

import com.alpha.backend.infrastructure.grpc.proto.RecruitRow;
import com.alpha.backend.infrastructure.grpc.proto.RowChunk;
import com.alpha.backend.infrastructure.grpc.client.EmbeddingGrpcClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Recruit ItemReader
 *
 * Recruit 도메인의 gRPC Stream을 수신하여 개별 RecruitRow 반환
 */
@Slf4j
public class RecruitItemReader extends DomainItemReader<RecruitRow> {

    private final EmbeddingGrpcClient embeddingGrpcClient;
    private final UUID lastProcessedUuid;
    private final int chunkSize;

    public RecruitItemReader(
            EmbeddingGrpcClient embeddingGrpcClient,
            UUID lastProcessedUuid,
            int chunkSize) {
        this.embeddingGrpcClient = embeddingGrpcClient;
        this.lastProcessedUuid = lastProcessedUuid;
        this.chunkSize = chunkSize;
    }

    @Override
    protected void startStreaming() {
        log.info("[READER] Starting gRPC stream | Domain: {} | Last UUID: {} | Chunk Size: {}",
                getDomainName(), lastProcessedUuid, chunkSize);

        Flux<RowChunk> stream = embeddingGrpcClient.streamEmbeddings(lastProcessedUuid, chunkSize);

        // 백그라운드 스레드에서 스트림 처리
        stream
            .doOnNext(chunk -> {
                // Proto 구조: RowChunk에서 Recruit 데이터 추출
                if (!chunk.hasRecruit()) {
                    log.warn("Received chunk without recruit data, skipping");
                    return;
                }

                log.debug("[READER] Domain: {} | Received chunk with {} rows",
                        getDomainName(), chunk.getRecruit().getRowsCount());

                // RowChunk를 개별 RecruitRow로 분해하여 Queue에 추가
                for (RecruitRow row : chunk.getRecruit().getRowsList()) {
                    try {
                        rowQueue.put(row);  // Blocking - Queue가 가득 차면 대기
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("[READER] Domain: {} | Interrupted while adding row to queue",
                                getDomainName(), e);
                        throw new RuntimeException("Queue operation interrupted", e);
                    }
                }
            })
            .doOnComplete(() -> {
                streamCompleted.set(true);
                log.info("[READER] Domain: {} | gRPC stream completed", getDomainName());
            })
            .doOnError(error -> {
                streamCompleted.set(true);
                log.error("[READER] Domain: {} | gRPC stream error: {}",
                        getDomainName(), error.getMessage(), error);
            })
            .subscribe();  // 비동기 구독 시작
    }

    @Override
    protected String getDomainName() {
        return "recruit";
    }
}
