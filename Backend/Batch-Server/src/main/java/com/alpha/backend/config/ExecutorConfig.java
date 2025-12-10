package com.alpha.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Executor 설정
 * Virtual Thread 및 Reactive Scheduler 구성
 */
@Configuration
@Slf4j
public class ExecutorConfig {

    /**
     * Virtual Thread Executor
     * JPA 등 Blocking I/O 작업을 처리하기 위한 Virtual Thread 기반 Executor
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        log.info("Initializing Virtual Thread Executor");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * JPA Scheduler
     * Reactive Stream에서 Blocking I/O (JPA) 처리를 위한 Scheduler
     * Virtual Thread를 활용하여 효율적인 처리
     */
    @Bean(name = "jpaScheduler")
    public Scheduler jpaScheduler(Executor virtualThreadExecutor) {
        log.info("Initializing JPA Scheduler with Virtual Threads");
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    /**
     * Bounded Elastic Scheduler
     * 제한된 병렬성을 가진 Elastic Scheduler
     * DB 커넥션 풀 고갈 방지
     */
    @Bean(name = "boundedElasticScheduler")
    public Scheduler boundedElasticScheduler() {
        log.info("Initializing Bounded Elastic Scheduler");
        return Schedulers.boundedElastic();
    }
}
