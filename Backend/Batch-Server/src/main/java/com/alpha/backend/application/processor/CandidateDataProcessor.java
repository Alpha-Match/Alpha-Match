package com.alpha.backend.application.processor;

import com.alpha.backend.application.processor.dto.CandidateRowDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Candidate 도메인 데이터 프로세서 (스켈레톤)
 *
 * Python 서버의 PklCandidateLoader와 대응
 * 추후 Candidate 도메인 구현 시 확장 예정
 *
 * TODO:
 * - CandidateMetadata Entity 정의
 * - CandidateEmbedding Entity 정의
 * - Repository 구현
 * - DB 저장 로직 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateDataProcessor implements DataProcessor<CandidateRowDto> {

    private final ObjectMapper objectMapper;

    @Override
    public String getDomain() {
        return "candidate";
    }

    @Override
    public List<CandidateRowDto> parseChunk(byte[] jsonChunk) {
        log.warn("[CANDIDATE] Domain: {} | Thread: {} | parseChunk() 미구현",
                getDomain(), Thread.currentThread().getName());
        throw new UnsupportedOperationException(
                "Candidate 도메인은 아직 구현되지 않았습니다. 추후 확장 예정입니다."
        );
    }

    @Override
    public void saveToDatabase(List<CandidateRowDto> entities) {
        log.warn("[CANDIDATE] Domain: {} | Thread: {} | saveToDatabase() 미구현",
                getDomain(), Thread.currentThread().getName());
        throw new UnsupportedOperationException(
                "Candidate 도메인은 아직 구현되지 않았습니다. 추후 확장 예정입니다."
        );
    }
}
