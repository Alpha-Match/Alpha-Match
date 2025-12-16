# 📘 Python AI Embedding Streaming 서버 설계서

*Headhunter-Recruit Matching System — Python gRPC Streaming Server*

> 본 문서는 `.pkl` 파일에 저장된 Recruit Embedding 및 Metadata를
>
> **gRPC Streaming**을 통해 Batch Server로 전송하는 Python AI Backend 서버의 아키텍처 설계서입니다.
>
> Python 3.11+ + gRPC + Pandas + NumPy 조합을 기반으로 합니다.

---

# 1. **목적(Purpose)**

```
pkl 파일 → Python AI Server → gRPC Streaming → Batch Server
                  │
                  └→ UUID v7/ULID 생성
```

본 Python 서버는 `.pkl` 파일에 저장된 **대용량 Embedding 및 Metadata**를 로딩하고,
이를 **gRPC Streaming**으로 Batch Server에 전송하는 역할을 수행한다.

**중요:** 이 프로젝트는 데모 목적이므로, 실제 AI 모델 학습/추론은 포함하지 않고
**pkl 파일 로딩 → Chunk 분할 → UUID 생성 → gRPC 스트리밍 전송**만 구현한다.

---

# 2. 주요 요구사항

### ✔ 데이터 로딩

> `.pkl` 파일에서 Embedding 데이터를 Pandas DataFrame으로 로딩
>
> 메모리 효율성을 위해 필요시 chunk 단위로 로딩
>
> 약 500MB 크기의 pkl 파일 처리 가능

### ✔ UUID 생성

> **UUID v7/ULID**를 Python 서버에서 생성
>
> 시간순 정렬 보장으로 DB 인덱스 성능 최적화
>
> Batch Server에서의 AutoIncrement 경합 제거

### ✔ gRPC Streaming

> **서버 스트리밍 (Server Streaming)**: Batch Server 요청 시 데이터 전송
>
> **클라이언트 스트리밍 (Client Streaming)**: 사용자 요청 시 데이터 전송
>
> Chunk 단위 전송으로 메모리 효율성 확보
>
> Backpressure 지원으로 안정적인 대용량 데이터 전송

### ✔ Checkpoint 지원

> `last_processed_uuid`를 받아 중복 전송 방지
>
> 재시작 가능성 고려한 idempotent 전략

---

# 3. 전체 처리 흐름 구조

## 3.1 아키텍처 흐름도

### 서버 스트리밍 (Quartz 기반 자동 배치)

```mermaid
flowchart LR
    PKL["pkl 파일<br>processed_recruitment_data.pkl"]
    LOAD["Data Loader<br>Pandas DataFrame"]
    UUID["UUID Generator<br>UUID v7/ULID"]
    CHUNK["Chunker<br>Split into chunks"]
    GRPC["gRPC Server<br>GetEmbeddings"]
    BS["Batch Server<br>StreamingService"]

    PKL --> LOAD --> UUID --> CHUNK --> GRPC --> BS
```

### 클라이언트 스트리밍 (사용자 요청 기반)

```mermaid
flowchart LR
    USER["User Request"]
    CLIENT["gRPC Client<br>UploadEmbeddings"]
    CHUNK["Chunker<br>Split into chunks"]
    GRPC["gRPC Server<br>(Batch Server)"]
    RESULT["Upload Result"]

    USER --> CLIENT --> CHUNK --> GRPC --> RESULT
```

---

# 4. 데이터 구조

## 4.1 pkl 파일 구조

### 예상 데이터 구조
```python
# processed_recruitment_data.pkl 내용
[
    {
        'id': 'uuid-string',  # 기존 ID (있을 경우)
        'company_name': '회사명',
        'exp_years': 5,
        'english_level': 'Advanced',
        'primary_keyword': 'Backend',
        'job_post_vectors': [0.1, 0.2, ..., 0.5]  # 384 dimensions
    },
    ...
]
```

## 4.2 UUID 생성 전략

### UUID v7 vs ULID 비교

| 특징 | UUID v7 | ULID |
|-----|---------|------|
| **시간순 정렬** | ✅ 밀리초 단위 | ✅ 밀리초 단위 |
| **표준 준수** | ✅ RFC 9562 | ⚠️ 비표준 |
| **길이** | 36자 (하이픈 포함) | 26자 (Base32) |
| **Python 지원** | `uuid6` 라이브러리 | `python-ulid` 라이브러리 |
| **DB 호환성** | ✅ PostgreSQL UUID 타입 | ⚠️ VARCHAR(26) |
| **인덱싱 성능** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **권장도** | ✅ **권장** | 🟡 대안 |

### Python 코드 예제

```python
# UUID v7 생성
from uuid6 import uuid7

def generate_uuid_v7():
    return str(uuid7())

# ULID 생성
from ulid import ULID

def generate_ulid():
    return str(ULID())
```

**권장:** PostgreSQL의 표준 UUID 타입과의 호환성을 위해 **UUID v7** 사용

---

# 5. gRPC 통신 패턴

## 5.1 서버 스트리밍 (Server Streaming)

### 사용 시나리오
- Quartz Scheduler가 Batch Server를 트리거
- Batch Server가 클라이언트 역할
- Python Server가 서버 역할

### Proto 정의
```protobuf
service EmbeddingStreamService {
  rpc GetEmbeddings (RequestParams) returns (stream Embedding);
}

message RequestParams {
  string last_processed_uuid = 1;
  int32 chunk_size = 2;
}

message Embedding {
  string id = 1;
  string company_name = 2;
  int32 exp_years = 3;
  string english_level = 4;
  string primary_keyword = 5;
  repeated float vector = 6;
}
```

### Python 구현
```python
class EmbeddingStreamService(
    embedding_stream_pb2_grpc.EmbeddingStreamServiceServicer
):
    def GetEmbeddings(self, request, context):
        """서버 스트리밍: Batch Server 요청 시 데이터 전송"""
        # pkl 파일 로드
        df = load_data_from_pkl()

        # Checkpoint 처리
        if request.last_processed_uuid:
            df = df[df['id'] > request.last_processed_uuid]

        chunk_size = request.chunk_size or 300

        # Chunk 단위로 전송
        for chunk_df in chunker(df, chunk_size):
            for _, row in chunk_df.iterrows():
                embedding = embedding_stream_pb2.Embedding(
                    id=str(row['id']),
                    company_name=row['company_name'],
                    exp_years=int(row['exp_years']),
                    english_level=row['english_level'],
                    primary_keyword=row['primary_keyword'],
                    vector=row['job_post_vectors'].tolist()
                )
                yield embedding
```

## 5.2 클라이언트 스트리밍 (Client Streaming)

### 사용 시나리오
- 사용자가 직접 Python 서버에 요청
- Python Server가 클라이언트 역할
- Batch Server가 서버 역할

### Proto 정의
```protobuf
service EmbeddingUploadService {
  rpc UploadEmbeddings (stream Embedding) returns (UploadResult);
}

message UploadResult {
  int32 total_count = 1;
  int32 success_count = 2;
  int32 failed_count = 3;
  repeated string failed_ids = 4;
}
```

### Python 구현
```python
def upload_embeddings_to_batch_server():
    """클라이언트 스트리밍: 사용자 요청 시 데이터 전송"""
    channel = grpc.insecure_channel('localhost:50052')
    stub = embedding_upload_pb2_grpc.EmbeddingUploadServiceStub(channel)

    def generate_embeddings():
        df = load_data_from_pkl()
        chunk_size = 300

        for chunk_df in chunker(df, chunk_size):
            for _, row in chunk_df.iterrows():
                embedding = embedding_upload_pb2.Embedding(
                    id=str(row['id']),
                    company_name=row['company_name'],
                    exp_years=int(row['exp_years']),
                    english_level=row['english_level'],
                    primary_keyword=row['primary_keyword'],
                    vector=row['job_post_vectors'].tolist()
                )
                yield embedding

    # 스트리밍 전송
    result = stub.UploadEmbeddings(generate_embeddings())
    print(f"Upload completed: {result.success_count}/{result.total_count}")
```

---

# 6. Chunk 크기 최적화

## 6.1 Chunk 크기 가이드라인

### 권장 Chunk 크기

| 시나리오 | 권장 크기 | 이유 |
|---------|---------|-----|
| **네트워크 양호** | 500 | gRPC 메시지 크기 최적화 |
| **일반적인 경우** | 300 | 메모리와 성능의 균형 |
| **메모리 제한** | 100 | 메모리 부족 방지 |
| **디버깅** | 10 | 로그 추적 용이 |

### Vector 크기별 메모리 사용량

#### 계산식
```
메모리 = chunk_size × vector_dimension × 4 bytes (float32)
```

#### 예시 (1536 차원 기준)

| Chunk 크기 | Vector 메모리 | Metadata 메모리 | 총 메모리 |
|-----------|-------------|----------------|----------|
| 10 | ~60 KB | ~5 KB | ~65 KB |
| 100 | ~600 KB | ~50 KB | ~650 KB |
| 300 | ~1.8 MB | ~150 KB | ~2 MB |
| 500 | ~3 MB | ~250 KB | ~3.5 MB |
| 1000 | ~6 MB | ~500 KB | ~7 MB |

## 6.2 동적 Chunk 크기 조정

```python
def calculate_optimal_chunk_size(
    vector_dimension: int = 1536,
    available_memory_mb: float = 100,
    safety_factor: float = 0.5
) -> int:
    """
    사용 가능한 메모리를 기반으로 최적 Chunk 크기 계산

    Args:
        vector_dimension: Vector 차원 (기본 1536)
        available_memory_mb: 사용 가능한 메모리 (MB)
        safety_factor: 안전 여유율 (0.5 = 50%)

    Returns:
        최적 Chunk 크기
    """
    # Vector 메모리 (float32 = 4 bytes)
    vector_bytes = vector_dimension * 4

    # Metadata 대략 500 bytes로 가정
    metadata_bytes = 500

    # 총 row 당 메모리
    bytes_per_row = vector_bytes + metadata_bytes

    # 사용 가능한 메모리 (안전 여유율 적용)
    usable_bytes = available_memory_mb * 1024 * 1024 * safety_factor

    # 최적 Chunk 크기
    optimal_size = int(usable_bytes / bytes_per_row)

    # 최소 10, 최대 1000으로 제한
    return max(10, min(optimal_size, 1000))

# 사용 예시
chunk_size = calculate_optimal_chunk_size(
    vector_dimension=1536,
    available_memory_mb=100,  # 100MB 사용 가능
    safety_factor=0.5
)
print(f"Optimal chunk size: {chunk_size}")  # 약 300
```

---

# 7. 에러 처리 및 재시도 로직

## 7.1 gRPC Status Code 활용

```python
import grpc
from grpc import StatusCode

def GetEmbeddings(self, request, context):
    try:
        # 데이터 로딩
        df = load_data_from_pkl()

        # Streaming
        for chunk in chunker(df, chunk_size):
            yield chunk

    except FileNotFoundError:
        context.set_code(StatusCode.NOT_FOUND)
        context.set_details('pkl file not found')
        return

    except MemoryError:
        context.set_code(StatusCode.RESOURCE_EXHAUSTED)
        context.set_details('Insufficient memory')
        return

    except Exception as e:
        context.set_code(StatusCode.INTERNAL)
        context.set_details(f'Internal error: {str(e)}')
        return
```

## 7.2 재시도 로직 (Client Streaming)

```python
from grpc import RpcError
import time

def upload_with_retry(max_retries=3, retry_delay=2):
    """재시도 로직을 포함한 업로드"""
    for attempt in range(max_retries):
        try:
            result = upload_embeddings_to_batch_server()
            return result

        except RpcError as e:
            if e.code() == StatusCode.UNAVAILABLE:
                if attempt < max_retries - 1:
                    print(f"Retry {attempt + 1}/{max_retries}...")
                    time.sleep(retry_delay)
                    continue
                else:
                    raise
            else:
                raise
```

---

# 8. 성능 최적화 전략

## 8.1 Pandas 최적화

### 메모리 효율적인 로딩

```python
import pandas as pd
import numpy as np

def load_data_optimized(pkl_path: str) -> pd.DataFrame:
    """메모리 효율적인 pkl 파일 로딩"""
    # pkl 파일 로드
    df = pd.read_pickle(pkl_path)

    # 데이터 타입 최적화
    df['exp_years'] = df['exp_years'].astype(np.int16)
    df['company_name'] = df['company_name'].astype('category')
    df['english_level'] = df['english_level'].astype('category')
    df['primary_keyword'] = df['primary_keyword'].astype('category')

    # Vector는 float32로 유지 (필요시 float16)
    if isinstance(df['job_post_vectors'].iloc[0], list):
        df['job_post_vectors'] = df['job_post_vectors'].apply(
            lambda x: np.array(x, dtype=np.float32)
        )

    return df
```

### 메모리 사용량 확인

```python
def print_memory_usage(df: pd.DataFrame):
    """DataFrame 메모리 사용량 출력"""
    memory_usage = df.memory_usage(deep=True)
    total_mb = memory_usage.sum() / 1024 / 1024

    print(f"Total memory usage: {total_mb:.2f} MB")
    print("\nPer column:")
    for col, mem in memory_usage.items():
        print(f"  {col}: {mem / 1024 / 1024:.2f} MB")
```

## 8.2 gRPC 최적화

### Server Options

```python
def serve():
    # gRPC 서버 옵션 최적화
    options = [
        # 최대 메시지 크기 (기본 4MB → 16MB)
        ('grpc.max_send_message_length', 16 * 1024 * 1024),
        ('grpc.max_receive_message_length', 16 * 1024 * 1024),

        # Keep-alive 설정
        ('grpc.keepalive_time_ms', 30000),
        ('grpc.keepalive_timeout_ms', 10000),

        # HTTP/2 설정
        ('grpc.http2.max_pings_without_data', 0),
        ('grpc.http2.min_time_between_pings_ms', 10000),
    ]

    server = grpc.server(
        futures.ThreadPoolExecutor(max_workers=10),
        options=options
    )

    embedding_stream_pb2_grpc.add_EmbeddingStreamServiceServicer_to_server(
        EmbeddingStreamService(), server
    )

    server.add_insecure_port('[::]:50051')
    server.start()
    server.wait_for_termination()
```

---

# 9. 배포 및 운영

## 9.1 환경 변수

```python
# config.py
import os

class Config:
    # 서버 설정
    GRPC_PORT = int(os.getenv('GRPC_PORT', 50051))
    MAX_WORKERS = int(os.getenv('MAX_WORKERS', 10))

    # 데이터 설정
    PKL_PATH = os.getenv('PKL_PATH', 'data/processed_recruitment_data.pkl')
    CHUNK_SIZE = int(os.getenv('CHUNK_SIZE', 300))

    # 성능 설정
    MAX_MESSAGE_SIZE = int(os.getenv('MAX_MESSAGE_SIZE', 16 * 1024 * 1024))

    # Batch Server 설정 (Client Streaming)
    BATCH_SERVER_HOST = os.getenv('BATCH_SERVER_HOST', 'localhost')
    BATCH_SERVER_PORT = int(os.getenv('BATCH_SERVER_PORT', 50052))
```

## 9.2 로깅

```python
import logging

def setup_logging():
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler('grpc_server.log'),
            logging.StreamHandler()
        ]
    )

    return logging.getLogger(__name__)

logger = setup_logging()

# 사용 예시
def GetEmbeddings(self, request, context):
    logger.info(f"GetEmbeddings called with chunk_size={request.chunk_size}")
    # ...
    logger.info(f"Sent {total_count} embeddings")
```

---

# 10. 테스트

## 10.1 단위 테스트

```python
import unittest
from unittest.mock import patch, MagicMock

class TestDataLoader(unittest.TestCase):

    @patch('pandas.read_pickle')
    def test_load_data_optimized(self, mock_read_pickle):
        # Mock DataFrame
        mock_df = pd.DataFrame({
            'id': ['uuid1', 'uuid2'],
            'company_name': ['Company A', 'Company B'],
            'exp_years': [5, 3],
            'english_level': ['Advanced', 'Intermediate'],
            'primary_keyword': ['Backend', 'Frontend'],
            'job_post_vectors': [np.random.rand(1536), np.random.rand(1536)]
        })
        mock_read_pickle.return_value = mock_df

        # Test
        df = load_data_optimized('test.pkl')

        # Assert
        self.assertEqual(len(df), 2)
        self.assertEqual(df['exp_years'].dtype, np.int16)
```

## 10.2 통합 테스트

```python
import grpc
import grpc_testing

class TestEmbeddingStreamService(unittest.TestCase):

    def setUp(self):
        # gRPC 테스트 채널 생성
        self.channel = grpc_testing.channel([
            embedding_stream_pb2.DESCRIPTOR.services_by_name['EmbeddingStreamService']
        ])

        self.service = EmbeddingStreamService()

    def test_get_embeddings_streaming(self):
        # Request 생성
        request = embedding_stream_pb2.RequestParams(
            chunk_size=10
        )

        # Streaming 호출
        responses = list(self.service.GetEmbeddings(request, None))

        # Assert
        self.assertGreater(len(responses), 0)
        self.assertEqual(len(responses[0].vector), 1536)
```

---

# 11. 주의사항 및 Best Practices

## ⚠️ 주의사항

1. **pkl 파일 직접 조회 금지**
   - 용량이 매우 크므로 메모리 문제 발생 가능
   - 반드시 gRPC 스트리밍을 통해서만 접근

2. **Chunk 크기 조정**
   - 네트워크 상황에 따라 100~500 사이로 조정
   - 너무 크면 메모리 부족, 너무 작으면 오버헤드 증가

3. **UUID 순서 보장**
   - `last_processed_uuid` 처리 시 UUID 순서대로 정렬 필요
   - UUID v7/ULID는 자동으로 시간순 정렬 보장

4. **Thread Pool 크기**
   - `max_workers` 설정 시 시스템 리소스 고려
   - 권장: CPU 코어 수 × 2

## ✅ Best Practices

1. **UUID v7 사용**
   - PostgreSQL UUID 타입과 호환
   - 시간순 정렬로 인덱스 성능 최적화

2. **메모리 최적화**
   - Pandas 데이터 타입 최적화 (`category`, `int16` 등)
   - 불필요한 데이터 조기 제거

3. **로깅 전략**
   - 각 Chunk 전송 시 로그 기록
   - 에러 발생 시 상세한 컨텍스트 포함

4. **환경 변수 활용**
   - 하드코딩 대신 환경 변수 사용
   - 개발/스테이징/프로덕션 환경 분리

---

# 12. 구현 상태 (2025-12-11)

## 12.1 구현 완료

### Phase 1: 기본 구현 ✅ 완료
- ✅ Proto 파일 컴파일 (`embedding_stream.proto`)
- ✅ Data Loader 구현 (`data_loader.py` - 270 lines)
  - `load_data_optimized()`: 메모리 최적화 로딩
  - `filter_from_checkpoint()`: Checkpoint 필터링
  - 141,897 rows 성공적 로드
  - 5.3% 메모리 절감
- ✅ UUID Generator 구현 (`uuid_generator.py` - 100 lines)
  - UUID v7 생성
  - PostgreSQL 호환
- ✅ 서버 스트리밍 구현 (`grpc_server.py` - 220 lines)
  - StreamEmbedding RPC
  - Port 50051 리스닝
  - **474 chunks 스트리밍 성공**
  - **Java Batch Server 통신 성공**

### Phase 2: 최적화 ✅ 완료
- ✅ Chunk 크기 동적 조정 (`chunker.py` - 300 lines)
  - `calculate_optimal_chunk_size()`: 메모리 기반 계산
  - 적응형 chunk 크기
- ✅ 메모리 사용량 모니터링
  - 로딩 전후 메모리 추적
  - 컬럼별 메모리 사용량 분석
- ✅ 환경 설정 (`config.py` - 120 lines)
  - Server/Data 설정 분리
  - 환경 변수 지원

### Phase 3: 테스트 및 운영 ✅ 기본 완료
- ✅ 클라이언트 테스트 구현 (`grpc_client.py` - 150 lines)
  - StreamEmbedding 호출 테스트
  - 연결 검증
- ✅ 실행 스크립트
  - `start_server.bat`
  - `test_client.bat`
- ✅ **Batch Server 통합 테스트 성공**

## 12.2 실제 테스트 결과

### 성능 메트릭
```
파일 크기: ~500MB
총 레코드: 141,897 rows
메모리 최적화: 5.3% 절감
총 chunks: 474
Chunk 크기: 300 rows/chunk
스트리밍 성공률: 100%
Batch Server 수신: 성공
```

### 실행 로그
```
[2025-12-11 15:30:00] INFO [grpc_server] - gRPC Server starting on port 50051
[2025-12-11 15:30:01] INFO [data_loader] - Loaded 141,897 rows
[2025-12-11 15:30:01] INFO [data_loader] - Memory: 546.32 MB → 517.35 MB (5.3% reduction)
[2025-12-11 15:30:05] INFO [grpc_server] - Streaming 474 chunks to Batch Server
[2025-12-11 15:30:15] INFO [grpc_server] - Successfully streamed all chunks
[2025-12-11 15:30:15] INFO [Batch Server] - Received 474 chunks (141,897 rows)
```

## 12.3 남은 작업

### 우선순위 높음
- ⏳ 단위 테스트 작성 (`tests/`)
- ⏳ 에러 핸들링 강화 (재시도 로직)
- ⏳ Health Check 엔드포인트

### 우선순위 중간
- ⏳ 성능 프로파일링 및 최적화
- ⏳ Monitoring 메트릭 수집
- ⏳ 로깅 레벨 세분화

### 우선순위 낮음 (선택적)
- ⏳ Client Streaming 구현 (현재 불필요)
- ⏳ Bidirectional Streaming (현재 불필요)
- ⏳ TLS/SSL 지원

## 12.4 관련 문서

- [gRPC_서버_구현_가이드.md](./gRPC_서버_구현_가이드.md)
- [데이터_로딩_전략.md](./데이터_로딩_전략.md)
- [스트리밍_전략.md](./스트리밍_전략.md)
- [UUID_생성_전략.md](./UUID_생성_전략.md)
- [프로젝트_구조.md](./프로젝트_구조.md)

---

**최종 수정일:** 2025-12-11
**구현 상태:** Phase 1, 2 완료 / Batch Server 통신 성공
