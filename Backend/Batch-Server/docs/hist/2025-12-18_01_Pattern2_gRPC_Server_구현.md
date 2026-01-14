# 2025-12-18: Pattern 2 gRPC Server 구현 및 Python 통신 정렬

## 📋 작업 개요

**목표**: Pattern 2 (Client Streaming) gRPC Server 구현 및 Python-Java 통신 정렬

**작업 기간**: 2025-12-18
**담당**: Claude Code

---

## 🎯 구현 내용

### 1. Pattern 2 gRPC Server 구현 (Batch Server)

#### 1.1 gRPC Server
- **파일**: `infrastructure/grpc/server/EmbeddingStreamServiceImpl.java`
- **어노테이션**: `@GrpcService`
- **RPC**: `IngestDataStream(stream IngestDataRequest) returns (IngestDataResponse)`
- **기능**:
  - Client Streaming 수신
  - 첫 메시지: IngestMetadata (도메인, 파일명, 벡터 차원)
  - 이후 메시지: JSON bytes 청크
  - StreamObserver 패턴으로 비동기 처리

#### 1.2 Data Processor Layer
- **인터페이스**: `DataProcessor`
  - `int processChunk(byte[] jsonChunk)`: JSON 파싱 및 DB 저장
  - `String getDomain()`: 도메인 이름 반환

- **구현체**:
  - `RecruitDataProcessor`: Recruit 도메인 처리 (2개 테이블)
  - `CandidateDataProcessor`: Candidate 도메인 처리 (3개 테이블)

- **Factory**: `DataProcessorFactory`
  - Spring 자동 빈 주입 (`List<DataProcessor>`)
  - Map 기반 도메인 라우팅

#### 1.3 DTO Layer
- `RecruitRowDto`: Recruit JSON 역직렬화
- `CandidateRowDto`: Candidate JSON 역직렬화
- Jackson `@JsonProperty` 사용

### 2. 스케줄러 설정 수정

**파일**: `application.yml`

```yaml
scheduler:
  enabled: true
  jobs:
    recruit:
      enabled: false   # Pattern 1 비활성화
    candidate:
      enabled: false   # Pattern 1 비활성화
```

**전략**:
- 두 패턴 모두 코드 구현
- Pattern 2 (Client Streaming)만 기본 활성화
- Pattern 1 (Server Streaming)은 필요 시 활성화

### 3. Python 통신 정렬

#### 3.1 JSON 인코딩 수정
**Before (이중 인코딩):**
```python
json_chunk = [item.model_dump_json() for item in chunk_data]
encoded_chunk = json.dumps(json_chunk).encode('utf-8')
# 결과: '["{\\"id\\": \\"...\\", ...}", ...]'
```

**After (올바른 방식):**
```python
json_chunk = [item.model_dump() for item in chunk_data]
encoded_chunk = json.dumps(json_chunk).encode('utf-8')
# 결과: [{"id": "...", ...}, {...}]
```

#### 3.2 Candidate 모델 수정
- `vector` → `skills_vector` (Java DTO 매칭)
- `candidate_id` alias 제거
- BaseData의 id 필드 충돌 해결 (property로 리다이렉트)

#### 3.3 SkillEmbeddingDic 모델 수정
- `vector` → `skill_vector` (Java DTO 매칭)

---

## 🏗️ 아키텍처

### Pattern 1 (Server Streaming) - 기존
```
Batch Server (Client) --[StreamEmbedding]--> Python Server (Server)
- 트리거: Quartz Scheduler
- 데이터: Proto 메시지 (RowChunk)
- 포트: Python 50053
```

### Pattern 2 (Client Streaming) - 신규
```
Python Server (Client) --[IngestDataStream]--> Batch Server (Server)
- 트리거: FastAPI 엔드포인트
- 데이터: JSON bytes
- 포트: Batch 50051
```

---

## 📊 데이터 플로우

```
FastAPI POST /data/ingest/{domain}
  ↓
ingestion_service.py (파일 로드)
  ↓
grpc_clients.py (Client Streaming)
  ├─ 1. IngestMetadata
  └─ 2. JSON chunks
       ↓
Batch Server:50051
  ↓
EmbeddingStreamServiceImpl
  ↓
DataProcessorFactory
  ↓
[RecruitDataProcessor | CandidateDataProcessor]
  ↓
Repository.upsertAll()
  ↓
PostgreSQL (pgvector)
```

---

## 🔧 기술적 결정사항

### 1. 왜 두 패턴을 모두 지원하는가?
- **Pattern 1 (Server Streaming)**: 정기 배치 작업에 적합
  - 매일 정해진 시간에 자동 실행
  - Proto 메시지로 타입 안전

- **Pattern 2 (Client Streaming)**: 실시간 처리에 적합
  - 사용자 요청 시 즉시 실행
  - JSON으로 유연성 확보
  - HTTP 엔드포인트 트리거

### 2. 왜 JSON을 사용하는가? (Pattern 2)
- Python에서 Pydantic 모델을 직접 JSON으로 변환 가능
- Proto 재컴파일 없이 필드 추가/수정 가능
- FastAPI와의 통합 용이

### 3. 왜 Factory 패턴을 사용하는가?
- 도메인 확장 시 Factory에 자동 등록 (Spring 자동 주입)
- gRPC Server는 도메인 로직 모름 (단일 책임 원칙)
- 테스트 용이

---

## ✅ 검증 완료

### 빌드 검증
```bash
✅ ./gradlew compileJava - SUCCESS
✅ ./gradlew build -x test - SUCCESS
```

### 의존성 주입 검증
```
✅ @GrpcService → Spring 자동 등록
✅ @Component → DataProcessor 구현체들 자동 주입
✅ DataProcessorFactory → List<DataProcessor> 자동 주입
✅ Repository → JPA 자동 프록시 생성
```

### 코드 품질
```
✅ Clean Architecture 준수
✅ 단일 책임 원칙
✅ 의존성 역전 원칙
✅ 에러 처리 완비
✅ 로깅 완비
```

---

## 🎓 학습 내용

### gRPC Client Streaming
- `StreamObserver<T>` 패턴
- onNext, onCompleted, onError 핸들링
- Metadata + Data 패턴

### Spring gRPC
- `@GrpcService` 어노테이션
- Spring Boot 4.0 + gRPC 통합
- 자동 빈 주입 및 라이프사이클 관리

### Jackson JSON 처리
- `@JsonProperty` 스네이크 케이스 매핑
- `TypeReference<List<T>>` 제네릭 역직렬화
- ObjectMapper 재사용

---

## 📝 남은 작업

### 단기 (다음 단계)
- [ ] SkillEmbeddingDic 도메인 Processor 구현
- [ ] Python-Java 통합 테스트
- [ ] 에러 케이스 테스트 (JSON 파싱 실패, 벡터 차원 불일치 등)

### 중기
- [ ] DLQ 재처리 로직
- [ ] 모니터링 메트릭 추가
- [ ] 성능 테스트 (대용량 데이터)

---

## 📦 커밋 내역

1. **8a26e92**: `feat(batch): Pattern 2 gRPC Server 구현 (Client Streaming)`
2. **1d2f7b9**: `fix(batch): Pattern 1 스케줄러 모두 비활성화 (기본 Pattern 2 사용)`
3. **1b86d13**: `fix(python): Python-Java gRPC 통신을 위한 데이터 모델 수정`

---

**작성일**: 2025-12-18
**상태**: ✅ 완료
