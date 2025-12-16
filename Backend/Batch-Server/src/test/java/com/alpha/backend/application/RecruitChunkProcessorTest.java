package com.alpha.backend.application;

import com.alpha.backend.application.service.DlqService;
import com.alpha.backend.config.BatchProperties;
import com.alpha.backend.domain.embedding.recruit.RecruitEmbeddingEntity;
import com.alpha.backend.domain.embedding.recruit.RecruitEmbeddingRepository;
import com.alpha.backend.domain.metadata.recruit.RecruitMetadataEntity;
import com.alpha.backend.domain.metadata.recruit.RecruitMetadataRepository;
import com.alpha.backend.presentation.grpc.proto.RecruitRow;
import com.alpha.backend.presentation.grpc.proto.RowChunk;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RecruitChunkProcessor 테스트
 * Recruit 도메인의 Chunk 처리 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitChunkProcessor 테스트")
class RecruitChunkProcessorTest {

    @Mock
    private RecruitMetadataRepository metadataRepository;

    @Mock
    private RecruitEmbeddingRepository embeddingRepository;

    @Mock
    private DlqService dlqService;

    @Mock
    private BatchProperties batchProperties;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private RecruitChunkProcessor processor;

    private BatchProperties.DomainConfig recruitConfig;

    @BeforeEach
    void setUp() {
        // BatchProperties Mock 설정 (lenient - 일부 테스트에서 사용하지 않을 수 있음)
        recruitConfig = new BatchProperties.DomainConfig();
        recruitConfig.setVectorDimension(384);
        recruitConfig.setTablePrefix("recruit");

        lenient().when(batchProperties.getDomainConfig("recruit")).thenReturn(recruitConfig);
    }

    @Test
    @DisplayName("getDomain() - 도메인명 'recruit' 반환")
    void getDomain_should_return_recruit() {
        // When
        String domain = processor.getDomain();

        // Then
        assertThat(domain).isEqualTo("recruit");
    }

    @Test
    @DisplayName("processChunk() - 정상 처리 성공")
    void processChunk_should_process_successfully() throws Exception {
        // Given
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        RecruitRow row1 = createRecruitRow(uuid1, "Company A", 3, "Advanced", "Java");
        RecruitRow row2 = createRecruitRow(uuid2, "Company B", 5, "Intermediate", "Python");

        RowChunk chunk = RowChunk.newBuilder()
                .addRows(row1)
                .addRows(row2)
                .build();

        // When
        ChunkProcessorInterface.ChunkProcessingResult result = processor.processChunk(chunk);

        // Then
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.lastSuccessUuid()).isEqualTo(uuid2);
        assertThat(result.isAllSuccess()).isTrue();
        assertThat(result.hasFailures()).isFalse();

        // Repository 호출 검증
        verify(metadataRepository, times(1)).upsertAll(anyList());
        verify(embeddingRepository, times(1)).upsertAll(anyList());
        verify(dlqService, never()).saveToDlq(anyString(), any(UUID.class), anyString(), anyString());
    }

    @Test
    @DisplayName("processChunk() - Vector 차원 불일치 시 DLQ 저장")
    void processChunk_should_save_to_dlq_when_vector_dimension_mismatch() throws Exception {
        // Given
        UUID uuid = UUID.randomUUID();
        RecruitRow invalidRow = createRecruitRowWithInvalidVector(uuid, 100); // 잘못된 차원 (100 != 384)

        RowChunk chunk = RowChunk.newBuilder()
                .addRows(invalidRow)
                .build();

        when(jsonMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");

        // When
        ChunkProcessorInterface.ChunkProcessingResult result = processor.processChunk(chunk);

        // Then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.isAllFailed()).isTrue();

        // DLQ 저장 검증
        verify(dlqService, times(1)).saveToDlq(
                eq("recruit"),
                eq(uuid),
                contains("Vector dimension mismatch"),
                anyString()
        );

        // Repository는 호출되지 않아야 함
        verify(metadataRepository, never()).upsertAll(anyList());
        verify(embeddingRepository, never()).upsertAll(anyList());
    }

    @Test
    @DisplayName("processChunk() - 빈 청크 처리")
    void processChunk_should_handle_empty_chunk() {
        // Given
        RowChunk emptyChunk = RowChunk.newBuilder().build();

        // When
        ChunkProcessorInterface.ChunkProcessingResult result = processor.processChunk(emptyChunk);

        // Then
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.lastSuccessUuid()).isNull();

        // Repository 호출 안됨
        verify(metadataRepository, never()).upsertAll(anyList());
        verify(embeddingRepository, never()).upsertAll(anyList());
    }

    @Test
    @DisplayName("processChunk() - 부분 성공 (일부 row 실패)")
    void processChunk_should_handle_partial_success() throws Exception {
        // Given
        UUID validUuid = UUID.randomUUID();
        UUID invalidUuid = UUID.randomUUID();

        RecruitRow validRow = createRecruitRow(validUuid, "Company A", 3, "Advanced", "Java");
        RecruitRow invalidRow = createRecruitRowWithInvalidVector(invalidUuid, 100);

        RowChunk chunk = RowChunk.newBuilder()
                .addRows(validRow)
                .addRows(invalidRow)
                .build();

        when(jsonMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");

        // When
        ChunkProcessorInterface.ChunkProcessingResult result = processor.processChunk(chunk);

        // Then
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.lastSuccessUuid()).isEqualTo(validUuid);
        assertThat(result.hasFailures()).isTrue();
        assertThat(result.isAllSuccess()).isFalse();

        // 성공한 데이터는 Repository에 저장
        ArgumentCaptor<List<RecruitMetadataEntity>> metadataCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataRepository, times(1)).upsertAll(metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).hasSize(1);

        // 실패한 데이터는 DLQ에 저장
        verify(dlqService, times(1)).saveToDlq(
                eq("recruit"),
                eq(invalidUuid),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("processChunk() - 잘못된 UUID 형식 처리")
    void processChunk_should_handle_invalid_uuid_format() throws Exception {
        // Given
        RecruitRow invalidRow = RecruitRow.newBuilder()
                .setId("invalid-uuid-format")
                .setCompanyName("Company A")
                .setExpYears(3)
                .setEnglishLevel("Advanced")
                .setPrimaryKeyword("Java")
                .addAllVector(createValidVector(384))
                .build();

        RowChunk chunk = RowChunk.newBuilder()
                .addRows(invalidRow)
                .build();

        when(jsonMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");

        // When
        ChunkProcessorInterface.ChunkProcessingResult result = processor.processChunk(chunk);

        // Then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isEqualTo(1);

        // DLQ 저장 시 null UUID 허용
        verify(dlqService, times(1)).saveToDlq(
                eq("recruit"),
                isNull(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("processChunk() - Batch Upsert 실패 시 DLQ 저장 및 예외 발생")
    void processChunk_should_save_to_dlq_and_throw_exception_when_batch_upsert_fails() throws Exception {
        // Given
        UUID uuid = UUID.randomUUID();
        RecruitRow row = createRecruitRow(uuid, "Company A", 3, "Advanced", "Java");

        RowChunk chunk = RowChunk.newBuilder()
                .addRows(row)
                .build();

        when(jsonMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"json\"}");

        // Metadata upsert 실패 시뮬레이션
        doThrow(new RuntimeException("Database connection failed"))
                .when(metadataRepository).upsertAll(anyList());

        // When & Then
        assertThatThrownBy(() -> processor.processChunk(chunk))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Batch upsert failed");

        // DLQ에 저장 확인
        verify(dlqService, times(1)).saveToDlq(
                eq("recruit"),
                eq(uuid),
                contains("Database connection failed"),
                anyString()
        );
    }

    @Test
    @DisplayName("processChunk() - DLQ 저장 실패 시 예외 처리 (로깅만)")
    void processChunk_should_handle_dlq_save_failure_gracefully() throws Exception {
        // Given
        UUID uuid = UUID.randomUUID();
        RecruitRow invalidRow = createRecruitRowWithInvalidVector(uuid, 100);

        RowChunk chunk = RowChunk.newBuilder()
                .addRows(invalidRow)
                .build();

        // DLQ 저장 실패 시뮬레이션
        when(jsonMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON serialization failed"));

        // When
        ChunkProcessorInterface.ChunkProcessingResult result = processor.processChunk(chunk);

        // Then - 예외를 던지지 않고 로깅만 함
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isEqualTo(1);

        // DLQ 저장은 시도되지 않음 (JSON 직렬화 실패)
        verify(dlqService, never()).saveToDlq(anyString(), any(UUID.class), anyString(), anyString());
    }

    // ========== Helper Methods ==========

    /**
     * 유효한 RecruitRow 생성 (384차원 벡터)
     */
    private RecruitRow createRecruitRow(UUID uuid, String companyName, int expYears,
                                         String englishLevel, String primaryKeyword) {
        return RecruitRow.newBuilder()
                .setId(uuid.toString())
                .setCompanyName(companyName)
                .setExpYears(expYears)
                .setEnglishLevel(englishLevel)
                .setPrimaryKeyword(primaryKeyword)
                .addAllVector(createValidVector(384))
                .build();
    }

    /**
     * 잘못된 Vector 차원을 가진 RecruitRow 생성
     */
    private RecruitRow createRecruitRowWithInvalidVector(UUID uuid, int invalidDimension) {
        return RecruitRow.newBuilder()
                .setId(uuid.toString())
                .setCompanyName("Company A")
                .setExpYears(3)
                .setEnglishLevel("Advanced")
                .setPrimaryKeyword("Java")
                .addAllVector(createValidVector(invalidDimension))
                .build();
    }

    /**
     * 지정된 차원의 유효한 벡터 생성
     */
    private List<Float> createValidVector(int dimension) {
        List<Float> vector = new java.util.ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            vector.add((float) Math.random());
        }
        return vector;
    }
}
