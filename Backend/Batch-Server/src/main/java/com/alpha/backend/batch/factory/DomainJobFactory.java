package com.alpha.backend.batch.factory;

import com.alpha.backend.application.batch.dto.DomainItem;
import com.alpha.backend.application.batch.processor.RecruitItemProcessor;
import com.alpha.backend.application.batch.reader.RecruitItemReader;
import com.alpha.backend.application.batch.writer.DomainItemWriter;
import com.alpha.backend.application.usecase.DlqService;
import com.alpha.backend.batch.listener.EmbeddingJobListener;
import com.alpha.backend.batch.listener.EmbeddingStepListener;
import com.alpha.backend.domain.recruit.entity.RecruitEmbeddingEntity;
import com.alpha.backend.domain.recruit.entity.RecruitMetadataEntity;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.grpc.client.EmbeddingGrpcClient;
import com.alpha.backend.infrastructure.grpc.proto.RecruitRow;
import com.alpha.backend.infrastructure.persistence.CheckpointJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitEmbeddingJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitMetadataJpaRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

/**
 * DomainJobFactory: 도메인별 Spring Batch Job/Step 동적 생성
 *
 * 현재 지원 도메인:
 * - recruit (384d)
 *
 * 미래 확장 예정:
 * - candidate (768d) - proto 파일에 CandidateRow 추가 필요
 *
 * 패턴:
 * - Factory Method Pattern
 * - 도메인별 Reader/Processor/Writer를 조합하여 Job/Step 생성
 *
 * 사용 예시:
 * <pre>
 * {@code
 * Job recruitJob = domainJobFactory.createJob("recruit");
 * }
 * </pre>
 *
 * @author Alpha-Match Team
 * @since 2025-12-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainJobFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EmbeddingJobListener embeddingJobListener;
    private final EmbeddingStepListener embeddingStepListener;

    // Common dependencies
    private final EmbeddingGrpcClient embeddingGrpcClient;
    private final CheckpointJpaRepository checkpointRepository;
    private final BatchProperties batchProperties;
    private final DlqService dlqService;
    private final JsonMapper jsonMapper;

    // Recruit domain dependencies
    private final RecruitMetadataJpaRepository recruitMetadataRepository;
    private final RecruitEmbeddingJpaRepository recruitEmbeddingRepository;

    /**
     * 도메인별 Embedding Processing Job 생성
     *
     * @param domain 도메인명 (recruit, candidate 등)
     * @return Spring Batch Job
     * @throws IllegalArgumentException 지원하지 않는 도메인인 경우
     */
    public Job createJob(String domain) {
        log.info("[DOMAIN_JOB_FACTORY] Creating Job for domain: {}", domain);

        return switch (domain.toLowerCase()) {
            case "recruit" -> createRecruitJob();
            // case "candidate" -> createCandidateJob();  // ⏳ 예정
            default -> throw new IllegalArgumentException("Unsupported domain: " + domain);
        };
    }

    /**
     * 도메인별 Embedding Processing Step 생성
     *
     * @param domain 도메인명
     * @return Spring Batch Step
     * @throws IllegalArgumentException 지원하지 않는 도메인인 경우
     */
    public Step createStep(String domain) {
        log.info("[DOMAIN_JOB_FACTORY] Creating Step for domain: {}", domain);

        return switch (domain.toLowerCase()) {
            case "recruit" -> createRecruitStep();
            // case "candidate" -> createCandidateStep();  // ⏳ 예정
            default -> throw new IllegalArgumentException("Unsupported domain: " + domain);
        };
    }

    // ==================== Recruit Domain ====================

    /**
     * Recruit Embedding Processing Job
     */
    private Job createRecruitJob() {
        return new JobBuilder("recruitEmbeddingProcessingJob", jobRepository)
                .listener(embeddingJobListener)
                .start(createRecruitStep())
                .build();
    }

    /**
     * Recruit Embedding Processing Step
     *
     * Chunk 기반 처리:
     * 1. Reader: gRPC Stream → RecruitRow proto
     * 2. Processor: RecruitRow → DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>
     * 3. Writer: Batch Upsert to DB
     */
    private Step createRecruitStep() {
        int chunkSize = batchProperties.getChunkSize();

        return new StepBuilder("recruitEmbeddingProcessingStep", jobRepository)
                .<RecruitRow, DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>>chunk(chunkSize)
                .reader(createRecruitReader())
                .processor(createRecruitProcessor())
                .writer(createRecruitWriter())
                .transactionManager(transactionManager)
                .listener(embeddingStepListener)
                // Fault Tolerance 설정
                .faultTolerant()
                .skip(Exception.class)  // 모든 예외를 skip 대상으로 (DLQ에 저장됨)
                .skipLimit(100)  // 최대 100개까지 skip 허용
                .build();
    }

    /**
     * Recruit ItemReader 생성
     */
    private ItemReader<RecruitRow> createRecruitReader() {
        UUID lastProcessedUuid = checkpointRepository
                .findByDomain("recruit")
                .map(checkpoint -> checkpoint.getLastProcessedUuid())
                .orElse(null);

        int chunkSize = batchProperties.getChunkSize();

        log.info("[DOMAIN_JOB_FACTORY] Creating RecruitItemReader | Last UUID: {} | Chunk Size: {}",
                lastProcessedUuid, chunkSize);

        return new RecruitItemReader(embeddingGrpcClient, lastProcessedUuid, chunkSize);
    }

    /**
     * Recruit ItemProcessor 생성
     */
    private ItemProcessor<RecruitRow, DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>> createRecruitProcessor() {
        return new RecruitItemProcessor(batchProperties);
    }

    /**
     * Recruit ItemWriter 생성
     */
    private ItemWriter<DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>> createRecruitWriter() {
        return new DomainItemWriter<>(
                "recruit",
                recruitMetadataRepository,
                recruitEmbeddingRepository,
                dlqService,
                jsonMapper,
                recruitMetadataRepository::upsertAll,  // Method reference
                recruitEmbeddingRepository::upsertAll  // Method reference
        );
    }

    // ==================== Candidate Domain (예정) ====================

    /**
     * ⏳ Candidate Embedding Processing Job
     *
     * 구현 예정:
     * - CandidateRow proto 정의 필요
     * - CandidateItemReader 구현 필요
     * - CandidateItemProcessor 구현 필요
     */
    // private Job createCandidateJob() { ... }

    /**
     * ⏳ Candidate Embedding Processing Step
     */
    // private Step createCandidateStep() { ... }
}
