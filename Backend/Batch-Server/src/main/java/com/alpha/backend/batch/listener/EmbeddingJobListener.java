package com.alpha.backend.batch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Embedding Batch Job Listener
 *
 * Job 시작 및 종료 시 로깅 및 통계 수집
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String domain = jobExecution.getJobParameters().getString("domain", "N/A");

        log.info("========================================");
        log.info("Embedding Batch Job Started");
        log.info("Job Name: {}", jobName);
        log.info("Domain: {}", domain);
        log.info("Start Time: {}", LocalDateTime.now());
        log.info("========================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String domain = jobExecution.getJobParameters().getString("domain", "N/A");
        String status = jobExecution.getStatus().name();

        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        Duration duration = Duration.between(startTime, endTime);

        log.info("========================================");
        log.info("Embedding Batch Job Completed");
        log.info("Job Name: {}", jobName);
        log.info("Domain: {}", domain);
        log.info("Status: {}", status);
        log.info("Start Time: {}", startTime);
        log.info("End Time: {}", endTime);
        log.info("Duration: {} seconds", duration.getSeconds());
        log.info("Exit Code: {}", jobExecution.getExitStatus().getExitCode());

        // 에러가 있는 경우 로깅
        if (jobExecution.getAllFailureExceptions().size() > 0) {
            log.error("Job completed with {} errors", jobExecution.getAllFailureExceptions().size());
            jobExecution.getAllFailureExceptions().forEach(throwable ->
                    log.error("Error: {}", throwable.getMessage(), throwable)
            );
        }

        log.info("========================================");
    }
}
