package com.alpha.backend.application.grpc.processor;

/**
 * gRPC Client Streaming 데이터 처리 인터페이스
 * <p>
 * Python 서버로부터 IngestDataStream으로 전송된 JSON 데이터를 파싱하고 저장합니다.
 */
public interface DataProcessor {

    /**
     * JSON 청크를 파싱하고 데이터베이스에 저장
     * <p>
     * JSON bytes → DTO 파싱 → Entity 변환 → DB 저장
     *
     * @param jsonChunk JSON 인코딩된 바이트 배열
     * @return 처리된 row 개수
     * @throws com.fasterxml.jackson.core.JsonProcessingException JSON 파싱 실패 시
     */
    int processChunk(byte[] jsonChunk);

    /**
     * 지원하는 도메인 이름 반환
     *
     * @return 도메인 이름 (예: "recruit", "candidate")
     */
    String getDomain();
}
