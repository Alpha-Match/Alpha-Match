package com.alpha.backend.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * ChunkProcessorFactory 테스트
 * 도메인별 ChunkProcessor를 Factory 패턴으로 관리하는 기능 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChunkProcessorFactory 테스트")
class ChunkProcessorFactoryTest {

    @Mock
    private ChunkProcessorInterface recruitProcessor;

    @Mock
    private ChunkProcessorInterface candidateProcessor;

    private ChunkProcessorFactory factory;

    @BeforeEach
    void setUp() {
        // Mock Processor 설정
        when(recruitProcessor.getDomain()).thenReturn("recruit");
        when(candidateProcessor.getDomain()).thenReturn("candidate");

        // Factory 생성 (Mock Processor 주입)
        List<ChunkProcessorInterface> processors = Arrays.asList(
                recruitProcessor,
                candidateProcessor
        );
        factory = new ChunkProcessorFactory(processors);
    }

    @Test
    @DisplayName("getProcessor() - 도메인별 Processor 조회 성공")
    void getProcessor_should_return_correct_processor_for_domain() {
        // When
        ChunkProcessorInterface result1 = factory.getProcessor("recruit");
        ChunkProcessorInterface result2 = factory.getProcessor("candidate");

        // Then
        assertThat(result1).isEqualTo(recruitProcessor);
        assertThat(result2).isEqualTo(candidateProcessor);
    }

    @Test
    @DisplayName("getProcessor() - 지원하지 않는 도메인 조회 시 예외 발생")
    void getProcessor_should_throw_exception_for_unsupported_domain() {
        // When & Then
        assertThatThrownBy(() -> factory.getProcessor("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No ChunkProcessor found for domain 'unknown'")
                .hasMessageContaining("Available domains: ");
    }

    @Test
    @DisplayName("getProcessor() - null 도메인 조회 시 예외 발생")
    void getProcessor_should_throw_exception_for_null_domain() {
        // When & Then
        assertThatThrownBy(() -> factory.getProcessor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No ChunkProcessor found for domain 'null'");
    }

    @Test
    @DisplayName("getSupportedDomains() - 지원하는 도메인 목록 조회")
    void getSupportedDomains_should_return_all_registered_domains() {
        // When
        List<String> supportedDomains = factory.getSupportedDomains();

        // Then
        assertThat(supportedDomains)
                .hasSize(2)
                .containsExactlyInAnyOrder("recruit", "candidate");
    }

    @Test
    @DisplayName("getSupportedDomains() - 반환된 리스트는 불변")
    void getSupportedDomains_should_return_immutable_list() {
        // When
        List<String> supportedDomains = factory.getSupportedDomains();

        // Then - 리스트 수정 시 예외 발생 확인
        assertThatThrownBy(() -> supportedDomains.add("new_domain"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("supports() - 지원하는 도메인 확인")
    void supports_should_return_true_for_registered_domain() {
        // When & Then
        assertThat(factory.supports("recruit")).isTrue();
        assertThat(factory.supports("candidate")).isTrue();
    }

    @Test
    @DisplayName("supports() - 지원하지 않는 도메인 확인")
    void supports_should_return_false_for_unregistered_domain() {
        // When & Then
        assertThat(factory.supports("unknown")).isFalse();
        assertThat(factory.supports("company")).isFalse();
    }

    @Test
    @DisplayName("supports() - null 도메인 확인")
    void supports_should_return_false_for_null_domain() {
        // When & Then
        assertThat(factory.supports(null)).isFalse();
    }

    @Test
    @DisplayName("Factory 초기화 - Processor가 없는 경우")
    void factory_should_initialize_with_empty_processor_list() {
        // Given
        List<ChunkProcessorInterface> emptyProcessors = List.of();

        // When
        ChunkProcessorFactory emptyFactory = new ChunkProcessorFactory(emptyProcessors);

        // Then
        assertThat(emptyFactory.getSupportedDomains()).isEmpty();
        assertThat(emptyFactory.supports("recruit")).isFalse();
    }

    @Test
    @DisplayName("Factory 초기화 - 중복된 도메인명을 가진 Processor가 있는 경우")
    void factory_should_handle_duplicate_domain_names() {
        // Given
        ChunkProcessorInterface duplicateProcessor = org.mockito.Mockito.mock(ChunkProcessorInterface.class);
        when(duplicateProcessor.getDomain()).thenReturn("recruit"); // 중복 도메인

        List<ChunkProcessorInterface> duplicateProcessors = Arrays.asList(
                recruitProcessor,
                duplicateProcessor
        );

        // When & Then
        // Map 생성 시 마지막 값으로 덮어씌워짐
        assertThatThrownBy(() -> new ChunkProcessorFactory(duplicateProcessors))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate key");
    }

    @Test
    @DisplayName("Factory 초기화 - 다양한 도메인 Processor 등록")
    void factory_should_register_multiple_processors_correctly() {
        // Given
        ChunkProcessorInterface companyProcessor = org.mockito.Mockito.mock(ChunkProcessorInterface.class);
        when(companyProcessor.getDomain()).thenReturn("company");

        List<ChunkProcessorInterface> multipleProcessors = Arrays.asList(
                recruitProcessor,
                candidateProcessor,
                companyProcessor
        );

        // When
        ChunkProcessorFactory multiFactory = new ChunkProcessorFactory(multipleProcessors);

        // Then
        assertThat(multiFactory.getSupportedDomains())
                .hasSize(3)
                .containsExactlyInAnyOrder("recruit", "candidate", "company");
        assertThat(multiFactory.supports("company")).isTrue();
        assertThat(multiFactory.getProcessor("company")).isEqualTo(companyProcessor);
    }
}
