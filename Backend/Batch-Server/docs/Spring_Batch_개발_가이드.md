# Spring Batch 개발 가이드

**작성일**: 2025-12-17
**대상**: Batch-Server
**목적**: Spring Batch 6.0 기반 개발 패턴 및 실제 구현 가이드

---

## 📋 개요

Batch-Server는 **Spring Batch 6.0 + Java 21 + Virtual Thread**를 사용하여 Python AI Server로부터 Embedding 데이터를 수신하고 PostgreSQL(pgvector)에 저장합니다.

### 핵심 아키텍처

```
Python AI Server (gRPC Stream)
    ↓
Spring Batch Job
    ├─ ItemReader: gRPC Stream → Queue → Item
    ├─ ItemProcessor: Proto → Entity 변환
    └─ ItemWriter: Batch Upsert (Metadata → Embedding)
    ↓
PostgreSQL(pgvector) + DLQ + Checkpoint
```

---

## 🏗️ Spring Batch 6.0 핵심 패턴

### 1. ItemReader/Processor/Writer 패턴

#### ItemReader
**역할**: 데이터 소스로부터 데이터를 읽어 Item 단위로 반환

```java
@Component
public class RecruitItemReader extends DomainItemReader<RecruitRow> {

    private final EmbeddingGrpcClient grpcClient;
    private final CheckpointRepository checkpointRepository;

    @Override
    protected Flux<RecruitRow> createStream(UUID lastProcessedUuid) {
        int chunkSize = batchProperties.getChunkSize();

        return grpcClient.streamEmbeddings(lastProcessedUuid, chunkSize)
            .flatMapIterable(RowChunk::getRowsList)
            .map(row -> row.getRecruitChunk())
            .filter(Objects::nonNull);
    }
}
```

**DomainItemReader 추상 클래스**:
```java
public abstract class DomainItemReader<T> implements ItemReader<T> {

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>(1000);
    private volatile boolean streamCompleted = false;

    @PostConstruct
    public void init() {
        UUID lastProcessedUuid = getLastCheckpoint();

        createStream(lastProcessedUuid)
            .doOnNext(queue::offer)
            .doOnComplete(() -> streamCompleted = true)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }

    @Override
    public T read() throws Exception {
        T item = queue.poll(100, TimeUnit.MILLISECONDS);

        if (item == null && streamCompleted && queue.isEmpty()) {
            return null; // Stream 종료
        }

        return item;
    }

    protected abstract Flux<T> createStream(UUID lastProcessedUuid);
}
```

#### ItemProcessor
**역할**: Item을 변환하여 Entity로 반환

```java
@Component
public class RecruitItemProcessor extends DomainItemProcessor<RecruitRow, RecruitMetadataEntity, RecruitEmbeddingEntity> {

    @Override
    public DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity> process(RecruitRow item) {
        UUID id = UUID.fromString(item.getId());

        // Metadata Entity
        RecruitMetadataEntity metadata = new RecruitMetadataEntity();
        metadata.setId(id);
        metadata.setCompanyName(item.getCompanyName());
        metadata.setExpYears(item.getExpYears());
        metadata.setEnglishLevel(item.getEnglishLevel());
        metadata.setPrimaryKeyword(item.getPrimaryKeyword());

        // Embedding Entity
        float[] vectorArray = convertToFloatArray(item.getVectorList());
        validateVectorDimension(vectorArray, id);

        RecruitEmbeddingEntity embedding = RecruitEmbeddingEntity.fromFloatArray(id, vectorArray);

        return new DomainItem<>(metadata, embedding);
    }

    private void validateVectorDimension(float[] vector, UUID id) {
        int expectedDim = batchProperties.getDomainConfig("recruit").getVectorDimension();
        if (vector.length != expectedDim) {
            throw new IllegalArgumentException(
                String.format("Vector dimension mismatch for UUID %s: expected=%d, actual=%d",
                    id, expectedDim, vector.length)
            );
        }
    }
}
```

**DomainItemProcessor 추상 클래스**:
```java
public abstract class DomainItemProcessor<I, M extends BaseMetadataEntity, E extends BaseEmbeddingEntity>
        implements ItemProcessor<I, DomainItem<M, E>> {

    @Autowired
    protected BatchProperties batchProperties;

    protected float[] convertToFloatArray(List<Float> vectorList) {
        float[] array = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            array[i] = vectorList.get(i);
        }
        return array;
    }
}
```

#### ItemWriter
**역할**: Entity를 DB에 Batch Upsert

```java
@Component
public class DomainItemWriter<M extends BaseMetadataEntity, E extends BaseEmbeddingEntity>
        implements ItemWriter<DomainItem<M, E>> {

    private final JpaRepository<M, UUID> metadataRepository;
    private final JpaRepository<E, UUID> embeddingRepository;
    private final DlqService dlqService;

    @Override
    @Transactional
    public void write(Chunk<? extends DomainItem<M, E>> chunk) throws Exception {
        List<DomainItem<M, E>> items = chunk.getItems();

        List<M> successMetadata = new ArrayList<>();
        List<E> successEmbedding = new ArrayList<>();

        // 1. 개별 item 처리 (실패 시 DLQ)
        for (DomainItem<M, E> item : items) {
            try {
                successMetadata.add(item.getMetadata());
                successEmbedding.add(item.getEmbedding());
            } catch (Exception e) {
                log.error("Failed to process item: {}", item.getMetadata().getId(), e);
                dlqService.saveToDlq(getDomain(), item.getMetadata().getId(), e.getMessage(), toJson(item));
            }
        }

        // 2. Batch Upsert (순서 중요: metadata → embedding)
        if (!successMetadata.isEmpty()) {
            metadataUpsertFunction.upsertAll(successMetadata);
            embeddingUpsertFunction.upsertAll(successEmbedding);
        }
    }
}
```

**DomainItem DTO**:
```java
@Getter
@AllArgsConstructor
public class DomainItem<M extends BaseMetadataEntity, E extends BaseEmbeddingEntity> {
    private final M metadata;
    private final E embedding;
}
```

---

### 2. DomainJobFactory (Factory Method 패턴)

**목적**: 도메인별 Job/Step을 동적으로 생성

```java
@Component
@RequiredArgsConstructor
public class DomainJobFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties batchProperties;

    // Reader/Processor/Writer Beans
    private final RecruitItemReader recruitItemReader;
    private final RecruitItemProcessor recruitItemProcessor;
    private final ApplicationContext applicationContext;

    /**
     * 도메인별 Job 생성
     */
    public Job createJob(String domain) {
        return switch (domain.toLowerCase()) {
            case "recruit" -> createRecruitJob();
            case "candidate" -> createCandidateJob();
            default -> throw new IllegalArgumentException("Unsupported domain: " + domain);
        };
    }

    private Job createRecruitJob() {
        return new JobBuilder("recruitEmbeddingProcessingJob", jobRepository)
                .listener(embeddingJobListener)
                .start(createRecruitStep())
                .build();
    }

    private Step createRecruitStep() {
        int chunkSize = batchProperties.getChunkSize();

        // Generic Writer 생성
        DomainItemWriter<RecruitMetadataEntity, RecruitEmbeddingEntity> writer =
            new DomainItemWriter<>(
                "recruit",
                recruitMetadataRepository,
                recruitEmbeddingRepository,
                metadataRepository::upsertAll,
                embeddingRepository::upsertAll,
                dlqService
            );

        return new StepBuilder("recruitEmbeddingStep", jobRepository)
                .<RecruitRow, DomainItem<RecruitMetadataEntity, RecruitEmbeddingEntity>>chunk(chunkSize, transactionManager)
                .reader(recruitItemReader)
                .processor(recruitItemProcessor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .listener(embeddingStepListener)
                .build();
    }
}
```

**BatchJobConfig에서 사용**:
```java
@Configuration
public class BatchJobConfig {

    private final DomainJobFactory domainJobFactory;

    @Bean
    public Job recruitEmbeddingProcessingJob() {
        return domainJobFactory.createJob("recruit");
    }

    @Bean
    public Job candidateEmbeddingProcessingJob() {
        return domainJobFactory.createJob("candidate");
    }
}
```

---

### 3. Quartz Scheduler 통합

#### QuartzConfig
```java
@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setQuartzProperties(quartzProperties());
        factory.setJobFactory(springBeanJobFactory());
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setAutoStartup(true);
        return factory;
    }

    private Properties quartzProperties() {
        Properties properties = new Properties();

        // JDBC JobStore
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        properties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        properties.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");

        // ThreadPool
        properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        properties.setProperty("org.quartz.threadPool.threadCount", "10");

        // Misfire
        properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");

        return properties;
    }
}
```

#### BatchSchedulerConfig
```java
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
     * Spring Batch 6.0 패턴: JobRegistry + JobOperator
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

                log.info("Job Started | ExecutionId={}, Status={}", execution.getId(), execution.getStatus());

            } catch (JobExecutionAlreadyRunningException | JobRestartException |
                     JobInstanceAlreadyCompleteException | InvalidJobParametersException e) {
                throw new JobExecutionException("Job execution failed", e);
            }
        }
    }
}
```

**application.yml 설정**:
```yaml
batch:
  scheduler:
    enabled: true
    jobs:
      recruit:
        cron: "0 0 2 * * ?"  # 매일 새벽 2시
        enabled: true
      candidate:
        cron: "0 30 2 * * ?"  # 매일 새벽 2시 30분
        enabled: false
```

---

## 🔧 Virtual Thread 사용

### ExecutorConfig
```java
@Configuration
public class ExecutorConfig {

    /**
     * Virtual Thread Executor (Java 21)
     * JPA 등 Blocking I/O 작업용
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

### 사용 예시 (ItemReader 내부)
```java
public abstract class DomainItemReader<T> implements ItemReader<T> {

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    @PostConstruct
    public void init() {
        createStream(lastProcessedUuid)
            .doOnNext(queue::offer)
            .subscribeOn(Schedulers.fromExecutor(virtualThreadExecutor))
            .subscribe();
    }
}
```

### Virtual Thread 주의사항

#### ⚠️ synchronized 사용 금지 (Pinning 발생)
```java
// Bad: synchronized block (Carrier Thread Pinning)
synchronized(lock) {
    repository.save(entity);
}

// Good: ReentrantLock 사용
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    repository.save(entity);
} finally {
    lock.unlock();
}
```

#### ⚠️ Connection Pool 고갈 방지
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Virtual Thread 수에 맞게 조정
```

---

## 📡 gRPC 통신

### Proto 파일 정의

**embedding_stream.proto**:
```protobuf
syntax = "proto3";

package embedding;

option java_multiple_files = true;
option java_package = "com.alpha.backend.grpc.proto";

service EmbeddingStreamService {
  rpc StreamEmbedding(StreamEmbeddingRequest) returns (stream RowChunk);
}

message StreamEmbeddingRequest {
  string last_processed_uuid = 1;  // Checkpoint UUID
  int32 chunk_size = 2;             // Chunk 크기
}

message RowChunk {
  oneof chunk_data {
    RecruitRowChunk recruit_chunk = 1;
    CandidateRowChunk candidate_chunk = 2;
    SkillEmbeddingDicRowChunk skill_embedding_dic_chunk = 3;
  }
}

message RecruitRowChunk {
  repeated RecruitRow rows = 1;
}

message RecruitRow {
  string id = 1;                    // UUID
  string company_name = 2;
  int32 exp_years = 3;
  string english_level = 4;
  string primary_keyword = 5;
  repeated float vector = 6;        // 384d
}

message CandidateRowChunk {
  repeated CandidateRow rows = 1;
}

message CandidateRow {
  string candidate_id = 1;          // UUID
  string position_category = 2;
  int32 experience_years = 3;
  string original_resume = 4;
  repeated string skills = 5;       // Array
  repeated float skills_vector = 6; // 768d
}

message SkillEmbeddingDicRowChunk {
  repeated SkillEmbeddingDicRow rows = 1;
}

message SkillEmbeddingDicRow {
  string skill = 1;                 // String PK
  repeated float vector = 2;        // 768d
}
```

### EmbeddingGrpcClient
```java
@Component
@Slf4j
public class EmbeddingGrpcClient {

    private final ManagedChannel channel;
    private final EmbeddingStreamServiceGrpc.EmbeddingStreamServiceStub asyncStub;

    public Flux<RowChunk> streamEmbeddings(UUID lastProcessedUuid, int chunkSize) {
        Sinks.Many<RowChunk> sink = Sinks.many().unicast().onBackpressureBuffer();

        StreamEmbeddingRequest.Builder requestBuilder = StreamEmbeddingRequest.newBuilder()
                .setChunkSize(chunkSize);

        if (lastProcessedUuid != null) {
            requestBuilder.setLastProcessedUuid(lastProcessedUuid.toString());
        }

        asyncStub.streamEmbedding(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(RowChunk chunk) {
                sink.tryEmitNext(chunk);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Error in embedding stream: {}", throwable.getMessage());
                sink.tryEmitError(throwable);
            }

            @Override
            public void onCompleted() {
                log.info("Embedding stream completed");
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }
}
```

### CacheInvalidateGrpcClient
```java
@Component
@Slf4j
public class CacheInvalidateGrpcClient {

    private final ManagedChannel channel;
    private final CacheServiceGrpc.CacheServiceBlockingStub blockingStub;
    private final AtomicBoolean invalidating = new AtomicBoolean(false);

    /**
     * 캐시 무효화 요청 (동시성 제어)
     */
    public Mono<Boolean> invalidateCache(String target) {
        return Mono.defer(() -> {
            // CAS로 중복 호출 방지
            if (!invalidating.compareAndSet(false, true)) {
                log.warn("Cache invalidation already in progress, skipping");
                return Mono.just(false);
            }

            try {
                CacheInvalidateRequest request = CacheInvalidateRequest.newBuilder()
                        .setTarget(target)
                        .build();

                CacheInvalidateResponse response = blockingStub
                        .withDeadlineAfter(10, TimeUnit.SECONDS)
                        .invalidateCache(request);

                return Mono.just(response.getSuccess());

            } catch (StatusRuntimeException e) {
                log.error("gRPC error during cache invalidation: {}", e.getStatus());
                return Mono.error(e);
            } finally {
                invalidating.set(false);
            }
        })
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }
}
```

---

## 🎯 실제 구현 예시

### Recruit 도메인 전체 플로우

#### 1. Entity
```java
@Entity
@Table(name = "recruit_metadata")
public class RecruitMetadataEntity extends BaseMetadataEntity {
    private String companyName;
    private Integer expYears;
    private String englishLevel;
    private String primaryKeyword;
}

@Entity
@Table(name = "recruit_embedding")
public class RecruitEmbeddingEntity extends BaseEmbeddingEntity {
    public static final int VECTOR_DIMENSION = 384;

    public static RecruitEmbeddingEntity fromFloatArray(UUID id, float[] vectorArray) {
        if (vectorArray.length != VECTOR_DIMENSION) {
            throw new IllegalArgumentException("Vector dimension mismatch");
        }

        RecruitEmbeddingEntity entity = new RecruitEmbeddingEntity();
        entity.setId(id);
        entity.setVector(new PGvector(vectorArray));
        return entity;
    }
}
```

#### 2. Repository (Upsert)
```java
public interface RecruitMetadataRepository extends JpaRepository<RecruitMetadataEntity, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO recruit_metadata (id, company_name, exp_years, english_level, primary_keyword, created_at, updated_at)
        VALUES (:#{#entity.id}, :#{#entity.companyName}, :#{#entity.expYears}, :#{#entity.englishLevel}, :#{#entity.primaryKeyword}, :#{#entity.createdAt}, :#{#entity.updatedAt})
        ON CONFLICT (id) DO UPDATE SET
            company_name = EXCLUDED.company_name,
            exp_years = EXCLUDED.exp_years,
            english_level = EXCLUDED.english_level,
            primary_keyword = EXCLUDED.primary_keyword,
            updated_at = EXCLUDED.updated_at
        """, nativeQuery = true)
    void upsert(@Param("entity") RecruitMetadataEntity entity);

    default void upsertAll(List<RecruitMetadataEntity> entities) {
        for (RecruitMetadataEntity entity : entities) {
            upsert(entity);
        }
    }
}
```

#### 3. Job 등록
```java
@Configuration
public class BatchJobConfig {

    @Bean
    public Job recruitEmbeddingProcessingJob(DomainJobFactory factory) {
        return factory.createJob("recruit");
    }
}
```

#### 4. Scheduler 설정
```yaml
batch:
  scheduler:
    enabled: true
    jobs:
      recruit:
        cron: "0 0 2 * * ?"
        enabled: true
```

---

## 📊 성능 최적화

### 1. Chunk Size 조정
```yaml
batch:
  embedding:
    chunk-size: 300  # 100-500 사이 조정
```

### 2. HikariCP 최적화
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 3. JPA Batch Size
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 300  # Chunk Size와 동일
```

---

## 📚 참고 문서

- **도메인 확장 가이드**: `/docs/도메인_확장_가이드.md`
- **동시성 제어 가이드**: `/docs/동시성_제어_가이드.md`
- **Backend 공통 문서**:
  - `/Backend/docs/DB_스키마_가이드.md`
  - `/Backend/docs/table_specification.md`
  - `/Backend/docs/Flyway_마이그레이션_가이드.md`

---

**최종 수정일**: 2025-12-17
