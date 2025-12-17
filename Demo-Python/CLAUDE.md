# Demo-Python (AI Backend) - Claude Instructions

**프로젝트명:** Alpha-Match Demo Python Server
**작성일자:** 2025-12-17
**기술 스택:** Python 3.11+ + gRPC + FastAPI + Pandas + NumPy + PyArrow

---

## 📋 프로젝트 개요

Alpha-Match의 AI Backend 데모 서버로, 다양한 포맷(`.pkl`, `.csv`, `.parquet`)의 Embedding 데이터를 gRPC Streaming을 통해 Batch Server로 전송하는 역할을 수행합니다.

**중요:** 이 프로젝트는 데모 목적이므로, 실제 AI 모델 학습/추론은 포함하지 않고 **파일 로딩 → Chunk 분할 → gRPC 스트리밍 전송**만 구현합니다.

---

## 🎯 핵심 역할

1. **다중 포맷 파일 로딩** (2025-12-17)
   - **pkl**: Pickle 파일 (Pandas 직렬화)
   - **csv**: CSV 파일 (Vector/Array 파싱 지원)
   - **parquet**: Parquet 파일 (PyArrow 기반)
   - 도메인별 자동 로더 선택 (2-tier registry)

2. **Chunk 단위 분할** (Iterator 패턴)
   - 대용량 데이터를 Chunk(기본 1000 rows)로 분할
   - 메모리 효율성 확보 (전체 로딩 방지)
   - Iterator 패턴으로 점진적 처리

3. **gRPC Client Streaming 전송**
   - Batch Server로 `IngestDataStream` RPC 호출
   - Chunk 단위 스트리밍
   - 도메인별 데이터 검증 (Pydantic)

4. **도메인별 확장성**
   - Recruit (384d vector)
   - Candidate (768d vector)
   - SkillEmbeddingDic (768d vector)
   - Protocol 기반 제네릭 구조

---

## 🏗️ 기술 스택

### Core
- **Python 3.11+**: 최신 Python 버전
- **FastAPI**: HTTP API 서버 (비동기)
- **gRPC**: 고성능 RPC 프레임워크
- **Pandas**: 데이터 처리
- **NumPy**: 수치 연산
- **Pydantic**: 데이터 검증 (Field Validator)
- **PyArrow**: Parquet 파일 처리

### Optional (추후)
- **PyTorch**: 실제 Embedding 생성 시
- **Transformers**: Pre-trained 모델 활용 시

---

## 📂 프로젝트 구조

```
Demo-Python/
├── src/
│   ├── main.py                          # FastAPI 앱 진입점
│   ├── api/
│   │   └── endpoints.py                 # FastAPI 엔드포인트
│   ├── services/
│   │   └── ingestion_service.py         # 데이터 수집 비즈니스 로직
│   ├── infrastructure/
│   │   ├── loaders.py                   # 도메인별 데이터 로더
│   │   └── grpc_clients.py              # gRPC 클라이언트 (Batch Server 연결)
│   ├── domain/
│   │   ├── models.py                    # 도메인 모델
│   │   └── utils.py                     # UUID 유틸리티
│   ├── config/
│   │   └── settings.py                  # 환경 설정
│   └── proto/                           # Generated protobuf 파일
│       ├── embedding_stream.proto
│       ├── embedding_stream_pb2.py
│       └── embedding_stream_pb2_grpc.py
│
├── data/
│   ├── processed_recruitment_data.pkl   # Recruit Embedding 데이터 (약 500MB)
│   └── processed_headhunter_data.pkl    # Headhunter Embedding 데이터
│
├── docs/                                # 설계 문서
│   ├── Python_서버_개발_가이드.md       # 메인 개발 가이드 ⭐
│   ├── 데이터_처리_가이드.md            # Chunk Loader + 도메인 모델
│   ├── gRPC_통신_가이드.md              # Client Streaming
│   └── hist/                           # 작업 히스토리 (Read-Only)
│       ├── 2025-12-12_01_FastAPI_및_클라이언트_스트리밍_구현.md
│       └── 구현_완료_보고서_2025-12-11.md
│
├── requirements.txt                     # Python 의존성
├── start_server.bat                     # 서버 시작 스크립트 (Windows)
├── README.md                            # 실행 가이드
└── CLAUDE.md                            # 현재 문서
```

**상세 구조**: `/docs/프로젝트_구조.md` 참조

---

## 🎉 2025-12-12 구현 완료: FastAPI + gRPC Client 아키텍처

### 핵심 성과
Demo-Python 서버를 **FastAPI + gRPC Client** 아키텍처로 리팩토링하여 **HTTP API 기반 데이터 수집 트리거** 시스템으로 전환했습니다.

### 아키텍처 변경 사항

**변경 전 (2025-12-11):**
- Python gRPC Server (Port 50051) - Server Streaming
- Batch Server가 Client로 연결

**변경 후 (2025-12-12):**
- Python FastAPI Server (Port 8000) - HTTP API
- Python gRPC Client - Batch Server에 Client Streaming
- Batch Server가 gRPC Server (Port 50052)

### 구현된 파일 및 기능

#### 1. `src/main.py`
- FastAPI 애플리케이션 진입점
- HTTP 서버 실행 (Port 8000)
- 라우터 등록

#### 2. `src/api/endpoints.py`
- `POST /data/ingest/{domain}`: 데이터 수집 트리거
- `GET /health`: 헬스 체크
- Query Parameters: `file_name`, `chunk_size`

#### 3. `src/services/ingestion_service.py`
- 데이터 수집 비즈니스 로직
- 도메인별 로더 호출
- gRPC 클라이언트 연동
- 통계 정보 반환

#### 4. `src/infrastructure/loaders.py`
- `load_recruit_data()`: Recruit 도메인 로더
- `load_headhunter_data()`: Headhunter 도메인 로더
- **메모리 최적화: 5.3% 절감**
- 데이터 타입 최적화 (category, int16, float32)

#### 5. `src/infrastructure/grpc_clients.py`
- gRPC Client 구현
- `IngestDataStream` RPC 호출 (Client Streaming)
- Batch Server (Port 50052) 연결
- Chunk 단위 스트리밍

#### 6. `src/domain/models.py`
- 도메인 모델 정의
- `RecruitRow`, `HeadhunterRow`
- Pydantic/Dataclass 기반

#### 7. `src/domain/utils.py`
- UUID v7 생성 로직
- PostgreSQL UUID 타입 호환
- 시간순 정렬 보장

#### 8. `src/config/settings.py`
- FastAPI 설정 (Host, Port)
- gRPC Batch Server 설정
- Data 디렉토리 및 Chunk 크기 설정

#### 9. Proto 파일
- `embedding_stream.proto`: Client Streaming RPC 정의
- Python 코드 생성 완료
- Java Batch Server와 호환성 확인

#### 10. 실행 스크립트
- `start_server.bat`: FastAPI 서버 시작 (main.py 호출)

### 테스트 결과

#### 성공 메트릭
```
API Server: http://localhost:8000
Total Rows Loaded: 141,897
Memory Optimization: 5.3% reduction
Total Chunks Streamed: 474
Chunk Size: 300 rows/chunk
Streaming Success: 100%
Java Batch Server Reception: Success
```

#### 실행 로그 샘플
```
[INFO] FastAPI starting on http://0.0.0.0:8000
[INFO] POST /data/ingest/recruit received
[INFO] Loading recruit data from processed_recruitment_data.pkl
[INFO] Loaded 141,897 rows from pkl file
[INFO] Memory optimization: 5.3% reduction
[INFO] Connecting to Batch Server at localhost:50052
[INFO] Starting gRPC Client Streaming
[INFO] Streaming 474 chunks to Batch Server
[INFO] Chunk 1/474 sent (300 rows)
...
[INFO] All chunks successfully streamed
[INFO] Batch Server confirmed receipt
[INFO] Response: Successfully sent 141897 rows in 474 chunks
```

### 기술적 하이라이트

1. **FastAPI + gRPC 하이브리드 아키텍처**
   - HTTP API로 데이터 수집 트리거 (유연성)
   - gRPC Client Streaming으로 대용량 전송 (성능)
   - 도메인별 엔드포인트 분리 (확장성)

2. **메모리 효율성**
   - Category 타입 활용으로 문자열 메모리 절감
   - float32 사용으로 vector 메모리 50% 절감
   - 점진적 chunk 전송으로 메모리 피크 방지

3. **스트리밍 안정성**
   - gRPC Client Streaming (단방향)
   - Chunk 기반 데이터 전송
   - 에러 핸들링 및 상세 로깅

4. **Python-Java 상호 운용성**
   - Protobuf 직렬화 성공
   - NumPy array → proto repeated float 변환
   - Java Batch Server gRPC Server와 통신 성공

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
        'job_post_vectors': np.random.rand(384).astype(np.float32)
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

### 🚨 먼저 읽어야 할 문서 (2025-12-17 통합 완료)
- **Python 서버 개발 가이드**: `/docs/Python_서버_개발_가이드.md` ⭐ - 전체 아키텍처, 개발 가이드
- **데이터 처리 가이드**: `/docs/데이터_처리_가이드.md` 📊 - Chunk Loader, 도메인 모델
- **gRPC 통신 가이드**: `/docs/gRPC_통신_가이드.md` 🔌 - Client Streaming, Proto 파일

> **📝 문서 통합 완료**: 기존 분산된 6개 문서를 3개 핵심 문서로 통합했습니다.
> - 구식 문서 (Server Streaming 기준) 제거
> - 최신 아키텍처 (FastAPI + gRPC Client, Chunk Loader) 반영
> - 중복 내용 제거 및 명확한 역할 분리

### 🗄️ Backend 공통 문서 (DB 스키마 참조 시 필수)
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md` ⭐
- **테이블 명세서**: `/Backend/docs/table_specification.md` ⭐
- **ERD 다이어그램**: `/Backend/docs/ERD_다이어그램.md`

> **🚨 Proto 파일 작성 시 주의:**
> 도메인 모델 작성, Proto 메시지 정의 시 반드시 `/Backend/docs/table_specification.md`를 먼저 확인하세요.
> DB 스키마와 Proto 메시지 구조가 일치해야 합니다.

### 📚 관련 프로젝트 문서
- [루트 CLAUDE.md](../CLAUDE.md)
- [Batch Server CLAUDE.md](../Backend/Batch-Server/CLAUDE.md)
- [Entire Structure](../Backend/Batch-Server/docs/Entire_Structure.md)
- [Batch 설계서](../Backend/Batch-Server/docs/Batch설계서.md)

---

## ✅ 현재 진행 상황

### 완료 (2025-12-17)
- ✅ **Chunk Loader 완전 구현** - 3가지 파일 포맷 지원
  - `BaseChunkLoader[T_Row]` - Protocol 기반 제네릭 추상 클래스
  - `PklChunkLoader` - Pickle 파일 Chunk 로딩
  - `CsvChunkLoader` - CSV 파일 Chunk 로딩 (Vector/Array 파싱)
  - `ParquetChunkLoader` - Parquet 파일 배치 로딩 (PyArrow)
  - 2-tier registry: `(domain, format)` → Loader 매핑
  - Auto-detection: 파일 확장자로 자동 포맷 감지
- ✅ **도메인 모델 확장** - 3개 도메인 지원
  - `RecruitData` - 384d vector (기존)
  - `CandidateData` - 768d vector (신규)
  - `SkillEmbeddingDicData` - 768d vector (신규)
  - Pydantic Field Validator로 벡터 차원 검증
  - skills 배열 검증 (최소 1개 이상)
- ✅ **Proto 파일 확장** - oneof 패턴
  - 3개 도메인 메시지 (RecruitRow, CandidateRow, SkillEmbeddingDicRow)
  - oneof chunk_data로 도메인 분기
  - Java Batch Server와 호환성 확인
- ✅ **Ingestion Service 업데이트**
  - Chunk Iterator 기반 처리
  - 도메인별 벡터 차원 검증
  - 메타데이터 전송 (domain, file_name, vector_dimension)

### 완료 (2025-12-12)
- ✅ **FastAPI + gRPC Client 아키텍처 구현 완료**
  - `main.py`: FastAPI 앱 진입점
  - `api/endpoints.py`: HTTP API 엔드포인트
  - `services/ingestion_service.py`: 비즈니스 로직
  - `infrastructure/grpc_clients.py`: gRPC Client (Client Streaming)
  - `infrastructure/loaders.py`: 도메인별 데이터 로더
  - `domain/models.py`: 도메인 모델
  - `domain/utils.py`: UUID v7 유틸리티
  - `config/settings.py`: 환경 설정
- ✅ **데이터 로딩 최적화**
  - 141,897 rows 성공적 로드
  - 메모리 최적화 5.3% 절감
  - 도메인별 로더 분리 (recruit, candidate)
- ✅ **gRPC Client Streaming 구현**
  - Batch Server (Port 50051)와 통신
  - 474 chunks 전송 완료
  - Java Batch Server에서 정상 수신 확인
  - IngestDataStream RPC (Client Streaming)
- ✅ **HTTP API 엔드포인트**
  - `POST /data/ingest/{domain}`: 데이터 수집 트리거
  - `GET /health`: 헬스 체크
  - Query Parameters: `file_name`, `chunk_size`
- ✅ **Proto 파일 컴파일**
  - `embedding_stream.proto`: Client Streaming RPC 정의
  - Python/Java 상호 운용성 확인
- ✅ **Protocol 기반 제네릭 구조 구현**
  - `DataLoader[T_Row]` Protocol (구조적 타입)
  - TypeVar 공변성(covariant=True) 활용
  - 팩토리 패턴 (get_loader 함수)
  - Batch-Server의 DataProcessor<T> 패턴과 매핑
- ✅ **테스트 코드 정리**
  - 제거: test_client.bat (테스트 전용)
  - 유지: 실제 프로덕션 코드 (main.py, endpoints.py 등)
  - start_server.bat 업데이트 (main.py 호출)
- ✅ **계층별 커밋 완료** (2025-12-12)
  - 7개 레이어별 커밋: 문서 → 스크립트 → Config → Domain → Infrastructure → Service → API
  - 각 레이어별 기능/역할 명확화
- ✅ **문서 및 스크립트 정리**
  - README.md 업데이트 (FastAPI 기준)
  - CLAUDE.md 업데이트
  - start_server.bat 업데이트 (main.py 호출)

### 예정
- ⏳ 단위 테스트 작성 (pytest)
- ⏳ 성능 벤치마크 및 최적화
- ⏳ 에러 핸들링 강화
- ⏳ Monitoring 메트릭 수집
- ⏳ docs/ 문서들 최신화

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

## 📋 최근 업데이트

### 2025-12-17 - Chunk Loader 완전 구현 + 도메인 확장
- ✅ **Chunk Loader 3가지 구현** - 메모리 효율적 Iterator 패턴
  - `BaseChunkLoader[T_Row]` - ABC + Generic 추상 클래스
  - `PklChunkLoader` - Pandas read_pickle + iloc slicing
  - `CsvChunkLoader` - Pandas read_csv(chunksize) + Vector 파싱
  - `ParquetChunkLoader` - PyArrow iter_batches + batch_size
- ✅ **2-Tier Registry 패턴** - (domain, format) tuple key
  - `_loader_class_registry: Dict[Tuple[str, DataFormat], Type[BaseChunkLoader]]`
  - `get_loader(domain, format)` - 명시적 로더 획득
  - `get_loader_auto(domain, file_path)` - 확장자 기반 자동 감지
- ✅ **Vector/Array 파싱** - CSV 문자열 → Python List
  - `_parse_vector()` - JSON 파싱 또는 공백 분리
  - `_parse_array()` - skills 배열 파싱
  - JSON/문자열 형태 모두 지원
- ✅ **도메인 모델 확장** - 2개 도메인 추가
  - `CandidateData` - candidate_id, position_category, experience_years, original_resume, skills[], vector(768d)
  - `SkillEmbeddingDicData` - skill(PK), position_category, vector(768d)
  - @field_validator로 차원 검증 및 필수 필드 검증
- ✅ **Proto 파일 확장** - oneof 패턴으로 3개 도메인 지원
  - `oneof chunk_data { RecruitRowChunk | CandidateRowChunk | SkillEmbeddingDicRowChunk }`
  - 도메인별 Row 메시지 정의
  - Java Batch Server와 상호 운용성 검증
- ✅ **Ingestion Service 리팩토링**
  - Chunk Iterator 기반 처리 (전체 로딩 제거)
  - 첫 번째 Chunk로 벡터 차원 검증
  - 도메인 설정 기반 검증 강화

### 2025-12-12 - FastAPI + gRPC Client 아키텍처 구현 완료
- ✅ Python gRPC Server → FastAPI + gRPC Client 전환
- ✅ HTTP API 기반 데이터 수집 트리거 (`POST /data/ingest/{domain}`)
- ✅ Client Streaming RPC (`IngestDataStream`)
- ✅ 메모리 최적화 5.3% 절감
- ✅ 141,897 rows 성공적 전송

---

**최종 수정일:** 2025-12-17
