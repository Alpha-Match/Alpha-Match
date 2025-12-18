# Batch Server

> **대규모 Embedding 데이터 수집 및 저장 시스템**

Spring Batch 기반 배치 서버로, Python AI Server로부터 gRPC Streaming으로 Embedding 데이터를 수신하여 PostgreSQL(pgvector)에 저장합니다.

---

## 📋 주요 기능

- 🔄 **gRPC Streaming 수신**: Python Server로부터 대용량 데이터 실시간 수신
- 💾 **도메인별 저장**: Recruit, Candidate, SkillEmbeddingDic 등 다중 도메인 지원
- ⚡ **Chunk 기반 Batch Upsert**: 기본 300개 단위로 효율적 저장
- 🚨 **DLQ (Dead Letter Queue)**: 실패 레코드 격리 및 재처리
- ✅ **Checkpoint 관리**: 중단 지점부터 재시작 지원
- 🔔 **캐시 무효화**: Batch 완료 시 API Server 캐시 무효화 (예정)

---

## 🏗️ 아키텍처

### Clean Architecture 적용

```
┌─────────────────────────────────────┐
│      Application Layer              │
│  (Use Case / Batch Components)      │
│  - ItemReader / Processor / Writer  │
│  - DlqService / CheckpointService   │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│         Domain Layer                │
│         (Port / Entity)             │
│  - Repository Interface             │
│  - Entity (JPA)                     │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│    Infrastructure Layer             │
│       (Adapter)                     │
│  - JpaRepository (Upsert Query)     │
│  - gRPC Client                      │
└─────────────────────────────────────┘
```

### 도메인별 처리 플로우

```
Python Server (gRPC Stream)
    ↓
GrpcStreamReader (ItemReader)
    ↓
DomainItemProcessor (Proto → Entity 변환)
    ↓
DomainItemWriter (Batch Upsert)
    ↓
PostgreSQL (pgvector)
    ↓
CheckpointService (진행 상태 저장)
```

---

## 🛠️ 기술 스택

### Core
- **Java 21**: Virtual Thread, Record, Pattern Matching
- **Spring Boot 4.0**: 최신 Spring 생태계
- **Spring Batch**: 대규모 배치 처리 프레임워크

### Scheduler
- **Quartz**: JDBC JobStore 기반 클러스터링 지원

### Database
- **PostgreSQL 16**: pgvector 확장
- **Flyway**: DB 마이그레이션 관리
- **JPA/Hibernate**: ORM

### Communication
- **gRPC**: Python Server와 고성능 통신
- **Protocol Buffers**: 효율적 직렬화

---

## 📂 프로젝트 구조

```
Backend/Batch-Server/
│
├── src/main/java/com/alpha/backend/
│   │
│   ├── config/                        # 설정 레이어
│   │   ├── batch/
│   │   │   ├── BatchJobConfig.java        # Job/Step 정의
│   │   │   ├── DomainJobFactory.java      # Factory 패턴
│   │   │   ├── BatchProperties.java       # 도메인별 설정
│   │   │   └── BatchSchedulerConfig.java  # Quartz 통합
│   │   ├── quartz/
│   │   │   └── QuartzConfig.java          # Quartz 설정
│   │   ├── database/
│   │   │   └── JpaConfig.java             # JPA 설정
│   │   └── grpc/
│   │       └── GrpcChannelConfig.java     # gRPC Channel
│   │
│   ├── domain/                        # 도메인 레이어 (Port)
│   │   ├── recruit/
│   │   │   ├── entity/
│   │   │   │   ├── RecruitMetadataEntity.java
│   │   │   │   └── RecruitEmbeddingEntity.java
│   │   │   └── repository/
│   │   │       ├── RecruitMetadataRepository.java
│   │   │       └── RecruitEmbeddingRepository.java
│   │   │
│   │   ├── candidate/
│   │   │   ├── entity/
│   │   │   │   ├── CandidateEntity.java
│   │   │   │   ├── CandidateSkillEntity.java
│   │   │   │   └── CandidateSkillsEmbeddingEntity.java
│   │   │   └── repository/                (4개 Repository)
│   │   │
│   │   ├── checkpoint/
│   │   │   ├── entity/CheckpointEntity.java
│   │   │   └── repository/CheckpointRepository.java
│   │   │
│   │   └── dlq/
│   │       ├── entity/DlqEntity.java
│   │       └── repository/DlqRepository.java
│   │
│   ├── infrastructure/                # 인프라 레이어 (Adapter)
│   │   ├── persistence/
│   │   │   ├── RecruitMetadataJpaRepository.java   # Upsert Native Query
│   │   │   ├── RecruitEmbeddingJpaRepository.java
│   │   │   └── Candidate*JpaRepository.java        (4개)
│   │   │
│   │   └── grpc/
│   │       └── GrpcStreamClient.java               # gRPC 통신
│   │
│   ├── application/                   # 애플리케이션 레이어 (Use Case)
│   │   ├── batch/
│   │   │   ├── reader/
│   │   │   │   └── GrpcStreamReader.java           # ItemReader
│   │   │   ├── processor/
│   │   │   │   ├── recruit/RecruitItemProcessor.java
│   │   │   │   └── candidate/CandidateItemProcessor.java
│   │   │   └── writer/
│   │   │       ├── recruit/RecruitItemWriter.java
│   │   │       └── candidate/CandidateItemWriter.java
│   │   │
│   │   └── usecase/
│   │       ├── DlqService.java                     # DLQ 처리
│   │       └── CheckpointService.java              # Checkpoint 관리
│   │
│   └── BatchServerApplication.java
│
├── src/main/proto/
│   └── embedding_service.proto        # gRPC Proto 정의
│
├── src/main/resources/
│   ├── db/migration/
│   │   └── V1__init_database_schema.sql            # Flyway 마이그레이션
│   ├── application.yml                             # 메인 설정
│   └── application-batch.yml                       # Batch 도메인별 설정
│
├── docs/                              # 개발 문서
│   ├── Spring_Batch_개발_가이드.md
│   ├── 도메인_확장_가이드.md
│   ├── 동시성_제어.md
│   └── hist/                                       # 히스토리 문서
│
├── build.gradle
├── CLAUDE.md                          # AI 개발 가이드
└── README.md                          # 이 문서
```

---

## 🚀 빠른 시작

### 사전 요구사항

- **Java** 21+
- **PostgreSQL** 16+ (pgvector 확장 설치)
- **Demo-Python Server** 실행 중

### 1. PostgreSQL 설정

```sql
-- pgvector 확장 설치
CREATE EXTENSION IF NOT EXISTS vector;

-- 데이터베이스 생성
CREATE DATABASE alpha_match;
```

### 2. 환경 변수 설정

`src/main/resources/application.yml` 확인:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/alpha_match
    username: postgres
    password: postgres

grpc:
  channel:
    python-server:
      address: static://localhost:50051
```

### 3. 빌드 및 실행

```bash
# 프로젝트 루트에서
cd Backend/Batch-Server

# Gradle 빌드
./gradlew clean build

# 실행
./gradlew bootRun
```

### 4. 스케줄 확인

기본적으로 Recruit Job은 매일 새벽 2시에 실행됩니다.
수동 실행 또는 스케줄 변경은 `application-batch.yml` 참조.

---

## 📝 코드 컨벤션

### 1. Clean Architecture 레이어 분리

**Domain Layer (Port):**
- 비즈니스 로직 및 인터페이스 정의
- Entity, Repository Interface
- 외부 의존성 없음

**Infrastructure Layer (Adapter):**
- JpaRepository 구현
- Native Query (Upsert)
- gRPC Client

**Application Layer (Use Case):**
- Spring Batch Components (Reader, Processor, Writer)
- Service (DLQ, Checkpoint)

### 2. Factory 패턴

도메인별 Job/Step 생성을 `DomainJobFactory`에 위임:

```java
@Component
public class DomainJobFactory {

    public Job createDomainJob(String domain) {
        return jobBuilderFactory.get(domain + "Job")
            .start(createStep(domain))
            .build();
    }
}
```

### 3. Upsert 패턴

Native Query로 효율적 Upsert:

```java
@Query(value = """
    INSERT INTO recruit_metadata (...)
    VALUES (...)
    ON CONFLICT (recruit_uuid) DO UPDATE SET ...
    """, nativeQuery = true)
void upsert(...);
```

### 4. Chunk 기반 처리

ItemReader → ItemProcessor → ItemWriter:

```java
return stepBuilderFactory.get("recruitStep")
    .<ProtoMessage, EntityList>chunk(chunkSize)
    .reader(reader)
    .processor(processor)
    .writer(writer)
    .build();
```

---

## 🔧 설정 가이드

### application-batch.yml

도메인별 설정:

```yaml
batch:
  domains:
    recruit:
      chunk-size: 300
      enabled: true

  scheduler:
    jobs:
      recruit:
        enabled: true
        cron: "0 0 2 * * ?"  # 매일 새벽 2시
```

### Quartz 스케줄 변경

```yaml
batch:
  scheduler:
    jobs:
      recruit:
        cron: "0 */10 * * * ?"  # 10분마다
```

---

## 📚 개발 가이드

### 새로운 도메인 추가

상세 가이드: `docs/도메인_확장_가이드.md`

**7단계 체크리스트:**
1. Proto 메시지 정의
2. Entity 생성
3. Repository 생성 (Domain + Infrastructure)
4. Processor 구현
5. Writer 구현
6. Factory 등록
7. YAML 설정 추가

### DLQ 레코드 재처리

```bash
# DLQ 조회
SELECT * FROM dlq WHERE domain = 'recruit' AND status = 'FAILED';

# 재처리 (코드 수정 후)
# DlqService.reprocessDlq() 호출
```

### Checkpoint 확인

```bash
# 진행 상황 확인
SELECT * FROM checkpoint WHERE domain = 'recruit';
```

---

## 🧪 테스트

### 단위 테스트

```bash
./gradlew test
```

### 통합 테스트

```bash
# Python Server 먼저 실행
cd Demo-Python
python src/grpc_server.py

# Batch Server 실행
cd Backend/Batch-Server
./gradlew bootRun
```

---

## 📖 관련 문서

- [Spring Batch 개발 가이드](docs/Spring_Batch_개발_가이드.md)
- [도메인 확장 가이드](docs/도메인_확장_가이드.md)
- [동시성 제어](docs/동시성_제어.md)
- [DB 스키마 가이드](/Backend/docs/DB_스키마_가이드.md)
- [테이블 명세서](/Backend/docs/table_specification.md)

---

## 🐛 트러블슈팅

### gRPC 연결 실패

```
Error: UNAVAILABLE: io exception
```

**해결:**
1. Python Server 실행 확인
2. 포트 50051 사용 중인지 확인
3. `application.yml`에서 gRPC 주소 확인

### DB Connection Pool 고갈

```
Error: HikariPool-1 - Connection is not available
```

**해결:**
- Virtual Thread 사용 시 Connection Pool 크기 증가
- `spring.datasource.hikari.maximum-pool-size` 조정

### Flyway 마이그레이션 실패

```
Error: Migration checksum mismatch
```

**해결:**
```bash
# 마이그레이션 초기화 (개발 환경만)
./gradlew flywayClean
./gradlew flywayMigrate
```

---

**최종 수정일:** 2025-12-18
