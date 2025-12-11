# Api-Server (Spring WebFlux + GraphQL) - Claude Instructions

**프로젝트명:** Alpha-Match API Server
**작성일자:** 2025-12-10
**기술 스택:** Java 21 + Spring Boot 4.0 + Spring WebFlux + Spring GraphQL + PostgreSQL(pgvector)

---

## 📋 프로젝트 개요

Alpha-Match의 핵심 API 서버로, Reactive Programming 기반의 고성능 GraphQL API를 제공합니다. pgvector를 활용한 Vector Similarity Search와 멀티 레이어 캐싱(Caffeine + Redis)을 통해 효율적인 추천 시스템을 구현합니다.

---

## 🎯 핵심 역할

1. **GraphQL API 제공**
   - Frontend로부터 GraphQL 쿼리 처리
   - Resolver → Service → Repository 패턴

2. **캐싱 계층 관리**
   - Caffeine (L1 캐시): 메모리 기반 고속 캐싱
   - Redis (L2 캐시): 분산 캐싱
   - byte[] 기반 직렬화로 성능 최적화

3. **Vector Similarity Search**
   - pgvector를 활용한 Embedding 유사도 검색
   - `<->` 연산자를 통한 L2 거리 계산

4. **gRPC 통신**
   - **Client**: AI Backend 호출 (Embedding/추천)
   - **Server**: Batch Server로부터 캐시 무효화 요청 수신

---

## 🏗️ 기술 스택

### Core
- **Java 21**: Virtual Thread 지원
- **Spring Boot 4.0**: 최신 Spring 생태계
- **Spring WebFlux**: Reactive Programming

### GraphQL
- **Spring for GraphQL**: GraphQL 통합

### Database & Cache
- **PostgreSQL + pgvector**: Vector DB
- **Caffeine**: In-memory 캐시
- **Redis**: 분산 캐시

### Communication
- **gRPC**: AI Backend 및 Batch Server 연동

---

## 📂 프로젝트 구조 (예정)

```
Backend/Api-Server/
├── src/main/java/com/alpha/api/
│   ├── config/
│   │   ├── CacheConfig.java           # Caffeine + Redis 설정
│   │   ├── GraphQLConfig.java
│   │   └── GrpcConfig.java            # gRPC Client/Server 설정
│   │
│   ├── graphql/
│   │   ├── resolver/
│   │   │   ├── QueryResolver.java     # GraphQL Query Resolver
│   │   │   └── MutationResolver.java
│   │   └── type/
│   │       └── RecruitType.java       # GraphQL Type 정의
│   │
│   ├── grpc/
│   │   ├── client/
│   │   │   └── AiBackendClient.java   # AI Server gRPC Client
│   │   └── server/
│   │       └── CacheInvalidateService.java  # gRPC Server (캐시 무효화)
│   │
│   ├── domain/
│   │   ├── recruit/
│   │   │   ├── Recruit.java           # Domain Model
│   │   │   ├── RecruitRepository.java
│   │   │   └── RecruitService.java
│   │   └── cache/
│   │       └── CacheService.java      # 캐시 관리
│   │
│   └── ApiServerApplication.java
│
├── src/main/resources/
│   ├── graphql/
│   │   └── schema.graphqls            # GraphQL Schema
│   └── application.yml
│
└── CLAUDE.md                          # 현재 문서
```

---

## 🔧 주요 기능

### 1. GraphQL API

#### Schema 예시
```graphql
type Recruit {
  id: ID!
  companyName: String!
  expYears: Int!
  englishLevel: String
  primaryKeyword: String
  similarity: Float
}

type Query {
  # 키워드 기반 검색
  searchRecruits(keyword: String!, limit: Int): [Recruit!]!

  # Vector 유사도 검색
  findSimilarRecruits(vector: [Float!]!, limit: Int): [Recruit!]!

  # ID로 조회
  getRecruit(id: ID!): Recruit
}
```

#### Resolver 예시
```java
@Controller
public class QueryResolver {

    @Autowired
    private RecruitService recruitService;

    @QueryMapping
    public Flux<Recruit> searchRecruits(
        @Argument String keyword,
        @Argument Integer limit
    ) {
        return recruitService.searchByKeyword(keyword, limit);
    }
}
```

---

### 2. 캐싱 전략

#### 멀티 레이어 캐싱
```java
@Service
public class RecruitService {

    // L1: Caffeine (메모리)
    @Cacheable(value = "recruits", key = "#id")
    public Mono<Recruit> findById(UUID id) {
        // L2: Redis 확인
        return redisTemplate.get(id)
            .switchIfEmpty(
                // Cache Miss: DB 조회
                recruitRepository.findById(id)
                    .doOnNext(recruit -> redisTemplate.set(id, recruit))
            );
    }
}
```

#### 캐시 무효화 (gRPC Server)
```java
@GrpcService
public class CacheInvalidateService
    extends CacheServiceGrpc.CacheServiceImplBase {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void invalidateCache(
        CacheInvalidateRequest request,
        StreamObserver<CacheInvalidateResponse> responseObserver
    ) {
        // Monitor Lock으로 동시성 제어
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

### 3. Vector Similarity Search

#### Repository 구현
```java
@Repository
public interface RecruitRepository
    extends R2dbcRepository<Recruit, UUID> {

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

## 🚀 통신 구조

### 1. GraphQL (Frontend ↔ API Server)
- **프로토콜:** HTTP/1.1
- **포트:** 8080
- **엔드포인트:** `/graphql`

### 2. gRPC Client (API Server → AI Backend)
- **포트:** 50051
- **용도:** Embedding 생성, 추천 요청

### 3. gRPC Server (Batch Server → API Server)
- **포트:** 50052
- **용도:** 캐시 무효화 수신

---

## 📝 개발 가이드

### 초기 설정
```bash
cd Backend/Api-Server
./gradlew build
./gradlew bootRun
```

### application.yml 구조
```yaml
spring:
  application:
    name: api-server

  # R2DBC (Reactive DB)
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/alpha_match
    username: postgres
    password: postgres

  # GraphQL
  graphql:
    graphiql:
      enabled: true
      path: /graphiql

  # Cache
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=10m

# gRPC
grpc:
  server:
    port: 50052
  client:
    ai-backend:
      address: static://localhost:50051
      negotiation-type: plaintext

# Redis
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

---

## 🎓 기술적 포인트

### 1. Reactive Programming
- **Mono/Flux**: 비동기 데이터 스트림
- **Backpressure**: 부하 제어
- **Non-blocking I/O**: 고성능 처리

### 2. 동시성 제어
- **Monitor Lock**: 캐시 무효화 시 Race Condition 방지
- **AtomicBoolean**: 중복 실행 방지

### 3. byte[] 캐싱
- 직렬화 비용 절감
- 메모리 효율 향상

---

## 🔗 관련 문서

- [루트 CLAUDE.md](../../CLAUDE.md)
- [Batch Server CLAUDE.md](../Batch-Server/CLAUDE.md)
- [Demo-Python CLAUDE.md](../../Demo-Python/CLAUDE.md)
- [Entire Structure](../Batch-Server/docs/Entire_Structure.md)

---

## ✅ 현재 진행 상황

### 예정
- ⏳ Spring Boot 프로젝트 초기 설정
- ⏳ GraphQL Schema 설계
- ⏳ Resolver 구현
- ⏳ R2DBC Repository 구현
- ⏳ Caffeine + Redis 캐싱 구현
- ⏳ gRPC Server (캐시 무효화) 구현
- ⏳ gRPC Client (AI Backend 연동) 구현

---

**최종 수정일:** 2025-12-10
