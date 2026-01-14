# API Server

> **Reactive GraphQL API 서버 (Spring WebFlux + pgvector)**

Spring WebFlux 기반 Reactive 프로그래밍으로 구현된 고성능 GraphQL API 서버입니다. pgvector를 활용한 벡터 유사도 검색과 멀티 레이어 캐싱을 제공합니다.

---

## 📋 주요 기능

- 🔍 **GraphQL API**: 유연한 쿼리 인터페이스 (7개 Query 구현)
- ⚡ **Reactive Programming**: Non-blocking I/O로 고성능 처리
- 🗄️ **pgvector 검색**: Vector Similarity Search (Cosine/L2 거리)
- 💾 **멀티 레이어 캐싱**: Caffeine (L1) + Redis (L2)
- 📊 **Dashboard API**: 카테고리 분포, 역량 매칭 분석
- 🎯 **스킬 정규화**: 스킬 벡터 기반 유사도 검색

---

## 🏗️ 아키텍처

### Reactive 플로우

```
Frontend (GraphQL Query)
    ↓
QueryResolver (Controller)
    ↓
Service Layer
    ↓
Cache Check (Caffeine → Redis)
    ├─ Hit: Return Cached Data
    └─ Miss: ↓
         R2dbcRepository (Reactive)
         ↓
         PostgreSQL (pgvector)
         ↓
         Cache Update
```

### 멀티 레이어 캐싱

```
Request → L1 (Caffeine) → L2 (Redis) → DB (PostgreSQL)
           ↑                ↑             ↑
           10초 TTL        10분 TTL      Vector Search
```

---

## 🛠️ 기술 스택

### Core
- **Java 21**: Virtual Thread 지원
- **Spring Boot 4.0**: 최신 Spring 생태계
- **Spring WebFlux**: Reactive Framework
- **Spring for GraphQL**: GraphQL 통합

### Database & Cache
- **PostgreSQL 16** + **pgvector**: Vector DB (1536d)
- **R2DBC**: Reactive DB 드라이버
- **Caffeine**: In-memory 캐시 (L1, 10초 TTL)
- **Redis**: 분산 캐시 (L2, 10분 TTL)

### Communication
- **gRPC**: Batch Server 캐시 무효화 통신 (예정)

---

## 📂 프로젝트 구조

```
Backend/Api-Server/
│
├── src/main/java/com/alpha/api/
│   │
│   ├── config/                          # 설정
│   │   ├── GraphQLConfig.java
│   │   ├── CacheConfig.java             # Caffeine + Redis
│   │   ├── R2dbcConfig.java
│   │   ├── GrpcClientConfig.java        # AI Backend 호출
│   │   └── GrpcServerConfig.java        # Batch Server 수신
│   │
│   ├── graphql/                         # GraphQL 레이어
│   │   ├── resolver/
│   │   │   ├── QueryResolver.java       # Query 처리
│   │   │   └── MutationResolver.java
│   │   └── type/
│   │       ├── RecruitType.java
│   │       └── CandidateType.java
│   │
│   ├── domain/                          # 도메인 레이어
│   │   ├── recruit/
│   │   │   ├── Recruit.java             # Domain Model
│   │   │   ├── RecruitRepository.java   # R2DBC Repository
│   │   │   └── RecruitService.java      # Service (Mono/Flux)
│   │   │
│   │   ├── candidate/
│   │   └── cache/
│   │       └── CacheService.java        # 캐시 관리
│   │
│   ├── grpc/                            # gRPC 레이어
│   │   ├── client/
│   │   │   └── AiBackendClient.java     # AI Backend 호출
│   │   └── server/
│   │       └── CacheInvalidateService.java  # 캐시 무효화 수신
│   │
│   └── ApiServerApplication.java
│
├── src/main/resources/
│   ├── graphql/
│   │   └── schema.graphqls              # GraphQL Schema
│   └── application.yml
│
├── docs/                                # 개발 문서 (작성 예정)
│   ├── GraphQL_스키마_설계.md
│   ├── 캐싱_전략.md
│   └── Reactive_패턴_가이드.md
│
├── build.gradle
├── CLAUDE.md                            # AI 개발 가이드
└── README.md                            # 이 문서
```

---

## 🚀 빠른 시작 (구현 후)

### 사전 요구사항

- **Java** 21+
- **PostgreSQL** 16+ (pgvector)
- **Redis** 7+

### 1. 의존성 설치

```bash
cd Backend/Api-Server
./gradlew build
```

### 2. 환경 변수 설정

`src/main/resources/application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/alpha_match
    username: postgres
    password: postgres

  graphql:
    graphiql:
      enabled: true
      path: /graphiql

  data:
    redis:
      host: localhost
      port: 6379

grpc:
  server:
    port: 50052  # Batch Server로부터 수신
  client:
    ai-backend:
      address: static://localhost:50051
```

### 3. 서버 실행

```bash
./gradlew bootRun
```

GraphiQL: http://localhost:8080/graphiql

---

## 📝 코드 컨벤션 (설계)

### 1. Reactive 패턴

**Mono (단일 결과):**
```java
public Mono<Recruit> findById(UUID id) {
    return recruitRepository.findById(id);
}
```

**Flux (다중 결과):**
```java
public Flux<Recruit> searchByKeyword(String keyword) {
    return recruitRepository.findByKeyword(keyword);
}
```

### 2. GraphQL Resolver

```java
@Controller
public class QueryResolver {

    @Autowired
    private RecruitService recruitService;

    @QueryMapping
    public Mono<Recruit> recruit(@Argument UUID id) {
        return recruitService.findById(id);
    }

    @QueryMapping
    public Flux<Recruit> searchRecruits(
        @Argument String keyword,
        @Argument Integer limit
    ) {
        return recruitService.searchByKeyword(keyword)
            .take(limit != null ? limit : 10);
    }
}
```

### 3. Cache 사용

```java
@Service
public class RecruitService {

    @Cacheable(value = "recruits", key = "#id")
    public Mono<Recruit> findById(UUID id) {
        return cacheService.get(id)
            .switchIfEmpty(
                recruitRepository.findById(id)
                    .flatMap(recruit -> cacheService.set(id, recruit)
                        .thenReturn(recruit))
            );
    }
}
```

### 4. pgvector 쿼리

```java
@Repository
public interface RecruitRepository extends R2dbcRepository<Recruit, UUID> {

    @Query("""
        SELECT * FROM recruit_embedding
        ORDER BY vector <-> CAST(:queryVector AS vector)
        LIMIT :limit
        """)
    Flux<Recruit> findSimilarByVector(
        @Param("queryVector") String queryVector,
        @Param("limit") int limit
    );
}
```

---

## 📚 개발 가이드 (예정)

### GraphQL Schema 작성

`src/main/resources/graphql/schema.graphqls`:

```graphql
type Recruit {
  id: ID!
  companyName: String!
  expYears: Int!
  primaryKeyword: String
  similarity: Float
}

type Query {
  recruit(id: ID!): Recruit
  searchRecruits(keyword: String!, limit: Int): [Recruit!]!
  findSimilarRecruits(vector: [Float!]!, limit: Int): [Recruit!]!
}
```

### 캐시 무효화 (gRPC Server)

```java
@GrpcService
public class CacheInvalidateService
    extends CacheServiceGrpc.CacheServiceImplBase {

    @Override
    public void invalidateCache(
        CacheInvalidateRequest request,
        StreamObserver<CacheInvalidateResponse> responseObserver
    ) {
        synchronized(cacheLock) {
            cacheManager.getCache("recruits").clear();
            redisTemplate.delete("recruit:*");
        }

        responseObserver.onNext(
            CacheInvalidateResponse.newBuilder()
                .setSuccess(true)
                .build()
        );
        responseObserver.onCompleted();
    }
}
```

---

## 🔧 설정 가이드 (예정)

### Caffeine Cache

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.SECONDS)
        );
        return cacheManager;
    }
}
```

---

## 🧪 테스트 (예정)

### GraphQL Query 테스트

```graphql
query {
  searchRecruits(keyword: "React", limit: 5) {
    id
    companyName
    similarity
  }
}
```

---

## 📖 관련 문서

- [Batch Server README](../Batch-Server/README.md) - 유사한 Clean Architecture 패턴
- [DB 스키마 가이드](/Backend/docs/DB_스키마_가이드.md)
- [테이블 명세서](/Backend/docs/table_specification.md)

---

## 🐛 트러블슈팅 (예정)

### Reactive 타입 변환 에러

```
Error: Cannot convert Mono to Object
```

**해결:**
- `.block()` 절대 사용 금지 (Reactive 체인 깨짐)
- `flatMap`, `map`, `switchIfEmpty` 사용

### pgvector 쿼리 실패

```
Error: operator does not exist: vector <-> text
```

**해결:**
- `CAST(:queryVector AS vector)` 필수
- 벡터 문자열 포맷: `[0.1, 0.2, ...]`

---

## 🚀 현재 구현 상태

### ✅ 완료
- Spring Boot 프로젝트 초기 설정 (R2DBC, Redis, gRPC)
- Entity 9개 구현 (Recruit, Candidate, Skill Dictionary)
- R2DBC Repository 구현 (pgvector 쿼리 포함)
- GraphQL Schema 및 Resolver 구현 (7개 Query)
- Multi-layer Caching 시스템 (Caffeine + Redis)
- Dashboard 분석 API (카테고리 분포, 역량 매칭)
- Clean Architecture 전면 리팩토링 (4-Layer)
- 캐시 성능 테스트 (12.9x 속도 향상)

### ⏳ 예정
- gRPC Server 구현 (캐시 무효화 수신)
- Redis L2 캐시 실전 연동

---

**최종 수정일:** 2026-01-14
