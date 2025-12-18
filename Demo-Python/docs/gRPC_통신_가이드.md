# gRPC 통신 가이드

**작성일**: 2025-12-17
**프로젝트**: Alpha-Match Demo-Python Server
**대상**: gRPC Client Streaming, Proto 파일, Batch Server 연동

---

## 📋 목차

1. [gRPC 통신 개요](#1-grpc-통신-개요)
2. [Proto 파일 정의](#2-proto-파일-정의)
3. [Client Streaming 구현](#3-client-streaming-구현)
4. [에러 처리](#4-에러-처리)
5. [성능 최적화](#5-성능-최적화)

---

## 1. gRPC 통신 개요

### 1.1 아키텍처 변경 (2025-12-12)

**변경 전 (2025-12-11):**
```
Python Server (Port 50051) ←── Batch Server (Client)
         gRPC Server                 Server Streaming
```

**변경 후 (2025-12-12):**
```
Python Client ───→ Batch Server (Port 50052)
FastAPI + gRPC Client    gRPC Server
                         Client Streaming
```

### 1.2 Client Streaming 패턴

```
Python Server (Client)
    │
    ├──> Metadata 전송
    │
    ├──> Chunk 1 (1000 rows)
    ├──> Chunk 2 (1000 rows)
    ├──> Chunk 3 (1000 rows)
    │    ...
    └──> onCompleted()
    │
    ▼
Batch Server (Server)
    │
    └──> IngestDataResponse (결과 반환)
```

**특징:**
- Python이 **능동적으로** 데이터 전송
- Batch Server는 **수동적으로** 데이터 수신
- 대용량 데이터를 Chunk 단위로 스트리밍
- Backpressure 자동 처리 (gRPC 내장)

---

## 2. Proto 파일 정의

### 2.1 전체 구조

```protobuf
syntax = "proto3";

package embedding;

option java_multiple_files = true;
option java_package = "com.alpha.backend.grpc";

// ==================== Service ====================

service EmbeddingStreamService {
  // Client Streaming: Python → Batch Server
  rpc IngestDataStream (stream IngestDataRequest) returns (IngestDataResponse);
}

// ==================== Request (Client → Server) ====================

message IngestDataRequest {
  // 첫 번째 메시지: 메타데이터
  IngestMetadata metadata = 1;

  // 이후 메시지: 데이터 청크 (oneof로 도메인 분기)
  oneof chunk_data {
    RecruitRowChunk recruit_chunk = 2;
    CandidateRowChunk candidate_chunk = 3;
    SkillEmbeddingDicRowChunk skill_embedding_dic_chunk = 4;
  }
}

// ==================== Metadata ====================

message IngestMetadata {
  string domain = 1;             // "recruit", "candidate", "skill_embedding_dic"
  string file_name = 2;          // "recruit_embeddings.pkl"
  int32 vector_dimension = 3;    // 384 or 768
}

// ==================== Domain-specific Chunks ====================

message RecruitRowChunk {
  repeated RecruitRow rows = 1;
}

message RecruitRow {
  string id = 1;                 // UUID v7
  string company_name = 2;
  int32 exp_years = 3;
  string english_level = 4;
  string primary_keyword = 5;
  repeated float vector = 6;     // 384 dimensions
}

message CandidateRowChunk {
  repeated CandidateRow rows = 1;
}

message CandidateRow {
  string candidate_id = 1;       // UUID v7
  string position_category = 2;
  int32 experience_years = 3;
  string original_resume = 4;
  repeated string skills = 5;
  repeated float vector = 6;     // 768 dimensions
}

message SkillEmbeddingDicRowChunk {
  repeated SkillEmbeddingDicRow rows = 1;
}

message SkillEmbeddingDicRow {
  string skill = 1;              // PK
  string position_category = 2;
  repeated float vector = 3;     // 768 dimensions
}

// ==================== Response (Server → Client) ====================

message IngestDataResponse {
  string status = 1;             // "success" or "failed"
  string message = 2;            // 상세 메시지
  int32 total_rows_received = 3; // 수신한 총 rows 수
  int32 total_chunks_received = 4; // 수신한 총 chunks 수
}
```

### 2.2 oneof 패턴 (도메인 분기)

#### 개념

`oneof` 키워드는 여러 메시지 중 **하나만** 선택할 수 있도록 합니다.

```protobuf
message IngestDataRequest {
  IngestMetadata metadata = 1;

  oneof chunk_data {
    RecruitRowChunk recruit_chunk = 2;
    CandidateRowChunk candidate_chunk = 3;
    SkillEmbeddingDicRowChunk skill_embedding_dic_chunk = 4;
  }
}
```

#### Python에서의 사용

```python
# Case 1: Metadata 전송
request = embedding_stream_pb2.IngestDataRequest(
    metadata=metadata
)

# Case 2: Recruit Chunk 전송
request = embedding_stream_pb2.IngestDataRequest(
    recruit_chunk=recruit_row_chunk
)

# Case 3: Candidate Chunk 전송
request = embedding_stream_pb2.IngestDataRequest(
    candidate_chunk=candidate_row_chunk
)
```

#### Java에서의 수신

```java
IngestDataRequest request = ...;

switch (request.getChunkDataCase()) {
    case RECRUIT_CHUNK:
        handleRecruitChunk(request.getRecruitChunk());
        break;
    case CANDIDATE_CHUNK:
        handleCandidateChunk(request.getCandidateChunk());
        break;
    case SKILL_EMBEDDING_DIC_CHUNK:
        handleSkillEmbeddingDicChunk(request.getSkillEmbeddingDicChunk());
        break;
    case CHUNKDATA_NOT_SET:
        // Metadata만 전송된 경우
        break;
}
```

### 2.3 Proto 컴파일

#### Python 코드 생성

```bash
# Batch Server proto 복사
cp ../Backend/Batch-Server/src/main/proto/embedding_stream.proto src/proto/

# Python 코드 생성
python -m grpc_tools.protoc \
    -I src/proto \
    --python_out=src/proto \
    --pyi_out=src/proto \
    --grpc_python_out=src/proto \
    src/proto/embedding_stream.proto
```

#### 생성된 파일

```
src/proto/
├── embedding_stream.proto
├── embedding_stream_pb2.py       # 메시지 클래스
├── embedding_stream_pb2.pyi      # 타입 힌트
└── embedding_stream_pb2_grpc.py  # 서비스 클래스
```

---

## 3. Client Streaming 구현

### 3.1 전체 구조 (`infrastructure/grpc_clients.py`)

```python
import grpc
from typing import Iterator, List
from itertools import chain

from proto import embedding_stream_pb2
from proto import embedding_stream_pb2_grpc
from config.settings import settings

async def stream_data_to_batch_server(
    domain: str,
    chunks: Iterator[List[DomainData]],
    metadata: dict
) -> dict:
    """
    Batch Server로 Client Streaming 전송

    Args:
        domain: "recruit", "candidate", "skill_embedding_dic"
        chunks: Chunk Iterator (from Chunk Loader)
        metadata: {file_name, vector_dimension}

    Returns:
        {"status", "message", "rows_sent", "chunks_sent"}
    """

    # 1. gRPC 채널 생성
    channel = grpc.aio.insecure_channel(
        f'{settings.BATCH_SERVER_HOST}:{settings.BATCH_SERVER_PORT}',
        options=[
            ('grpc.max_send_message_length', 16 * 1024 * 1024),  # 16MB
            ('grpc.max_receive_message_length', 16 * 1024 * 1024),
        ]
    )

    # 2. Stub 생성
    stub = embedding_stream_pb2_grpc.EmbeddingStreamServiceStub(channel)

    try:
        # 3. Request Generator 생성
        request_iter = _generate_requests(domain, chunks, metadata)

        # 4. Client Streaming 호출
        response: embedding_stream_pb2.IngestDataResponse = await stub.IngestDataStream(request_iter)

        # 5. 응답 처리
        return {
            "status": response.status,
            "message": response.message,
            "rows_sent": response.total_rows_received,
            "chunks_sent": response.total_chunks_received
        }

    except grpc.aio.AioRpcError as e:
        # 6. 에러 처리
        logger.error(f"gRPC error: code={e.code()}, details={e.details()}")
        raise

    finally:
        # 7. 채널 종료
        await channel.close()
```

### 3.2 Request Generator

```python
def _generate_requests(
    domain: str,
    chunks: Iterator[List[DomainData]],
    metadata: dict
) -> Iterator[embedding_stream_pb2.IngestDataRequest]:
    """
    IngestDataRequest Iterator 생성

    1. 첫 번째 메시지: Metadata
    2. 이후 메시지: Chunk Data
    """

    # 1. Metadata 전송 (첫 번째 메시지)
    metadata_msg = embedding_stream_pb2.IngestMetadata(
        domain=domain,
        file_name=metadata['file_name'],
        vector_dimension=metadata['vector_dimension']
    )

    yield embedding_stream_pb2.IngestDataRequest(metadata=metadata_msg)

    logger.info(f"Sent metadata: domain={domain}, vector_dim={metadata['vector_dimension']}")

    # 2. Chunk 전송 (이후 메시지)
    chunk_count = 0
    total_rows = 0

    for chunk in chunks:
        chunk_count += 1
        total_rows += len(chunk)

        # Chunk 메시지 생성 (도메인별 분기)
        chunk_message = _create_chunk_message(domain, chunk)

        yield embedding_stream_pb2.IngestDataRequest(**{f"{domain}_chunk": chunk_message})

        if chunk_count % 10 == 0:
            logger.info(f"Sent {chunk_count} chunks ({total_rows} rows)")

    logger.info(f"Completed: sent {chunk_count} chunks ({total_rows} rows)")
```

### 3.3 Chunk 메시지 생성

```python
def _create_chunk_message(domain: str, chunk: List[DomainData]):
    """도메인별 Chunk 메시지 생성"""

    if domain == "recruit":
        return _create_recruit_chunk(chunk)
    elif domain == "candidate":
        return _create_candidate_chunk(chunk)
    elif domain == "skill_embedding_dic":
        return _create_skill_embedding_dic_chunk(chunk)
    else:
        raise ValueError(f"Unknown domain: {domain}")

def _create_recruit_chunk(chunk: List[RecruitData]) -> embedding_stream_pb2.RecruitRowChunk:
    """Recruit Chunk 생성"""
    rows = []
    for data in chunk:
        row = embedding_stream_pb2.RecruitRow(
            id=data.id,
            company_name=data.company_name,
            exp_years=data.exp_years,
            english_level=data.english_level,
            primary_keyword=data.primary_keyword,
            vector=data.vector
        )
        rows.append(row)

    return embedding_stream_pb2.RecruitRowChunk(rows=rows)

def _create_candidate_chunk(chunk: List[CandidateData]) -> embedding_stream_pb2.CandidateRowChunk:
    """Candidate Chunk 생성"""
    rows = []
    for data in chunk:
        row = embedding_stream_pb2.CandidateRow(
            candidate_id=data.candidate_id,
            position_category=data.position_category,
            experience_years=data.experience_years,
            original_resume=data.original_resume,
            skills=data.skills,  # List[str]
            vector=data.vector
        )
        rows.append(row)

    return embedding_stream_pb2.CandidateRowChunk(rows=rows)

def _create_skill_embedding_dic_chunk(
    chunk: List[SkillEmbeddingDicData]
) -> embedding_stream_pb2.SkillEmbeddingDicRowChunk:
    """SkillEmbeddingDic Chunk 생성"""
    rows = []
    for data in chunk:
        row = embedding_stream_pb2.SkillEmbeddingDicRow(
            skill=data.skill,
            position_category=data.position_category,
            vector=data.vector
        )
        rows.append(row)

    return embedding_stream_pb2.SkillEmbeddingDicRowChunk(rows=rows)
```

---

## 4. 에러 처리

### 4.1 gRPC 에러 코드

```python
import grpc

async def stream_data_to_batch_server(...):
    try:
        response = await stub.IngestDataStream(request_iter)
        return response

    except grpc.aio.AioRpcError as e:
        # gRPC 에러 코드 확인
        if e.code() == grpc.StatusCode.UNAVAILABLE:
            logger.error("Batch Server unavailable")
        elif e.code() == grpc.StatusCode.DEADLINE_EXCEEDED:
            logger.error("Request timeout")
        elif e.code() == grpc.StatusCode.RESOURCE_EXHAUSTED:
            logger.error("Batch Server resource exhausted")
        elif e.code() == grpc.StatusCode.INVALID_ARGUMENT:
            logger.error(f"Invalid argument: {e.details()}")
        else:
            logger.error(f"Unknown gRPC error: code={e.code()}, details={e.details()}")

        raise
```

### 4.2 재시도 로직

```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    reraise=True
)
async def stream_data_to_batch_server_with_retry(...):
    """재시도 로직 포함 스트리밍"""
    return await stream_data_to_batch_server(...)

# 사용
try:
    result = await stream_data_to_batch_server_with_retry(...)
except Exception as e:
    logger.error(f"Failed after 3 retries: {e}")
```

### 4.3 Timeout 설정

```python
async def stream_data_to_batch_server(...):
    # Timeout 10분
    response = await stub.IngestDataStream(
        request_iter,
        timeout=600  # 10 minutes
    )
```

---

## 5. 성능 최적화

### 5.1 gRPC 옵션

```python
# Channel Options
options = [
    # 메시지 크기 제한
    ('grpc.max_send_message_length', 16 * 1024 * 1024),      # 16MB
    ('grpc.max_receive_message_length', 16 * 1024 * 1024),   # 16MB

    # Keep-alive 설정
    ('grpc.keepalive_time_ms', 30000),                       # 30초
    ('grpc.keepalive_timeout_ms', 10000),                    # 10초
    ('grpc.keepalive_permit_without_calls', 1),

    # HTTP/2 설정
    ('grpc.http2.max_pings_without_data', 0),
    ('grpc.http2.min_time_between_pings_ms', 10000),

    # Connection Pool
    ('grpc.use_local_subchannel_pool', 1),
]

channel = grpc.aio.insecure_channel(
    f'{host}:{port}',
    options=options
)
```

### 5.2 Chunk Size 조정

#### 네트워크 대역폭 기반

```python
def calculate_optimal_chunk_size(
    network_bandwidth_mbps: float = 100,  # 100 Mbps
    vector_dimension: int = 384,
    target_latency_ms: float = 100        # 100ms
) -> int:
    """네트워크 대역폭 기반 최적 Chunk 크기"""

    # 1. 초당 전송 가능 바이트
    bytes_per_second = (network_bandwidth_mbps * 1024 * 1024) / 8

    # 2. 목표 레이턴시 내 전송 가능 바이트
    bytes_per_chunk = bytes_per_second * (target_latency_ms / 1000)

    # 3. Row 당 바이트 (vector + metadata)
    bytes_per_row = (vector_dimension * 4) + 500

    # 4. 최적 Chunk 크기
    optimal_size = int(bytes_per_chunk / bytes_per_row)

    return max(100, min(optimal_size, 10000))
```

### 5.3 Batch Size (Rows per Chunk)

#### 권장 크기

| 네트워크 | Vector 차원 | Chunk Size | 메시지 크기 | Latency |
|---------|-----------|-----------|------------|---------|
| 100 Mbps | 384 | 1000 | ~1.5 MB | ~120 ms |
| 100 Mbps | 768 | 500 | ~1.5 MB | ~120 ms |
| 1 Gbps | 384 | 5000 | ~7.5 MB | ~60 ms |
| 1 Gbps | 768 | 2500 | ~7.5 MB | ~60 ms |

---

## 6. 테스트

### 6.1 단위 테스트 (TODO)

```python
import pytest
from unittest.mock import AsyncMock, patch

@pytest.mark.asyncio
async def test_stream_data_to_batch_server():
    """gRPC Client Streaming 테스트"""

    # Mock Stub
    mock_stub = AsyncMock()
    mock_stub.IngestDataStream.return_value = embedding_stream_pb2.IngestDataResponse(
        status="success",
        message="OK",
        total_rows_received=1000,
        total_chunks_received=1
    )

    # Mock 데이터
    chunks = [
        [RecruitData(...) for _ in range(1000)]
    ]

    metadata = {
        "file_name": "test.pkl",
        "vector_dimension": 384
    }

    # 실행
    with patch('infrastructure.grpc_clients.embedding_stream_pb2_grpc.EmbeddingStreamServiceStub', return_value=mock_stub):
        result = await stream_data_to_batch_server("recruit", iter(chunks), metadata)

    # 검증
    assert result["status"] == "success"
    assert result["rows_sent"] == 1000
    assert result["chunks_sent"] == 1
```

### 6.2 통합 테스트

```python
import pytest

@pytest.mark.integration
@pytest.mark.asyncio
async def test_integration_with_batch_server():
    """Batch Server와 실제 통신 테스트"""

    # 1. 테스트 데이터 로드
    from infrastructure.loaders import get_loader_auto

    loader = get_loader_auto("recruit", "data/test_recruit_embeddings.pkl")
    chunks = loader.load_chunks("data/test_recruit_embeddings.pkl", chunk_size=100)

    # 2. gRPC 전송
    metadata = {
        "file_name": "test_recruit_embeddings.pkl",
        "vector_dimension": 384
    }

    result = await stream_data_to_batch_server("recruit", chunks, metadata)

    # 3. 검증
    assert result["status"] == "success"
    assert result["rows_sent"] > 0
```

---

## 7. 참조

### 관련 문서
- **Python 서버 개발 가이드**: `/docs/Python_서버_개발_가이드.md` - 전체 아키텍처
- **데이터 처리 가이드**: `/docs/데이터_처리_가이드.md` - Chunk Loader

### Backend 공통 문서
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md` ⭐
- **테이블 명세서**: `/Backend/docs/table_specification.md` ⭐

### 히스토리
- **FastAPI + gRPC Client 구현**: `/docs/hist/2025-12-12_01_FastAPI_및_클라이언트_스트리밍_구현.md`

---

**최종 수정일**: 2025-12-17
**구현 상태**: Client Streaming 완료 / oneof 패턴 3개 도메인 지원
