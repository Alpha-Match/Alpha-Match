package com.alpha.backend.application.batch.writer;

import com.alpha.backend.application.batch.dto.DomainItem;
import com.alpha.backend.application.usecase.DlqService;
import com.alpha.backend.domain.common.BaseEmbeddingEntity;
import com.alpha.backend.domain.common.BaseMetadataEntity;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain ItemWriter (Generic)
 *
 * Processor에서 변환된 Entity를 DB에 Batch Upsert
 *
 * 제네릭 설계:
 * - 모든 도메인(Recruit, Candidate 등)에서 재사용 가능
 * - Repository 인터페이스를 통한 추상화
 *
 * 트랜잭션:
 * - @Transactional로 원자성 보장
 * - metadata → embedding 순서로 저장 (FK 제약)
 *
 * DLQ 처리:
 * - 개별 row 실패 시 DLQ에 저장
 * - Batch upsert 실패 시 전체 chunk를 DLQ에 저장
 */
@Slf4j
@RequiredArgsConstructor
public class DomainItemWriter<M extends BaseMetadataEntity, E extends BaseEmbeddingEntity>
        implements ItemWriter<DomainItem<M, E>> {

    private final String domain;
    private final JpaRepository<M, UUID> metadataRepository;
    private final JpaRepository<E, UUID> embeddingRepository;
    private final DlqService dlqService;
    private final JsonMapper jsonMapper;

    // Functional interface for upsert operation
    private final UpsertFunction<M> metadataUpsertFunction;
    private final UpsertFunction<E> embeddingUpsertFunction;

    @Override
    @Transactional
    public void write(Chunk<? extends DomainItem<M, E>> chunk) throws Exception {
        if (chunk.isEmpty()) {
            log.warn("[WRITER] Domain: {} | Empty chunk received", domain);
            return;
        }

        long startTime = System.currentTimeMillis();
        int chunkSize = chunk.getItems().size();
        String threadName = Thread.currentThread().getName();

        log.info("[WRITER] Domain: {} | Thread: {} | Processing {} items",
                domain, threadName, chunkSize);

        // 성공/실패 통계
        int successCount = 0;
        int failureCount = 0;
        UUID lastSuccessUuid = null;

        // 성공한 엔티티만 저장할 리스트
        List<M> metadataList = new ArrayList<>(chunkSize);
        List<E> embeddingList = new ArrayList<>(chunkSize);

        // 1. 개별 item 처리 (실패 시 DLQ 저장)
        for (DomainItem<M, E> item : chunk.getItems()) {
            try {
                M metadata = item.getMetadata();
                E embedding = item.getEmbedding();

                if (metadata == null || embedding == null) {
                    throw new IllegalStateException("Metadata or Embedding is null");
                }

                metadataList.add(metadata);
                embeddingList.add(embedding);

                successCount++;
                lastSuccessUuid = metadata.getId();

            } catch (Exception e) {
                failureCount++;
                UUID failedId = item.getMetadata() != null ? item.getMetadata().getId() : null;

                log.warn("[WRITER] Item processing failed | Domain: {} | ID: {} | Error: {}",
                        domain, failedId, e.getMessage());

                try {
                    String payloadJson = jsonMapper.writeValueAsString(item);
                    dlqService.saveToDlq(domain, failedId, e.getMessage(), payloadJson);
                } catch (Exception dlqError) {
                    log.error("[WRITER] DLQ save failed | Domain: {} | ID: {} | Error: {}",
                            domain, failedId, dlqError.getMessage());
                }
            }
        }

        // 2. 성공한 데이터만 DB에 Batch Upsert
        if (!metadataList.isEmpty()) {
            try {
                // Metadata 먼저 저장 (FK 제약 조건)
                long metadataStartTime = System.currentTimeMillis();
                metadataUpsertFunction.upsertAll(metadataList);
                long metadataElapsed = System.currentTimeMillis() - metadataStartTime;
                log.debug("[WRITER] Domain: {} | Metadata upsert: {}ms", domain, metadataElapsed);

                // Embedding 저장
                long embeddingStartTime = System.currentTimeMillis();
                embeddingUpsertFunction.upsertAll(embeddingList);
                long embeddingElapsed = System.currentTimeMillis() - embeddingStartTime;
                log.debug("[WRITER] Domain: {} | Embedding upsert: {}ms", domain, embeddingElapsed);

                // 완료 로깅
                long totalElapsed = System.currentTimeMillis() - startTime;
                log.info("[WRITER] Domain: {} | Thread: {} | Total: {} | Success: {} | Failure: {} | Last UUID: {} | Time: {}ms",
                        domain, threadName, chunkSize, successCount, failureCount, lastSuccessUuid, totalElapsed);

            } catch (Exception e) {
                log.error("[WRITER] Batch upsert failed | Domain: {} | Error: {}", domain, e.getMessage(), e);

                // Batch upsert 실패 시 모든 데이터를 DLQ에 저장
                saveBatchToDlq(metadataList, e.getMessage());

                throw new RuntimeException("Batch upsert failed for domain: " + domain, e);
            }
        } else {
            log.warn("[WRITER] Domain: {} | No successful items to save. All items failed.", domain);
        }
    }

    /**
     * Batch upsert 실패 시 전체 데이터를 DLQ에 저장
     */
    private void saveBatchToDlq(List<M> metadataList, String errorMessage) {
        log.warn("[WRITER] Saving {} failed metadata records to DLQ | Domain: {}",
                metadataList.size(), domain);

        for (M metadata : metadataList) {
            try {
                String payloadJson = jsonMapper.writeValueAsString(metadata);
                dlqService.saveToDlq(domain, metadata.getId(), errorMessage, payloadJson);
            } catch (Exception e) {
                log.error("[WRITER] DLQ save failed | Domain: {} | ID: {} | Error: {}",
                        domain, metadata.getId(), e.getMessage());
            }
        }
    }

    /**
     * Functional interface for upsert operation
     */
    @FunctionalInterface
    public interface UpsertFunction<T> {
        void upsertAll(List<T> entities);
    }
}
