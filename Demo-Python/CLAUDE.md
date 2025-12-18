# Demo-Python Server - Claude Instructions

**역할:** Embedding 데이터 파일을 Chunk 단위로 분할 → gRPC Streaming으로 Batch Server 전송
**기술 스택:** Python 3.11+ + gRPC + FastAPI + Pandas

---

## 📋 문서 목적

- **CLAUDE.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명

---

## 🗺️ 핵심 문서 경로

### 필수 참조
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **Python 서버 개발 가이드**: `docs/Python_서버_개발_가이드.md`
- **데이터 처리 가이드**: `docs/데이터_처리_가이드.md` (Chunk Loader, 도메인 모델)
- **gRPC 통신 가이드**: `docs/gRPC_통신_가이드.md` (Client Streaming)

### Backend 공통
- **테이블 명세서**: `/Backend/docs/table_specification.md` (Proto 메시지 참조용)
- **gRPC Proto**: `/Backend/Batch-Server/src/main/proto/embedding_service.proto`

---

## 📂 구현된 코드 위치 (AI가 읽어야 할 경로)

### 🚀 엔트리포인트

- `src/grpc_server.py` - gRPC Server 실행
- `src/main.py` - FastAPI 서버 실행 (HTTP API)

### ⚙️ Configuration

- `src/config/grpc_config.py` - gRPC Client 설정 (Batch Server 연결)
- `src/config/settings.py` - 환경 변수 및 기본 설정

### 🏗️ Domain Layer

**도메인 모델 (Pydantic):**
- `src/domain/recruit_data.py` - RecruitData (384d 벡터)
- `src/domain/candidate_data.py` - CandidateData (768d 벡터)
- `src/domain/skill_embedding_dic_data.py` - SkillEmbeddingDicData (768d 벡터)

**프로토콜 (Generic Interface):**
- `src/domain/base_data.py` - BaseData Protocol (TypeVar covariant)

### 🔧 Infrastructure Layer

**Chunk Loader (Iterator 패턴):**
- `src/infrastructure/chunk_loader/base_chunk_loader.py` - BaseChunkLoader 추상 클래스
- `src/infrastructure/chunk_loader/recruit/` - Recruit 도메인 Loader
  - `recruit_pkl_loader.py` - Pickle 파일
  - `recruit_csv_loader.py` - CSV 파일
  - `recruit_parquet_loader.py` - Parquet 파일
- `src/infrastructure/chunk_loader/candidate/` - Candidate 도메인 Loader (동일 구조)
- `src/infrastructure/chunk_loader/skill_embedding_dic/` - SkillEmbeddingDic 도메인 Loader

**Loader Factory:**
- `src/infrastructure/chunk_loader/loader_factory.py` - 도메인 + 포맷별 Loader 선택

### 📡 Service Layer

**gRPC Client:**
- `src/service/grpc_client_service.py` - Batch Server로 Client Streaming 전송

**파일 처리:**
- `src/service/file_service.py` - 파일 읽기 및 Chunk 분할 관리

### 🌐 API Layer (FastAPI)

- `src/api/health.py` - Health Check 엔드포인트
- `src/api/ingest.py` - 데이터 전송 트리거 엔드포인트 (HTTP)

### 📋 설정 파일

- `requirements.txt` - Python 의존성
- `pyproject.toml` - 프로젝트 메타데이터
- `.env` - 환경 변수 (예시: `.env.example`)

### 📁 데이터 파일

- `data/recruit/*.pkl` - Recruit 임베딩 데이터
- `data/candidate/*.pkl` - Candidate 임베딩 데이터
- `data/skill_embedding_dic/*.pkl` - SkillEmbeddingDic 데이터

---

## 🚀 현재 구현 상태

### ✅ 완료
- gRPC Server 구현 (StreamEmbedding RPC)
- gRPC Client 구현 (IngestDataStream RPC - Batch Server 전송)
- Chunk Loader (BaseChunkLoader + Iterator 패턴)
- 3가지 포맷 지원 (pkl, csv, parquet)
- 3개 도메인 구현 (Recruit, Candidate, SkillEmbeddingDic)
- Pydantic 기반 데이터 검증
- FastAPI HTTP API

### 🔄 진행 중
- 없음

### ⏳ 예정
- 실제 Embedding 모델 통합 (Sentence Transformers)
- 벡터 생성 API
- 배치 처리 최적화

---

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 코드 컨벤션 참조
**상세 컨벤션은 README.md 참조!** AI는 코드 작성 전에:
1. `README.md` 읽기 (아키텍처 패턴 이해)
2. 해당 도메인의 기존 코드 읽기 (위 경로 참조)
3. 같은 패턴으로 구현

### 2. .pkl 파일 직접 읽기 금지
**메모리 문제 방지**: 대용량 pkl 파일은 반드시 Chunk Loader를 통해 Iterator로 읽기

### 3. 도메인 추가 시
- `domain/` - Pydantic 모델 추가 (BaseData Protocol 준수)
- `infrastructure/chunk_loader/{domain}/` - 3가지 포맷 Loader 구현
- `loader_factory.py` - Factory에 등록

### 4. 벡터 차원 검증
- Recruit: 384d
- Candidate: 768d
- SkillEmbeddingDic: 768d

Pydantic validator로 차원 검증 필수

### 5. gRPC Proto 동기화
Java Batch Server의 Proto 파일과 동기화 필수:
`/Backend/Batch-Server/src/main/proto/embedding_service.proto`

---

**최종 수정일:** 2025-12-18
