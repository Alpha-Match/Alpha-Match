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

### 🎯 GraphQL Layer

**Resolver:**
- `src/main/java/com/alpha/api/graphql/resolver/QueryResolver.java` - Query 처리
- `src/main/java/com/alpha/api/graphql/resolver/MutationResolver.java` - Mutation 처리

**Type:**
- `src/main/java/com/alpha/api/graphql/type/RecruitType.java` - GraphQL 타입
- `src/main/java/com/alpha/api/graphql/type/CandidateType.java`
- `src/main/java/com/alpha/api/graphql/input/SearchInput.java` - Input 타입

### 📦 Domain Layer (Port)

**Recruit 도메인:**
- `src/main/java/com/alpha/api/domain/recruit/entity/Recruit.java` - Domain Model
- `src/main/java/com/alpha/api/domain/recruit/entity/RecruitDescription.java`
- `src/main/java/com/alpha/api/domain/recruit/entity/RecruitSkillsEmbedding.java`
- `src/main/java/com/alpha/api/domain/recruit/repository/RecruitRepository.java` - Port Interface
- `src/main/java/com/alpha/api/domain/recruit/service/RecruitService.java` - Service Layer

**Candidate 도메인:**
- `src/main/java/com/alpha/api/domain/candidate/entity/Candidate.java`
- `src/main/java/com/alpha/api/domain/candidate/entity/CandidateDescription.java`
- `src/main/java/com/alpha/api/domain/candidate/entity/CandidateSkillsEmbedding.java`
- `src/main/java/com/alpha/api/domain/candidate/repository/CandidateRepository.java`
- `src/main/java/com/alpha/api/domain/candidate/service/CandidateService.java`

**Skill Dictionary 도메인:**
- `src/main/java/com/alpha/api/domain/skilldic/entity/SkillCategoryDic.java`
- `src/main/java/com/alpha/api/domain/skilldic/entity/SkillEmbeddingDic.java`
- `src/main/java/com/alpha/api/domain/skilldic/repository/SkillEmbeddingDicRepository.java`
- `src/main/java/com/alpha/api/domain/skilldic/service/SkillNormalizationService.java` - 스킬 정규화

**Cache 도메인:**
- `src/main/java/com/alpha/api/domain/cache/service/CacheService.java` - 캐시 관리
- `src/main/java/com/alpha/api/domain/cache/service/CacheInvalidationService.java` - 캐시 무효화

### 🏗️ Infrastructure Layer (Adapter)

**Persistence (R2DBC):**
- `src/main/java/com/alpha/api/infrastructure/persistence/RecruitR2dbcRepository.java`
- `src/main/java/com/alpha/api/infrastructure/persistence/RecruitDescriptionR2dbcRepository.java`
- `src/main/java/com/alpha/api/infrastructure/persistence/RecruitSkillsEmbeddingR2dbcRepository.java`
- `src/main/java/com/alpha/api/infrastructure/persistence/CandidateR2dbcRepository.java`
- `src/main/java/com/alpha/api/infrastructure/persistence/CandidateDescriptionR2dbcRepository.java`
- `src/main/java/com/alpha/api/infrastructure/persistence/CandidateSkillsEmbeddingR2dbcRepository.java`
- `src/main/java/com/alpha/api/infrastructure/persistence/SkillEmbeddingDicR2dbcRepository.java`

**gRPC Server (캐시 무효화 수신):**
- `src/main/java/com/alpha/api/infrastructure/grpc/server/CacheInvalidateServiceImpl.java` - Batch Server로부터 수신

**Cache:**
- `src/main/java/com/alpha/api/infrastructure/cache/CaffeineCacheAdapter.java` - L1 Cache
- `src/main/java/com/alpha/api/infrastructure/cache/RedisCacheAdapter.java` - L2 Cache

### 📋 설정 파일

- `src/main/resources/application.yml` - 메인 설정
- `src/main/resources/application-cache.yml` - 캐싱 설정
- `build.gradle` - 의존성 및 protobuf 플러그인

---

## 🚀 현재 구현 상태

### ✅ 완료
- 없음 (구현 시작 전)

### 🔄 진행 중
- 없음

### ⏳ 예정 (우선순위 순)
1. Spring Boot 프로젝트 초기 설정 (build.gradle)
2. Entity 구현 (Recruit, Candidate, Skill Dictionary)
3. R2DBC Repository 구현 (pgvector 쿼리)
4. Service Layer 구현 (Reactive Mono/Flux)
5. GraphQL Schema 설계 (schema.graphqls)
6. GraphQL Resolver 구현
7. Caffeine + Redis 멀티 레이어 캐싱
8. gRPC Server (캐시 무효화 수신)
9. 스킬 정규화 로직 구현

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

**최종 수정일:** 2025-12-23
