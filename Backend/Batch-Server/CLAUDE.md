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

### 🆕 Backend 공통 문서 (2025-12-12 추가)
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md` 🗄️
- **Flyway 마이그레이션 가이드**: `/Backend/docs/Flyway_마이그레이션_가이드.md` 📦
- **ERD 다이어그램**: `/Backend/docs/ERD_다이어그램.md` 📊
- **도메인 확장 가이드**: `/docs/도메인_확장_가이드.md` ➕

### 🔧 기술 상세 문서
- **gRPC 통신 가이드**: `/docs/gRPC_통신_가이드.md` 🔌
- **Reactive + Blocking 혼합전략**: `/docs/Reactive_Blocking_혼합전략.md` ⚡
- **동시성 제어**: `/docs/동시성_제어.md` 🔐
- **서비스 레이어 구현 가이드**: `/docs/서비스_레이어_구현_가이드.md` 💡 (2025-12-12 추가)

### 📚 히스토리 문서
- **hist/**: 작업 과정, 의사결정, 변경 이력 (날짜별)

---

## 🚀 현재 진행 상황

### ✅ 완료
- gRPC proto 파일 (embedding_stream.proto, cache_service.proto)
- DB 스키마 (Flyway migration V1)
- application.yml 설정
- build.gradle 의존성 (pgvector 포함)
- Domain Entities (MetadataEntity, EmbeddingEntity, DlqEntity, CheckpointEntity)
- Repositories (JPA + Native Query for Upsert)
- Config 클래스 (BatchProperties, ExecutorConfig, GrpcClientConfig)
- gRPC Clients (EmbeddingGrpcClient, CacheInvalidateGrpcClient)
- **gRPC 통신 검증 완료** (2025-12-11)
  - Python Server와 통신 성공 (141,897 rows 수신)
  - Checkpoint 재개 기능 검증
- **서비스 레이어 구현 완료** (2025-12-12)
  - ChunkProcessor: RowChunk → DB 저장 (metadata + embedding 분리)
  - EmbeddingStreamingService: gRPC Stream → DB 파이프라인 (Reactive → Virtual Thread)
  - EmbeddingStreamRunner: 통합 테스트 자동 실행 (조건부 실행)
  - Vector 차원 검증 완료 (384)
  - 상세 로깅 구현 (스레드, 청크 사이즈, 마지막 UUID, 마지막 데이터)
  - 빌드 성공 확인
- **도메인별 DB 스키마 설계 및 Flyway 마이그레이션 정책 수립** (2025-12-12)
  - Flyway V2~V5 마이그레이션 파일 작성
    - V2: Candidate 스키마 (768 dimensions)
    - V3: Domain 컬럼 추가 (DLQ, Checkpoint 범용화)
    - V4: 성능 인덱스 추가
    - V5: 제약조건, 트리거, 헬퍼 함수
  - Base Entity 및 도메인별 Entity 설계 (recruit, candidate)
    - BaseMetadataEntity, BaseEmbeddingEntity (@MappedSuperclass)
    - RecruitMetadataEntity (384d), CandidateMetadataEntity (768d)
  - DLQ, Checkpoint 도메인 범용화
  - Backend 공통 문서 작성 (DB 스키마 가이드, Flyway 가이드, ERD, 도메인 확장 가이드)
- **Jackson 3 마이그레이션 완료** (2025-12-12)
  - Spring Boot 4.0+ 권장 사항 적용
  - ObjectMapper → JsonMapper 전환 (JacksonConfig)
  - RecruitDataProcessor, CandidateDataProcessor 업데이트
  - jackson-datatype-jsr310 의존성 추가
  - 빌드 성공 확인
- **도메인별 제네릭 프로세서 패턴 구현** (2025-12-12)
  - DataProcessor<T> 인터페이스 (Python의 DataLoader 패턴 매핑)
  - DataProcessorFactory (Spring Bean 자동 등록)
  - RecruitDataProcessor, CandidateDataProcessor 구현
  - JSON → Entity 변환 및 DB 저장 분리
- **테스트 코드 정리** (2025-12-12)
  - 제거: GrpcStreamTestService, GrpcTestRunner (테스트 전용)
  - 유지: EmbeddingStreamRunner (@ConditionalOnProperty 사용)
    - 실제 프로덕션 코드 테스트 (EmbeddingStreamingService)
    - 기본 비활성화 (grpc.test.enabled: true로 활성화)
- **계층별 커밋 완료** (2025-12-12)
  - 7개 레이어별 커밋: Config → Database → Domain → Backend Docs → Batch Docs
- **도메인별 리팩토링 완료** (2025-12-15)
  - BatchProperties: 도메인별 Map 구조 (Map<domain, DomainConfig>)
  - Base Entity 패턴 (BaseMetadataEntity, BaseEmbeddingEntity)
  - 도메인별 Entity/Repository (Recruit, Candidate)
  - ChunkProcessorInterface + Factory 패턴 (자동 Spring Bean 등록)
  - 각 도메인별 Native Query Upsert 구현
  - 도메인 확장성 향상 (새 도메인 추가 간소화)
- **Clean Architecture 리팩토링 완료** (2025-12-16)
  - Domain 계층과 Infrastructure 계층 분리 (Port & Adapter 패턴)
  - Domain Repository 인터페이스 정의 (비즈니스 로직 명세)
    - RecruitMetadataRepository, RecruitEmbeddingRepository
    - CandidateMetadataRepository, CandidateEmbeddingRepository
    - DlqRepository, CheckpointRepository
  - Infrastructure JpaRepository 구현체 생성 (기술 구현)
    - RecruitMetadataJpaRepository, RecruitEmbeddingJpaRepository
    - CandidateMetadataJpaRepository, CandidateEmbeddingJpaRepository
    - DlqJpaRepository, CheckpointJpaRepository
  - 도메인 디렉토리 구조 통일 (entity/, repository/)
  - Spring Data JPA 의존성을 Infrastructure로 격리
  - 빌드 성공 확인

### 🔄 진행 중
- 통합 테스트 (Python Server + Batch Server + PostgreSQL)

### ⏳ 예정
- DLQ 처리 로직 (우선순위: 높음)
- 캐시 무효화 통합 (CacheInvalidateGrpcClient 연동)
- Batch Configuration (Job, Step, Listener)
- BatchScheduler (Quartz 기반)

**상세 일정**: `/../../docs/개발_우선순위.md` 참조

---

## 📂 간단 구조 (Clean Architecture)

```
src/main/java/com/alpha/backend/
├── infrastructure/                # 인프라 계층 (Adapter)
│   ├── config/                    # 설정
│   │   ├── BatchProperties        # 도메인별 설정 (Map<domain, DomainConfig>)
│   │   ├── ExecutorConfig         # Virtual Thread Executor
│   │   ├── GrpcClientConfig       # gRPC 클라이언트 설정
│   │   └── JacksonConfig          # Jackson 설정
│   ├── grpc/                      # gRPC 클라이언트
│   │   └── client/
│   │       ├── EmbeddingGrpcClient        # Python AI Server 연동
│   │       └── CacheInvalidateGrpcClient  # API Server 캐시 무효화
│   └── persistence/               # JPA Repository 구현체 (Adapter)
│       ├── RecruitMetadataJpaRepository
│       ├── RecruitEmbeddingJpaRepository
│       ├── CandidateMetadataJpaRepository
│       ├── CandidateEmbeddingJpaRepository
│       ├── DlqJpaRepository
│       └── CheckpointJpaRepository
├── domain/                        # 도메인 계층 (Business Logic)
│   ├── common/                    # 공통 Base Entity
│   │   ├── BaseMetadataEntity     # 모든 Metadata Entity의 부모
│   │   └── BaseEmbeddingEntity    # 모든 Embedding Entity의 부모
│   ├── recruit/                   # Recruit 도메인
│   │   ├── entity/
│   │   │   ├── RecruitMetadataEntity
│   │   │   └── RecruitEmbeddingEntity
│   │   └── repository/            # Repository 인터페이스 (Port)
│   │       ├── RecruitMetadataRepository
│   │       └── RecruitEmbeddingRepository
│   ├── candidate/                 # Candidate 도메인
│   │   ├── entity/
│   │   │   ├── CandidateMetadataEntity
│   │   │   └── CandidateEmbeddingEntity
│   │   └── repository/            # Repository 인터페이스 (Port)
│   │       ├── CandidateMetadataRepository
│   │       └── CandidateEmbeddingRepository
│   ├── dlq/                       # DLQ (Dead Letter Queue)
│   │   ├── entity/
│   │   │   └── DlqEntity          # 도메인 범용화 완료
│   │   └── repository/            # Repository 인터페이스 (Port)
│   │       └── DlqRepository
│   └── checkpoint/                # Checkpoint
│       ├── entity/
│       │   └── CheckpointEntity   # 도메인 범용화 완료
│       └── repository/            # Repository 인터페이스 (Port)
│           └── CheckpointRepository
├── application/                   # 애플리케이션 계층 (Use Case)
│   ├── batch/
│   │   ├── dto/
│   │   ├── processor/
│   │   ├── reader/
│   │   └── writer/
│   └── usecase/
│       └── DlqServiceImpl
└── batch/                         # Spring Batch
    ├── job/
    │   └── BatchJobConfig
    └── listener/
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
    max-retry: 3                  # 재시도 횟수
    retry-backoff-ms: 1000        # 재시도 대기 시간 (밀리초)
    domains:                      # 도메인별 설정
      recruit:
        vector-dimension: 384     # Recruit Vector 차원
        table-prefix: recruit     # 테이블 접두사
      candidate:
        vector-dimension: 768     # Candidate Vector 차원
        table-prefix: candidate   # 테이블 접두사

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

#### 3.3 데이터 전송 (Python FastAPI 엔드포인트)
```bash
curl -X POST "http://localhost:8000/data/ingest/recruit?file_name=processed_recruitment_data.pkl"
```

#### 3.4 로그 확인
- **Python**: 스트리밍 진행 상황 (chunk 전송, row 수)
- **Batch**: Processor 선택, DB 저장 (스레드, 청크 사이즈, 마지막 UUID)
- **PostgreSQL**: 데이터 확인 (`recruit_metadata`, `recruit_embedding` 테이블)

**검증 완료 항목:**
- 141,897 rows 데이터 수신
- Checkpoint 재개 기능
- Vector 차원 검증 (384)

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

---

## 📚 핵심 패턴 및 설계

### 1. ChunkProcessor Factory 패턴

도메인별로 다른 처리 로직을 사용하기 위한 Factory 패턴 구현:

```java
// 1. 인터페이스 정의
public interface ChunkProcessorInterface {
    ChunkProcessingResult processChunk(RowChunk chunk);
    String getDomain();
}

// 2. 도메인별 구현체 (Spring Bean으로 자동 등록)
@Service
public class RecruitChunkProcessor implements ChunkProcessorInterface {
    public String getDomain() { return "recruit"; }
    // ...
}

// 3. Factory가 자동으로 모든 구현체를 Map으로 관리
@Component
public class ChunkProcessorFactory {
    private final Map<String, ChunkProcessorInterface> processorMap;

    public ChunkProcessorFactory(List<ChunkProcessorInterface> processors) {
        this.processorMap = processors.stream()
            .collect(Collectors.toMap(
                ChunkProcessorInterface::getDomain,
                Function.identity()
            ));
    }
}
```

**장점:**
- 새 도메인 추가 시 ChunkProcessorInterface 구현체만 작성하면 자동 등록
- 도메인별 처리 로직 분리 (단일 책임 원칙)
- 런타임에 도메인별 Processor 동적 선택

### 2. Base Entity 패턴

공통 필드를 Base Entity로 추출하여 중복 제거:

```java
// 공통 메타데이터 필드
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

// 도메인별 Entity는 Base를 상속
@Entity
@Table(name = "recruit_metadata")
public class RecruitMetadataEntity extends BaseMetadataEntity {
    // 도메인 특화 필드만 정의
    private String companyName;
    private Integer expYears;
}
```

### 3. BatchProperties 도메인별 설정

도메인마다 다른 설정을 Map 구조로 관리:

```java
@ConfigurationProperties(prefix = "batch.embedding")
public class BatchProperties {
    private Map<String, DomainConfig> domains = new HashMap<>();

    public DomainConfig getDomainConfig(String domain) {
        return domains.getOrDefault(domain, getDefaultDomainConfig());
    }

    public static class DomainConfig {
        private int vectorDimension;    // 도메인별 Vector 차원
        private String tablePrefix;      // 도메인별 테이블 접두사
    }
}
```

**장점:**
- 도메인별 설정 중앙 관리
- YAML 파일에서 직관적으로 설정 가능
- 존재하지 않는 도메인은 기본값 반환 (Fail-safe)

---

**최종 수정일:** 2025-12-16 (Clean Architecture 리팩토링 완료)