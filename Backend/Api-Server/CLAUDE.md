# API Server - Claude Instructions

**역할:** GraphQL API 제공 → Frontend 요청 처리 + pgvector 검색 + 멀티 레이어 캐싱
**기술 스택:** Spring Boot 4.0 + Spring WebFlux + GraphQL + R2DBC + pgvector

---

## 📋 문서 목적

- **CLAUDE.md (이 문서)**: AI가 참조할 메타정보 + 코드 위치
- **README.md**: 사람이 읽을 아키텍처/컨벤션 상세 설명 (작성 예정)

---

## 🗺️ 핵심 문서 경로

### 필수 참조 (작성 예정)
- **아키텍처 및 컨벤션**: `README.md` (이 디렉토리)
- **GraphQL 스키마 설계**: `docs/GraphQL_스키마_설계.md`
- **캐싱 전략**: `docs/캐싱_전략.md`
- **Reactive 패턴**: `docs/Reactive_패턴_가이드.md`

### Backend 공통
- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md`
- **테이블 명세서**: `/Backend/docs/table_specification.md` ⭐ Single Source of Truth
- **ERD 다이어그램**: `/Backend/docs/ERD_다이어그램.md`

---

## 📂 예상 코드 구조 (구현 전)

### ⚙️ Configuration

**GraphQL:**
- `src/main/java/com/alpha/api/config/GraphQLConfig.java` - GraphQL 설정
- `src/main/resources/graphql/schema.graphqls` - GraphQL Schema 정의

**Cache:**
- `src/main/java/com/alpha/api/config/CacheConfig.java` - Caffeine + Redis 설정

**Database:**
- `src/main/java/com/alpha/api/config/R2dbcConfig.java` - R2DBC 설정

**gRPC:**
- `src/main/java/com/alpha/api/config/GrpcClientConfig.java` - AI Backend 연동
- `src/main/java/com/alpha/api/config/GrpcServerConfig.java` - Batch Server 연동

### 🎯 GraphQL Layer

**Resolver:**
- `src/main/java/com/alpha/api/graphql/resolver/QueryResolver.java` - Query 처리
- `src/main/java/com/alpha/api/graphql/resolver/MutationResolver.java` - Mutation 처리

**Type:**
- `src/main/java/com/alpha/api/graphql/type/RecruitType.java` - GraphQL 타입
- `src/main/java/com/alpha/api/graphql/type/CandidateType.java`

### 📦 Domain Layer

**Recruit:**
- `src/main/java/com/alpha/api/domain/recruit/Recruit.java` - Domain Model
- `src/main/java/com/alpha/api/domain/recruit/RecruitRepository.java` - R2DBC Repository
- `src/main/java/com/alpha/api/domain/recruit/RecruitService.java` - Service Layer

**Candidate:**
- `src/main/java/com/alpha/api/domain/candidate/` (동일 구조)

**Cache:**
- `src/main/java/com/alpha/api/domain/cache/CacheService.java` - 캐시 관리

### 🔌 gRPC Layer

**Client (AI Backend 호출):**
- `src/main/java/com/alpha/api/grpc/client/AiBackendClient.java` - Embedding 요청

**Server (Batch Server로부터 수신):**
- `src/main/java/com/alpha/api/grpc/server/CacheInvalidateService.java` - 캐시 무효화

### 📋 설정 파일

- `src/main/resources/application.yml` - 메인 설정
- `build.gradle` - 의존성

---

## 🚀 현재 구현 상태

### ✅ 완료
- 없음 (아직 구현 시작 전)

### 🔄 진행 중
- 설계 및 구현 준비

### ⏳ 예정 (우선순위 순)
1. Spring Boot 프로젝트 초기 설정
2. GraphQL Schema 설계
3. R2DBC Repository 구현 (pgvector 쿼리)
4. Service Layer 구현 (Reactive Mono/Flux)
5. GraphQL Resolver 구현
6. Caffeine + Redis 멀티 레이어 캐싱
7. gRPC Server (캐시 무효화 수신)
8. gRPC Client (AI Backend 연동)

---

## ⚠️ AI가 반드시 알아야 할 규칙

### 1. 구현 시작 전 필수 확인
- **DB 스키마**: `/Backend/docs/table_specification.md` 먼저 읽기
- **Batch Server 패턴**: `/Backend/Batch-Server/` 참조 (비슷한 구조)
- **Reactive 패턴**: Batch Server의 ChunkProcessor 참조

### 2. Reactive Programming 필수
- **Mono**: 단일 결과 (findById)
- **Flux**: 다중 결과 (findAll, search)
- **Non-blocking**: 절대 blocking 코드 사용 금지

### 3. 캐싱 전략
- L1 (Caffeine): 메모리 기반, 빠름
- L2 (Redis): 분산 캐싱, 공유
- 캐시 무효화: Batch 작업 완료 시 gRPC로 수신

### 4. GraphQL Schema 작성
- `schema.graphqls`에 먼저 정의
- Type, Query, Mutation 명확히 분리
- DB 스키마와 1:1 매핑 (불필요한 변환 최소화)

### 5. pgvector 쿼리
- `<->` 연산자: L2 거리 (유사도)
- CAST 필수: `CAST(:vector AS vector)`
- ORDER BY + LIMIT: 성능 최적화

### 6. 동시성 제어
- 캐시 무효화: synchronized 또는 Lock 사용
- Race Condition 주의

---

## 📚 참고할 Batch Server 패턴

API Server 구현 시 Batch Server의 다음 패턴을 참조:
- **Clean Architecture**: Domain/Infrastructure 분리
- **Repository Pattern**: Interface + JpaRepository (여기서는 R2dbcRepository)
- **Configuration 분리**: Config 패키지 구조

---

**최종 수정일:** 2025-12-18
