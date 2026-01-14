# Batch-Server 구현 상세 문서

**프로젝트**: Alpha-Match (Headhunter-Recruit Matching System)
**작성일**: 2026-01-12
**버전**: 1.0

---

## 1. 시스템 개요

### 1.1 기술 스택

| 구분 | 기술 | 버전 | 용도 |
|------|------|------|------|
| Framework | Spring Boot | 4.0.1 | 애플리케이션 프레임워크 |
| Batch | Spring Batch | 6.0 | 대용량 배치 처리 |
| Scheduler | Quartz | 2.3.2 | 스케줄링 |
| Database | JPA + Hibernate | 6.x | ORM |
| Vector DB | pgvector | 0.5.1 | 벡터 저장 |
| gRPC | grpc-java | 1.60.0 | Python 서버 통신 |
| Build | Gradle | 8.x | 빌드 도구 |
| Java | OpenJDK | 21 | 런타임 |

### 1.2 아키텍처 패턴

```
┌─────────────────────────────────────────────────────────────────┐
│              Batch Server Architecture                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│   │   Quartz     │───▶│   Domain     │───▶│   gRPC       │    │
│   │  Scheduler   │    │  JobFactory  │    │   Client     │    │
│   └──────────────┘    └──────────────┘    └──────────────┘    │
│          │                   │                   │             │
│          ▼                   ▼                   ▼             │
│   ┌──────────────────────────────────────────────────────┐    │
│   │                   Spring Batch                        │    │
│   │  ┌─────────┐    ┌─────────┐    ┌─────────┐          │    │
│   │  │  Reader │───▶│Processor│───▶│  Writer │          │    │
│   │  │ (gRPC)  │    │ (Transform)│   │ (JPA)  │          │    │
│   │  └─────────┘    └─────────┘    └─────────┘          │    │
│   └──────────────────────────────────────────────────────┘    │
│                              │                                 │
│                              ▼                                 │
│   ┌──────────────────────────────────────────────────────┐    │
│   │              PostgreSQL + pgvector                    │    │
│   └──────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 정량적 지표

### 2.1 코드베이스 규모

| 계층 | 파일 수 | 주요 컴포넌트 |
|------|--------|--------------|
| Domain Entities | 11 | Recruit(5), Candidate(5), SkillDic(2) |
| Repositories | 16 | Domain(8) + JPA Custom(8) |
| Batch Components | 8 | Factory(1), Reader(2), Processor(2), Writer(3) |
| gRPC | 4 | Client(1), Service(1), Processor(2) |
| Configuration | 6 | Batch, JPA, Quartz, gRPC |
| **Total** | **45+** | - |

### 2.2 데이터 처리 규모

| 도메인 | 레코드 수 | 처리 시간 | 처리량 |
|--------|----------|----------|--------|
| Recruit | 87,488 | 12m 54.8s | 113.0 rps |
| Candidate | 118,741 | 30m 50.1s | 64.2 rps |
| Skill Dictionary | 105 | 1.69s | 62.2 rps |
| **Total** | **206,334** | **44m 46.6s** | **76.8 rps** |

### 2.3 벡터 마이그레이션

| 지표 | Before | After |
|------|--------|-------|
| 벡터 차원 | 384d | 1536d |
| 임베딩 모델 | all-MiniLM-L6-v2 | text-embedding-3-small |
| 인덱스 타입 | IVFFlat | HNSW |
| HNSW m | 16 | 32 |
| HNSW ef_construction | 64 | 128 |

---

## 3. 핵심 기능 구현

### 3.1 Factory 패턴 기반 Job 생성

```java
@Component
@RequiredArgsConstructor
public class DomainJobFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public Job createJob(String domain, Step step) {
        return new JobBuilder(domain + "Job", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(step)
            .build();
    }

    public Step createStep(String domain, int chunkSize,
                          ItemReader<?> reader,
                          ItemProcessor<?, ?> processor,
                          ItemWriter<?> writer) {
        return new StepBuilder(domain + "Step", jobRepository)
            .chunk(chunkSize, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
```

### 3.2 gRPC Client Streaming

```
┌─────────────────┐         ┌─────────────────┐
│   Batch Server  │         │  Python Server  │
│   (Java/gRPC)   │         │   (gRPC)        │
└────────┬────────┘         └────────┬────────┘
         │                           │
         │  StreamRequest(domain)    │
         │─────────────────────────▶│
         │                           │
         │  DataRow (chunk 1)        │
         │◀─────────────────────────│
         │                           │
         │  DataRow (chunk 2)        │
         │◀─────────────────────────│
         │                           │
         │       ...                 │
         │                           │
         │  DataRow (chunk N)        │
         │◀─────────────────────────│
         │                           │
         │  StreamComplete           │
         │◀─────────────────────────│
         │                           │
```

**Proto 정의**:
```protobuf
service EmbeddingStream {
    rpc StreamData(StreamRequest) returns (stream DataRow);
}

message StreamRequest {
    string domain = 1;      // "recruit" | "candidate" | "skill_dic"
    int32 chunk_size = 2;   // 100
}

message DataRow {
    string recruit_id = 1;
    string position = 2;
    string company_name = 3;
    int32 experience_years = 4;
    repeated float skills_vector = 5;  // 1536 dimensions
    // ... more fields
}
```

### 3.3 벡터 직렬화 (pgvector)

**문제**: JPA에서 `float[]` → PostgreSQL `vector` 타입 변환 오류

**해결**: Custom Converter + Native Query

```java
// Entity
@Column(name = "skills_vector", columnDefinition = "vector(1536)")
private List<Float> skillsVector;

// Repository - Native Upsert Query
@Modifying
@Query(value = """
    INSERT INTO recruit_skills_embedding (recruit_id, skills_vector)
    VALUES (:recruitId, CAST(:skillsVector AS vector))
    ON CONFLICT (recruit_id) DO UPDATE
    SET skills_vector = CAST(:skillsVector AS vector),
        updated_at = CURRENT_TIMESTAMP
    """, nativeQuery = true)
void upsert(@Param("recruitId") UUID recruitId,
            @Param("skillsVector") String skillsVector);
```

**벡터 변환 유틸**:
```java
public static String toVectorString(List<Float> vector) {
    return "[" + vector.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(",")) + "]";
}
```

---

## 4. 성능 최적화

### 4.1 Chunk 기반 처리

| Chunk Size | 처리 시간 | 메모리 사용 | 권장 |
|-----------|----------|------------|------|
| 50 | 느림 | 낮음 | X |
| 100 | 적정 | 적정 | O (기본값) |
| 500 | 빠름 | 높음 | 대용량 |
| 1000 | 매우 빠름 | 매우 높음 | 고사양 환경 |

### 4.2 JVM 튜닝

```properties
# gradle.properties
org.gradle.jvmargs=-Xms2g -Xmx8g -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled
```

| 설정 | 값 | 설명 |
|------|-----|------|
| Initial Heap | 2GB | 시작 힙 메모리 |
| Max Heap | 8GB | 최대 힙 메모리 |
| GC | G1GC | 가비지 컬렉터 |
| GC Pause Target | 200ms | 최대 GC 중단 시간 |

### 4.3 Batch 메타데이터 관리

```sql
-- Spring Batch 메타데이터 테이블 (9개)
batch_job_instance
batch_job_execution
batch_job_execution_params
batch_job_execution_context
batch_step_execution
batch_step_execution_context
batch_step_execution_seq
batch_job_execution_seq
batch_job_seq
```

---

## 5. 데이터 모델

### 5.1 Entity 구조

#### Recruit 도메인 (5개)
```
RecruitEntity
├── recruitId: UUID (PK)
├── position: String
├── companyName: String
├── experienceYears: Integer
├── primaryKeyword: String
├── englishLevel: String
├── publishedAt: OffsetDateTime
├── createdAt: OffsetDateTime
└── updatedAt: OffsetDateTime

RecruitDescriptionEntity
├── recruitId: UUID (PK, FK)
├── shortDescription: String
├── longDescription: String (TEXT)
└── requirements: String (TEXT)

RecruitSkillEntity
├── recruitId: UUID (PK, FK)
├── skill: String (PK)
└── createdAt: OffsetDateTime

RecruitSkillsEmbeddingEntity
├── recruitId: UUID (PK, FK)
├── skillsVector: List<Float> (1536d)
└── updatedAt: OffsetDateTime
```

#### Candidate 도메인 (5개)
```
CandidateEntity
├── candidateId: UUID (PK)
├── positionCategory: String
├── experienceYears: Integer
├── originalResume: String (TEXT)
├── createdAt: OffsetDateTime
└── updatedAt: OffsetDateTime

CandidateDescriptionEntity
├── candidateId: UUID (PK, FK)
├── shortBio: String
├── fullBio: String (TEXT)
└── strengths: String

CandidateSkillEntity
├── candidateId: UUID (PK, FK)
├── skill: String (PK)
└── createdAt: OffsetDateTime

CandidateSkillsEmbeddingEntity
├── candidateId: UUID (PK, FK)
├── skillsVector: List<Float> (1536d)
└── updatedAt: OffsetDateTime
```

#### Skill Dictionary 도메인 (2개)
```
SkillCategoryDicEntity
├── categoryId: UUID (PK)
├── category: String (UNIQUE)
├── createdAt: OffsetDateTime
└── updatedAt: OffsetDateTime

SkillEmbeddingDicEntity
├── skillId: UUID (PK)
├── categoryId: UUID (FK)
├── skill: String (UNIQUE)
├── skillVector: List<Float> (1536d)
├── createdAt: OffsetDateTime
└── updatedAt: OffsetDateTime
```

### 5.2 테이블 용량

| 테이블 | 레코드 수 | 예상 용량 |
|--------|----------|----------|
| recruit | 87,488 | ~50MB |
| recruit_description | 87,488 | ~200MB |
| recruit_skill | 612,416 | ~30MB |
| recruit_skills_embedding | 87,488 | ~530MB |
| candidate | 118,741 | ~60MB |
| candidate_description | 118,741 | ~300MB |
| candidate_skill | 593,705 | ~30MB |
| candidate_skills_embedding | 118,741 | ~720MB |
| skill_embedding_dic | 105 | ~1MB |
| skill_category_dic | 6 | <1KB |
| **Total** | **~1.5M** | **~1.9GB** |

---

## 6. Flyway 마이그레이션

### 6.1 마이그레이션 히스토리

| 버전 | 파일명 | 설명 |
|------|--------|------|
| V1 | init_schema.sql | 초기 스키마 생성 |
| V2 | init_batch_metadata.sql | Spring Batch 메타데이터 |
| V3 | init_indexes.sql | 기본 인덱스 생성 |
| V4 | embedding_indexes_concurrently.sql | HNSW 인덱스 (동시) |
| V5 | optimize_vector_indexes.sql | 인덱스 최적화 |
| V6 | add_candidate_description_fields.sql | 필드 추가 |

### 6.2 HNSW 인덱스 설정

```sql
-- 벡터 인덱스 생성 (HNSW)
CREATE INDEX CONCURRENTLY idx_recruit_skills_embedding_vector
ON recruit_skills_embedding
USING hnsw (skills_vector vector_cosine_ops)
WITH (m = 32, ef_construction = 128);

CREATE INDEX CONCURRENTLY idx_candidate_skills_embedding_vector
ON candidate_skills_embedding
USING hnsw (skills_vector vector_cosine_ops)
WITH (m = 32, ef_construction = 128);

CREATE INDEX CONCURRENTLY idx_skill_embedding_dic_vector
ON skill_embedding_dic
USING hnsw (skill_vector vector_cosine_ops)
WITH (m = 32, ef_construction = 128);
```

---

## 7. gRPC 통신 상세

### 7.1 Proto 메시지 (v2)

```protobuf
// RecruitRow: 11 필드
message RecruitRow {
    string recruit_id = 1;
    string position = 2;
    string company_name = 3;
    int32 experience_years = 4;
    string primary_keyword = 5;
    string english_level = 6;
    string short_description = 7;
    string long_description = 8;
    string requirements = 9;
    repeated string skills = 10;
    repeated float skills_vector = 11;  // 1536d
}

// CandidateRow: 10 필드
message CandidateRow {
    string candidate_id = 1;
    string position_category = 2;
    int32 experience_years = 3;
    string original_resume = 4;
    string short_bio = 5;
    string full_bio = 6;
    string strengths = 7;
    repeated string skills = 8;
    repeated float skills_vector = 9;   // 1536d
}

// SkillDicRow: 4 필드
message SkillDicRow {
    string skill_id = 1;
    string category = 2;
    string skill = 3;
    repeated float skill_vector = 4;    // 1536d
}
```

### 7.2 연결 설정

| 설정 | 값 |
|------|-----|
| Python Server | localhost:50051 |
| Max Message Size | 100MB |
| Deadline | 10분 |
| Keep Alive | 30초 |

---

## 8. 스케줄링

### 8.1 Quartz 설정

```yaml
spring:
  quartz:
    job-store-type: memory  # RAMJobStore
    auto-startup: false     # 수동 트리거
    properties:
      org.quartz.threadPool.threadCount: 5
```

### 8.2 Job 트리거 방식

| 방식 | 설명 | 사용 시나리오 |
|------|------|-------------|
| Manual | REST API 호출 | 개발/테스트 |
| Scheduled | Cron 표현식 | 운영 환경 (예: 매일 02:00) |
| Event | 메시지 수신 | 데이터 업데이트 시 |

---

## 9. 에러 처리

### 9.1 재시도 정책

```java
@Bean
public RetryPolicy retryPolicy() {
    return RetryPolicy.builder()
        .maxRetries(3)
        .backoffPeriod(1000)  // 1초
        .retryableExceptions(TransientDataAccessException.class)
        .build();
}
```

### 9.2 DLQ (Dead Letter Queue)

```java
@Entity
@Table(name = "dlq")
public class DlqEntity {
    @Id
    private UUID dlqId;
    private String domain;
    private String recordId;
    private String errorMessage;
    private String rawData;
    private OffsetDateTime createdAt;
}
```

---

## 10. 테스트

### 10.1 테스트 커버리지

| 테스트 유형 | 파일 수 | 테스트 케이스 |
|------------|--------|--------------|
| Unit Test | 5 | 25 |
| Integration Test | 3 | 15 |
| **Total** | **8** | **40** |

### 10.2 주요 테스트 파일

- `DomainJobFactoryTest.java` - Job 생성 테스트
- `BatchPropertiesTest.java` - 설정 테스트
- `RecruitRepositoryTest.java` - 레포지토리 테스트
- `SkillEmbeddingDicRepositoryTest.java` - 스킬 사전 테스트

---

## 11. 배포 정보

| 항목 | 값 |
|------|-----|
| 포트 | 8080 (HTTP), 9090 (gRPC Server) |
| JVM 옵션 | `-Xms2g -Xmx8g -XX:+UseG1GC` |
| 빌드 시간 | ~35초 |
| JAR 크기 | ~95MB |
| 기동 시간 | ~12초 |

---

## 12. 모니터링 지표

### 12.1 Job 실행 통계

```sql
SELECT
    job_name,
    status,
    start_time,
    end_time,
    EXTRACT(EPOCH FROM (end_time - start_time)) AS duration_sec
FROM batch_job_execution bje
JOIN batch_job_instance bji ON bje.job_instance_id = bji.job_instance_id
ORDER BY start_time DESC
LIMIT 10;
```

### 12.2 처리량 계산

```
처리량(TPS) = 처리 레코드 수 / 처리 시간(초)

Recruit:    87,488 / 774.8s = 112.9 TPS
Candidate: 118,741 / 1850.1s = 64.2 TPS
Skill Dic:     105 / 1.69s = 62.1 TPS
```

---

**문서 끝**
