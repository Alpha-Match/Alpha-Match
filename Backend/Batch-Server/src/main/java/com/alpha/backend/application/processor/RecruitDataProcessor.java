package com.alpha.backend.application.processor;

import com.alpha.backend.application.processor.dto.RecruitRowDto;
import com.alpha.backend.config.BatchProperties;
import com.alpha.backend.domain.embedding.EmbeddingEntity;
import com.alpha.backend.domain.embedding.EmbeddingRepository;
import com.alpha.backend.domain.metadata.MetadataEntity;
import com.alpha.backend.domain.metadata.MetadataRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Recruit 도메인 데이터 프로세서
 *
 * Python 서버의 PklRecruitLoader와 대응되는 구현
 * JSON 청크 → Entity 변환 → DB 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitDataProcessor implements DataProcessor<RecruitRowDto> {

    private final MetadataRepository metadataRepository;
    private final EmbeddingRepository embeddingRepository;
    private final BatchProperties batchProperties;
    private final JsonMapper jsonMapper;

    @Override
    public String getDomain() {
        return "recruit";
    }

    @Override
    public List<RecruitRowDto> parseChunk(byte[] jsonChunk) {
        try {
            long startTime = System.currentTimeMillis();

            // JSON 배열 문자열을 List<String>으로 파싱
            String jsonArrayString = new String(jsonChunk, "UTF-8");
            List<String> jsonItems = jsonMapper.readValue(
                    jsonArrayString,
                    new TypeReference<List<String>>() {}
            );

            // 각 JSON 문자열을 RecruitRowDto로 파싱
            List<RecruitRowDto> rows = new ArrayList<>();
            for (String jsonItem : jsonItems) {
                RecruitRowDto dto = jsonMapper.readValue(jsonItem, RecruitRowDto.class);
                rows.add(dto);
            }

            long parseTime = System.currentTimeMillis() - startTime;
            log.debug("[PARSE] Domain: {} | Thread: {} | Rows: {} | Time: {}ms",
                    getDomain(), Thread.currentThread().getName(), rows.size(), parseTime);

            return rows;

        } catch (Exception e) {
            log.error("[PARSE_ERROR] Domain: {} | Thread: {} | Error: {}",
                    getDomain(), Thread.currentThread().getName(), e.getMessage(), e);
            throw new RuntimeException("JSON 청크 파싱 실패", e);
        }
    }

    @Override
    @Transactional
    public void saveToDatabase(List<RecruitRowDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            log.warn("[SAVE] Domain: {} | Thread: {} | No data to save",
                    getDomain(), Thread.currentThread().getName());
            return;
        }

        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();

        try {
            // DTO → Entity 변환
            long convertStart = System.currentTimeMillis();
            List<MetadataEntity> metadataList = new ArrayList<>();
            List<EmbeddingEntity> embeddingList = new ArrayList<>();

            for (RecruitRowDto dto : dtos) {
                UUID uuid = UUID.fromString(dto.getId());

                // Metadata Entity
                MetadataEntity metadata = MetadataEntity.builder()
                        .id(uuid)
                        .companyName(dto.getCompanyName())
                        .expYears(dto.getExpYears())
                        .englishLevel(dto.getEnglishLevel())
                        .primaryKeyword(dto.getPrimaryKeyword())
                        .build();
                metadataList.add(metadata);

                // Embedding Entity
                if (dto.getVector() != null) {
                    // Vector 차원 검증
                    int expectedDim = batchProperties.getVectorDimension();
                    int actualDim = dto.getVector().size();

                    if (actualDim != expectedDim) {
                        log.error("[VECTOR_DIM_ERROR] Domain: {} | ID: {} | Expected: {} | Actual: {}",
                                getDomain(), dto.getId(), expectedDim, actualDim);
                        throw new IllegalArgumentException(
                                String.format("Vector dimension mismatch: expected %d, got %d",
                                        expectedDim, actualDim)
                        );
                    }

                    // List<Float> → float[]
                    float[] vectorArray = new float[dto.getVector().size()];
                    for (int i = 0; i < dto.getVector().size(); i++) {
                        vectorArray[i] = dto.getVector().get(i);
                    }

                    EmbeddingEntity embedding = EmbeddingEntity.builder()
                            .id(uuid)
                            .vector(new PGvector(vectorArray))
                            .build();
                    embeddingList.add(embedding);
                }
            }
            long convertTime = System.currentTimeMillis() - convertStart;

            // DB 저장 (Metadata → Embedding 순서)
            long saveStart = System.currentTimeMillis();
            metadataRepository.upsertAll(metadataList);
            embeddingRepository.upsertAll(embeddingList);
            long saveTime = System.currentTimeMillis() - saveStart;

            // 마지막 레코드 정보
            RecruitRowDto lastRow = dtos.get(dtos.size() - 1);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[SAVE] Domain: {} | Thread: {} | Chunk: {} rows | " +
                            "Last UUID: {} | Last Company: {} | " +
                            "Times: convert={}ms, db_save={}ms, total={}ms",
                    getDomain(), threadName, dtos.size(),
                    lastRow.getId(), lastRow.getCompanyName(),
                    convertTime, saveTime, totalTime);

        } catch (Exception e) {
            log.error("[SAVE_ERROR] Domain: {} | Thread: {} | Chunk size: {} | Error: {}",
                    getDomain(), threadName, dtos.size(), e.getMessage(), e);
            throw new RuntimeException("데이터베이스 저장 실패", e);
        }
    }
}
