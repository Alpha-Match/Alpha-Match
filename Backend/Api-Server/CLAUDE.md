# Api-Server - Claude Instructions

**역할:** GraphQL API 제공 → Frontend 요청 처리 + pgvector 검색 + 멀티 레이어 캐싱
**기술 스택:** Spring Boot 4.0 + Spring WebFlux + GraphQL + R2DBC + pgvector

---

## 📋 문서 목적

- **CLAUDE.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명

---

## 🗺️ 핵심 문서 경로

### 필수 참조
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **GraphQL API 개발 가이드**: `docs/GraphQL_API_개발_가이드.md` ⭐
- **캐싱 전략 가이드**: `docs/캐싱_전략_가이드.md`
- **Reactive 프로그래밍 가이드**: `docs/Reactive_프로그래밍_가이드.md`

### Backend 공통 (DB 작업 시 필수)
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md`
- **테이블 명세서**: `/Backend/docs/table_specification.md` ⭐ Single Source of Truth
- **ERD 다이어그램**: `/Backend/docs/ERD_다이어그램.md`

---

## 📂 구현된 코드 위치 (AI가 읽어야 할 경로)

### ⚙️ Configuration

**GraphQL:**
- `src/main/java/com/alpha/api/config/GraphQLConfig.java` - GraphQL 설정
- `src/main/resources/graphql/schema.graphqls` - GraphQL Schema 정의

**Cache:**
- `src/main/java/com/alpha/api/config/CacheConfig.java` - Caffeine + Redis 설정

**Database:**
- `src/main/java/com/alpha/api/config/R2dbcConfig.java` - R2DBC 설정
- `src/main/java/com/alpha/api/config/R2dbcCustomConversions.java` - PGvector 타입 변환

**gRPC:**
- `src/main/java/com/alpha/api/config/grpc/GrpcClientConfig.java` - AI Backend 연동
- `src/main/proto/cache_service.proto` - Cache 서비스 Proto 정의

### 📦 Domain Layer (비즈니스 핵심)

**Recruit 도메인:**
- `src/main/java/com/alpha/api/domain/recruit/entity/Recruit.java` - Domain Model
- `src/main/java/com/alpha/api/domain/recruit/entity/RecruitDescription.java`
- `src/main/java/com/alpha/api/domain/recruit/entity/RecruitSkillsEmbedding.java`
- `src/main/java/com/alpha/api/domain/recruit/repository/RecruitRepository.java` - Port Interface (기본 CRUD)
- `src/main/java/com/alpha/api/domain/recruit/repository/RecruitSearchRepository.java` - Port Interface (벡터 검색)

**Candidate 도메인:**
- `src/main/java/com/alpha/api/domain/candidate/entity/Candidate.java`
- `src/main/java/com/alpha/api/domain/candidate/entity/CandidateDescription.java`
- `src/main/java/com/alpha/api/domain/candidate/entity/CandidateSkillsEmbedding.java`
- `src/main/java/com/alpha/api/domain/candidate/repository/CandidateRepository.java` - Port Interface (기본 CRUD)
- `src/main/java/com/alpha/api/domain/candidate/repository/CandidateSearchRepository.java` - Port Interface (벡터 검색)

**Skill Dictionary 도메인:**
- `src/main/java/com/alpha/api/domain/skilldic/entity/SkillCategoryDic.java`
- `src/main/java/com/alpha/api/domain/skilldic/entity/SkillEmbeddingDic.java`
- `src/main/java/com/alpha/api/domain/skilldic/repository/SkillEmbeddingDicRepository.java`
- `src/main/java/com/alpha/api/domain/skilldic/service/SkillNormalizationService.java` - Domain Service (스킬 정규화, 벡터 계산)

**Cache Port:**
- `src/main/java/com/alpha/api/domain/cache/port/CachePort.java` - Port Interface (캐싱 추상화)

**Common:**
- `src/main/java/com/alpha/api/domain/common/SkillCount.java` - Value Object

### 🎯 Application Layer (Use Case 구현)

**DTO:**
- `src/main/java/com/alpha/api/application/dto/RecruitSearchResult.java`
- `src/main/java/com/alpha/api/application/dto/CandidateSearchResult.java`

**Application Services:**
- `src/main/java/com/alpha/api/application/service/SearchService.java` - 검색 Use Case (스킬 기반 매칭)
- `src/main/java/com/alpha/api/application/service/DashboardService.java` - 대시보드 Use Case (통계 생성)
- `src/main/java/com/alpha/api/application/service/CacheService.java` - 캐싱 Use Case (Multi-layer Cache 관리)

### 🏗️ Infrastructure Layer (기술 구현)

**Persistence (R2DBC):**
- `src/main/java/com/alpha/api/infrastructure/persistence/RecruitR2dbcRepository.java` - RecruitRepository 구현
- `src/main/java/com/alpha/api/infrastructure/persistence/RecruitCustomRepositoryImpl.java` - RecruitSearchRepository 구현
- `src/main/java/com/alpha/api/infrastructure/persistence/CandidateR2dbcRepository.java` - CandidateRepository 구현
- `src/main/java/com/alpha/api/infrastructure/persistence/CandidateCustomRepositoryImpl.java` - CandidateSearchRepository 구현
- `src/main/java/com/alpha/api/infrastructure/persistence/SkillEmbeddingDicR2dbcRepository.java`

**GraphQL (Input Adapter):**
- `src/main/java/com/alpha/api/infrastructure/graphql/resolver/QueryResolver.java` - Query Resolver
- `src/main/java/com/alpha/api/infrastructure/graphql/type/` - GraphQL 타입 정의
- `src/main/java/com/alpha/api/infrastructure/graphql/input/` - GraphQL Input 타입

**Cache (Output Adapter):**
- `src/main/java/com/alpha/api/infrastructure/cache/CaffeineCacheAdapter.java` - CachePort 구현 (L1)
- `src/main/java/com/alpha/api/infrastructure/cache/RedisCacheAdapter.java` - CachePort 구현 (L2)

**gRPC Server (Input Adapter - 캐시 무효화 수신):**
- `src/main/java/com/alpha/api/infrastructure/grpc/server/CacheInvalidateServiceImpl.java` - Batch Server로부터 수신

**Configuration (Framework 설정):**
- `src/main/java/com/alpha/api/infrastructure/config/CacheConfig.java` - Caffeine + Redis 설정
- `src/main/java/com/alpha/api/infrastructure/config/CorsConfig.java` - CORS 설정
- `src/main/java/com/alpha/api/infrastructure/config/R2dbcConfig.java` - R2DBC + PGvector 설정

### 📋 설정 파일

- `src/main/resources/application.yml` - 메인 설정
- `src/main/resources/application-cache.yml` - 캐싱 설정
- `build.gradle` - 의존성 및 protobuf 플러그인

---

## 🚀 현재 구현 상태

### ✅ 완료
- **Spring Boot 프로젝트 초기 설정** (build.gradle, R2DBC, Redis, gRPC)
- **Entity 구현** (Recruit, Candidate, Skill Dictionary - 9개 엔티티)
- **R2DBC Repository 구현** (pgvector 쿼리 포함)
  - Port 인터페이스 (Domain Layer): RecruitRepository, RecruitSearchRepository, CandidateRepository, CandidateSearchRepository
  - Adapter 구현 (Infrastructure Layer): R2dbcRepository, CustomRepositoryImpl
- **Service Layer 구현** (Reactive Mono/Flux)
  - SearchService (검색 통합)
  - SkillNormalizationService (스킬 정규화)
  - DashboardService (통계)
- **GraphQL Schema 설계** (schema.graphqls - 7개 쿼리, 3개 뮤테이션)
- **GraphQL Resolver 구현** (QueryResolver)
- **Multi-layer Caching 시스템** (2025-12-29)
  - CachePort (Domain Layer) + CacheService
  - CaffeineCacheAdapter (L1) + RedisCacheAdapter (L2)
  - ObjectMapper Bean 추가 (Jackson serialization)
- **Postman 컬렉션** (7개 테스트 쿼리 + 성능 측정 스크립트)
- **Clean Architecture 전면 리팩토링** (2025-12-29)
  - 3-Layer 원칙 적용 (Domain → Application → Infrastructure)
  - 총 16개 파일 이동 (Services 3개, GraphQL 10개, Config 3개)
  - 의존성 방향 검증 완료
  - Gradle Build 성공 (29s, 9 tasks)
  - 히스토리 문서: `docs/hist/2025-12-29_02_Complete_Clean_Architecture_Refactoring.md`
- **Caffeine 캐시 성능 테스트** (2025-12-29) ✅
  - Cold Start (DB): 338.98ms
  - Warm Cache (L1): 26.36ms
  - Speedup: 12.9x faster (92.2% improvement)
  - TTL 10초 정확히 작동
  - 히스토리 문서: `docs/hist/2025-12-29_03_Caffeine_Cache_Performance_Test.md`

### 🔄 진행 중
- 없음

### ⏳ 예정 (우선순위 순)
1. **TTL 최적화** (getSkillCategories: 10s → 60s, Dashboard: 30s)
2. **Dashboard 캐싱 적용** (getDashboardData)
3. **Redis L2 캐시 연동 및 성능 테스트**
4. **gRPC Server 구현** (캐시 무효화 수신)
5. **GraphQL Mutation 구현** (캐시 무효화 API)
6. **부하 테스트** (동시 요청 100/1000/10000)

---

## 🔧 시스템 구성 요소

| 서버 | 기술 스택 | 포트 | 역할 |
|-----|---------|-----|-----|
| **Api-Server** | Spring WebFlux + GraphQL | 8080, 50052 | GraphQL API, 캐싱, gRPC |
| **Demo-Python** | Python + gRPC | 50051 | Embedding 스트리밍 |
| **Batch-Server** | Spring Batch | N/A, 9090 | Embedding 수신/저장 |
| **PostgreSQL** | pgvector | 5432 | Vector DB |
| **Redis** | - | 6379 | 분산 캐싱 |

---

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 개발 시 금지 사항
- **Blocking 코드 금지**: WebFlux 환경에서 `.block()` 사용 절대 금지
- **DB 작업 전 table_specification.md 확인 필수**: 임의로 스키마 추정 금지
- **synchronized 사용 최소화**: Reactive 환경에서 성능 저하 발생 가능

### 2. Reactive Programming 패턴
- **Mono**: 단일 결과 (findById, save)
- **Flux**: 다중 결과 (findAll, search)
- **flatMap/map/switchIfEmpty**: 체이닝 필수

### 3. GraphQL Schema 설계
- `schema.graphqls`에 먼저 정의
- Type, Query, Mutation 명확히 분리
- DB 스키마와 매핑 (table_specification.md 참조)

### 4. 캐싱 전략
- **L1 (Caffeine)**: In-memory, 10초 TTL, 빠른 조회
- **L2 (Redis)**: 분산 캐싱, 10분 TTL, 여러 인스턴스 공유
- **캐시 키 설계**: `{domain}:{id}` 형식 (예: `recruit:uuid`)
- **캐시 무효화**: Batch 작업 완료 시 gRPC로 수신

### 5. pgvector 쿼리
- `<->` 연산자: L2 거리 (Euclidean)
- `<=>` 연산자: Cosine 거리
- **CAST 필수**: `CAST(:vector AS vector)`
- **ORDER BY + LIMIT**: 성능 최적화

### 6. 스킬 정규화 플로우
```
입력: 기술 스택 List (예: ["Java", "Python", "C"])
  ↓
skill_embedding_dic에서 각 스킬 벡터 조회
  ↓
쿼리 벡터 생성 (벡터 평균/합산)
  ↓
{domain}_skills_embedding에서 유사도 검색
  ↓
상위 N개 결과 반환 (유사도 0.7 이상)
```

### 7. 동시성 제어
- 캐시 무효화: `synchronized` 또는 `ReentrantLock` 사용
- Race Condition 주의

---

## 📚 참고할 Batch Server 패턴

Api-Server 구현 시 Batch-Server의 다음 패턴을 참조:
- **Clean Architecture**: Domain/Infrastructure 분리
- **Repository Pattern**: Interface + Adapter 구현
- **Configuration 분리**: Config 패키지 구조
- **PGvector 타입 변환**: Custom Converter 구현

---

## 🛠️ 빠른 시작

### 사전 요구사항
- **Java** 21+
- **PostgreSQL** 15+ (pgvector)
- **Redis** 7+
- **Gradle** 8+

### Api Server 실행 (구현 후)
```bash
cd Backend/Api-Server
./gradlew bootRun
```

### GraphiQL 접속
http://localhost:8080/graphiql

---

---

## 📜 히스토리 문서

### 2025-12-29
- **아키텍처 리팩토링 및 캐싱 구현**: `docs/hist/2025-12-29_01_Architecture_Refactoring_and_Caching_Implementation.md`
  - Clean Architecture 적용 (Port-Adapter 패턴)
  - Repository 계층 분리 (RecruitSearchRepository, CandidateSearchRepository)
  - Multi-layer Caching 시스템 구현 (CacheService + Caffeine/Redis Adapter)
  - Postman 컬렉션 및 성능 테스트 스크립트 작성
- **Clean Architecture 전면 리팩토링**: `docs/hist/2025-12-29_02_Complete_Clean_Architecture_Refactoring.md`
  - 3-Layer 분리 (Domain → Application → Infrastructure)
  - 16개 파일 이동 및 의존성 방향 검증
- **Caffeine 캐시 성능 테스트**: `docs/hist/2025-12-29_03_Caffeine_Cache_Performance_Test.md`
  - L1 캐시 성능 측정 (12.9x speedup)
  - ObjectMapper Bean 추가

---

**최종 수정일:** 2025-12-29 (Caffeine 캐시 성능 테스트 완료)
