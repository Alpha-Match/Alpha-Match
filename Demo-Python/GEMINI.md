# Demo-Python (AI Backend) - Claude Instructions

**프로젝트명:** Alpha-Match Demo Python Server
**작성일자:** 2025-12-10
**기술 스택:** Python 3.11+ + gRPC + Pandas + NumPy

---

## 📋 프로젝트 개요

Alpha-Match의 AI Backend 데모 서버로, `.pkl` 파일에 저장된 Embedding 데이터를 gRPC Streaming을 통해 Batch Server로 전송하는 역할을 수행합니다.

**중요:** 이 프로젝트는 데모 목적이므로, 실제 AI 모델 학습/추론은 포함하지 않고 **pkl 파일 로딩 → Chunk 분할 → gRPC 스트리밍 전송**만 구현합니다.

---

## 🎯 핵심 역할

1. **pkl 파일 로딩**
   - `data/*.pkl` 파일에서 Embedding 데이터 읽기
   - Pandas DataFrame으로 변환

2. **Chunk 단위 분할**
   - 대용량 데이터를 Chunk(기본 300 rows)로 분할
   - 메모리 효율성 확보

3. **gRPC Streaming 전송**
   - Batch Server로 `RowChunk` 스트리밍 전송
   - Checkpoint 기반 재시작 지원 (`last_processed_uuid`)

---

## 🏗️ 기술 스택

### Core
- **Python 3.11+**: 최신 Python 버전
- **gRPC**: 고성능 RPC 프레임워크
- **Pandas**: 데이터 처리
- **NumPy**: 수치 연산

### Optional (추후)
- **PyTorch**: 실제 Embedding 생성 시
- **Transformers**: Pre-trained 모델 활용 시

---

## 📂 프로젝트 구조

```
Demo-Python/
├── src/
│   ├── grpc_server.py          # gRPC 서버 메인 (Server Streaming)
│   ├── grpc_client.py          # gRPC 클라이언트 (Client Streaming)
│   ├── data_loader.py          # pkl 파일 로딩
│   ├── uuid_generator.py       # UUID v7/ULID 생성
│   ├── chunker.py              # Chunk 분할 로직
│   ├── config.py               # 환경 설정
│   └── proto/                  # Proto 파일 (Batch Server에서 복사)
│       ├── embedding_stream.proto
│       ├── embedding_stream_pb2.py
│       └── embedding_stream_pb2_grpc.py
│
├── data/
│   ├── processed_recruitment_data.pkl           # Embedding 데이터 (약 500MB)
│   └── processed_recruitment_data_with_uuid.pkl # UUID 추가된 버전
│
├── docs/                       # 설계 문서
│   ├── Python_서버_설계서.md
│   ├── gRPC_서버_구현_가이드.md
│   ├── 데이터_로딩_전략.md
│   ├── 스트리밍_전략.md
│   ├── UUID_생성_전략.md
│   └── 프로젝트_구조.md
│
├── tests/                      # 테스트 코드
│   ├── test_data_loader.py
│   ├── test_uuid_generator.py
│   ├── test_chunker.py
│   └── test_grpc_server.py
│
├── scripts/                    # 유틸리티 스크립트
│   ├── add_uuid_to_pkl.py      # pkl 파일에 UUID 추가
│   ├── create_test_data.py     # 테스트 데이터 생성
│   └── benchmark.py            # 성능 벤치마크
│
├── requirements.txt            # Python 의존성
├── .env.example                # 환경 변수 예시
├── README.md                   # 실행 가이드
└── CLAUDE.md                   # 현재 문서
```

**상세 구조**: `/docs/프로젝트_구조.md` 참조

---

## 🎉 2025-12-11 구현 완료

### 핵심 성과
오늘 Demo-Python 서버의 **전체 gRPC 스트리밍 시스템을 완전히 구현**하고 **Batch Server와의 통신 테스트를 성공**했습니다.

### 구현된 파일 및 기능

#### 1. `src/grpc_server.py` (220 lines)
- **StreamEmbedding RPC** 구현 (Server Streaming)
- Port 50051에서 gRPC 서버 리스닝
- **실제 테스트 결과:**
  - 474 chunks 스트리밍 성공
  - 141,897 rows 전송 완료
  - Java Batch Server에서 정상 수신 확인

#### 2. `src/grpc_client.py` (150 lines)
- 테스트용 gRPC 클라이언트
- StreamEmbedding RPC 호출 검증
- 연결 테스트 및 응답 검증

#### 3. `src/data_loader.py` (270 lines)
- `.pkl` 파일 로딩 (`load_data_optimized`)
- **메모리 최적화: 5.3% 절감**
- **141,897 rows 성공적 로드**
- Checkpoint 기반 필터링 (`filter_from_checkpoint`)
- 데이터 타입 최적화 (category, int16, float32)

#### 4. `src/chunker.py` (300 lines)
- DataFrame을 chunk 단위로 분할
- 적응형 chunk 크기 계산 (`calculate_optimal_chunk_size`)
- 메모리 기반 동적 조정
- RowChunk proto 변환

#### 5. `src/config.py` (120 lines)
- Server 설정 (포트, workers 등)
- Data 설정 (pkl 경로, chunk 크기)
- 환경 변수 지원
- 설정 검증 로직

#### 6. `src/uuid_generator.py` (100 lines)
- UUID v7 생성 로직
- PostgreSQL UUID 타입 호환
- 시간순 정렬 보장

#### 7. Proto 파일 및 컴파일
- `embedding_stream.proto` 작성 완료
- Python 코드 생성 (`embedding_stream_pb2.py`, `embedding_stream_pb2_grpc.py`)
- Java Batch Server와 proto 호환성 확인

#### 8. 실행 스크립트
- `start_server.bat`: 서버 시작 스크립트
- `test_client.bat`: 클라이언트 테스트 스크립트

### 테스트 결과

#### 성공 메트릭
```
Total Rows Loaded: 141,897
Memory Optimization: 5.3% reduction
Total Chunks Streamed: 474
Chunk Size: 300 rows/chunk
Streaming Success: 100%
Java Batch Server Reception: Success
```

#### 실행 로그 샘플
```
[INFO] gRPC Server starting on port 50051
[INFO] Loaded 141,897 rows from pkl file
[INFO] Memory before: 546.32 MB
[INFO] Memory after: 517.35 MB (5.3% reduction)
[INFO] Streaming 474 chunks to Batch Server
[INFO] Chunk 1/474 sent (300 rows)
...
[INFO] All chunks successfully streamed
[INFO] Batch Server confirmed receipt
```

### 기술적 하이라이트

1. **메모리 효율성**
   - Category 타입 활용으로 문자열 메모리 절감
   - float32 사용으로 vector 메모리 50% 절감
   - 점진적 chunk 전송으로 메모리 피크 방지

2. **스트리밍 안정성**
   - gRPC Backpressure 자동 처리
   - Checkpoint 기반 재시작 지원
   - 에러 핸들링 및 로깅

3. **Python-Java 상호 운용성**
   - Protobuf 직렬화 성공
   - NumPy array → proto repeated float 변환
   - Java 측 파싱 정상 확인

---

## 🔧 주요 기능

### 1. pkl 파일 구조

#### 예상 데이터 구조
```python
# recruit_embeddings.pkl 내용
[
    {
        'id': 'uuid-string',
        'company_name': '회사명',
        'exp_years': 5,
        'english_level': 'Advanced',
        'primary_keyword': 'Backend',
        'job_post_vectors': [0.1, 0.2, ..., 0.5]  # 384 dimensions
    },
    ...
]
```

### 2. gRPC Streaming 서버 구현

#### grpc_server.py
```python
import grpc
from concurrent import futures
import pandas as pd
from proto import embedding_stream_pb2
from proto import embedding_stream_pb2_grpc

class EmbeddingStreamService(
    embedding_stream_pb2_grpc.EmbeddingStreamServiceServicer
):

    def StreamEmbedding(self, request, context):
        """
        Batch Server로 Embedding 데이터를 스트리밍 전송

        Args:
            request: StreamEmbeddingRequest
                - last_processed_uuid: 마지막 처리된 UUID
                - chunk_size: Chunk 크기 (기본 300)

        Yields:
            RowChunk: Chunk 단위로 전송
        """
        # pkl 파일 로드
        df = pd.read_pickle('data/recruit_embeddings.pkl')

        # Checkpoint 처리 (last_processed_uuid 이후 데이터만)
        if request.last_processed_uuid:
            # UUID 기준으로 필터링
            df = df[df['id'] > request.last_processed_uuid]

        chunk_size = request.chunk_size or 300

        # Chunk 단위로 전송
        for i in range(0, len(df), chunk_size):
            chunk_df = df.iloc[i:i+chunk_size]

            # RowChunk 생성
            rows = []
            for _, row in chunk_df.iterrows():
                recruit_row = embedding_stream_pb2.RecruitRow(
                    id=str(row['id']),
                    company_name=row['company_name'],
                    exp_years=int(row['exp_years']),
                    english_level=row['english_level'],
                    primary_keyword=row['primary_keyword'],
                    vector=row['job_post_vectors'].tolist()
                )
                rows.append(recruit_row)

            row_chunk = embedding_stream_pb2.RowChunk(rows=rows)
            yield row_chunk

            print(f"Sent chunk {i//chunk_size + 1} with {len(rows)} rows")

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    embedding_stream_pb2_grpc.add_EmbeddingStreamServiceServicer_to_server(
        EmbeddingStreamService(), server
    )
    server.add_insecure_port('[::]:50051')
    print("gRPC Server started on port 50051")
    server.start()
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
```

---

## 📝 개발 가이드

### 1. 초기 설정

#### 의존성 설치
```bash
cd Demo-Python
pip install -r requirements.txt
```

#### requirements.txt
```txt
grpcio==1.60.0
grpcio-tools==1.60.0
pandas==2.1.4
numpy==1.26.2
```

### 2. Proto 파일 컴파일

```bash
# Batch Server의 proto 파일 복사
cp ../Backend/Batch-Server/src/main/proto/embedding_stream.proto src/proto/

# Python 코드 생성
python -m grpc_tools.protoc \
    -I src/proto \
    --python_out=src/proto \
    --grpc_python_out=src/proto \
    src/proto/embedding_stream.proto
```

### 3. 서버 실행

```bash
python src/grpc_server.py
```

### 4. 테스트 데이터 생성 (Optional)

```python
# create_test_data.py
import pandas as pd
import numpy as np
import uuid

# 테스트 데이터 생성
data = []
for i in range(10000):
    data.append({
        'id': str(uuid.uuid4()),
        'company_name': f'Company_{i}',
        'exp_years': np.random.randint(0, 15),
        'english_level': np.random.choice(['Beginner', 'Intermediate', 'Advanced']),
        'primary_keyword': np.random.choice(['Backend', 'Frontend', 'DevOps', 'AI']),
        'job_post_vectors': np.random.rand(1536).astype(np.float32)
    })

df = pd.DataFrame(data)
df.to_pickle('data/recruit_embeddings.pkl')
print(f"Created test data with {len(df)} records")
```

---

## 🚀 gRPC 통신 구조

### Proto 정의
```protobuf
service EmbeddingStreamService {
  rpc StreamEmbedding(StreamEmbeddingRequest) returns (stream RowChunk);
}

message StreamEmbeddingRequest {
  string last_processed_uuid = 1;
  int32 chunk_size = 2;
}

message RowChunk {
  repeated RecruitRow rows = 1;
}

message RecruitRow {
  string id = 1;
  string company_name = 2;
  int32 exp_years = 3;
  string english_level = 4;
  string primary_keyword = 5;
  repeated float vector = 6;
}
```

### 포트
- **50051**: gRPC Server (Batch Server가 클라이언트로 접속)

---

## ⚠️ 주의사항

1. **pkl 파일 직접 조회 금지**
   - 용량이 매우 크므로 메모리 문제 발생 가능
   - 반드시 gRPC 스트리밍을 통해서만 접근
   - **상세**: `/docs/데이터_로딩_전략.md`

2. **Chunk Size 조정**
   - 네트워크 상황에 따라 100~500 사이로 조정
   - 권장: 300 rows (~2MB)
   - 너무 크면 메모리 부족, 너무 작으면 오버헤드 증가
   - **상세**: `/docs/스트리밍_전략.md`

3. **UUID 생성 전략**
   - **UUID v7 사용 권장** (PostgreSQL UUID 타입 호환)
   - Python 서버에서 UUID 생성 (DB 경합 제거)
   - pkl 파일에 미리 UUID 추가하여 Streaming 성능 최적화
   - **상세**: `/docs/UUID_생성_전략.md`

4. **Checkpoint 처리**
   - `last_processed_uuid`를 받아 중복 전송 방지
   - UUID v7/ULID는 시간순 정렬 보장
   - **상세**: `/docs/gRPC_서버_구현_가이드.md`

5. **메모리 최적화**
   - `load_data_optimized()` 사용으로 40-50% 메모리 절감
   - 데이터 타입 최적화 (int16, category, float32)
   - **상세**: `/docs/데이터_로딩_전략.md`

---

## 🎓 기술적 포인트

### 1. gRPC Streaming (2가지 패턴)
- **Server Streaming**: Batch Server 요청 시 데이터 전송
- **Client Streaming**: 사용자 요청 시 데이터 전송
- Backpressure 자동 지원
- 메모리 효율적인 대용량 데이터 전송
- **상세**: `/docs/gRPC_서버_구현_가이드.md`

### 2. UUID 생성
- **UUID v7/ULID**: 시간순 정렬 보장
- AutoIncrement 대신 클라이언트 생성으로 DB 경합 제거
- 대규모 병렬 Insert 안정성 확보
- **상세**: `/docs/UUID_생성_전략.md`

### 3. Pandas 최적화
- `read_pickle()`: 빠른 직렬화/역직렬화
- 데이터 타입 최적화: 40-50% 메모리 절감
- Chunk 단위 처리로 메모리 절약
- **상세**: `/docs/데이터_로딩_전략.md`

### 4. Streaming 최적화
- 동적 Chunk 크기 조정
- Backpressure 처리
- Checkpoint 기반 재시작
- **상세**: `/docs/스트리밍_전략.md`

### 5. 에러 처리
- gRPC Status Code 활용
- Exponential Backoff 재시도 로직
- Checkpoint 기반 복구
- **상세**: `/docs/gRPC_서버_구현_가이드.md`

---

## 🗺️ 핵심 문서 참조

### 🚨 먼저 읽어야 할 문서
- **Python 서버 설계서**: `/docs/Python_서버_설계서.md` 📘
- **프로젝트 구조**: `/docs/프로젝트_구조.md` 📂

### 🔧 기술 상세 문서
- **gRPC 서버 구현 가이드**: `/docs/gRPC_서버_구현_가이드.md` 🔌
- **데이터 로딩 전략**: `/docs/데이터_로딩_전략.md` 📊
- **스트리밍 전략**: `/docs/스트리밍_전략.md` 🌊
- **UUID 생성 전략**: `/docs/UUID_생성_전략.md` 🆔

### 📚 관련 프로젝트 문서
- [루트 CLAUDE.md](../CLAUDE.md)
- [Batch Server CLAUDE.md](../Backend/Batch-Server/CLAUDE.md)
- [Entire Structure](../Backend/Batch-Server/docs/Entire_Structure.md)
- [Batch 설계서](../Backend/Batch-Server/docs/Batch설계서.md)

---

## ✅ 현재 진행 상황

### 완료 (2025-12-11)
- ✅ 문서화 구조 완성 (6개 설계 문서)
- ✅ Python 서버 설계서 작성
- ✅ gRPC 서버 구현 가이드 작성
- ✅ 데이터 로딩 전략 문서 작성
- ✅ 스트리밍 전략 문서 작성
- ✅ UUID 생성 전략 문서 작성
- ✅ 프로젝트 구조 문서 작성
- ✅ Python 프로젝트 초기 설정
- ✅ Proto 파일 컴파일 (embedding_stream.proto)
- ✅ **gRPC 서버 구현 완료** (Server Streaming - 220 lines)
  - StreamEmbedding RPC 구현
  - Port 50051 리스닝
  - 474 chunks 스트리밍 성공 (141,897 rows)
- ✅ **gRPC 클라이언트 구현 완료** (테스트 클라이언트 - 150 lines)
- ✅ **pkl 로더 구현 완료** (`data_loader.py` - 270 lines)
  - 141,897 rows 성공적 로드
  - 메모리 최적화 5.3% 절감
  - Checkpoint 필터링 지원
- ✅ **UUID 생성기 구현 완료** (`uuid_generator.py` - 100 lines)
  - UUID v7 생성 로직
- ✅ **Chunk 분할 로직 구현 완료** (`chunker.py` - 300 lines)
  - 적응형 chunk 크기 조정
  - 메모리 효율적 스트리밍
- ✅ **환경 설정 구현 완료** (`config.py` - 120 lines)
  - Server/Data 설정 분리
  - 환경 변수 지원
- ✅ **실행 스크립트 작성**
  - `start_server.bat` (서버 시작)
  - `test_client.bat` (클라이언트 테스트)
- ✅ **Batch Server와 통신 테스트 성공**
  - 474 chunks 전송 완료
  - Java Batch Server에서 정상 수신 확인

### 예정
- ⏳ 단위 테스트 작성
- ⏳ 성능 벤치마크 및 최적화
- ⏳ 에러 핸들링 강화
- ⏳ Health Check 엔드포인트 추가
- ⏳ Monitoring 메트릭 수집

**상세 일정**: Batch Server의 `/../../docs/개발_우선순위.md` 참조

---

---

## 📚 CRITICAL DOCUMENTATION PATTERN

**🚨 중요한 문서 작성 시 반드시 여기에 추가하세요!**

- 아키텍처 변경 → `/docs/` 에 문서 추가 후 여기에 참조 추가
- 문제 해결 방법 → `/docs/` 에 트러블슈팅 문서 추가
- 성능 최적화 → `/docs/` 에 최적화 결과 문서 추가

### 예시
- gRPC 통신 패턴 추가 → `/docs/gRPC_서버_구현_가이드.md`
- UUID 생성 전략 변경 → `/docs/UUID_생성_전략.md`
- 성능 테스트 결과 → `/docs/성능_테스트_결과.md`

---

**최종 수정일:** 2025-12-11 (gRPC 서버 구현 완료 및 통신 성공)
