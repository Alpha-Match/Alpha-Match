package com.alpha.backend.application.grpc.processor;

import com.alpha.backend.infrastructure.grpc.proto.RowChunk;

/**
 * gRPC Client Streaming 데이터 처리 인터페이스
 * <p>
 * Python 서버로부터 IngestDataStream으로 전송된 Protobuf 이진 데이터를 처리하고 저장합니다.
 */
public interface DataProcessor {

    /**
     * RowChunk를 파싱하고 데이터베이스에 저장
     * <p>
     * RowChunk → 도메인별 Row 리스트 → Entity 변환 → DB 저장
     *
     * @param rowChunk Protobuf RowChunk 메시지
     * @return 처리된 row 개수
     */
    int processChunk(RowChunk rowChunk);

    /**
     * 지원하는 도메인 이름 반환
     *
     * @return 도메인 이름 (예: "recruit", "candidate")
     */
    String getDomain();
}
