package com.alpha.backend.application.processor;

import java.util.List;

/**
 * 데이터 프로세서 인터페이스
 *
 * Python 서버의 DataLoader 패턴과 유사하게,
 * 도메인별로 JSON 청크를 엔티티로 변환하고 DB에 저장하는 공통 인터페이스
 *
 * @param <T> 처리할 엔티티 타입 (MetadataEntity, CandidateMetadata 등)
 */
public interface DataProcessor<T> {

    /**
     * JSON 청크를 파싱하여 엔티티 리스트로 변환
     *
     * @param jsonChunk JSON 인코딩된 바이트 배열
     * @return 파싱된 엔티티 리스트
     */
    List<T> parseChunk(byte[] jsonChunk);

    /**
     * 엔티티 리스트를 데이터베이스에 저장
     * Metadata → Embedding 순서로 저장 (FK 제약)
     *
     * @param entities 저장할 엔티티 리스트
     */
    void saveToDatabase(List<T> entities);

    /**
     * 지원하는 도메인 이름 반환
     *
     * @return 도메인 이름 (예: "recruit", "candidate")
     */
    String getDomain();
}
