# Batch Server - Claude Instructions

**프로젝트명:** Alpha-Match Batch Server
**최종 업데이트:** 2025-12-17
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

#### Backend 공통 문서 (DB 작업 시 필수 참조)
- `/Backend/docs/DB_스키마_가이드.md` - DB 스키마 전체 구조 ⭐
- `/Backend/docs/table_specification.md` - 테이블 명세서 (단일 소스) ⭐
- `/Backend/docs/ERD_다이어그램.md` - ERD 다이어그램
- `/Backend/docs/Flyway_마이그레이션_가이드.md` - 마이그레이션 정책

#### Batch-Server 고정 문서 (최신 상태 유지 필수)
1. **Spring_Batch_개발_가이드.md** - Spring Batch 6.0 아키텍처 및 패턴
   - ItemReader/Processor/Writer 구현 방법
   - DomainJobFactory 패턴
   - Quartz Scheduler 통합
   - Proto 파일 정의
   - gRPC Client 구현
2. **도메인_확장_가이드.md** - 새 도메인 추가 절차
   - Entity/Repository 작성
   - Spring Batch 컴포넌트 구현
   - DomainJobFactory 등록
   - Quartz Scheduler 설정
3. **동시성_제어.md** - Race Condition 대응
   - Checkpoint 경쟁 방지
   - Upsert 순서 보장
   - UUID 기반 병렬 처리
   - DLQ 패턴

> **🚨 DB 작업 시 주의:**
> Entity 작성, Repository 구현, Batch Writer 개발 시 반드시 `/Backend/docs/` 문서를 먼저 확인하세요.
> 특히 `table_specification.md`는 DB 스키마의 단일 소스(Single Source of Truth)입니다.

### 🟢 Tier 3: 히스토리 문서 (작업 이력)
- `/docs/hist/` - 날짜별 작업 이력 (Read-Only)
  - 중요한 기술 결정 사항 기록
  - 문제 해결 과정 문서화
  - 고정 문서에 반영할 내용 정리

---

## 📝 문서화 규칙 (2025-12-17 정립)

### 1. 고정 문서 vs 히스토리 문서

#### 고정 문서 (3개)
- **Spring_Batch_개발_가이드.md** - 아키텍처 및 개발 패턴
- **도메인_확장_가이드.md** - 도메인 추가 절차
- **동시성_제어.md** - 동시성 제어 전략

**특징:**
- 항상 최신 상태 유지
- 코드 변경 시 즉시 업데이트
- 참조는 이 3개 문서로만 진행

#### 히스토리 문서 (`/docs/hist/`)
- **2025-12-11_01_gRPC_Client_구현_및_통신_검증.md**
- **2025-12-12_01_도메인별_스키마_설계_및_Flyway_정책.md**
- **2025-12-16_01_Spring_Batch_6.0_마이그레이션_및_스케줄러_구현.md**
- ... (날짜별 추가)

**특징:**
- Read-Only (작성 후 수정 불가)
- 날짜별 작업 이력 기록
- `YYYY-MM-DD_NN_제목.md` 형식

### 2. 작업 프로세스

#### 새 기능 구현 시
1. **코드 작성 및 테스트**
2. **히스토리 문서 작성** (`/docs/hist/YYYY-MM-DD_NN_제목.md`)
   - 구현 내용
   - 기술 결정 사항
   - 문제 해결 과정
3. **고정 문서 업데이트** (필요 시)
   - 아키텍처 변경 → Spring_Batch_개발_가이드.md
   - 도메인 추가 절차 변경 → 도메인_확장_가이드.md
   - 동시성 패턴 추가 → 동시성_제어.md
4. **CLAUDE.md 업데이트** (구현 상태 반영)
5. **Commit**

#### 예시: Candidate Job 구현 완료 시
```bash
# 1. 히스토리 문서 작성
docs/hist/2025-12-18_01_Candidate_Job_구현_완료.md

# 2. 고정 문서 업데이트
Spring_Batch_개발_가이드.md
  - Section 6: Candidate Job 예시 추가
도메인_확장_가이드.md
  - Section 5: Candidate 구현 예시 업데이트

# 3. CLAUDE.md 업데이트
- "⏳ 구현 예정" → "✅ 완료된 기능"
- "최근 업데이트" 섹션에 날짜 + 내용 추가
```

### 3. 문서 참조 원칙

#### Good ✅
```markdown
자세한 내용은 [Spring Batch 개발 가이드](./Spring_Batch_개발_가이드.md)를 참조하세요.
```

#### Bad ❌
```markdown
자세한 내용은 [Batch 설계서](./Batch설계서.md)를 참조하세요.  # 삭제된 문서
자세한 내용은 [gRPC 통신 가이드](./gRPC_통신_가이드.md)를 참조하세요.  # 삭제된 문서
```

### 4. 히스토리 문서 작성 가이드

**파일명 규칙:**
```
YYYY-MM-DD_NN_간략한_제목.md
예: 2025-12-17_01_문서_구조_개선.md
```

**내용 구조:**
```markdown
# [제목]

**날짜:** YYYY-MM-DD
**작업 범위:** [간략 설명]

---

## 배경
[왜 이 작업을 했는가?]

## 구현 내용
[무엇을 구현했는가?]

## 기술 결정 사항
[어떤 선택을 했고, 왜 그렇게 했는가?]

## 문제 해결
[어떤 문제를 만났고, 어떻게 해결했는가?]

## 고정 문서 반영 사항
[어떤 고정 문서를 업데이트했는가?]
```

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
│   │   │   ├── CandidateEntity              # ✅ Candidate 기본 정보
│   │   │   ├── CandidateSkillEntity         # ✅ Candidate 스킬 (1:N)
│   │   │   ├── CandidateSkillId             # ✅ Composite PK (candidateId, skill)
│   │   │   └── CandidateSkillsEmbeddingEntity # ✅ Candidate 벡터 (768d)
│   │   └── repository/
│   │       ├── CandidateRepository          # ✅ Port (인터페이스)
│   │       ├── CandidateSkillRepository     # ✅ Port (인터페이스)
│   │       └── CandidateSkillsEmbeddingRepository # ✅ Port (인터페이스)
│   ├── skilldic/
│   │   ├── entity/
│   │   │   └── SkillEmbeddingDicEntity      # ✅ Skill Dictionary (String PK, 768d)
│   │   └── repository/
│   │       └── SkillEmbeddingDicRepository  # ✅ Port (인터페이스)
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
│       ├── CandidateJpaRepository               # ✅ Candidate 기본 정보 (2025-12-17)
│       ├── CandidateSkillJpaRepository          # ✅ Composite PK Upsert (2025-12-17)
│       ├── CandidateSkillsEmbeddingJpaRepository # ✅ PostgreSQL Array + Vector (2025-12-17)
│       ├── SkillEmbeddingDicJpaRepository       # ✅ String PK Upsert (2025-12-17)
│       ├── DlqJpaRepository
│       └── CheckpointJpaRepository
│
├── application/                     # 애플리케이션 계층 (Use Case)
│   ├── batch/
│   │   ├── dto/
│   │   │   ├── DomainItem<M, E>             # ✅ Metadata + Embedding 묶음
│   │   │   └── CandidateItem                # ✅ Candidate 전용 DTO (3-table split)
│   │   ├── reader/
│   │   │   ├── DomainItemReader<T>          # ✅ 추상 Reader (gRPC Stream → Queue)
│   │   │   └── RecruitItemReader            # ✅ Recruit 구현체
│   │   ├── processor/
│   │   │   ├── DomainItemProcessor<I,M,E>   # ✅ 추상 Processor (Proto → Entity)
│   │   │   ├── RecruitItemProcessor         # ✅ Recruit 구현체
│   │   │   └── CandidateItemProcessor       # ✅ Candidate 구현체 (2025-12-17)
│   │   └── writer/
│   │       ├── DomainItemWriter<M,E>        # ✅ Generic Writer (Batch Upsert)
│   │       └── CandidateItemWriter          # ✅ Candidate 전용 Writer (3-table split, 2025-12-17)
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

### 1. Candidate Job 통합 (Phase 1 & 2 완료, Phase 3 대기)
- ✅ **Phase 1: Repository Infrastructure** - 완료 (2025-12-17)
  - CandidateJpaRepository (Upsert with ON CONFLICT)
  - CandidateSkillJpaRepository (Composite PK Upsert)
  - CandidateSkillsEmbeddingJpaRepository (PostgreSQL Array + Vector)
- ✅ **Phase 2: Batch Processor/Writer** - 완료 (2025-12-17)
  - CandidateItem DTO (3-table aggregation)
  - CandidateItemProcessor (Proto → 3 Entities split)
  - CandidateItemWriter (Ordered Upsert: candidate → candidate_skill → candidate_skills_embedding)
- ⏳ **Phase 3: Job Integration** - 대기
  - CandidateItemReader (gRPC Stream → CandidateRow 변환)
  - DomainJobFactory에 candidateEmbeddingProcessingJob 추가
  - BatchSchedulerConfig에 Candidate Job 스케줄 추가

### 2. SkillEmbeddingDic Job
- ✅ **Phase 1: Repository Infrastructure** - 완료 (2025-12-17)
  - SkillEmbeddingDicJpaRepository (String PK Upsert)
- ⏳ **Phase 2: Batch Processor/Writer** - 구현 필요
  - SkillEmbeddingDicItemProcessor
  - SkillEmbeddingDicItemWriter
- ⏳ **Phase 3: Job Integration** - 구현 필요
  - DomainJobFactory에 skillEmbeddingDicProcessingJob 추가

### 3. gRPC Server (양방향 통신)
- ⏳ **IngestDataStream Server** - Python → Batch (Client Streaming 수신)
  - 현재: EmbeddingGrpcClient만 있음 (Batch → Python 요청)
  - 필요: gRPC Server 구현 (Python의 Client Streaming 수신)

### 4. Checkpoint 자동화
- ⏳ Writer에서 마지막 UUID 자동 저장
- ⏳ Job 재시작 시 자동 재개

### 5. Factory 패턴 고도화 (선택)
- ⏳ **ChunkProcessorFactory** - (테스트 코드만 존재)
- ⏳ **ChunkProcessorInterface** - (문서에만 언급됨)

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

### 5. Candidate 3-Table Split 패턴 ✅ (2025-12-17)

**목적:** 복잡한 도메인을 여러 테이블로 분산 저장

**배경:**
- Candidate 도메인은 DDD Aggregate 패턴으로 4개 테이블에 분산 저장
  - candidate (기본 정보)
  - candidate_skill (1:N 관계, Composite PK)
  - candidate_skills_embedding (벡터)
  - skill_embedding_dic (별도 도메인, String PK)

**구현 패턴:**
```java
// 1. Flat DTO for gRPC transmission (Proto)
message CandidateRow {
  string candidate_id = 1;
  string position_category = 2;
  int32 experience_years = 3;
  string original_resume = 4;
  repeated string skills = 5;           // Array
  repeated float skills_vector = 6;     // 768d
}

// 2. Aggregation DTO for processing (CandidateItem.java)
@Builder
public class CandidateItem {
    private CandidateEntity candidate;                  // candidate 테이블
    private List<CandidateSkillEntity> skills;          // candidate_skill 테이블 (1:N)
    private CandidateSkillsEmbeddingEntity embedding;   // candidate_skills_embedding 테이블
}

// 3. Processor: Proto → 3 Entities (CandidateItemProcessor.java)
public CandidateItem process(CandidateRow protoRow) {
    UUID candidateId = UUID.fromString(protoRow.getCandidateId());

    // 1:N 관계 처리
    List<CandidateSkillEntity> skills = protoRow.getSkillsList().stream()
        .map(skillName -> {
            CandidateSkillEntity skill = new CandidateSkillEntity();
            skill.setCandidateId(candidateId);
            skill.setSkill(skillName);
            return skill;
        })
        .collect(Collectors.toList());

    return CandidateItem.builder()
        .candidate(createCandidate(protoRow, candidateId))
        .skills(skills)
        .embedding(createEmbedding(protoRow, candidateId))
        .build();
}

// 4. Writer: Ordered Upsert (FK 제약 고려)
@Transactional
public void write(Chunk<? extends CandidateItem> chunk) {
    // 1. candidate 테이블 (PK 먼저)
    candidateRepository.upsertAll(candidates);

    // 2. candidate_skill 테이블 (FK → candidate)
    candidateSkillRepository.upsertAll(allSkills);

    // 3. candidate_skills_embedding 테이블 (FK → candidate)
    candidateSkillsEmbeddingRepository.upsertAll(embeddings);
}

// 5. Composite PK Upsert (CandidateSkillJpaRepository.java)
@Query(value = """
    INSERT INTO candidate_skill (candidate_id, skill, updated_at)
    VALUES (:#{#entity.candidateId}, :#{#entity.skill}, NOW())
    ON CONFLICT (candidate_id, skill)
    DO UPDATE SET updated_at = NOW()
    """, nativeQuery = true)
void upsert(@Param("entity") CandidateSkillEntity entity);
```

**핵심 포인트:**
1. **Flat DTO 전송** - gRPC는 Flat 구조 (성능 최적화)
2. **Aggregation 처리** - Processor에서 1:N 관계 분해
3. **Ordered Upsert** - FK 제약 조건 순서 보장
4. **Composite PK** - @IdClass 패턴으로 복합 키 처리
5. **PostgreSQL Array** - skills VARCHAR(50)[] 처리

### 6. Quartz Scheduler 패턴 ✅

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

### 2025-12-17 - 문서 구조 전면 개선 완료
- ✅ **문서화 규칙 정립** - 고정 문서 vs 히스토리 문서 개념 확립
  - 고정 문서 3개: Spring_Batch_개발_가이드.md, 도메인_확장_가이드.md, 동시성_제어.md
  - 히스토리 문서: `/docs/hist/` (날짜별 작업 이력)
  - 작업 프로세스 정의 (코드 작성 → 히스토리 문서 작성 → 고정 문서 업데이트)
- ✅ **obsolete 문서 9개 삭제**
  - Batch설계서.md, Entire_Structure.md, gRPC_클라이언트_구현.md
  - gRPC_통신_가이드.md, Reactive_Blocking_혼합전략.md
  - 구현_요약_2025-12-12.md, 도메인별_제네릭_구조_구현.md
  - 서비스_레이어_구현_가이드.md, 프로젝트_구조.md
- ✅ **고정 문서 3개 업데이트**
  - Spring_Batch_개발_가이드.md (새로 작성, 616 lines)
  - 도메인_확장_가이드.md (Spring Batch 패턴으로 전면 수정)
  - 동시성_제어.md (Section 2-7 Spring Batch 패턴으로 업데이트)
- ✅ **CLAUDE.md 업데이트**
  - 문서 계층 구조 섹션 확장 (고정 문서 3개 명시)
  - 문서화 규칙 섹션 추가 (작업 프로세스, 참조 원칙, 히스토리 가이드)
- **결과:** 11개 문서 (5,000+ 줄) → 3개 고정 문서 (~2,000 줄), 문서 중복 73% 제거

### 2025-12-17 - Candidate 도메인 Phase 1 & 2 구현 완료
- ✅ **Proto 파일 확장** - 3개 도메인 (Recruit, Candidate, SkillEmbeddingDic)
  - oneof chunk_data로 도메인 분기
  - CandidateRow - Flat DTO (skills 배열 포함)
  - SkillEmbeddingDicRow - String PK
- ✅ **Phase 1: Repository Infrastructure** - 4개 JpaRepository 구현
  - CandidateJpaRepository - ON CONFLICT (candidate_id) Upsert
  - CandidateSkillJpaRepository - Composite PK (candidate_id, skill) Upsert
  - CandidateSkillsEmbeddingJpaRepository - PostgreSQL Array + pgvector 처리
  - SkillEmbeddingDicJpaRepository - String PK Upsert
- ✅ **Phase 2: Batch Processor/Writer** - 3-table split 패턴 구현
  - CandidateItem DTO - Aggregation (candidate + skills + embedding)
  - CandidateItemProcessor - Proto → 3 Entities 변환, skills 배열 분해
  - CandidateItemWriter - Ordered Upsert (FK 제약 순서 보장)
- ✅ **Entity 설계** - DDD Aggregate 패턴
  - CandidateEntity - @AttributeOverride로 candidate_id 매핑
  - CandidateSkillEntity - @IdClass로 Composite PK 처리
  - CandidateSkillsEmbeddingEntity - PostgreSQL Array (String[]) + PGvector
  - SkillEmbeddingDicEntity - skill String PK
- ✅ **문서 업데이트** - CLAUDE.md 구조 섹션 반영

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

**최종 수정일:** 2025-12-17
**CLAUDE.md 업데이트 완료 ✅**
