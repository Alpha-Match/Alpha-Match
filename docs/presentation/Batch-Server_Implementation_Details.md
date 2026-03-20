# Batch-Server 구현 상세 문서

**프로젝트**: Alpha-Match (Headhunter-Recruit Matching System)
**작성일**: 2026-01-14 (적재 테스트 성능 비교 섹션 추가)
**버전**: 1.1

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

### 2.4 적재 테스트 성능 비교

3차에 걸친 적재 테스트를 통해 시스템 성능을 검증하고 최적화하였습니다.

#### 2.4.1 테스트 단계 개요

| 단계 | 테스트 일자 | 벡터 차원 | 쓰기 방식 | 목적 |
|------|-----------|----------|----------|------|
| **1차 적재** | 2025-12-26 | 384d | Sequential | Baseline 측정 |
| **2차 적재** | 2025-12-26 | 384d | Virtual Thread Parallel | 병렬화 최적화 |
| **3차 적재** | 2026-01-06 | 1536d | Virtual Thread Parallel | 벡터 차원 증가 영향 |

#### 2.4.2 도메인별 성능 비교

##### Recruit 도메인 (주요 비교 대상)

| 지표 | 1차 (Baseline) | 2차 (Virtual Thread) | 3차 (1536d) |
|------|---------------|---------------------|-------------|
| **레코드 수** | 87,488 | 87,488 | 89,618 |
| **처리 시간** | 12m 54.8s | 8m 38.2s | 23m 18s |
| **처리량 (RPS)** | 113.0 | 168.8 | 64.19 |
| **개선율** | - | **+49.4%** | -62.0% (vs 2차) |
| **벡터 크기/row** | 1.5KB | 1.5KB | 6.1KB |

##### 전체 도메인 종합 (1차 vs 3차)

| 도메인 | 1차 (384d) | 3차 (1536d) | 레코드 차이 |
|--------|-----------|-------------|------------|
| **Recruit** | 87,488건, 12m 54.8s, 113.0 rps | 89,618건, 23m 18s, 64.19 rps | +2,130건 |
| **Candidate** | 118,741건, 30m 50.1s, 64.2 rps | *(3차 미적재)* | - |
| **Skill Dictionary** | 105건, 1.69s, 62.2 rps | 147건, ~0.5s | +42건 |
| **Total** | 206,334건, 44m 46.6s, 76.8 rps | 89,765건, 23m 18s, 64.21 rps | - |

#### 2.4.3 Virtual Thread 병렬화 효과

**기존 Sequential 방식 (1차)**:
```
시간 ──────────────────────────────────────────────────────────────────►
     [recruit]───────►[skill]─────►[desc]────►[embedding]────►
     |──── 197ms ────||── 150ms ──||─ 100ms ─||── 200ms ────|
                                                    총: 647ms/청크
```

**Virtual Thread Parallel 방식 (2차)**:
```
시간 ──────────────────────────────────────────────────────►
     [recruit]───────►┌─[skill]───────┐
                      ├─[desc]────────┼──► 동시 완료
                      └─[embedding]───┘
     |──── 197ms ────||──── 200ms ────|
                           (max)      총: 397ms/청크
```

**성능 개선 결과**:
- 청크당 처리 시간: 647ms → 397ms (**38.6% 단축**)
- 전체 처리 시간: 12m 54.8s → 8m 38.2s (**33.1% 단축**)
- 처리량: 113.0 rps → 168.8 rps (**49.4% 증가**)

#### 2.4.4 벡터 차원 증가 영향 분석 (384d → 1536d)

| 지표 | 384d (2차) | 1536d (3차) | 변화율 |
|------|-----------|-------------|--------|
| **벡터 크기** | 384 × 4B = 1,536B | 1536 × 4B = 6,144B | **+300%** |
| **네트워크 전송량** | ~134MB (Recruit) | ~537MB (Recruit) | **+300%** |
| **DB 쓰기 부하** | 1.0x | 4.0x | **+300%** |
| **처리량 (RPS)** | 168.8 | 64.19 | **-62.0%** |

**분석 결론**:
- 벡터 차원 4배 증가 시 처리량 약 62% 감소
- 이론적으로 4배 감소 예상이나, Virtual Thread 병렬화로 일부 상쇄
- 네트워크 I/O와 DB 쓰기 모두 병목 발생

#### 2.4.5 테이블별 예상 용량

| 테이블 | 1차 (384d) | 3차 (1536d) | 증가율 |
|--------|-----------|-------------|--------|
| recruit_skills_embedding | ~130MB | ~530MB | +308% |
| candidate_skills_embedding | ~180MB | ~720MB | +300% |
| skill_embedding_dic | ~160KB | ~900KB | +463% |
| **벡터 테이블 총합** | ~310MB | ~1,250MB | **+303%** |

#### 2.4.6 결론 및 권장사항

**1. 성능 최적화 효과**
- Virtual Thread 병렬 쓰기로 **33~49% 성능 향상** 달성
- FK 제약이 없는 테이블(skill, description, embedding) 병렬화가 핵심

**2. 벡터 차원 증가 Trade-off**
- 검색 품질 향상 (1536d OpenAI 모델) vs 처리 성능 감소 (-62%)
- 저장 용량 약 4배 증가 (310MB → 1.25GB)

**3. 권장 인프라 사양**
- JVM Heap: `-Xms2g -Xmx8g` (1536d 기준)
- HikariCP Pool: 최소 20개 (Virtual Thread 3개 동시 사용)
- PostgreSQL: shared_buffers 2GB 이상 권장

**4. 향후 최적화 방향**
- 청크 사이즈 튜닝 (100 → 200~300 비교 테스트)
- Candidate 도메인 1536d 적재 테스트
- HNSW 인덱스 파라미터 최적화 (m=32, ef_construction=128)

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

## 13. 💡 예외 및 디버깅 (Edge Cases / Debugging)

### 13.1 PGvector 직렬화 오류: bytea → vector 변환 불가

**문제**: Vector Embedding을 PostgreSQL에 저장할 때 타입 변환 오류 발생

```
org.postgresql.util.PSQLException: 오류: bytea 자료형을 vector 자료형으로 변환할 수 없습니다.
Position: 164
```

**원인 분석**:

Hibernate Native Query에서 `PGvector` 객체를 SQL 파라미터로 바인딩할 때, Hibernate가 자동으로 **bytea (binary)** 타입으로 직렬화합니다. 이후 `CAST(:vector AS vector(1536))`를 수행하면 이미 bytea로 바인딩된 후라서 PostgreSQL이 변환할 수 없습니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Hibernate 직렬화 흐름                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   PGvector 객체          Hibernate               PostgreSQL         │
│   [0.1, 0.2, ...]   ──▶  bytea (binary)   ──▶   CAST 실패 ❌       │
│                          (자동 직렬화)           (bytea→vector 불가) │
│                                                                     │
│   PGvector.toString()    String              PostgreSQL             │
│   "[0.1,0.2,...]"   ──▶  varchar        ──▶   CAST 성공 ✅         │
│                          (명시적 변환)          (String→vector 가능) │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**해결책**: `.toString()` 메서드로 명시적 String 변환 후 CAST 수행


**학습 포인트**:
- Hibernate Native Query에서 Custom Type은 자동 변환을 신뢰하지 말 것
- PostgreSQL pgvector는 `String "[0.1,0.2,...]"` → `vector` 변환을 지원
- `bytea` → `vector` 변환은 지원되지 않음

---

### 13.2 HNSW 인덱스 도입 후 검색 결과 누락 이슈

**문제**: Python 정확 검색에는 존재하지만 Java API 서버(HNSW 인덱스)에서는 누락되는 검색 결과 발생

| Company   | Python 순위 | 유사도 점수 | Java 결과 |
|-----------|-------------|-------------|-----------|
| ProCoders | 4           | 66.08%      | ❌ 누락 |
| Softengi  | 10          | 65.51%      | ❌ 누락 |
| AGILENIX  | 24          | 64.66%      | ❌ 누락 |

**환경 차이점**:
- Python DB: HNSW 인덱스 없음 → **정확 검색 (Sequential Scan)**
- Java DB: HNSW 인덱스 적용 → **근사 검색 (Approximate Search)**

**원인 분석**:

HNSW(Hierarchical Navigable Small World)는 **근사 최근접 이웃 알고리즘**으로, 정확도 대신 속도를 선택합니다. 초기 설정된 낮은 파라미터(`m=16`, `ef_construction=64`)로 인해 **중간 유사도 구간(64~66%)**의 결과가 그래프 탐색 과정에서 누락되었습니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                   HNSW 그래프 탐색 특성                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   유사도 구간        HNSW 탐색 결과       원인                        │
│   ─────────────────────────────────────────────────────────────     │
│   90% 이상          ✅ 거의 누락 없음     그래프 중심부에 위치         │
│   70~90%            ✅ 대부분 발견        연결성 양호                  │
│   60~70%            ⚠️ 누락 발생 가능     그래프 외곽, 연결 부족       │
│   60% 미만          ─  임계값에 의해 제외  정상 동작                   │
│                                                                     │
│   ※ 누락된 기업들: 모두 64~66% 구간 (중간 유사도)                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**HNSW 파라미터 영향**:

| 파라미터 | 역할 | 낮은 값 | 높은 값 |
|----------|------|---------|---------|
| `m` | 레이어당 연결 수 | 그래프 희소 → 누락↑ | 연결성↑ → 정확도↑ |
| `ef_construction` | 인덱스 빌드 시 탐색 깊이 | 빌드 빠름, 품질↓ | 빌드 느림, 품질↑ |

**해결책**: HNSW 인덱스 파라미터 2배 증가

**성능 vs 정확도 Trade-off**:

| 지표 | Before (m=16) | After (m=32) | 변화 |
|------|---------------|--------------|------|
| 쿼리 속도 | ~10-20ms | ~20-40ms | +10-20ms |
| 중간 유사도 정확도 | 낮음 (누락 발생) | 높음 | ✅ 개선 |
| 인덱스 빌드 시간 | 빠름 | 느림 (약 2배) | 일회성 비용 |

**학습 포인트**:
- 정확 검색(Sequential Scan)과 근사 검색(HNSW)은 결과 일관성이 보장되지 않으므로 비교 검증이 필수이다.
- HNSW의 “그래프 중심부”는 검색 결과 랭킹이 아니라 절대 유사도 분포 기준으로 결정된다

---

**문서 끝**
