package com.alpha.backend.batch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Embedding Batch Step Listener
 *
 * Step 시작 및 종료 시 로깅 및 통계 수집
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingStepListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        String domain = stepExecution.getJobParameters().getString("domain", "N/A");

        log.info("----------------------------------------");
        log.info("Step Started: {}", stepName);
        log.info("Domain: {}", domain);
        log.info("Start Time: {}", LocalDateTime.now());
        log.info("----------------------------------------");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        String domain = stepExecution.getJobParameters().getString("domain", "N/A");
        String status = stepExecution.getStatus().name();

        LocalDateTime startTime = stepExecution.getStartTime();
        LocalDateTime endTime = stepExecution.getEndTime();
        Duration duration = Duration.between(startTime, endTime);

        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long commitCount = stepExecution.getCommitCount();
        long rollbackCount = stepExecution.getRollbackCount();

        log.info("----------------------------------------");
        log.info("Step Completed: {}", stepName);
        log.info("Domain: {}", domain);
        log.info("Status: {}", status);
        log.info("Duration: {} seconds", duration.getSeconds());
        log.info("Statistics:");
        log.info("  - Read Count: {}", readCount);
        log.info("  - Write Count: {}", writeCount);
        log.info("  - Commit Count: {}", commitCount);
        log.info("  - Rollback Count: {}", rollbackCount);

        // 에러가 있는 경우 로깅
        if (stepExecution.getFailureExceptions().size() > 0) {
            log.error("Step completed with {} errors", stepExecution.getFailureExceptions().size());
            stepExecution.getFailureExceptions().forEach(throwable ->
                    log.error("Error: {}", throwable.getMessage(), throwable)
            );
        }

        log.info("----------------------------------------");

        return stepExecution.getExitStatus();
    }
}
