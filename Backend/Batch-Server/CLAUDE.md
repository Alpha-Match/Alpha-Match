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
- `src/main/resources/db/migration/V1__init_database_schema.sql` - Flyway V1 (초기 스키마)
- `src/main/resources/db/migration/V2__restructure_schema_to_v2.sql` - Flyway V2 (스키마 재구조화, 2025-12-21)

**gRPC:**
- `src/main/java/com/alpha/backend/config/grpc/GrpcChannelConfig.java` - gRPC Channel
- `src/main/proto/embedding_stream.proto` - Proto 정의
- `src/main/proto/cache_service.proto` - Cache 서비스 Proto

### 📦 Domain Layer (Port)

**Recruit 도메인 (v2 - 4 tables):**
- `src/main/java/com/alpha/backend/domain/recruit/entity/RecruitEntity.java` (기존 RecruitMetadataEntity 대체)
- `src/main/java/com/alpha/backend/domain/recruit/entity/RecruitDescriptionEntity.java` (신규)
- `src/main/java/com/alpha/backend/domain/recruit/entity/RecruitSkillEntity.java` (신규)
- `src/main/java/com/alpha/backend/domain/recruit/entity/RecruitSkillId.java` (신규 - 복합 PK)
- `src/main/java/com/alpha/backend/domain/recruit/entity/RecruitSkillsEmbeddingEntity.java` (기존 RecruitEmbeddingEntity 대체)
- `src/main/java/com/alpha/backend/domain/recruit/repository/RecruitRepository.java`
- `src/main/java/com/alpha/backend/domain/recruit/repository/RecruitDescriptionRepository.java`
- `src/main/java/com/alpha/backend/domain/recruit/repository/RecruitSkillRepository.java`
- `src/main/java/com/alpha/backend/domain/recruit/repository/RecruitSkillsEmbeddingRepository.java`

**Candidate 도메인 (v2 - 4 tables):**
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateEntity.java`
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateDescriptionEntity.java` (신규)
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateSkillEntity.java`
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateSkillId.java`
- `src/main/java/com/alpha/backend/domain/candidate/entity/CandidateSkillsEmbeddingEntity.java`
- `src/main/java/com/alpha/backend/domain/candidate/repository/CandidateRepository.java`
- `src/main/java/com/alpha/backend/domain/candidate/repository/CandidateDescriptionRepository.java`
- `src/main/java/com/alpha/backend/domain/candidate/repository/CandidateSkillRepository.java`
- `src/main/java/com/alpha/backend/domain/candidate/repository/CandidateSkillsEmbeddingRepository.java`

**Skill Embedding Dictionary 도메인 (v2 - 2 tables):**
- `src/main/java/com/alpha/backend/domain/skilldic/entity/SkillCategoryDicEntity.java` (신규)
- `src/main/java/com/alpha/backend/domain/skilldic/entity/SkillEmbeddingDicEntity.java`
- `src/main/java/com/alpha/backend/domain/skilldic/repository/SkillCategoryDicRepository.java`
- `src/main/java/com/alpha/backend/domain/skilldic/repository/SkillEmbeddingDicRepository.java`

**공통:**
- `src/main/java/com/alpha/backend/domain/checkpoint/entity/CheckpointEntity.java`
- `src/main/java/com/alpha/backend/domain/dlq/entity/DlqEntity.java`

### 🏗️ Infrastructure Layer (Adapter)

**Persistence (JPA) - v2:**
- `src/main/java/com/alpha/backend/infrastructure/persistence/RecruitJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/RecruitDescriptionJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/RecruitSkillJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/RecruitSkillsEmbeddingJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/CandidateJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/CandidateDescriptionJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/CandidateSkillJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/CandidateSkillsEmbeddingJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/SkillCategoryDicJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/SkillEmbeddingDicJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/CheckpointJpaRepository.java`
- `src/main/java/com/alpha/backend/infrastructure/persistence/DlqJpaRepository.java`

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
- **v2 스키마 전환 (2025-12-21)**
  - Flyway V2 마이그레이션 (벡터 차원 통일: 384d, TIMESTAMPTZ, TEXT)
  - Entity 11개 (Recruit 5개, Candidate 5개, SkillDic 2개 - 신규 7개, 수정 4개)
  - Repository 12개 (Domain 6개 + Infrastructure 6개, Native Upsert 구현)
- **Recruit 도메인 v2 완전 구현 (4-table 구조)**
  - Entity: Recruit, RecruitDescription, RecruitSkill, RecruitSkillsEmbedding
  - Repository: 4개 Domain + 4개 JPA (복합키, 벡터검색 지원)
- **Candidate 도메인 v2 완전 구현 (4-table 구조)**
  - Entity: Candidate, CandidateDescription, CandidateSkill, CandidateSkillsEmbedding
  - Repository: 4개 Domain + 4개 JPA (기존 3개 + 신규 CandidateDescription)
- **SkillEmbeddingDic 도메인 v2 완전 구현 (2-table 구조)**
  - Entity: SkillCategoryDic, SkillEmbeddingDic
  - Repository: 2개 Domain + 2개 JPA (UUID 자동생성)
- gRPC Client 구현 (Pattern 1: Server Streaming)
- **gRPC Server 구현 (Pattern 2: Client Streaming)**
  - EmbeddingStreamServiceImpl (IngestDataStream RPC)
  - DataProcessor (Recruit, Candidate, SkillDic)
  - DataProcessorFactory (도메인별 자동 라우팅)
- Checkpoint/DLQ 도메인 범용화
- **DB 초기화 및 마이그레이션 실행 (2025-12-22)**
  - PostgreSQL alpha_match DB 초기화 완료
  - Flyway V1, V2 수동 마이그레이션 실행
  - 모든 v2 테이블 생성 (Recruit 4개, Candidate 4개, SkillDic 2개, 공통 2개)
  - 벡터 인덱스 생성 완료 (ivfflat, 384d)
- **Quartz Scheduler 설정 최적화 (2025-12-22)**
  - Pattern 1 비활성화에 따라 Quartz auto-startup 비활성화
  - JDBC JobStore → RAMJobStore (간소화)
  - Spring Boot 4.0 호환성 문제 해결
- **Batch Server 기동 성공 (2025-12-22)**
  - gRPC Server 포트 9090 대기 중 (Pattern 2)
  - WebFlux 포트 8080 실행
  - HikariCP DB 연결 풀 정상 작동
  - 14개 JPA Repository 로드 완료
- **PGvector 직렬화 문제 해결 (2025-12-22)**
  - Repository 3개 수정: RecruitSkillsEmbedding, CandidateSkillsEmbedding, SkillEmbeddingDic
  - PGvector → String 변환 (.toString()) 후 CAST 적용
  - bytea → vector 변환 오류 해결
- **End-to-End 파이프라인 검증 완료 (2025-12-22)**
  - **Recruit 도메인**: 87,488 레코드 처리 (471MB)
    - 4-table 동시 upsert 성공 (recruit, recruit_skill, recruit_description, recruit_skills_embedding)
    - Vector Embedding 384d 저장 완전 검증
  - **Skill_dic 도메인**: 105 레코드 처리 (358KB)
    - 2-table 동시 upsert 성공 (skill_category_dic, skill_embedding_dic)
    - FK 관계 처리 검증 (카테고리 자동 생성 → UUID 획득)
    - UK 기반 Upsert 전략 검증 (category, skill 컬럼 기준)
- **JVM 힙 메모리 및 로깅 최적화 (2025-12-26)**
  - `gradle.properties` 추가: `-Xms2g -Xmx8g -XX:+UseG1GC`
  - 로깅 레벨 DEBUG → INFO 조정 (OOM 방지)
  - 1.3GB 로그 파일 생성 문제 해결
- **전체 도메인 성능 테스트 완료 (2025-12-26)**
  - **Recruit**: 87,488건, 12m 54.8s, 113.0 rps
  - **Candidate**: 118,741건, 30m 50.1s, 64.2 rps
  - **Skill_dic**: 105건, 1.69s, 62.2 rps
  - **총 처리량**: 206,334건, 44m 46.6s, 평균 76.8 rps
  - 리포트: `docs/hist/2025-12-26_02_Performance_Test_Report.md`
- **Virtual Thread 병렬 테이블 쓰기 구현 (2025-12-26)**
  - RecruitDataProcessor, CandidateDataProcessor 적용
  - 4-table 쓰기: recruit 순차 (FK) → skill, description, embedding 병렬
  - **Recruit 성능 개선**: 12m 54.8s → 8m 38.2s (33.1% 단축, 168.8 rps)
  - 리포트: `docs/hist/2025-12-26_04_Virtual_Thread_Parallel_Write_Report.md`

### 🔄 진행 중
- 없음

### ⏳ 예정
- Candidate 도메인 병렬 쓰기 성능 테스트
- 청크 사이즈 튜닝 (100 → 200~300 비교)
- Batch Job v2 마이그레이션 (Reader, Processor, Writer - 4-table 구조 반영)
- JMX/Micrometer 메트릭 모니터링 추가

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
- **Virtual Thread 병렬 쓰기**: FK 없는 테이블은 병렬 처리 (skill, description, embedding)
- HikariCP Pool Size (20) > Virtual Thread 동시 수 (3) 유지
- Chunk Size: gRPC 100, JDBC batch 300
- Upsert 순서: main entity → dependent tables (FK 제약)

---

**최종 수정일:** 2025-12-26 (Virtual Thread 병렬 테이블 쓰기 구현, 33% 성능 개선)
