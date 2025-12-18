# Batch Server - Claude Instructions

**역할:** Python AI Server로부터 gRPC Streaming으로 Embedding 데이터 수신 → PostgreSQL 저장
**기술 스택:** Spring Boot 4.0 + Spring Batch + gRPC + pgvector

---

## 📋 문서 목적

- **CLAUDE.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명

---

## 🗺️ 핵심 문서 경로

### 필수 참조
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **Spring Batch 개발 가이드**: `docs/Spring_Batch_개발_가이드.md`
- **도메인 확장 가이드**: `docs/도메인_확장_가이드.md`
- **동시성 제어**: `docs/동시성_제어.md`

### Backend 공통 (DB 작업 시)
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md`
- **테이블 명세서**: `/Backend/docs/table_specification.md` ⭐ Single Source of Truth
- **Flyway 가이드**: `/Backend/docs/Flyway_마이그레이션_가이드.md`

---

## 📂 구현된 코드 위치 (AI가 읽어야 할 경로)

### 🔧 Configuration

**Batch 설정:**
- `src/main/java/com/alpha/backend/config/batch/BatchJobConfig.java` - Job/Step 정의
- `src/main/java/com/alpha/backend/config/batch/DomainJobFactory.java` - Factory 패턴
- `src/main/java/com/alpha/backend/config/batch/BatchProperties.java` - 도메인별 설정

**Scheduler:**
- `src/main/java/com/alpha/backend/config/quartz/QuartzConfig.java` - Quartz 설정
- `src/main/java/com/alpha/backend/config/batch/BatchSchedulerConfig.java` - Job 스케줄링

**Database:**
- `src/main/java/com/alpha/backend/config/database/JpaConfig.java` - JPA 설정
- `src/main/resources/db/migration/V1__init_database_schema.sql` - Flyway 마이그레이션

**gRPC:**
- `src/main/java/com/alpha/backend/config/grpc/GrpcChannelConfig.java` - gRPC Channel
- `src/main/proto/embedding_stream.proto` - Proto 정의
- `src/main/proto/cache_service.proto` - Cache 서비스 Proto

### 📦 Domain Layer (Port)

**Recruit 도메인:**
- `src/main/java/com/alpha/backend/domain/recruit/entity/RecruitMetadataEntity.java`
- `src/main/java/com/alpha/backend/domain/recruit/entity/RecruitEmbeddingEntity.java`
- `src/main/java/com/alpha/backend/domain/recruit/repository/RecruitMetadataRepository.java`
- `src/main/java/com/alpha/backend/domain/recruit/repository/RecruitEmbeddingRepository.java`

**Candidate 도메인:**
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateEntity.java`
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateSkillEntity.java`
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateSkillsEmbeddingEntity.java`
- `src/main/java/com/alpha/backend/domain/candidate/repository/` (4개 Repository)

**공통:**
- `src/main/java/com/alpha/backend/domain/checkpoint/entity/CheckpointEntity.java`
- `src/main/java/com/alpha/backend/domain/dlq/entity/DlqEntity.java`

### 🏗️ Infrastructure Layer (Adapter)

**Persistence (JPA):**
- `src/main/java/com/alpha/backend/infrastructure/persistence/RecruitMetadataJpaRepository.java` - Upsert Native Query
- `src/main/java/com/alpha/backend/infrastructure/persistence/RecruitEmbeddingJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/Candidate*JpaRepository.java` (4개)

**gRPC Client (Pattern 1: Server Streaming):**
- `src/main/java/com/alpha/backend/infrastructure/grpc/client/EmbeddingGrpcClient.java` - Python Server로 요청
- `src/main/java/com/alpha/backend/infrastructure/grpc/client/CacheInvalidateGrpcClient.java` - API Server 캐시 무효화

**gRPC Server (Pattern 2: Client Streaming):**
- `src/main/java/com/alpha/backend/infrastructure/grpc/server/EmbeddingStreamServiceImpl.java` - IngestDataStream 수신

**gRPC Processor:**
- `src/main/java/com/alpha/backend/application/grpc/processor/DataProcessor.java` - 인터페이스
- `src/main/java/com/alpha/backend/application/grpc/processor/RecruitDataProcessor.java` - Recruit JSON 처리
- `src/main/java/com/alpha/backend/application/grpc/processor/CandidateDataProcessor.java` - Candidate JSON 처리
- `src/main/java/com/alpha/backend/application/grpc/processor/DataProcessorFactory.java` - Factory 패턴

**gRPC DTO:**
- `src/main/java/com/alpha/backend/application/grpc/dto/RecruitRowDto.java`
- `src/main/java/com/alpha/backend/application/grpc/dto/CandidateRowDto.java`

### 🔄 Application Layer (Use Case)

**Batch Components:**
- `src/main/java/com/alpha/backend/application/batch/reader/GrpcStreamReader.java` - ItemReader
- `src/main/java/com/alpha/backend/application/batch/processor/recruit/RecruitItemProcessor.java` - Recruit Processor
- `src/main/java/com/alpha/backend/application/batch/processor/candidate/CandidateItemProcessor.java` - Candidate Processor
- `src/main/java/com/alpha/backend/application/batch/writer/recruit/RecruitItemWriter.java` - Recruit Writer
- `src/main/java/com/alpha/backend/application/batch/writer/candidate/CandidateItemWriter.java` - Candidate Writer

**Services:**
- `src/main/java/com/alpha/backend/application/usecase/DlqService.java` - DLQ 처리
- `src/main/java/com/alpha/backend/application/usecase/CheckpointService.java` - Checkpoint 관리

### 📋 설정 파일

- `src/main/resources/application.yml` - 메인 설정
- `src/main/resources/application-batch.yml` - Batch 도메인별 설정
- `build.gradle` - 의존성 및 protobuf 플러그인

---

## 🚀 현재 구현 상태

### ✅ 완료
- Factory 패턴 기반 도메인별 Job/Step 생성
- Quartz Scheduler 통합 (Cron 기반)
- Recruit 도메인 완전 구현 (Entity, Repository, Processor, Writer)
- Candidate 도메인 완전 구현 (3개 테이블 분산 저장)
- gRPC Client 구현 (Pattern 1: Server Streaming)
- **gRPC Server 구현 (Pattern 2: Client Streaming)**
  - EmbeddingStreamServiceImpl (IngestDataStream RPC)
  - DataProcessor (Recruit, Candidate)
  - DataProcessorFactory (도메인별 자동 라우팅)
- Checkpoint/DLQ 도메인 범용화
- Flyway V1 마이그레이션

### 🔄 진행 중
- 없음

### ⏳ 예정
- SkillEmbeddingDic 도메인 구현 (Entity, Repository, Processor)
- Pattern 1/2 통합 테스트
- 성능 최적화 및 모니터링

---

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 코드 컨벤션 참조
**상세 컨벤션은 README.md 참조!** AI는 코드 작성 전에:
1. `README.md` 읽기 (아키텍처 패턴 이해)
2. 해당 도메인의 기존 코드 읽기 (위 경로 참조)
3. 같은 패턴으로 구현

### 2. DB 작업 전 필수 확인
- Entity 작성 전: `/Backend/docs/table_specification.md` 확인
- 임의로 스키마 추정 금지

### 3. 도메인 추가 시
`docs/도메인_확장_가이드.md` 필수 참조 (7단계 체크리스트)

### 4. Batch 작업 시 주의
- Virtual Thread 사용: DB Connection Pool 고갈 방지
- Chunk Size: 기본 300 (application-batch.yml에서 조정)
- Upsert 순서: metadata → embedding (FK 제약)

---

**최종 수정일:** 2025-12-18
