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
│   ├── grpc_server.py          # gRPC 서버 메인
│   ├── data_loader.py          # pkl 파일 로딩
│   ├── chunker.py              # Chunk 분할 로직
│   └── proto/                  # Proto 파일 (Batch Server에서 복사)
│       ├── embedding_stream_pb2.py
│       └── embedding_stream_pb2_grpc.py
│
├── data/
│   └── recruit_embeddings.pkl  # Embedding 데이터 (용량 큼)
│
├── requirements.txt            # Python 의존성
├── README.md                   # 실행 가이드
└── CLAUDE.md                   # 현재 문서
```

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
        'job_post_vectors': [0.1, 0.2, ..., 0.5]  # 1536 dimensions
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

2. **Chunk Size 조정**
   - 네트워크 상황에 따라 100~500 사이로 조정
   - 너무 크면 메모리 부족, 너무 작으면 오버헤드 증가

3. **Checkpoint 처리**
   - `last_processed_uuid`를 받아 중복 전송 방지
   - UUID 순서대로 정렬되어 있어야 함

---

## 🎓 기술적 포인트

### 1. gRPC Streaming
- Server-side Streaming 사용
- Backpressure 지원
- 메모리 효율적인 대용량 데이터 전송

### 2. Pandas 최적화
- `read_pickle()`: 빠른 직렬화/역직렬화
- Chunk 단위 처리로 메모리 절약

### 3. 에러 처리
- gRPC Status Code 활용
- 재시도 로직 구현

---

## 🔗 관련 문서

- [루트 CLAUDE.md](../CLAUDE.md)
- [Batch Server CLAUDE.md](../Backend/Batch-Server/CLAUDE.md)
- [Entire Structure](../Backend/Batch-Server/docs/Entire_Structure.md)
- [Batch 설계서](../Backend/Batch-Server/docs/Batch설계서.md)

---

## ✅ 현재 진행 상황

### 예정
- ⏳ Python 프로젝트 초기 설정
- ⏳ Proto 파일 컴파일
- ⏳ gRPC 서버 구현
- ⏳ pkl 로더 구현
- ⏳ Chunk 분할 로직 구현
- ⏳ 테스트 데이터 생성
- ⏳ Batch Server와 통신 테스트

---

**최종 수정일:** 2025-12-10
