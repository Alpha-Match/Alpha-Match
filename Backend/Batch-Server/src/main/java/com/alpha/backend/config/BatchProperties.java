package com.alpha.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Batch 관련 설정 프로퍼티
 * application.yml의 batch.embedding 설정을 매핑
 */
@Configuration
@ConfigurationProperties(prefix = "batch.embedding")
@Getter
@Setter
public class BatchProperties {

    /**
     * Chunk 크기 (한 번에 처리할 row 수)
     */
    private int chunkSize = 300;

    /**
     * Vector 차원 (default: 1536 for OpenAI embeddings)
     * 현재 샘플데이터 기준 384차원
     */
    private int vectorDimension = 384;

    /**
     * 재시도 최대 횟수
     */
    private int maxRetry = 3;

    /**
     * 재시도 대기 시간 (밀리초)
     */
    private long retryBackoffMs = 1000;
}
