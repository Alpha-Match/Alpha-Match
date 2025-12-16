package com.alpha.backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Batch 관련 설정 프로퍼티
 * application.yml의 batch.embedding 설정을 매핑
 *
 * 도메인별 설정 예시:
 * batch:
 *   embedding:
 *     chunk-size: 300
 *     max-retry: 3
 *     domains:
 *       recruit:
 *         vector-dimension: 384
 *         table-prefix: recruit
 *       candidate:
 *         vector-dimension: 768
 *         table-prefix: candidate
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
     * 재시도 최대 횟수
     */
    private int maxRetry = 3;

    /**
     * 재시도 대기 시간 (밀리초)
     */
    private long retryBackoffMs = 1000;

    /**
     * 도메인별 설정 (Map<도메인명, DomainConfig>)
     */
    private Map<String, DomainConfig> domains = new HashMap<>();

    /**
     * 도메인별 설정을 조회
     * 존재하지 않으면 기본값 반환
     */
    public DomainConfig getDomainConfig(String domain) {
        return domains.getOrDefault(domain, getDefaultDomainConfig());
    }

    /**
     * 기본 도메인 설정 (recruit)
     */
    private DomainConfig getDefaultDomainConfig() {
        DomainConfig config = new DomainConfig();
        config.setVectorDimension(384);
        config.setTablePrefix("recruit");
        return config;
    }

    /**
     * 도메인별 상세 설정
     */
    @Getter
    @Setter
    public static class DomainConfig {
        /**
         * Vector 차원
         */
        private int vectorDimension;

        /**
         * 테이블 접두사 (recruit, candidate 등)
         */
        private String tablePrefix;
    }
}
