package com.alpha.backend.batch.factory;

import com.alpha.backend.application.batch.dto.CandidateItem;
import com.alpha.backend.application.batch.dto.RecruitItem;
import com.alpha.backend.application.batch.processor.CandidateItemProcessor;
import com.alpha.backend.application.batch.processor.RecruitItemProcessor;
import com.alpha.backend.application.batch.reader.CandidateItemReader;
import com.alpha.backend.application.batch.reader.RecruitItemReader;
import com.alpha.backend.application.batch.writer.CandidateItemWriter;
import com.alpha.backend.application.batch.writer.RecruitItemWriter;
import com.alpha.backend.application.usecase.DlqService;
import com.alpha.backend.batch.listener.EmbeddingJobListener;
import com.alpha.backend.batch.listener.EmbeddingStepListener;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.grpc.client.EmbeddingGrpcClient;
import com.alpha.backend.infrastructure.grpc.proto.CandidateRow;
import com.alpha.backend.infrastructure.grpc.proto.RecruitRow;
import com.alpha.backend.infrastructure.persistence.CandidateDescriptionJpaRepository;
import com.alpha.backend.infrastructure.persistence.CandidateJpaRepository;
import com.alpha.backend.infrastructure.persistence.CandidateSkillJpaRepository;
import com.alpha.backend.infrastructure.persistence.CandidateSkillsEmbeddingJpaRepository;
import com.alpha.backend.infrastructure.persistence.CheckpointJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitDescriptionJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitSkillJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitSkillsEmbeddingJpaRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.persistence.EntityManager;
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
 * - recruit (1536d)
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
    private final EntityManager entityManager;
    private final JsonMapper jsonMapper;

    // Recruit domain dependencies (v2)
    private final RecruitJpaRepository recruitRepository;
    private final RecruitSkillJpaRepository recruitSkillRepository;
    private final RecruitDescriptionJpaRepository recruitDescriptionRepository;
    private final RecruitSkillsEmbeddingJpaRepository recruitSkillsEmbeddingRepository;

    // Candidate domain dependencies (v2)
    private final CandidateJpaRepository candidateRepository;
    private final CandidateSkillJpaRepository candidateSkillRepository;
    private final CandidateDescriptionJpaRepository candidateDescriptionRepository;
    private final CandidateSkillsEmbeddingJpaRepository candidateSkillsEmbeddingRepository;

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
            case "candidate" -> createCandidateJob();
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
            case "candidate" -> createCandidateStep();
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
     * Recruit Embedding Processing Step (v2)
     *
     * Chunk 기반 처리:
     * 1. Reader: gRPC Stream → RecruitRow proto
     * 2. Processor: RecruitRow → RecruitItem (4개 Entity)
     * 3. Writer: Batch Upsert to 4 tables
     */
    private Step createRecruitStep() {
        int chunkSize = batchProperties.getChunkSize();

        return new StepBuilder("recruitEmbeddingProcessingStep", jobRepository)
                .<RecruitRow, RecruitItem>chunk(chunkSize)
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
     * Recruit ItemProcessor 생성 (v2)
     */
    private ItemProcessor<RecruitRow, RecruitItem> createRecruitProcessor() {
        return new RecruitItemProcessor(batchProperties);
    }

    /**
     * Recruit ItemWriter 생성 (v2)
     */
    private ItemWriter<RecruitItem> createRecruitWriter() {
        return new RecruitItemWriter(
                recruitRepository,
                recruitSkillRepository,
                recruitDescriptionRepository,
                recruitSkillsEmbeddingRepository,
                entityManager
        );
    }

    // ==================== Candidate Domain (v2) ====================

    /**
     * Candidate Embedding Processing Job (v2)
     */
    private Job createCandidateJob() {
        return new JobBuilder("candidateEmbeddingProcessingJob", jobRepository)
                .listener(embeddingJobListener)
                .start(createCandidateStep())
                .build();
    }

    /**
     * Candidate Embedding Processing Step (v2)
     *
     * Chunk 기반 처리:
     * 1. Reader: gRPC Stream → CandidateRow proto
     * 2. Processor: CandidateRow → CandidateItem (4개 Entity)
     * 3. Writer: Batch Upsert to 4 tables
     */
    private Step createCandidateStep() {
        int chunkSize = batchProperties.getChunkSize();

        return new StepBuilder("candidateEmbeddingProcessingStep", jobRepository)
                .<CandidateRow, CandidateItem>chunk(chunkSize)
                .reader(createCandidateReader())
                .processor(createCandidateProcessor())
                .writer(createCandidateWriter())
                .transactionManager(transactionManager)
                .listener(embeddingStepListener)
                // Fault Tolerance 설정
                .faultTolerant()
                .skip(Exception.class)  // 모든 예외를 skip 대상으로 (DLQ에 저장됨)
                .skipLimit(100)  // 최대 100개까지 skip 허용
                .build();
    }

    /**
     * Candidate ItemReader 생성 (v2)
     */
    private ItemReader<CandidateRow> createCandidateReader() {
        UUID lastProcessedUuid = checkpointRepository
                .findByDomain("candidate")
                .map(checkpoint -> checkpoint.getLastProcessedUuid())
                .orElse(null);

        int chunkSize = batchProperties.getChunkSize();

        log.info("[DOMAIN_JOB_FACTORY] Creating CandidateItemReader | Last UUID: {} | Chunk Size: {}",
                lastProcessedUuid, chunkSize);

        return new CandidateItemReader(embeddingGrpcClient, lastProcessedUuid, chunkSize);
    }

    /**
     * Candidate ItemProcessor 생성 (v2)
     */
    private ItemProcessor<CandidateRow, CandidateItem> createCandidateProcessor() {
        return new CandidateItemProcessor(batchProperties);
    }

    /**
     * Candidate ItemWriter 생성 (v2)
     */
    private ItemWriter<CandidateItem> createCandidateWriter() {
        return new CandidateItemWriter(
                candidateRepository,
                candidateSkillRepository,
                candidateDescriptionRepository,
                candidateSkillsEmbeddingRepository
        );
    }
}
