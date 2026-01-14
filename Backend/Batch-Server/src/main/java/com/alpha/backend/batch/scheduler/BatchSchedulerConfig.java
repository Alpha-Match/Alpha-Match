package com.alpha.backend.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.quartz.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Batch Scheduler 설정 (Quartz + Spring Batch 통합)
 *
 * Spring Batch 6.0 마이그레이션 완료:
 * - JobRegistry.getJob(String) → Job 객체 획득
 * - JobParametersBuilder → JobParameters 객체 생성
 * - JobOperator.start(Job, JobParameters) → JobExecution 반환
 * - 이전 방식: JobOperator.start(String, Properties) deprecated
 *
 * 핵심 패키지 (Spring Batch 6.0):
 * - org.springframework.batch.core.job.* (Job, JobExecution)
 * - org.springframework.batch.core.job.parameters.* (JobParameters, JobParametersBuilder)
 * - org.springframework.batch.core.launch.* (JobOperator, 예외들)
 * - org.springframework.batch.core.configuration.JobRegistry
 *
 * 스케줄러 활성화 조건:
 * - application.yml: batch.scheduler.enabled=true
 *
 * 도메인별 스케줄 설정:
 * - Recruit: batch.scheduler.jobs.recruit.cron (기본: 매일 새벽 2시)
 * - Candidate: batch.scheduler.jobs.candidate.cron (예정)
 *
 * Cron 표현식 예시:
 * - "0 0 2 * * ?" → 매일 새벽 2시
 * - "0 0/30 * * * ?" → 30분마다
 * - "0 0 0 * * MON" → 매주 월요일 자정
 *
 * JobDetail vs Trigger:
 * - JobDetail: 실행할 Job 정의 (QuartzJobBean)
 * - Trigger: 실행 시점 정의 (CronTrigger)
 *
 * @author Alpha-Match Team
 * @since 2025-12-16 (Spring Batch 6.0 완전 호환)
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "batch.scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BatchSchedulerConfig {

    @Value("${batch.scheduler.jobs.recruit.cron:0 0 2 * * ?}")
    private String recruitCronExpression;

    /**
     * Recruit Embedding Job - JobDetail
     *
     * JobDetail: Quartz가 실행할 Job 정의
     * - storeDurably(): Job이 Trigger 없이도 유지됨
     * - requestRecovery(): 서버 재시작 시 실행 중이던 Job 복구
     */
    @Bean
    public JobDetail recruitEmbeddingJobDetail() {
        return JobBuilder.newJob(RecruitEmbeddingQuartzJob.class)
                .withIdentity("recruitEmbeddingJobDetail", "embedding")
                .withDescription("Recruit Embedding Processing Job")
                .storeDurably()
                .requestRecovery()
                .build();
    }

    /**
     * Recruit Embedding Job - Trigger
     *
     * CronTrigger: Cron 표현식으로 실행 시점 정의
     * - Misfire 정책: DO_NOTHING (놓친 실행은 건너뜀)
     * - TimeZone: Asia/Seoul
     */
    @Bean
    public Trigger recruitEmbeddingTrigger(JobDetail recruitEmbeddingJobDetail) {
        log.info("[SCHEDULER_CONFIG] Creating Recruit Trigger | Cron: {}", recruitCronExpression);

        return TriggerBuilder.newTrigger()
                .forJob(recruitEmbeddingJobDetail)
                .withIdentity("recruitEmbeddingTrigger", "embedding")
                .withDescription("Recruit Embedding Cron Trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(recruitCronExpression)
                        .inTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
                        .withMisfireHandlingInstructionDoNothing())  // 놓친 실행은 건너뜀
                .build();
    }

    /**
     * ⏳ 예정: Candidate Embedding Job - JobDetail
     */
    // @Bean
    // public JobDetail candidateEmbeddingJobDetail() { ... }

    /**
     * ⏳ 예정: Candidate Embedding Job - Trigger
     */
    // @Bean
    // public Trigger candidateEmbeddingTrigger(JobDetail candidateEmbeddingJobDetail) { ... }

    // ==================== QuartzJobBean 구현 ====================

    /**
     * Recruit Embedding QuartzJob
     *
     * QuartzJobBean: Quartz가 실행할 실제 로직
     *
     * Spring Batch 6.0 실행 흐름:
     * 1. JobRegistry.getJob("recruitEmbeddingProcessingJob") → Job 객체 획득
     * 2. JobParametersBuilder().addString("timestamp", ...).toJobParameters() → JobParameters 생성
     * 3. JobOperator.start(Job, JobParameters) → JobExecution 반환
     * 4. JobExecution에서 실행 ID와 상태 확인
     *
     * 예외 처리:
     * - JobExecutionAlreadyRunningException: Job이 이미 실행 중
     * - JobRestartException: 재시작 불가능한 Job
     * - JobInstanceAlreadyCompleteException: 이미 완료된 Job 인스턴스
     * - InvalidJobParametersException: 잘못된 Job Parameters
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class RecruitEmbeddingQuartzJob extends QuartzJobBean {

        private final JobRegistry jobRegistry;
        private final JobOperator jobOperator;

        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            try {
                log.info("[QUARTZ_JOB] Starting Recruit Embedding Processing Job | Trigger: {}",
                        context.getTrigger().getKey());

                String jobName = "recruitEmbeddingProcessingJob";

                // JobRegistry에서 Job 가져오기
                Job job = jobRegistry.getJob(jobName);
                // Job Parameters: timestamp (재실행 허용)
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()))
                        .toJobParameters();

                // Spring Batch Job 실행
                JobExecution execution = jobOperator.start(job, jobParameters);

                log.info("[QUARTZ_JOB] Recruit Embedding Processing Job Started | ExecutionId={}, Status={}",
                        execution.getId(), execution.getStatus());
            } catch (JobExecutionAlreadyRunningException | JobRestartException |
                     JobInstanceAlreadyCompleteException | InvalidJobParametersException e) {
                log.error("[QUARTZ_JOB] Job execution failed", e);
                throw new JobExecutionException("Job execution failed", e);

            } catch (Exception e) {
                log.error("[QUARTZ_JOB] Unexpected error", e);
                throw new JobExecutionException("Unexpected error", e);
            }
        }
    }

    /**
     * ⏳ 예정: Candidate Embedding QuartzJob
     */
    // public static class CandidateEmbeddingQuartzJob extends QuartzJobBean { ... }
}
