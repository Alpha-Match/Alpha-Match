package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.application.grpc.dto.RecruitRowDto;
import com.alpha.backend.domain.recruit.entity.RecruitEmbeddingEntity;
import com.alpha.backend.domain.recruit.entity.RecruitMetadataEntity;
import com.alpha.backend.domain.recruit.repository.RecruitEmbeddingRepository;
import com.alpha.backend.domain.recruit.repository.RecruitMetadataRepository;
import com.alpha.backend.infrastructure.config.BatchProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Recruit 도메인 데이터 처리기
 * <p>
 * Python 서버로부터 전송된 Recruit 데이터를 JSON에서 파싱하여 DB에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitDataProcessor implements DataProcessor {

    private final ObjectMapper objectMapper;
    private final RecruitMetadataRepository metadataRepository;
    private final RecruitEmbeddingRepository embeddingRepository;
    private final BatchProperties batchProperties;

    @Override
    public int processChunk(byte[] jsonChunk) {
        try {
            // 1. bytes → UTF-8 문자열
            String jsonString = new String(jsonChunk, StandardCharsets.UTF_8);
            log.debug("Received JSON chunk: {} bytes", jsonChunk.length);

            // 2. JSON → DTO 리스트
            List<RecruitRowDto> dtos = objectMapper.readValue(
                    jsonString,
                    new TypeReference<List<RecruitRowDto>>() {
                    }
            );
            log.info("Parsed {} recruit rows from JSON", dtos.size());

            if (dtos.isEmpty()) {
                log.warn("Empty DTO list, skipping save");
                return 0;
            }

            // 3. DTO → Entity 변환 및 저장
            List<RecruitMetadataEntity> metadataEntities = dtos.stream()
                    .map(this::toMetadataEntity)
                    .collect(Collectors.toList());

            List<RecruitEmbeddingEntity> embeddingEntities = dtos.stream()
                    .map(this::toEmbeddingEntity)
                    .collect(Collectors.toList());

            // 4. DB 저장 (Metadata → Embedding 순서)
            metadataRepository.upsertAll(metadataEntities);
            log.info("Upserted {} recruit metadata entities", metadataEntities.size());

            embeddingRepository.upsertAll(embeddingEntities);
            log.info("Upserted {} recruit embedding entities", embeddingEntities.size());

            return dtos.size();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON chunk", e);
            throw new RuntimeException("JSON 파싱 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to process chunk", e);
            throw new RuntimeException("Chunk 처리 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDomain() {
        return "recruit";
    }

    /**
     * DTO → Metadata Entity 변환
     */
    private RecruitMetadataEntity toMetadataEntity(RecruitRowDto dto) {
        UUID id = UUID.fromString(dto.getId());

        RecruitMetadataEntity entity = new RecruitMetadataEntity();
        entity.setId(id);
        entity.setCompanyName(dto.getCompanyName());
        entity.setExpYears(dto.getExpYears());
        entity.setEnglishLevel(dto.getEnglishLevel());
        entity.setPrimaryKeyword(dto.getPrimaryKeyword());

        return entity;
    }

    /**
     * DTO → Embedding Entity 변환
     */
    private RecruitEmbeddingEntity toEmbeddingEntity(RecruitRowDto dto) {
        UUID id = UUID.fromString(dto.getId());

        // Vector 차원 검증
        int expectedDim = batchProperties.getDomainConfig("recruit").getVectorDimension();
        if (dto.getVector().size() != expectedDim) {
            throw new IllegalArgumentException(
                    String.format("Vector dimension mismatch for UUID %s: expected=%d, actual=%d",
                            id, expectedDim, dto.getVector().size())
            );
        }

        // List<Float> → float[]
        float[] vectorArray = new float[dto.getVector().size()];
        for (int i = 0; i < dto.getVector().size(); i++) {
            vectorArray[i] = dto.getVector().get(i);
        }

        RecruitEmbeddingEntity entity = new RecruitEmbeddingEntity();
        entity.setId(id);
        entity.setVector(new PGvector(vectorArray));

        return entity;
    }
}
