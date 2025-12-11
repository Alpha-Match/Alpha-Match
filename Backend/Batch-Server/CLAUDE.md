# Batch Server - Claude Instructions

**프로젝트명:** Alpha-Match Batch Server
**작성일자:** 2025-12-10
**기술 스택:** Spring Boot 4.0 + Java 21 + Spring Batch + WebFlux + gRPC + PostgreSQL(pgvector)

---

## 📋 프로젝트 개요

Python AI Server로부터 gRPC Streaming으로 Recruit Embedding 및 Metadata를 수신하여 PostgreSQL(pgvector)에 저장하는 배치 서버입니다.

### 핵심 기능
- 🔄 gRPC Streaming 수신 (Python AI Server)
- 💾 이중 테이블 저장 (metadata + vector)
- ⚡ Chunk 단위 Batch Upsert
- 🚨 DLQ 처리 (실패 레코드)
- ✅ Checkpoint 관리 (재시작 지원)
- 🔔 캐시 무효화 (API Server 호출)

### 주요 학습 목표
- Reactive(WebFlux) + Blocking(JPA) 혼합 구조
- Virtual Thread 활용
- Race Condition 대응
- pgvector 활용한 Vector DB 구현

---

## 🗺️ 핵심 문서 참조

### 🚨 먼저 읽어야 할 문서
- **Batch 설계서**: `/docs/Batch설계서.md` 📘
- **프로젝트 구조**: `/docs/프로젝트_구조.md` 📂
- **DB 스키마**: `/docs/DB_스키마.md` 🗄️
- **Entire Structure**: `/docs/Entire_Structure.md` 🏗️

### 🔧 기술 상세 문서
- **gRPC 통신 가이드**: `/docs/gRPC_통신_가이드.md` 🔌
- **Reactive + Blocking 혼합전략**: `/docs/Reactive_Blocking_혼합전략.md` ⚡
- **동시성 제어**: `/docs/동시성_제어.md` 🔐

### 📚 히스토리 문서
- **hist/**: 작업 과정, 의사결정, 변경 이력 (날짜별)

---

## 🚀 현재 진행 상황

### ✅ 완료
- gRPC proto 파일 (embedding_stream.proto, cache_service.proto)
- DB 스키마 (Flyway migration)
- application.yml 설정
- build.gradle 의존성 (pgvector 포함)
- Domain Entities (MetadataEntity, EmbeddingEntity, DlqEntity, CheckpointEntity)
- Repositories (JPA + Native Query for Upsert)
- Config 클래스 (BatchProperties, ExecutorConfig, GrpcClientConfig)
- gRPC Clients (EmbeddingGrpcClient, CacheInvalidateGrpcClient)
- **gRPC 통신 테스트 완료** (2025-12-11)
  - GrpcStreamTestService: 스트리밍 테스트 서비스
  - GrpcTestRunner: 자동 테스트 러너
  - Python Server와 통신 성공 (141,897 rows 수신)
  - Checkpoint 재개 기능 검증

### 🔄 진행 중
- Application Services (StreamingService, ChunkProcessor, CacheSyncService)
  - DB 저장 로직 구현 예정
  - Batch Job/Step 통합 예정

### ⏳ 예정
- Batch Configuration (Job, Step, Listener)
- BatchScheduler
- DLQ 처리 로직

**상세 일정**: `/../../docs/개발_우선순위.md` 참조

---

## 📂 간단 구조

```
src/main/java/com/alpha/backend/
├── config/         # 설정 (BatchProperties, ExecutorConfig, GrpcClientConfig)
├── grpc/           # gRPC 클라이언트 (Embedding, CacheInvalidate)
├── domain/         # Entity + Repository (metadata, embedding, dlq)
├── infrastructure/ # CheckpointEntity, CheckpointRepository
├── application/    # Service (GrpcStreamTestService, ChunkProcessor 등)
├── runner/         # GrpcTestRunner (테스트 자동 실행)
├── batch/          # Spring Batch (Job, Step, Listener)
└── scheduler/      # BatchScheduler
```

**상세 구조**: `/docs/프로젝트_구조.md` 참조

---

## 🔧 빠른 시작

### 1. 서버 실행
```bash
./gradlew bootRun
```

### 2. 주요 설정 (application.yml)
```yaml
batch:
  embedding:
    chunk-size: 300               # Chunk 크기
    vector-dimension: 1536        # Vector 차원
    max-retry: 3                  # 재시도 횟수

grpc:
  client:
    python-embedding:
      address: static://localhost:50051
    api-cache:
      address: static://localhost:50052
  test:
    enabled: true                 # gRPC 테스트 활성화 (개발 환경)
```

### 3. gRPC 통신 테스트
```bash
# Python Server 먼저 실행 (Demo-Python)
cd Demo-Python
python src/grpc_server.py

# Batch Server 실행 (테스트 자동 실행)
cd Backend/Batch-Server
./gradlew bootRun
```

**테스트 결과 예시:**
- 연결 성공 확인
- 141,897 rows 데이터 수신
- Checkpoint 기능 검증
- Vector 차원 검증 (1536)

---

## 📚 CRITICAL DOCUMENTATION PATTERN

**🚨 중요한 문서 작성 시 반드시 여기에 추가하세요!**

- 아키텍처 변경 → `/docs/` 에 문서 추가 후 여기에 참조 추가
- 문제 해결 방법 → `/docs/` 에 트러블슈팅 문서 추가
- 성능 최적화 → `/docs/` 에 최적화 결과 문서 추가

### 예시
- Spring Batch 구성 완료 → `/docs/Spring_Batch_구성.md`
- 성능 테스트 결과 → `/docs/성능_테스트_결과.md`

---

## ⚠️ 주의사항

### 1. Reactive + Blocking 혼합
```java
// ✅ Good: publishOn으로 Scheduler 전환
flux.publishOn(jpaScheduler)
    .flatMap(chunk -> saveToDb(chunk))
```
**상세**: `/docs/Reactive_Blocking_혼합전략.md`

### 2. Upsert 순서
```java
// ✅ Good: metadata → embedding 순서 (FK 제약)
metadataRepository.upsertAll(metadataList);
embeddingRepository.upsertAll(embeddingList);
```

### 3. 캐시 무효화 중복 방지
```java
// ✅ Good: AtomicBoolean 사용
if (invalidating.compareAndSet(false, true)) {
    // 캐시 무효화
}
```
**상세**: `/docs/동시성_제어.md`

---

## 🔗 관련 프로젝트

- **Demo-Python**: `/../../Demo-Python/CLAUDE.md`
- **API Server**: `/../../Backend/Api-Server/CLAUDE.md`
- **루트 프로젝트**: `/../../CLAUDE.md`

---

## 📝 작업 문서 작성 지침

**적용 범위:** docs/hist/ 디렉토리 내 히스토리 문서 작성 시

### 기본 원칙
- 파일명: `hist/YYYY-MM-DD_nn_주제.md`
- 본문 구조: 상황 요약 → 문제 분석 → 구현 내용 → 결과/검증

### 간결화 원칙
- 코드 예시 최소화 (함수 시그니처 + 핵심 파라미터만)
- 테스트 섹션 통합
- 응답 JSON 생략 (핵심 필드만)

**상세**: 루트 CLAUDE.md의 hist 작성 지침 참조

---
---

## 📋 다음 작업 단계

### 1. DB 저장 로직 구현 (우선순위: 높음)
- ChunkProcessor 구현
  - Metadata/Embedding 분리 로직
  - Batch Upsert 처리
  - DLQ 처리
- StreamingService 구현
  - gRPC Stream → DB 저장 파이프라인
  - Checkpoint 관리
- CacheSyncService 구현
  - API Server 캐시 무효화

### 2. Spring Batch Job/Step 구성
- EmbeddingProcessingJob
- receiveEmbeddingStep
- storeEmbeddingStep
- Listener 구현

### 3. Scheduler 구현
- Quartz 기반 배치 스케줄러
- Cron 설정

---

**최종 수정일:** 2025-12-11