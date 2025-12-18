# Demo-Python Server

> **Embedding 데이터 스트리밍 서버 (gRPC + FastAPI)**

Python 기반 데모 서버로, 다양한 포맷의 Embedding 데이터 파일을 Chunk 단위로 분할하여 gRPC Streaming으로 Batch Server에 전송합니다.

---

## 📋 주요 기능

- 📂 **다중 포맷 지원**: pkl, csv, parquet 파일 로딩
- 🔄 **Chunk 기반 Iterator**: 메모리 효율적 대용량 파일 처리
- 📡 **gRPC Client Streaming**: Batch Server로 데이터 전송
- 🏗️ **도메인별 제네릭 구조**: Recruit, Candidate, SkillEmbeddingDic
- ✅ **Pydantic 검증**: 벡터 차원 및 데이터 유효성 검증
- 🌐 **FastAPI HTTP API**: 상태 확인 및 트리거 엔드포인트

---

## 🏗️ 아키텍처

### 계층 구조

```
┌─────────────────────────────────┐
│       API Layer                 │
│     (FastAPI HTTP)              │
│  - Health Check                 │
│  - Ingest Trigger               │
└──────────────┬──────────────────┘
               │
┌──────────────┴──────────────────┐
│     Service Layer               │
│  - gRPC Client Service          │
│  - File Service                 │
└──────────────┬──────────────────┘
               │
┌──────────────┴──────────────────┐
│  Infrastructure Layer           │
│  - Chunk Loader (Iterator)      │
│  - Loader Factory               │
└──────────────┬──────────────────┘
               │
┌──────────────┴──────────────────┐
│     Domain Layer                │
│  - Pydantic Models              │
│  - BaseData Protocol            │
└─────────────────────────────────┘
```

### 데이터 처리 플로우

```
Data Files (.pkl/.csv/.parquet)
    ↓
LoaderFactory (도메인 + 포맷 선택)
    ↓
ChunkLoader (Iterator 패턴)
    ↓
Pydantic Validation (벡터 차원 검증)
    ↓
gRPC Client Streaming
    ↓
Batch Server (Java)
```

---

## 🛠️ 기술 스택

- **Python 3.11+**: 최신 Python
- **FastAPI**: 비동기 HTTP 서버
- **gRPC**: 고성능 RPC (Client)
- **Pydantic**: 데이터 검증
- **Pandas**: 데이터 처리
- **PyArrow**: Parquet 파일 지원

---

## 📂 프로젝트 구조

```
Demo-Python/
│
├── src/
│   ├── grpc_server.py              # gRPC Server 엔트리포인트
│   ├── main.py                     # FastAPI 엔트리포인트
│   │
│   ├── config/
│   │   ├── grpc_config.py          # gRPC Client 설정
│   │   └── settings.py             # 환경 변수
│   │
│   ├── domain/                     # Pydantic 모델
│   │   ├── base_data.py            # BaseData Protocol
│   │   ├── recruit_data.py         # RecruitData (384d)
│   │   ├── candidate_data.py       # CandidateData (768d)
│   │   └── skill_embedding_dic_data.py  # SkillEmbeddingDicData (768d)
│   │
│   ├── infrastructure/
│   │   └── chunk_loader/
│   │       ├── base_chunk_loader.py     # 추상 클래스
│   │       ├── loader_factory.py        # Factory
│   │       ├── recruit/                 # Recruit Loader (pkl/csv/parquet)
│   │       ├── candidate/               # Candidate Loader
│   │       └── skill_embedding_dic/     # SkillEmbeddingDic Loader
│   │
│   ├── service/
│   │   ├── grpc_client_service.py       # gRPC Client
│   │   └── file_service.py              # 파일 처리
│   │
│   └── api/
│       ├── health.py                    # Health Check
│       └── ingest.py                    # 데이터 전송 트리거
│
├── data/                           # 데이터 파일
│   ├── recruit/
│   ├── candidate/
│   └── skill_embedding_dic/
│
├── requirements.txt
├── pyproject.toml
├── CLAUDE.md                       # AI 개발 가이드
└── README.md                       # 이 문서
```

---

## 🚀 빠른 시작

### 사전 요구사항

- **Python** 3.11+
- **Batch Server** 실행 중 (gRPC 수신 대기)

### 1. 가상 환경 설정

```bash
cd Demo-Python

# 가상 환경 생성
python -m venv venv

# 활성화
# Windows
venv\Scripts\activate

# Linux/Mac
source venv/bin/activate
```

### 2. 의존성 설치

```bash
pip install -r requirements.txt
```

### 3. Proto 파일 컴파일

```bash
# gRPC Python 코드 생성
python -m grpc_tools.protoc \
    -I../Backend/Batch-Server/src/main/proto \
    --python_out=./src/generated \
    --grpc_python_out=./src/generated \
    ../Backend/Batch-Server/src/main/proto/embedding_service.proto
```

### 4. 서버 실행

**gRPC Server (Python → Java 수신):**
```bash
python src/grpc_server.py
```

**FastAPI Server (HTTP API):**
```bash
python src/main.py
```

---

## 📝 코드 컨벤션

### 1. Protocol 기반 제네릭

도메인 모델은 `BaseData` Protocol을 준수:

```python
from typing import Protocol, TypeVar

class BaseData(Protocol):
    def to_proto_message(self) -> Any:
        ...

T_co = TypeVar('T_co', bound=BaseData, covariant=True)
```

### 2. Chunk Loader (Iterator 패턴)

```python
class BaseChunkLoader(ABC, Generic[T_co]):
    def __iter__(self) -> Iterator[List[T_co]]:
        for chunk in self._load_chunks():
            yield chunk
```

### 3. Pydantic 검증

```python
from pydantic import BaseModel, field_validator

class RecruitData(BaseModel):
    vector: List[float]

    @field_validator('vector')
    def validate_vector_dimension(cls, v):
        if len(v) != 384:
            raise ValueError("Recruit vector must be 384-dim")
        return v
```

### 4. Loader Factory

도메인 + 포맷별 Loader 선택:

```python
def get_loader(domain: str, format: str, file_path: str) -> BaseChunkLoader:
    loader_map = {
        ("recruit", "pkl"): RecruitPklLoader,
        ("recruit", "csv"): RecruitCsvLoader,
        # ...
    }
    return loader_map[(domain, format)](file_path)
```

---

## 🔧 설정 가이드

### 환경 변수 (.env)

```.env
# gRPC Batch Server 주소
BATCH_SERVER_HOST=localhost
BATCH_SERVER_PORT=50051

# Chunk 크기
CHUNK_SIZE=1000

# 데이터 디렉토리
DATA_DIR=./data
```

---

## 📚 개발 가이드

### 새로운 도메인 추가

1. **Pydantic 모델 생성** (`domain/{domain}_data.py`)
2. **Chunk Loader 구현** (`infrastructure/chunk_loader/{domain}/`)
   - `{domain}_pkl_loader.py`
   - `{domain}_csv_loader.py`
   - `{domain}_parquet_loader.py`
3. **Factory 등록** (`loader_factory.py`)

### 벡터 차원 규칙

- **Recruit**: 384d
- **Candidate**: 768d
- **SkillEmbeddingDic**: 768d

Pydantic validator로 검증 필수!

---

## 🧪 테스트

### gRPC 통신 테스트

```bash
# Batch Server 먼저 실행
cd Backend/Batch-Server
./gradlew bootRun

# Python Server 실행
cd Demo-Python
python src/grpc_server.py
```

### HTTP API 테스트

```bash
# Health Check
curl http://localhost:8000/api/health

# 데이터 전송 트리거
curl -X POST http://localhost:8000/api/ingest \
  -H "Content-Type: application/json" \
  -d '{"domain": "recruit", "format": "pkl"}'
```

---

## 📖 관련 문서

- [Python 서버 개발 가이드](docs/Python_서버_개발_가이드.md)
- [데이터 처리 가이드](docs/데이터_처리_가이드.md)
- [gRPC 통신 가이드](docs/gRPC_통신_가이드.md)
- [gRPC Proto 정의](/Backend/Batch-Server/src/main/proto/embedding_service.proto)

---

## 🐛 트러블슈팅

### gRPC 연결 실패

```
Error: grpc._channel._InactiveRpcError
```

**해결:**
1. Batch Server 실행 확인
2. 포트 50051 확인
3. `grpc_config.py`에서 주소 확인

### pkl 파일 로딩 실패

```
Error: MemoryError
```

**해결:**
- Chunk 크기 감소 (기본 1000 → 500)
- 절대 pkl 파일을 한번에 로드하지 말 것!
- 반드시 ChunkLoader 사용

### 벡터 차원 불일치

```
ValidationError: Recruit vector must be 384-dim
```

**해결:**
- 데이터 파일의 벡터 차원 확인
- Pydantic 모델의 차원 검증 규칙 확인

---

**최종 수정일:** 2025-12-18
