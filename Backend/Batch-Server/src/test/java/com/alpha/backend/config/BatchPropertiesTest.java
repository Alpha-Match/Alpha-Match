package com.alpha.backend.config;

import com.alpha.backend.infrastructure.config.BatchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * BatchProperties 테스트
 * 도메인별 설정 로드 및 조회 검증
 */
@DisplayName("BatchProperties 테스트")
class BatchPropertiesTest {

    private BatchProperties batchProperties;

    @BeforeEach
    void setUp() {
        batchProperties = new BatchProperties();

        // 기본값 설정
        batchProperties.setChunkSize(300);
        batchProperties.setMaxRetry(3);
        batchProperties.setRetryBackoffMs(1000);
    }

    @Test
    @DisplayName("기본값 설정 확인")
    void should_have_correct_default_values() {
        // When & Then
        assertThat(batchProperties.getChunkSize()).isEqualTo(300);
        assertThat(batchProperties.getMaxRetry()).isEqualTo(3);
        assertThat(batchProperties.getRetryBackoffMs()).isEqualTo(1000);
    }

    @Test
    @DisplayName("getDomainConfig() - 등록된 도메인 설정 조회")
    void getDomainConfig_should_return_registered_domain_config() {
        // Given
        Map<String, BatchProperties.DomainConfig> domains = new HashMap<>();

        BatchProperties.DomainConfig recruitConfig = new BatchProperties.DomainConfig();
        recruitConfig.setVectorDimension(384);
        recruitConfig.setTablePrefix("recruit");
        domains.put("recruit", recruitConfig);

        BatchProperties.DomainConfig candidateConfig = new BatchProperties.DomainConfig();
        candidateConfig.setVectorDimension(768);
        candidateConfig.setTablePrefix("candidate");
        domains.put("candidate", candidateConfig);

        batchProperties.setDomains(domains);

        // When
        BatchProperties.DomainConfig result1 = batchProperties.getDomainConfig("recruit");
        BatchProperties.DomainConfig result2 = batchProperties.getDomainConfig("candidate");

        // Then
        assertThat(result1).isNotNull();
        assertThat(result1.getVectorDimension()).isEqualTo(384);
        assertThat(result1.getTablePrefix()).isEqualTo("recruit");

        assertThat(result2).isNotNull();
        assertThat(result2.getVectorDimension()).isEqualTo(768);
        assertThat(result2.getTablePrefix()).isEqualTo("candidate");
    }

    @Test
    @DisplayName("getDomainConfig() - 존재하지 않는 도메인 조회 시 기본값 반환")
    void getDomainConfig_should_return_default_config_for_unknown_domain() {
        // Given
        Map<String, BatchProperties.DomainConfig> domains = new HashMap<>();

        BatchProperties.DomainConfig recruitConfig = new BatchProperties.DomainConfig();
        recruitConfig.setVectorDimension(384);
        recruitConfig.setTablePrefix("recruit");
        domains.put("recruit", recruitConfig);

        batchProperties.setDomains(domains);

        // When - 존재하지 않는 도메인 조회
        BatchProperties.DomainConfig unknownConfig = batchProperties.getDomainConfig("unknown");

        // Then - 기본값 반환 (recruit 기본값: 384차원)
        assertThat(unknownConfig).isNotNull();
        assertThat(unknownConfig.getVectorDimension()).isEqualTo(384);
        assertThat(unknownConfig.getTablePrefix()).isEqualTo("recruit");
    }

    @Test
    @DisplayName("getDomainConfig() - null 도메인 조회 시 기본값 반환")
    void getDomainConfig_should_return_default_config_for_null_domain() {
        // When
        BatchProperties.DomainConfig nullConfig = batchProperties.getDomainConfig(null);

        // Then - 기본값 반환
        assertThat(nullConfig).isNotNull();
        assertThat(nullConfig.getVectorDimension()).isEqualTo(384);
        assertThat(nullConfig.getTablePrefix()).isEqualTo("recruit");
    }

    @Test
    @DisplayName("domains 설정 로드 검증 - 빈 Map")
    void should_handle_empty_domains_map() {
        // Given
        batchProperties.setDomains(new HashMap<>());

        // When
        BatchProperties.DomainConfig config = batchProperties.getDomainConfig("recruit");

        // Then - 기본값 반환
        assertThat(config).isNotNull();
        assertThat(config.getVectorDimension()).isEqualTo(384);
        assertThat(config.getTablePrefix()).isEqualTo("recruit");
    }

    @Test
    @DisplayName("domains 설정 로드 검증 - null Map")
    void should_handle_null_domains_map() {
        // Given
        batchProperties.setDomains(null);

        // When & Then - NullPointerException 발생 가능
        // getOrDefault 호출 시 domains가 null이면 NPE 발생
        assertThatThrownBy(() -> batchProperties.getDomainConfig("recruit"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("DomainConfig 값 설정 및 조회")
    void domainConfig_should_set_and_get_values_correctly() {
        // Given
        BatchProperties.DomainConfig config = new BatchProperties.DomainConfig();

        // When
        config.setVectorDimension(512);
        config.setTablePrefix("company");

        // Then
        assertThat(config.getVectorDimension()).isEqualTo(512);
        assertThat(config.getTablePrefix()).isEqualTo("company");
    }

    @Test
    @DisplayName("여러 도메인 설정 동시 관리")
    void should_manage_multiple_domain_configs() {
        // Given
        Map<String, BatchProperties.DomainConfig> domains = new HashMap<>();

        // 3개 도메인 설정
        BatchProperties.DomainConfig recruitConfig = new BatchProperties.DomainConfig();
        recruitConfig.setVectorDimension(384);
        recruitConfig.setTablePrefix("recruit");

        BatchProperties.DomainConfig candidateConfig = new BatchProperties.DomainConfig();
        candidateConfig.setVectorDimension(768);
        candidateConfig.setTablePrefix("candidate");

        BatchProperties.DomainConfig companyConfig = new BatchProperties.DomainConfig();
        companyConfig.setVectorDimension(512);
        companyConfig.setTablePrefix("company");

        domains.put("recruit", recruitConfig);
        domains.put("candidate", candidateConfig);
        domains.put("company", companyConfig);

        batchProperties.setDomains(domains);

        // When & Then
        assertThat(batchProperties.getDomainConfig("recruit").getVectorDimension()).isEqualTo(384);
        assertThat(batchProperties.getDomainConfig("candidate").getVectorDimension()).isEqualTo(768);
        assertThat(batchProperties.getDomainConfig("company").getVectorDimension()).isEqualTo(512);
    }

    @Test
    @DisplayName("chunkSize Setter/Getter 검증")
    void should_set_and_get_chunk_size() {
        // When
        batchProperties.setChunkSize(500);

        // Then
        assertThat(batchProperties.getChunkSize()).isEqualTo(500);
    }

    @Test
    @DisplayName("maxRetry Setter/Getter 검증")
    void should_set_and_get_max_retry() {
        // When
        batchProperties.setMaxRetry(5);

        // Then
        assertThat(batchProperties.getMaxRetry()).isEqualTo(5);
    }

    @Test
    @DisplayName("retryBackoffMs Setter/Getter 검증")
    void should_set_and_get_retry_backoff_ms() {
        // When
        batchProperties.setRetryBackoffMs(2000);

        // Then
        assertThat(batchProperties.getRetryBackoffMs()).isEqualTo(2000);
    }

    @Test
    @DisplayName("domains Map Setter/Getter 검증")
    void should_set_and_get_domains_map() {
        // Given
        Map<String, BatchProperties.DomainConfig> newDomains = new HashMap<>();

        BatchProperties.DomainConfig config = new BatchProperties.DomainConfig();
        config.setVectorDimension(1024);
        config.setTablePrefix("test");
        newDomains.put("test", config);

        // When
        batchProperties.setDomains(newDomains);

        // Then
        assertThat(batchProperties.getDomains()).isNotNull();
        assertThat(batchProperties.getDomains()).hasSize(1);
        assertThat(batchProperties.getDomains().get("test")).isEqualTo(config);
    }

    @Test
    @DisplayName("기본 DomainConfig 반환 검증 - 항상 동일한 객체")
    void getDefaultDomainConfig_should_return_consistent_values() {
        // Given
        batchProperties.setDomains(new HashMap<>());

        // When
        BatchProperties.DomainConfig config1 = batchProperties.getDomainConfig("unknown1");
        BatchProperties.DomainConfig config2 = batchProperties.getDomainConfig("unknown2");

        // Then - 기본값은 동일한 값을 가짐 (단, 다른 객체)
        assertThat(config1.getVectorDimension()).isEqualTo(config2.getVectorDimension());
        assertThat(config1.getTablePrefix()).isEqualTo(config2.getTablePrefix());

        // 서로 다른 객체임 (getDefaultDomainConfig()는 매번 새 객체 생성)
        assertThat(config1).isNotSameAs(config2);
    }

    @Test
    @DisplayName("DomainConfig 기본값 확인")
    void default_domain_config_should_have_expected_values() {
        // Given
        batchProperties.setDomains(new HashMap<>());

        // When
        BatchProperties.DomainConfig defaultConfig = batchProperties.getDomainConfig("any");

        // Then
        assertThat(defaultConfig.getVectorDimension()).isEqualTo(384);
        assertThat(defaultConfig.getTablePrefix()).isEqualTo("recruit");
    }

    @Test
    @DisplayName("Configuration Properties 패턴 검증 - prefix 확인")
    void should_use_correct_configuration_prefix() {
        // Given - @ConfigurationProperties(prefix = "batch.embedding")
        // 실제 Spring Boot 환경에서는 다음과 같이 로드됨:
        // batch.embedding.chunk-size=300
        // batch.embedding.max-retry=3
        // batch.embedding.domains.recruit.vector-dimension=384

        // When & Then
        // 이 테스트는 단위 테스트이므로 어노테이션 검증은 통합 테스트에서 수행
        assertThat(batchProperties.getClass().getAnnotation(
                org.springframework.boot.context.properties.ConfigurationProperties.class
        ).prefix()).isEqualTo("batch.embedding");
    }
}
