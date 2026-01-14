package com.alpha.backend.batch.job;

import com.alpha.backend.batch.factory.DomainJobFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch Job 설정 (Factory 패턴 적용)
 *
 * 변경 내역:
 * - 하드코딩된 Job/Step 생성 → DomainJobFactory로 위임
 * - 도메인별 설정을 Factory에서 중앙 관리
 * - 미래 확장성: 새 도메인 추가 시 Factory만 수정하면 됨
 *
 * ItemReader/Processor/Writer 패턴을 활용한 Chunk 기반 처리:
 * - Reader: gRPC Stream에서 Proto 객체 반환
 * - Processor: Proto → Entity 변환 (역직렬화 지연)
 * - Writer: Entity Batch Upsert
 *
 * Spring Boot 4.0.0 변경사항:
 * - @EnableJdbcJobRepository: JDBC 기반 JobRepository 활성화 (메타데이터 DB 저장)
 * - spring-boot-starter-batch-jdbc 사용 필수
 *
 * @author Alpha-Match Team
 * @since 2025-12-16 (Factory 패턴 리팩토링)
 */
@Slf4j
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@RequiredArgsConstructor
public class BatchJobConfig {

    private final DomainJobFactory domainJobFactory;

    /**
     * Recruit Embedding 처리 Job
     *
     * DomainJobFactory를 통해 동적으로 생성
     * Factory에서 Reader/Processor/Writer 조합 및 Listener 설정
     */
    @Bean
    public Job recruitEmbeddingProcessingJob() {
        log.info("[BATCH_CONFIG] Creating recruitEmbeddingProcessingJob via DomainJobFactory");
        return domainJobFactory.createJob("recruit");
    }

    /**
     * ⏳ 예정: Candidate Embedding 처리 Job
     *
     * proto 파일에 CandidateRow 추가 후 Factory에서 구현
     */
    // @Bean
    // public Job candidateEmbeddingProcessingJob() {
    //     log.info("[BATCH_CONFIG] Creating candidateEmbeddingProcessingJob via DomainJobFactory");
    //     return domainJobFactory.createJob("candidate");
    // }
}
