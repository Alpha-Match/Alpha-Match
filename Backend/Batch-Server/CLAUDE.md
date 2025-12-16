# Batch Server - Claude Instructions

**프로젝트명:** Alpha-Match Batch Server
**최종 업데이트:** 2025-12-16
**기술 스택:** Spring Boot 4.0 + Java 21 + Spring Batch + gRPC + PostgreSQL(pgvector)

---

## 🚨 AI 에이전트 필독 사항

**이 문서는 실제 구현된 코드를 기준으로 작성되었습니다.**
구현되지 않은 기능은 "⏳ 예정" 섹션에 명시되어 있습니다.

---

## 📋 프로젝트 개요

Python AI Server로부터 gRPC Streaming으로 Embedding 데이터를 수신하여 PostgreSQL(pgvector)에 저장하는 배치 서버입니다.

### 핵심 기능
- 🔄 **gRPC Streaming 수신** (Python AI Server)
- 💾 **이중 테이블 저장** (metadata + embedding)
- ⚡ **Chunk 기반 Batch Upsert** (기본 300개)
- 🚨 **DLQ 처리** (실패 레코드)
- ✅ **Checkpoint 관리** (재시작 지원)
- 🔔 **캐시 무효화** (API Server 호출)

### 주요 학습 목표
- Spring Batch ItemReader/Processor/Writer 패턴
- Clean Architecture (Domain/Infrastructure 분리)
- pgvector를 활용한 Vector DB 구현
- Fault Tolerance (Skip/Retry 정책)

---

## 🗺️ 문서 계층 구조 (우선순위 순)

### 🔴 Tier 1: 필수 문서 (AI 에이전트가 반드시 읽어야 함)
1. **현재 문서 (CLAUDE.md)** - 실제 구현 상태 및 구조
2. **BatchJobConfig.java** - Spring Batch Job 설정 (실제 코드)
3. **BatchProperties.java** - 도메인별 설정 구조

### 🟡 Tier 2: 참조 문서 (필요 시 참조)
- **Backend 공통 문서**
  - `/Backend/docs/DB_스키마_가이드.md` - DB 스키마 전체 구조
  - `/Backend/docs/Flyway_마이그레이션_가이드.md` - 마이그레이션 정책
  - `/Backend/docs/ERD_다이어그램.md` - ERD
- **Batch-Server 전용 문서**
  - `/docs/도메인_확장_가이드.md` - 새 도메인 추가 방법

### 🟢 Tier 3: 히스토리 문서
- `/docs/hist/` - 과거 작업 이력 (컨텍스트 참조용)

---

## 📂 실제 프로젝트 구조 (Clean Architecture)

```
src/main/java/com/alpha/backend/
│
├── domain/                          # 도메인 계층 (비즈니스 로직)
│   ├── common/
│   │   ├── BaseMetadataEntity       # ✅ 모든 Metadata의 부모 클래스
│   │   └── BaseEmbeddingEntity      # ✅ 모든 Embedding의 부모 클래스
│   ├── recruit/
│   │   ├── entity/
│   │   │   ├── RecruitMetadataEntity    # ✅ Recruit 메타데이터
│   │   │   └── RecruitEmbeddingEntity   # ✅ Recruit 임베딩 (384d)
│   │   └── repository/
│   │       ├── RecruitMetadataRepository    # ✅ Port (인터페이스)
│   │       └── RecruitEmbeddingRepository   # ✅ Port (인터페이스)
│   ├── candidate/
│   │   ├── entity/
│   │   │   ├── CandidateMetadataEntity    # ✅ Candidate 메타데이터
│   │   │   └── CandidateEmbeddingEntity   # ✅ Candidate 임베딩 (768d)
│   │   └── repository/
│   │       ├── CandidateMetadataRepository    # ✅ Port (인터페이스)
│   │       └── CandidateEmbeddingRepository   # ✅ Port (인터페이스)
│   ├── dlq/
│   │   ├── entity/
│   │   │   └── DlqEntity                # ✅ 실패 레코드 (도메인 범용)
│   │   └── repository/
│   │       └── DlqRepository            # ✅ Port (인터페이스)
│   └── checkpoint/
│       ├── entity/
│       │   └── CheckpointEntity         # ✅ 체크포인트 (도메인 범용)
│       └── repository/
│           └── CheckpointRepository     # ✅ Port (인터페이스)
│
├── infrastructure/                  # 인프라 계층 (기술 구현)
│   ├── config/
│   │   ├── BatchProperties          # ✅ 도메인별 설정 (Map<domain, DomainConfig>)
│   │   ├── ExecutorConfig           # ✅ Virtual Thread Executor
│   │   ├── GrpcClientConfig         # ✅ gRPC Client 설정
│   │   ├── JacksonConfig            # ✅ Jackson 설정
│   │   └── QuartzConfig             # ✅ Quartz Scheduler 설정 (JDBC JobStore)
│   ├── grpc/
│   │   └── client/
│   │       ├── EmbeddingGrpcClient           # ✅ Python AI Server 연동 (StreamEmbedding)
│   │       └── CacheInvalidateGrpcClient     # ✅ API Server 캐시 무효화
│   └── persistence/                 # Adapter (JPA 구현체)
│       ├── RecruitMetadataJpaRepository
│       ├── RecruitEmbeddingJpaRepository
│       ├── CandidateMetadataJpaRepository
│       ├── CandidateEmbeddingJpaRepository
│       ├── DlqJpaRepository
│       └── CheckpointJpaRepository
│
├── application/                     # 애플리케이션 계층 (Use Case)
│   ├── batch/
│   │   ├── dto/
│   │   │   └── DomainItem<M, E>             # ✅ Metadata + Embedding 묶음
│   │   ├── reader/
│   │   │   ├── DomainItemReader<T>          # ✅ 추상 Reader (gRPC Stream → Queue)
│   │   │   └── RecruitItemReader            # ✅ Recruit 구현체
│   │   ├── processor/
│   │   │   ├── DomainItemProcessor<I,M,E>   # ✅ 추상 Processor (Proto → Entity)
│   │   │   └── RecruitItemProcessor         # ✅ Recruit 구현체
│   │   └── writer/
│   │       └── DomainItemWriter<M,E>        # ✅ Generic Writer (Batch Upsert)
│   └── usecase/
│       ├── DlqService                       # ✅ 인터페이스
│       ├── DlqServiceImpl                   # ✅ DLQ 저장 로직
│       ├── CacheInvalidationService         # ✅ 인터페이스
│       └── CacheInvalidationServiceImpl     # ✅ 캐시 무효화 로직
│
└── batch/                           # Spring Batch 설정
    ├── factory/
    │   └── DomainJobFactory                 # ✅ 도메인별 Job/Step 동적 생성 (Factory 패턴)
    ├── job/
    │   └── BatchJobConfig                   # ✅ Job Bean 정의 (Factory 위임)
    ├── scheduler/
    │   └── BatchSchedulerConfig             # ✅ Quartz Scheduler 설정 (Cron + JobLauncher)
    └── listener/
        ├── EmbeddingJobListener             # ✅ Job 시작/종료 로깅
        └── EmbeddingStepListener            # ✅ Step 시작/종료 로깅
```

---

## 🚀 현재 구현 상태

### ✅ 완료된 기능

#### 1. Domain Layer (Clean Architecture)
- ✅ Base Entity 패턴 (`BaseMetadataEntity`, `BaseEmbeddingEntity`)
- ✅ Recruit Domain (Entity + Repository Interface)
- ✅ Candidate Domain (Entity + Repository Interface)
- ✅ DLQ/Checkpoint (도메인 범용화)

#### 2. Infrastructure Layer
- ✅ JpaRepository 구현체 (Port & Adapter 패턴)
- ✅ BatchProperties (도메인별 Map 구조)
- ✅ gRPC Client 2개 (EmbeddingGrpcClient, CacheInvalidateGrpcClient)
- ✅ Jackson 3 설정 (Spring Boot 4.0 호환)
- ✅ QuartzConfig (JDBC JobStore, ThreadPool 10개, Misfire 60초)

#### 3. Application Layer
- ✅ **DomainItemReader<T>** - 추상 Reader (gRPC Stream을 Queue로 변환)
- ✅ **DomainItemProcessor<I,M,E>** - 추상 Processor (Proto → Entity 변환)
- ✅ **DomainItemWriter<M,E>** - Generic Writer (Batch Upsert + DLQ)
- ✅ **RecruitItemReader/Processor** - Recruit 구현체
- ✅ DlqService, CacheInvalidationService

#### 4. Spring Batch
- ✅ **DomainJobFactory** - 도메인별 Job/Step 동적 생성 (Factory 패턴)
- ✅ **BatchJobConfig** - Job Bean 정의 (Factory로 위임)
- ✅ Chunk 기반 처리 (기본 300개)
- ✅ Fault Tolerance (Skip 정책, 최대 100개)
- ✅ Job/Step Listener (로깅)

#### 5. Quartz Scheduler
- ✅ **BatchSchedulerConfig** - Quartz + Spring Batch 통합
- ✅ **Recruit Job 스케줄** - Cron 기반 자동 실행 (기본: 매일 새벽 2시)
- ✅ **Misfire 정책** - DO_NOTHING (놓친 실행은 건너뜀)
- ✅ **YAML 설정** - batch.scheduler.jobs.recruit.cron

#### 6. Database
- ✅ Flyway V1~V5 마이그레이션
- ✅ pgvector 확장
- ✅ Native Query Upsert (CONFLICT 처리)

---

## ⏳ 구현 예정 (명시적으로 미구현)

### 1. Candidate Job
- ⏳ **CandidateItemReader** - Candidate용 Reader
- ⏳ **CandidateItemProcessor** - Candidate용 Processor
- ⏳ **candidateEmbeddingProcessingJob** - BatchJobConfig에 추가 필요
- ⏳ **CandidateRow proto** - proto 파일에 정의 필요

### 2. Factory 패턴 (고도화 필요)
- ⏳ **ChunkProcessorFactory** - (테스트 코드만 존재)
- ⏳ **ChunkProcessorInterface** - (문서에만 언급됨, 실제 구현 없음)

### 3. gRPC Server
- ⏳ **IngestDataStream Server** - Python → Batch (Client Streaming 수신)
  - 현재: EmbeddingGrpcClient만 있음 (Batch → Python)
  - 필요: gRPC Server 구현 (Python의 Client Streaming 수신)

### 4. Checkpoint 자동 업데이트
- ⏳ Writer에서 마지막 UUID 자동 저장
- ⏳ Job 재시작 시 자동 재개

---

## 📚 핵심 패턴 (실제 구현됨)

### 1. Base Entity 패턴 ✅

**목적:** 공통 필드 중복 제거

```java
// ✅ 실제 구현됨 (BaseMetadataEntity.java)
@MappedSuperclass
public abstract class BaseMetadataEntity {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}

// ✅ 도메인별 Entity는 Base를 상속
@Entity
@Table(name = "recruit_metadata")
public class RecruitMetadataEntity extends BaseMetadataEntity {
    private String companyName;
    private Integer expYears;
    // ...
}
```

### 2. Generic ItemWriter 패턴 ✅

**목적:** 모든 도메인에서 재사용 가능한 Writer

```java
// ✅ 실제 구현됨 (DomainItemWriter.java)
@RequiredArgsConstructor
public class DomainItemWriter<M extends BaseMetadataEntity, E extends BaseEmbeddingEntity>
        implements ItemWriter<DomainItem<M, E>> {

    private final String domain;
    private final JpaRepository<M, UUID> metadataRepository;
    private final JpaRepository<E, UUID> embeddingRepository;
    private final UpsertFunction<M> metadataUpsertFunction;
    private final UpsertFunction<E> embeddingUpsertFunction;

    @Override
    @Transactional
    public void write(Chunk<? extends DomainItem<M, E>> chunk) {
        // 1. 개별 item 처리 (실패 시 DLQ)
        // 2. 성공한 데이터만 Batch Upsert
        // 3. metadata → embedding 순서 (FK 제약)
    }
}
```

### 3. BatchProperties 도메인별 설정 ✅

**목적:** YAML 기반 도메인 설정 중앙 관리

```java
// ✅ 실제 구현됨 (BatchProperties.java)
@ConfigurationProperties(prefix = "batch.embedding")
public class BatchProperties {
    private Map<String, DomainConfig> domains = new HashMap<>();

    public DomainConfig getDomainConfig(String domain) {
        return domains.getOrDefault(domain, getDefaultDomainConfig());
    }

    @Data
    public static class DomainConfig {
        private int vectorDimension;    // 도메인별 Vector 차원
        private String tablePrefix;      // 도메인별 테이블 접두사
    }
}
```

**application.yml 예시:**
```yaml
batch:
  embedding:
    chunk-size: 300
    domains:
      recruit:
        vector-dimension: 384
        table-prefix: recruit
      candidate:
        vector-dimension: 768
        table-prefix: candidate
```

### 4. DomainJobFactory 패턴 ✅

**목적:** 도메인별 Job/Step을 동적으로 생성 (Factory Method 패턴)

```java
// ✅ 실제 구현됨 (DomainJobFactory.java)
@Component
@RequiredArgsConstructor
public class DomainJobFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    // ... dependencies

    /**
     * 도메인별 Job 생성
     */
    public Job createJob(String domain) {
        return switch (domain.toLowerCase()) {
            case "recruit" -> createRecruitJob();
            // case "candidate" -> createCandidateJob();  // ⏳ 예정
            default -> throw new IllegalArgumentException("Unsupported domain: " + domain);
        };
    }

    private Job createRecruitJob() {
        return new JobBuilder("recruitEmbeddingProcessingJob", jobRepository)
                .listener(embeddingJobListener)
                .start(createRecruitStep())
                .build();
    }

    // Reader/Processor/Writer 생성 로직
    // ...
}
```

**BatchJobConfig에서 사용:**
```java
@Configuration
public class BatchJobConfig {
    private final DomainJobFactory domainJobFactory;

    @Bean
    public Job recruitEmbeddingProcessingJob() {
        return domainJobFactory.createJob("recruit");
    }
}
```

### 5. Quartz Scheduler 패턴 ✅

**목적:** Spring Batch Job을 Cron 기반으로 자동 실행

**Spring Batch 6.0 마이그레이션 완료:**
- JobOperator.start(String, Properties) deprecated → JobOperator.start(Job, JobParameters) 사용
- JobRegistry로 Job 객체 획득
- JobParametersBuilder로 타입 안전한 파라미터 생성

**핵심 패키지 (Spring Batch 6.0):**
- `org.springframework.batch.core.job.*` (Job, JobExecution)
- `org.springframework.batch.core.job.parameters.*` (JobParameters, JobParametersBuilder)
- `org.springframework.batch.core.launch.*` (JobOperator, 예외들)
- `org.springframework.batch.core.configuration.JobRegistry`

```java
// ✅ 실제 구현됨 (BatchSchedulerConfig.java)
@Configuration
@ConditionalOnProperty(name = "batch.scheduler.enabled", havingValue = "true")
public class BatchSchedulerConfig {

    @Value("${batch.scheduler.jobs.recruit.cron:0 0 2 * * ?}")
    private String recruitCronExpression;

    @Bean
    public JobDetail recruitEmbeddingJobDetail() {
        return JobBuilder.newJob(RecruitEmbeddingQuartzJob.class)
                .withIdentity("recruitEmbeddingJobDetail", "embedding")
                .storeDurably()
                .requestRecovery()
                .build();
    }

    @Bean
    public Trigger recruitEmbeddingTrigger(JobDetail recruitEmbeddingJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(recruitEmbeddingJobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(recruitCronExpression)
                        .inTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * QuartzJobBean: Quartz가 실행할 실제 로직
     * Spring Batch 6.0 패턴:
     * 1. JobRegistry로 Job 객체 획득
     * 2. JobParametersBuilder로 JobParameters 생성
     * 3. JobOperator.start(Job, JobParameters) 실행
     */
    public static class RecruitEmbeddingQuartzJob extends QuartzJobBean {
        private final JobRegistry jobRegistry;
        private final JobOperator jobOperator;

        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            try {
                // 1. JobRegistry에서 Job 객체 가져오기
                Job job = jobRegistry.getJob("recruitEmbeddingProcessingJob");

                // 2. JobParameters 생성
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()))
                        .toJobParameters();

                // 3. Spring Batch Job 실행
                JobExecution execution = jobOperator.start(job, jobParameters);

                log.info("Job Started | ExecutionId={}, Status={}",
                        execution.getId(), execution.getStatus());
            } catch (JobExecutionAlreadyRunningException | JobRestartException |
                     JobInstanceAlreadyCompleteException | InvalidJobParametersException e) {
                throw new JobExecutionException("Job execution failed", e);
            }
        }
    }
}
```

**application.yml 설정:**
```yaml
batch:
  scheduler:
    enabled: true
    jobs:
      recruit:
        cron: "0 0 2 * * ?"  # 매일 새벽 2시
        enabled: true
```

---

## 🔧 빠른 시작

### 1. 서버 실행
```bash
cd Backend/Batch-Server
./gradlew bootRun
```

### 2. 주요 설정 (application.yml)
```yaml
batch:
  embedding:
    chunk-size: 300
    max-retry: 3
    retry-backoff-ms: 1000
    domains:
      recruit:
        vector-dimension: 384
        table-prefix: recruit
      candidate:
        vector-dimension: 768
        table-prefix: candidate

grpc:
  client:
    python-embedding:
      address: static://localhost:50051
    api-cache:
      address: static://localhost:50052
```

### 3. 통합 테스트

#### 3.1 Python 서버 시작
```bash
cd Demo-Python
python src/main.py
```

#### 3.2 Batch 서버 시작
```bash
cd Backend/Batch-Server
./gradlew bootRun
```

#### 3.3 수동 실행 (Python → Batch)
```bash
# Python FastAPI 엔드포인트 호출 → Python이 Batch에 Client Streaming 전송
curl -X POST "http://localhost:8000/data/ingest/recruit?file_name=processed_recruitment_data.pkl"
```

#### 3.4 자동 실행 (Quartz Scheduler)
```yaml
# application.yml에서 스케줄러 활성화
batch:
  scheduler:
    enabled: true
    jobs:
      recruit:
        cron: "0 0 2 * * ?"  # 매일 새벽 2시
        enabled: true

# 서버 시작 시 자동으로 스케줄 등록
# Cron 표현식에 따라 자동 실행
```

**수동 실행 (테스트용):**
```bash
# Quartz 스케줄러를 비활성화하고 수동 실행
# application.yml: batch.scheduler.enabled=false
# 또는 특정 Job만 비활성화: batch.scheduler.jobs.recruit.enabled=false
```

---

## 🎯 다음 작업 단계 (우선순위 순)

### Phase 1: Candidate Job 추가
1. ⏳ proto 파일에 CandidateRow 정의
2. ⏳ CandidateItemReader 구현
3. ⏳ CandidateItemProcessor 구현
4. ⏳ DomainJobFactory에 Candidate Job 추가
5. ⏳ BatchSchedulerConfig에 Candidate 스케줄 추가

### Phase 2: gRPC Server
1. ⏳ IngestDataStream Server 구현
2. ⏳ Python Client Streaming 수신
3. ⏳ Spring Batch Job 트리거

### Phase 3: Checkpoint 자동화
1. ⏳ Writer에서 마지막 UUID 자동 저장
2. ⏳ Job 재시작 시 자동 재개

### Phase 4: 고도화 (선택)
1. ⏳ ChunkProcessorFactory 구현 (테스트 코드 기반)
2. ⏳ ChunkProcessorInterface 설계

---

## ⚠️ 주의사항

### 1. 문서와 코드 간 괴리 방지
- **이 문서(CLAUDE.md)는 실제 코드 기준으로 작성됨**
- 구현되지 않은 기능은 "⏳ 예정" 섹션에 명시
- 새 기능 추가 시 반드시 문서 업데이트

### 2. Upsert 순서 (FK 제약)
```java
// ✅ Good: metadata → embedding 순서
metadataRepository.upsertAll(metadataList);
embeddingRepository.upsertAll(embeddingList);
```

### 3. DLQ 처리
- Writer에서 개별 item 실패 시 DLQ에 저장
- Batch upsert 실패 시 전체 chunk를 DLQ에 저장

---

## 🔗 관련 프로젝트

- **Demo-Python**: `/../../Demo-Python/CLAUDE.md`
- **API Server**: `/../../Backend/Api-Server/CLAUDE.md`
- **루트 프로젝트**: `/../../CLAUDE.md`

---

## 📋 최근 업데이트

### 2025-12-16 - Spring Batch 6.0 완전 마이그레이션 완료
- ✅ **JobOperator.start(String, Properties) deprecated 해결**
  - 이전: `jobOperator.start("jobName", properties)` (deprecated)
  - 현재: `jobOperator.start(job, jobParameters)` (Spring Batch 6.0 권장)
- ✅ **JobRegistry 패턴 적용**
  - `JobRegistry.getJob(String)` → `Job` 객체 획득
  - Job 이름으로 Job 객체를 동적으로 가져오는 패턴
- ✅ **타입 안전한 JobParameters**
  - `JobParametersBuilder().addString("key", value).toJobParameters()`
  - Properties 대신 강타입 JobParameters 사용
- ✅ **핵심 패키지 정리 (Spring Batch 6.0)**
  - `org.springframework.batch.core.job.*` (Job, JobExecution)
  - `org.springframework.batch.core.job.parameters.*` (JobParameters, JobParametersBuilder)
  - `org.springframework.batch.core.launch.*` (JobOperator, 예외들)
  - `org.springframework.batch.core.configuration.JobRegistry`
- ✅ **예외 처리 강화**
  - JobExecutionAlreadyRunningException
  - JobRestartException
  - JobInstanceAlreadyCompleteException
  - InvalidJobParametersException
- ✅ **빌드 성공, deprecation 경고 완전 제거**

### 2025-12-16 - Flyway 마이그레이션 전면 재작성 완료
- ✅ **테이블 명세 보완** - table_specification.md 업데이트
  - Recruit 도메인 추가 (recruit_metadata, recruit_embedding)
  - Candidate 도메인 복합 PK 명시 (candidate_skill)
  - 공통 테이블 추가 (dlq, checkpoint)
  - Spring Batch/Quartz 테이블 명세
  - 테이블 생성 순서 및 설계 원칙 문서화
- ✅ **Flyway 통합 관리** - V1__init_database_schema.sql (457 lines)
  - pgvector + uuid-ossp Extension
  - Candidate 도메인 (skill_embedding_dic, candidate, candidate_skill, candidate_skills_embedding)
  - Recruit 도메인 (recruit_metadata, recruit_embedding)
  - 공통 테이블 (dlq, checkpoint)
  - Spring Batch 메타데이터 테이블 (공식 스키마 v6.0)
  - Quartz 스케줄러 테이블 (공식 스키마 v2.3.2)
  - 성능 인덱스 (IVFFlat for vector columns)
- ✅ **DDD Aggregate 패턴** - candidate_skill 복합 PK (무결성 보장)
- ✅ **자동 생성 비활성화** - application.yml 수정
  - spring.batch.jdbc.initialize-schema: never
  - spring.quartz.jdbc.initialize-schema: never
  - org.quartz.jobStore.isClustered: false (단일 인스턴스)
  - org.quartz.jobStore.dataSource 삭제 (불필요)
- ✅ **기존 V1~V5 파일 삭제** - 단일 V1으로 통합

### 2025-12-16 - JobOperator 마이그레이션 + 테스트 코드 정리 완료
- ✅ **JobOperator 마이그레이션** - JobLauncher (Deprecated) → JobOperator
- ✅ **BatchSchedulerConfig 수정** - Properties 기반 JobParameters 전달
- ✅ **테스트 코드 정리** - 미구현 클래스 테스트 삭제 (ChunkProcessorFactory, RecruitChunkProcessor)
- ✅ **새로운 테스트 추가** - DomainJobFactoryTest 작성
- ✅ **빌드 확인** - ./gradlew clean build 성공
- ✅ **CLAUDE.md 업데이트** - JobOperator 패턴 반영

### 2025-12-16 - DomainJobFactory + Quartz Scheduler 구현 완료
- ✅ **DomainJobFactory 구현** - Factory Method 패턴으로 도메인별 Job/Step 동적 생성
- ✅ **BatchJobConfig 리팩토링** - 하드코딩된 Job 생성 → Factory 위임
- ✅ **QuartzConfig 구현** - JDBC JobStore, ThreadPool 10개, Misfire 60초
- ✅ **BatchSchedulerConfig 구현** - Quartz + Spring Batch 통합
- ✅ **Recruit Job 스케줄** - Cron 기반 자동 실행 (기본: 매일 새벽 2시)
- ✅ **YAML 설정** - batch.scheduler.jobs.recruit.cron으로 스케줄 관리

### 2025-12-16 - CLAUDE.md 전면 재작성
- ✅ 실제 코드 기준으로 문서 작성
- ✅ 구현된 기능 vs 예정 기능 명확히 분리
- ✅ 문서 계층 구조 정립 (Tier 1/2/3)
- ✅ AI 에이전트용 필독 사항 추가

### 2025-12-16 - Clean Architecture 리팩토링 완료
- ✅ Domain/Infrastructure 계층 분리
- ✅ Port & Adapter 패턴 적용
- ✅ JpaRepository 구현체 분리

### 2025-12-12 - 도메인별 제네릭 구조 완성
- ✅ Base Entity 패턴
- ✅ Generic ItemWriter
- ✅ BatchProperties 도메인별 Map 구조

---

**최종 수정일:** 2025-12-16
**CLAUDE.md 업데이트 완료 ✅**
