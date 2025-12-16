package com.alpha.backend.batch.job;

import com.alpha.backend.application.batch.dto.DomainItem;
import com.alpha.backend.application.batch.processor.RecruitItemProcessor;
import com.alpha.backend.application.batch.reader.RecruitItemReader;
import com.alpha.backend.application.batch.writer.DomainItemWriter;
import com.alpha.backend.application.usecase.DlqService;
import com.alpha.backend.domain.recruit.entity.RecruitEmbeddingEntity;
import com.alpha.backend.domain.recruit.entity.RecruitMetadataEntity;
import com.alpha.backend.infrastructure.grpc.proto.RecruitRow;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.persistence.CheckpointJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitEmbeddingJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitMetadataJpaRepository;
import com.alpha.backend.batch.listener.EmbeddingJobListener;
import com.alpha.backend.batch.listener.EmbeddingStepListener;
import com.alpha.backend.infrastructure.grpc.client.EmbeddingGrpcClient;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

/**
 * Spring Batch Job 설정
 *
 * ItemReader/Processor/Writer 패턴을 활용한 Chunk 기반 처리
 *
 * 메모리 최적화:
 * - Reader: gRPC Stream에서 Proto 객체(RecruitRow) 반환
 * - Processor: Proto → Entity 변환 (역직렬화 지연)
 * - Writer: Entity Batch Upsert
 *
 * Spring Boot 4.0.0 변경사항:
 * - @EnableJdbcJobRepository: JDBC 기반 JobRepository 활성화 (메타데이터 DB 저장)
 * - spring-boot-starter-batch-jdbc 사용 필수
 */
@Slf4j
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@RequiredArgsConstructor
public class BatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EmbeddingJobListener embeddingJobListener;
    private final EmbeddingStepListener embeddingStepListener;

    // Dependencies for ItemReader/Processor/Writer
    private final EmbeddingGrpcClient embeddingGrpcClient;
    private final CheckpointJpaRepository checkpointRepository;
    private final BatchProperties batchProperties;

    // Recruit domain dependencies
    private final RecruitMetadataJpaRepository recruitMetadataRepository;
    private final RecruitEmbeddingJpaRepository recruitEmbeddingRepository;

    // Common dependencies
    private final DlqService dlqService;
    private final JsonMapper jsonMapper;

    /**
     * Recruit Embedding 처리 Job
     *
     * Chunk 기반 처리:
     * 1. Reader: gRPC Stream → RecruitRow proto
     * 2. Processor: RecruitRow → DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>
     * 3. Writer: Batch Upsert to DB
     */
    @Bean
    public Job recruitEmbeddingProcessingJob() {
        return new JobBuilder("recruitEmbeddingProcessingJob", jobRepository)
                .listener(embeddingJobListener)
                .start(recruitEmbeddingProcessingStep())
                .build();
    }

    /**
     * Recruit Embedding 처리 Step (Chunk 기반)
     *
     * Chunk Size: BatchProperties에서 설정 (기본 300)
     * Fault Tolerance: Skip 정책 적용 (SerializationException, IllegalArgumentException)
     */
    @Bean
    public Step recruitEmbeddingProcessingStep() {
        int chunkSize = batchProperties.getChunkSize();

        return new StepBuilder("recruitEmbeddingProcessingStep", jobRepository)
                .<RecruitRow, DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>>chunk(chunkSize)
                .reader(recruitItemReader())
                .processor(recruitItemProcessor())
                .writer(recruitItemWriter())
                .transactionManager(transactionManager)  // Spring Batch 6.0: transaction manager 별도 지정
                .listener(embeddingStepListener)
                // Fault Tolerance 설정
                .faultTolerant()
                .skip(Exception.class)  // 모든 예외를 skip 대상으로 (DLQ에 저장됨)
                .skipLimit(100)  // 최대 100개까지 skip 허용
                .build();
    }

    /**
     * Recruit ItemReader Bean
     *
     * Job Parameter에서 lastProcessedUuid 추출
     */
    @Bean
    public ItemReader<RecruitRow> recruitItemReader() {
        // Checkpoint에서 마지막 처리 UUID 조회
        UUID lastProcessedUuid = checkpointRepository
                .findByDomain("recruit")
                .map(checkpoint -> checkpoint.getLastProcessedUuid())
                .orElse(null);

        int chunkSize = batchProperties.getChunkSize();

        log.info("[BATCH_CONFIG] Creating RecruitItemReader | Last UUID: {} | Chunk Size: {}",
                lastProcessedUuid, chunkSize);

        return new RecruitItemReader(embeddingGrpcClient, lastProcessedUuid, chunkSize);
    }

    /**
     * Recruit ItemProcessor Bean
     */
    @Bean
    public ItemProcessor<RecruitRow, DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>> recruitItemProcessor() {
        return new RecruitItemProcessor(batchProperties);
    }

    /**
     * Recruit ItemWriter Bean
     */
    @Bean
    public ItemWriter<DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>> recruitItemWriter() {
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
}
