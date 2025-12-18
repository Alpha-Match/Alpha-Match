package com.alpha.backend.batch.factory;

import com.alpha.backend.application.usecase.DlqService;
import com.alpha.backend.batch.listener.EmbeddingJobListener;
import com.alpha.backend.batch.listener.EmbeddingStepListener;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.alpha.backend.infrastructure.grpc.client.EmbeddingGrpcClient;
import com.alpha.backend.infrastructure.persistence.CheckpointJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitEmbeddingJpaRepository;
import com.alpha.backend.infrastructure.persistence.RecruitMetadataJpaRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.*;

/**
 * DomainJobFactory 테스트
 * 도메인별 Job/Step 동적 생성 검증
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DomainJobFactory 테스트")
class DomainJobFactoryTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private EmbeddingJobListener embeddingJobListener;

    @Mock
    private EmbeddingStepListener embeddingStepListener;

    @Mock
    private EmbeddingGrpcClient embeddingGrpcClient;

    @Mock
    private CheckpointJpaRepository checkpointRepository;

    @Mock
    private BatchProperties batchProperties;

    @Mock
    private DlqService dlqService;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private RecruitMetadataJpaRepository recruitMetadataRepository;

    @Mock
    private RecruitEmbeddingJpaRepository recruitEmbeddingRepository;

    private DomainJobFactory factory;

    @BeforeEach
    void setUp() {
        // BatchProperties Mock 설정 (chunk-size 반환)
        org.mockito.BDDMockito.given(batchProperties.getChunkSize()).willReturn(300);

        factory = new DomainJobFactory(
                jobRepository,
                transactionManager,
                embeddingJobListener,
                embeddingStepListener,
                embeddingGrpcClient,
                checkpointRepository,
                batchProperties,
                dlqService,
                jsonMapper,
                recruitMetadataRepository,
                recruitEmbeddingRepository
        );
    }

    @Test
    @DisplayName("createJob() - recruit 도메인 Job 생성 성공")
    void createJob_should_create_recruit_job_successfully() {
        // When
        Job job = factory.createJob("recruit");

        // Then
        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("recruitEmbeddingProcessingJob");
    }

    @Test
    @DisplayName("createJob() - 대소문자 무관하게 recruit Job 생성")
    void createJob_should_be_case_insensitive() {
        // When
        Job job1 = factory.createJob("RECRUIT");
        Job job2 = factory.createJob("Recruit");
        Job job3 = factory.createJob("recruit");

        // Then
        assertThat(job1.getName()).isEqualTo("recruitEmbeddingProcessingJob");
        assertThat(job2.getName()).isEqualTo("recruitEmbeddingProcessingJob");
        assertThat(job3.getName()).isEqualTo("recruitEmbeddingProcessingJob");
    }

    @Test
    @DisplayName("createJob() - 지원하지 않는 도메인 시 예외 발생")
    void createJob_should_throw_exception_for_unsupported_domain() {
        // When & Then
        assertThatThrownBy(() -> factory.createJob("candidate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported domain: candidate");

        assertThatThrownBy(() -> factory.createJob("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported domain: unknown");
    }

    @Test
    @DisplayName("createJob() - null 도메인 시 예외 발생")
    void createJob_should_throw_exception_for_null_domain() {
        // When & Then
        assertThatThrownBy(() -> factory.createJob(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("createStep() - recruit 도메인 Step 생성 성공")
    void createStep_should_create_recruit_step_successfully() {
        // When
        Step step = factory.createStep("recruit");

        // Then
        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("recruitEmbeddingProcessingStep");
    }

    @Test
    @DisplayName("createStep() - 지원하지 않는 도메인 시 예외 발생")
    void createStep_should_throw_exception_for_unsupported_domain() {
        // When & Then
        assertThatThrownBy(() -> factory.createStep("candidate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported domain: candidate");
    }

    @Test
    @DisplayName("생성된 Job은 매번 새로운 인스턴스")
    void createJob_should_create_new_instance_every_time() {
        // When
        Job job1 = factory.createJob("recruit");
        Job job2 = factory.createJob("recruit");

        // Then
        // 같은 이름이지만 다른 인스턴스
        assertThat(job1.getName()).isEqualTo(job2.getName());
        assertThat(job1).isNotSameAs(job2);
    }
}
