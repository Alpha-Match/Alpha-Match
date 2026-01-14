# Python 서버 개발 가이드

**작성일**: 2025-12-17
**프로젝트**: Alpha-Match Demo-Python Server
**기술 스택**: Python 3.11+ + FastAPI + gRPC Client + Pandas + NumPy

---

## 📋 목차

1. [아키텍처 개요](#1-아키텍처-개요)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [핵심 컴포넌트](#3-핵심-컴포넌트)
4. [개발 가이드](#4-개발-가이드)
5. [구현 상태](#5-구현-상태)

---

## 1. 아키텍처 개요

### 1.1 전체 구조

Demo-Python 서버는 **FastAPI + gRPC Client** 아키텍처를 사용합니다:

```
┌─────────────┐
│   User      │
└──────┬──────┘
       │ HTTP POST /data/ingest/{domain}
       ▼
┌─────────────────────────────────┐
│  FastAPI Server (Port 8000)     │
│  - 데이터 수집 트리거            │
│  - Health Check                 │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  Ingestion Service              │
│  - 비즈니스 로직                 │
│  - 도메인별 로더 선택            │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  Chunk Loader (Infrastructure)  │
│  - PklChunkLoader               │
│  - CsvChunkLoader               │
│  - ParquetChunkLoader           │
└──────┬──────────────────────────┘
       │ Iterator[Chunk]
       ▼
┌─────────────────────────────────┐
│  gRPC Client                    │
│  - IngestDataStream RPC         │
│  - Client Streaming             │
└──────┬──────────────────────────┘
       │ gRPC Stream
       ▼
┌─────────────────────────────────┐
│  Batch Server (Port 50052)      │
│  - Data Reception               │
│  - Database Upsert              │
└─────────────────────────────────┘
```

### 1.2 주요 변경 사항 (2025-12-12)

**변경 전 (2025-12-11):**
- Python gRPC Server (Port 50051)
- Server Streaming RPC
- Batch Server가 Client로 연결

**변경 후 (2025-12-12~17):**
- **FastAPI Server (Port 8000)** - HTTP API
- **gRPC Client** - Batch Server로 전송
- **Client Streaming RPC** - IngestDataStream
- **Chunk Loader** - Iterator 패턴 (메모리 효율)

---

## 2. 프로젝트 구조

### 2.1 디렉토리 구조

```
Demo-Python/
├── src/
│   ├── main.py                          # FastAPI 앱 진입점
│   ├── api/
│   │   └── endpoints.py                 # HTTP 엔드포인트
│   ├── services/
│   │   └── ingestion_service.py         # 비즈니스 로직
│   ├── infrastructure/
│   │   ├── loaders.py                   # Chunk Loader 구현
│   │   └── grpc_clients.py              # gRPC Client
│   ├── domain/
│   │   ├── models.py                    # 도메인 모델
│   │   └── utils.py                     # UUID 유틸리티
│   ├── config/
│   │   └── settings.py                  # 환경 설정
│   └── proto/                           # Protobuf 파일
│
├── data/                                # 데이터 파일
│   ├── recruit_embeddings.pkl
│   ├── candidate_embeddings.pkl
│   └── skill_embedding_dic.csv
│
├── docs/                                # 문서
│   ├── Python_서버_개발_가이드.md (현재 문서)
│   ├── 데이터_처리_가이드.md
│   └── gRPC_통신_가이드.md
│
├── requirements.txt
├── start_server.bat
├── README.md
└── CLAUDE.md
```

### 2.2 계층별 역할

| 계층 | 역할 | 주요 파일 |
|-----|------|---------|
| **API** | HTTP 엔드포인트 정의 | `api/endpoints.py` |
| **Service** | 비즈니스 로직 처리 | `services/ingestion_service.py` |
| **Domain** | 데이터 모델 및 유틸리티 | `domain/models.py`, `domain/utils.py` |
| **Infrastructure** | 외부 시스템 연동 | `infrastructure/loaders.py`, `infrastructure/grpc_clients.py` |
| **Config** | 환경 설정 | `config/settings.py` |

---

## 3. 핵심 컴포넌트

### 3.1 FastAPI 엔드포인트 (`api/endpoints.py`)

#### HTTP API

**1. 데이터 수집 트리거**
```python
POST /data/ingest/{domain}

Query Parameters:
- file_name: str (optional) - 파일명
- chunk_size: int (optional, default=1000) - Chunk 크기

Response:
{
  "status": "success",
  "domain": "recruit",
  "rows_sent": 141897,
  "chunks_sent": 142
}
```

**2. Health Check**
```python
GET /health

Response:
{
  "status": "healthy"
}
```

#### 사용 예시

```bash
# Recruit 도메인 수집
curl -X POST "http://localhost:8000/data/ingest/recruit?file_name=recruit_embeddings.pkl&chunk_size=1000"

# Candidate 도메인 수집
curl -X POST "http://localhost:8000/data/ingest/candidate?file_name=candidate_embeddings.csv"

# Health Check
curl "http://localhost:8000/health"
```

### 3.2 Ingestion Service (`services/ingestion_service.py`)

#### 책임

1. 도메인별 로더 선택 (2-tier registry)
2. Chunk Iterator 기반 데이터 처리
3. gRPC Client 연동
4. 벡터 차원 검증

#### 핵심 로직

```python
def ingest_data_from_file(domain: str, file_name: str, chunk_size: int):
    """데이터 수집 메인 로직"""

    # 1. Chunk Loader 획득 (2-tier registry)
    loader = get_loader_auto(domain, file_path)

    # 2. Chunk Iterator 생성
    chunk_iter = loader.load_chunks(file_path, chunk_size)

    # 3. 첫 Chunk로 벡터 차원 검증
    first_chunk = next(chunk_iter)
    validate_vector_dimension(first_chunk, domain)

    # 4. gRPC Client Streaming
    grpc_client.stream_data_to_batch_server(
        domain=domain,
        chunks=chain([first_chunk], chunk_iter),
        metadata=metadata
    )
```

### 3.3 Chunk Loader (`infrastructure/loaders.py`)

#### 지원 포맷

| 포맷 | Loader | 특징 |
|-----|--------|------|
| **pkl** | PklChunkLoader | Pandas read_pickle + iloc slicing |
| **csv** | CsvChunkLoader | Pandas read_csv(chunksize) + Vector 파싱 |
| **parquet** | ParquetChunkLoader | PyArrow iter_batches |

#### 2-Tier Registry 패턴

```python
# (domain, format) → Loader Class 매핑
_loader_class_registry: Dict[Tuple[str, DataFormat], Type[BaseChunkLoader]] = {
    ("recruit", DataFormat.PKL): PklChunkLoader,
    ("recruit", DataFormat.CSV): CsvChunkLoader,
    ("candidate", DataFormat.PKL): PklChunkLoader,
    ("candidate", DataFormat.CSV): CsvChunkLoader,
    ...
}

# 명시적 로더 획득
loader = get_loader("recruit", DataFormat.PKL)

# 자동 감지 (파일 확장자 기반)
loader = get_loader_auto("recruit", "recruit_embeddings.pkl")
```

**상세 내용**: `/docs/데이터_처리_가이드.md` 참조

### 3.4 gRPC Client (`infrastructure/grpc_clients.py`)

#### IngestDataStream RPC (Client Streaming)

```python
async def stream_data_to_batch_server(
    domain: str,
    chunks: Iterator[List[DomainData]],
    metadata: IngestMetadata
):
    """Batch Server로 Client Streaming 전송"""

    # 1. 메타데이터 전송
    yield IngestDataRequest(metadata=metadata)

    # 2. 데이터 청크 스트리밍
    for chunk in chunks:
        chunk_message = create_chunk_message(domain, chunk)
        yield IngestDataRequest(chunk=chunk_message)

    # 3. 응답 수신
    response = await call.read()
    return response
```

**상세 내용**: `/docs/gRPC_통신_가이드.md` 참조

### 3.5 도메인 모델 (`domain/models.py`)

#### 지원 도메인 (2025-12-17)

```python
@dataclass
class RecruitData:
    """Recruit 도메인 (384차원)"""
    id: str
    company_name: str
    exp_years: int
    english_level: str
    primary_keyword: str
    vector: List[float]  # 384d

    @field_validator('vector')
    def validate_vector_dimension(cls, v):
        assert len(v) == 384, f"Expected 384d, got {len(v)}d"
        return v

@dataclass
class CandidateData:
    """Candidate 도메인 (768차원)"""
    candidate_id: str
    position_category: str
    experience_years: int
    original_resume: str
    skills: List[str]
    vector: List[float]  # 768d

    @field_validator('vector')
    def validate_vector_dimension(cls, v):
        assert len(v) == 768, f"Expected 768d, got {len(v)}d"
        return v

@dataclass
class SkillEmbeddingDicData:
    """SkillEmbeddingDic 도메인 (768차원)"""
    skill: str  # PK
    position_category: str
    vector: List[float]  # 768d

    @field_validator('vector')
    def validate_vector_dimension(cls, v):
        assert len(v) == 768, f"Expected 768d, got {len(v)}d"
        return v
```

**상세 내용**: `/docs/데이터_처리_가이드.md` 참조

---

## 4. 개발 가이드

### 4.1 환경 설정

#### 의존성 설치

```bash
cd Demo-Python
pip install -r requirements.txt
```

#### 주요 의존성

```txt
fastapi==0.109.0
uvicorn[standard]==0.25.0
grpcio==1.60.0
grpcio-tools==1.60.0
pandas==2.1.4
numpy==1.26.2
pyarrow==14.0.2
pydantic==2.5.3
```

### 4.2 서버 실행

#### 개발 모드

```bash
# Windows
start_server.bat

# Linux/Mac
uvicorn src.main:app --reload --host 0.0.0.0 --port 8000
```

#### 프로덕션 모드

```bash
uvicorn src.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 4.3 Proto 파일 컴파일

#### Batch Server proto 복사

```bash
cp ../Backend/Batch-Server/src/main/proto/embedding_stream.proto src/proto/
```

#### Python 코드 생성

```bash
python -m grpc_tools.protoc \
    -I src/proto \
    --python_out=src/proto \
    --pyi_out=src/proto \
    --grpc_python_out=src/proto \
    src/proto/embedding_stream.proto
```

### 4.4 새 도메인 추가

#### Step 1: 도메인 모델 정의 (`domain/models.py`)

```python
@dataclass
class NewDomainData:
    """새 도메인"""
    id: str
    field1: str
    field2: int
    vector: List[float]  # 차원 명시

    @field_validator('vector')
    def validate_vector_dimension(cls, v):
        assert len(v) == 512, f"Expected 512d, got {len(v)}d"
        return v
```

#### Step 2: Chunk Loader 등록 (`infrastructure/loaders.py`)

```python
# Registry에 추가
_loader_class_registry = {
    ...
    ("new_domain", DataFormat.PKL): PklChunkLoader,
    ("new_domain", DataFormat.CSV): CsvChunkLoader,
}

# 도메인 설정 추가
_domain_config = {
    ...
    "new_domain": DomainConfig(
        expected_vector_dim=512,
        model_class=NewDomainData
    )
}
```

#### Step 3: Proto 파일 업데이트

```protobuf
message NewDomainRow {
  string id = 1;
  string field1 = 2;
  int32 field2 = 3;
  repeated float vector = 4;
}

message IngestDataRequest {
  oneof chunk_data {
    RecruitRowChunk recruit_chunk = 3;
    CandidateRowChunk candidate_chunk = 4;
    NewDomainRowChunk new_domain_chunk = 5;  // 추가
  }
}
```

#### Step 4: Proto 재컴파일

```bash
python -m grpc_tools.protoc ...
```

### 4.5 테스트

#### 단위 테스트 (TODO)

```bash
pytest tests/
```

#### 통합 테스트

```bash
# 1. Batch Server 실행 (Port 50052)
cd Backend/Batch-Server
./gradlew bootRun

# 2. Python Server 실행 (Port 8000)
cd Demo-Python
python src/main.py

# 3. API 호출
curl -X POST "http://localhost:8000/data/ingest/recruit?file_name=recruit_embeddings.pkl"
```

---

## 5. 구현 상태

### 5.1 완료 (2025-12-17)

#### FastAPI + gRPC Client 아키텍처 (2025-12-12)
- ✅ `main.py`: FastAPI 앱 진입점
- ✅ `api/endpoints.py`: HTTP 엔드포인트
- ✅ `services/ingestion_service.py`: 비즈니스 로직
- ✅ `infrastructure/grpc_clients.py`: gRPC Client
- ✅ `domain/models.py`: 도메인 모델
- ✅ `domain/utils.py`: UUID v7 유틸리티
- ✅ `config/settings.py`: 환경 설정

#### Chunk Loader (2025-12-17)
- ✅ `BaseChunkLoader[T_Row]`: Protocol 기반 제네릭 추상 클래스
- ✅ `PklChunkLoader`: Pickle 파일 Chunk 로딩
- ✅ `CsvChunkLoader`: CSV 파일 Chunk 로딩 + Vector 파싱
- ✅ `ParquetChunkLoader`: Parquet 파일 배치 로딩
- ✅ 2-tier registry: `(domain, format)` → Loader 매핑
- ✅ Auto-detection: 파일 확장자 기반 포맷 감지

#### 도메인 모델 (2025-12-17)
- ✅ `RecruitData`: 384d vector
- ✅ `CandidateData`: 768d vector
- ✅ `SkillEmbeddingDicData`: 768d vector
- ✅ Pydantic Field Validator: 벡터 차원 검증

#### Proto 파일 (2025-12-17)
- ✅ oneof 패턴: 3개 도메인 지원
- ✅ `IngestDataStream` RPC (Client Streaming)
- ✅ Java Batch Server 호환성 검증

### 5.2 테스트 결과

#### 성공 메트릭 (2025-12-12)
```
API Server: http://localhost:8000
Total Rows Loaded: 141,897
Memory Optimization: 5.3% reduction
Total Chunks Streamed: 474
Chunk Size: 300 rows/chunk
Streaming Success: 100%
Java Batch Server Reception: Success
```

### 5.3 예정 작업

#### 우선순위 높음
- ⏳ 단위 테스트 작성 (`tests/`)
- ⏳ 통합 테스트 작성 (FastAPI + Batch Server)
- ⏳ 에러 핸들링 강화 (재시도 로직)

#### 우선순위 중간
- ⏳ Health Check 엔드포인트 상세화
- ⏳ 성능 프로파일링 및 최적화
- ⏳ Monitoring 메트릭 수집

#### 우선순위 낮음 (선택적)
- ⏳ TLS/SSL 지원
- ⏳ Docker 컨테이너화
- ⏳ CI/CD 파이프라인

---

## 6. 참조 문서

### Demo-Python 상세 문서
- **데이터 처리 가이드**: `/docs/데이터_처리_가이드.md` - Chunk Loader, 도메인 모델
- **gRPC 통신 가이드**: `/docs/gRPC_통신_가이드.md` - Client Streaming, Proto 파일

### Backend 공통 문서 (DB 스키마 참조 필수)
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md` ⭐
- **테이블 명세서**: `/Backend/docs/table_specification.md` ⭐
- **ERD 다이어그램**: `/Backend/docs/ERD_다이어그램.md`

### 프로젝트 전체 문서
- **루트 CLAUDE.md**: `/CLAUDE.md` - 프로젝트 개요
- **Batch Server CLAUDE.md**: `/Backend/Batch-Server/CLAUDE.md` - Batch 서버 가이드

---

**최종 수정일**: 2025-12-17
**구현 상태**: FastAPI + gRPC Client 아키텍처 완료 / Chunk Loader 3가지 구현 완료
