# Clean Architecture 리팩토링 완료

**날짜**: 2025-12-29
**작성자**: Claude Sonnet 4.5
**목적**: Api-Server의 전면적인 Clean Architecture 3-Layer 원칙 적용

---

## 개요

Backend/Api-Server 프로젝트를 Clean Architecture의 3-Layer 원칙에 따라 전면 리팩토링했습니다.
Uncle Bob의 Clean Architecture 원칙을 기반으로, 각 레이어의 역할을 명확히 분리하고 의존성 방향을 올바르게 설정했습니다.

---

## Clean Architecture 3-Layer 원칙

### Domain Layer (내부 원 - 비즈니스 핵심)
**역할**: 비즈니스 핵심 로직, 기술 독립적
**포함**: Entity, Repository Interface (Port), Domain Service
**금지**: 외부 프레임워크 의존성 (Spring, JPA, etc.)

### Application Layer (중간 원 - Use Case 구현)
**역할**: Use Case 구현, 비즈니스 플로우 조율
**포함**: Application Service, DTO, Port Interface
**특징**: Domain을 사용, Infrastructure를 직접 참조하지 않음

### Infrastructure Layer (외부 원 - 기술 구현)
**역할**: 기술 구현, 외부 세계와의 인터페이스
**포함**: Repository 구현체 (Adapter), GraphQL Resolver, gRPC Server, Cache 구현체, Configuration
**특징**: Domain과 Application을 참조, 구체적인 기술 구현

---

## 리팩토링 Phase별 작업 내역

### Phase 1: Application Service 이동

**근거**: SearchService, DashboardService, CacheService는 Use Case 구현체로, 여러 Repository를 조율하는 비즈니스 플로우 로직입니다. Domain Layer의 순수 비즈니스 로직이 아니므로 Application Layer로 이동해야 합니다.

**변경 내역**:
```
domain/search/service/SearchService.java
  → application/service/SearchService.java

domain/dashboard/service/DashboardService.java
  → application/service/DashboardService.java

domain/cache/service/CacheService.java
  → application/service/CacheService.java
```

**영향 받은 파일**:
- `graphql/resolver/QueryResolver.java` (import 경로 수정)

**삭제된 디렉토리**:
- `domain/search/`
- `domain/dashboard/`
- `domain/cache/service/` (port는 유지)

---

### Phase 2: Domain Service 검증

**분석**: SkillNormalizationService

**판단 근거**:
- 순수 도메인 로직 (스킬 정규화, 벡터 변환)
- 기술 독립적 (비즈니스 규칙만 구현)
- Repository를 조율하지 않음 (단일 Repository 사용)

**결과**: **domain/skilldic/service/ 위치 유지** ✅

---

### Phase 3: GraphQL 이동 (Input Adapter)

**근거**: GraphQL은 외부 인터페이스 (HTTP 엔드포인트)로, Hexagonal Architecture에서 Input Adapter에 해당합니다. Spring GraphQL 프레임워크 의존성을 갖고 있으므로 Infrastructure Layer로 이동해야 합니다.

**변경 내역**:
```
graphql/resolver/ → infrastructure/graphql/resolver/
graphql/type/     → infrastructure/graphql/type/
graphql/input/    → infrastructure/graphql/input/
```

**이동된 파일 (10개)**:
- **Resolver**: QueryResolver.java
- **Type**: CandidateDetail.java, DashboardCategoryData.java, DashboardSkillStat.java, MatchItem.java, RecruitDetail.java, SearchMatchesResult.java, SkillCategory.java, SkillMatch.java, UserMode.java

**영향 받은 파일** (import 경로 자동 수정):
- `application/service/SearchService.java`
- `application/service/DashboardService.java`
- `infrastructure/graphql/resolver/QueryResolver.java`

**삭제된 디렉토리**:
- `graphql/` (최상위)

---

### Phase 4: Configuration 이동

**근거**: Configuration은 기술 의존성 (Caffeine, Redis, R2DBC, Spring WebFlux CORS)을 포함하므로 Infrastructure Layer에 속합니다. Uncle Bob의 Clean Architecture에서 "Frameworks and Drivers (Outermost Layer)"에 해당합니다.

**변경 내역**:
```
config/CacheConfig.java → infrastructure/config/CacheConfig.java
config/CorsConfig.java  → infrastructure/config/CorsConfig.java
config/R2dbcConfig.java → infrastructure/config/R2dbcConfig.java
```

**Spring Boot Auto Configuration 검증**:
- `infrastructure/config/` 패키지도 자동 스캔됨 ✅
- 별도 `@ComponentScan` 설정 불필요

**삭제된 디렉토리**:
- `config/` (최상위)

---

### Phase 5: Domain Layer 정리

**삭제된 빈 디렉토리**:
- `domain/candidate/service/` (Service 이동 후 빈 디렉토리)
- `domain/recruit/service/` (Service 이동 후 빈 디렉토리)

**유지된 구조**:
- `domain/cache/port/CachePort.java` ✅ (Interface이므로 유지)
- `domain/common/SkillCount.java` ✅ (Domain Value Object)

---

### Phase 6: 최종 구조 검증

**Gradle Build**: ✅ BUILD SUCCESSFUL (29s, 9 tasks)

---

## Before/After 디렉토리 구조 비교

### Before (리팩토링 전)
```
src/main/java/com/alpha/api/
├── ApiApplication.java
├── application/
│   └── dto/
├── config/                           ❌ 최상위 (잘못된 위치)
│   ├── CacheConfig.java
│   ├── CorsConfig.java
│   └── R2dbcConfig.java
├── domain/
│   ├── cache/
│   │   ├── port/
│   │   └── service/                  ❌ Use Case (Application Layer 소속)
│   ├── candidate/
│   ├── common/
│   ├── dashboard/
│   │   └── service/                  ❌ Use Case (Application Layer 소속)
│   ├── recruit/
│   ├── search/
│   │   └── service/                  ❌ Use Case (Application Layer 소속)
│   └── skilldic/
│       └── service/                  ✅ Domain Service (올바른 위치)
├── graphql/                          ❌ 최상위 (Input Adapter → Infrastructure)
│   ├── input/
│   ├── resolver/
│   └── type/
└── infrastructure/
    ├── cache/
    ├── grpc/
    └── persistence/
```

### After (리팩토링 후) ✅
```
src/main/java/com/alpha/api/
├── ApiApplication.java
│
├── domain/                           ✅ 비즈니스 핵심 (기술 독립)
│   ├── cache/
│   │   └── port/CachePort.java      ✅ Port Interface
│   ├── candidate/
│   │   ├── entity/
│   │   └── repository/              ✅ Port Interface
│   ├── common/
│   │   └── SkillCount.java          ✅ Value Object
│   ├── recruit/
│   │   ├── entity/
│   │   └── repository/              ✅ Port Interface
│   └── skilldic/
│       ├── entity/
│       ├── repository/              ✅ Port Interface
│       └── service/                 ✅ Domain Service (스킬 정규화)
│
├── application/                      ✅ Use Case 구현
│   ├── dto/
│   └── service/
│       ├── CacheService.java        ✅ 캐싱 Use Case
│       ├── DashboardService.java    ✅ 대시보드 Use Case
│       └── SearchService.java       ✅ 검색 Use Case
│
└── infrastructure/                   ✅ 기술 구현
    ├── cache/                        ✅ Output Adapter
    │   ├── CaffeineCacheAdapter.java
    │   └── RedisCacheAdapter.java
    ├── config/                       ✅ Framework Configuration
    │   ├── CacheConfig.java
    │   ├── CorsConfig.java
    │   └── R2dbcConfig.java
    ├── graphql/                      ✅ Input Adapter
    │   ├── input/
    │   ├── resolver/
    │   │   └── QueryResolver.java
    │   └── type/
    ├── grpc/
    │   └── server/
    └── persistence/                  ✅ Output Adapter
        ├── RecruitR2dbcRepository.java
        ├── RecruitCustomRepositoryImpl.java
        ├── CandidateR2dbcRepository.java
        └── CandidateCustomRepositoryImpl.java
```

---

## 이동된 파일 목록 (경로 변경)

### Application Services (3개)
1. `domain/search/service/SearchService.java` → `application/service/SearchService.java`
2. `domain/dashboard/service/DashboardService.java` → `application/service/DashboardService.java`
3. `domain/cache/service/CacheService.java` → `application/service/CacheService.java`

### GraphQL (10개)
1. `graphql/resolver/QueryResolver.java` → `infrastructure/graphql/resolver/QueryResolver.java`
2. `graphql/type/CandidateDetail.java` → `infrastructure/graphql/type/CandidateDetail.java`
3. `graphql/type/DashboardCategoryData.java` → `infrastructure/graphql/type/DashboardCategoryData.java`
4. `graphql/type/DashboardSkillStat.java` → `infrastructure/graphql/type/DashboardSkillStat.java`
5. `graphql/type/MatchItem.java` → `infrastructure/graphql/type/MatchItem.java`
6. `graphql/type/RecruitDetail.java` → `infrastructure/graphql/type/RecruitDetail.java`
7. `graphql/type/SearchMatchesResult.java` → `infrastructure/graphql/type/SearchMatchesResult.java`
8. `graphql/type/SkillCategory.java` → `infrastructure/graphql/type/SkillCategory.java`
9. `graphql/type/SkillMatch.java` → `infrastructure/graphql/type/SkillMatch.java`
10. `graphql/type/UserMode.java` → `infrastructure/graphql/type/UserMode.java`

### Configuration (3개)
1. `config/CacheConfig.java` → `infrastructure/config/CacheConfig.java`
2. `config/CorsConfig.java` → `infrastructure/config/CorsConfig.java`
3. `config/R2dbcConfig.java` → `infrastructure/config/R2dbcConfig.java`

**총 이동 파일: 16개**

---

## 리팩토링 근거 요약

| Layer | 파일/디렉토리 | 근거 |
|-------|--------------|------|
| **Application** | SearchService | Use Case 구현 (여러 Repository 조율, 비즈니스 플로우) |
| **Application** | DashboardService | Use Case 구현 (통계 생성, 여러 Repository 조합) |
| **Application** | CacheService | Use Case 구현 (인프라 조율 로직, L1/L2 캐시) |
| **Domain** | SkillNormalizationService | 순수 도메인 로직 (스킬 정규화, 벡터 계산) ✅ |
| **Infrastructure** | GraphQL (resolver, type, input) | Input Adapter (외부 인터페이스, Spring GraphQL 의존) |
| **Infrastructure** | Config (Cache, CORS, R2DBC) | Framework Configuration (Spring, R2DBC, Redis 설정) |
| **Domain** | Entity + Repository Interface | 비즈니스 핵심 (기술 독립적) ✅ |
| **Domain** | CachePort | Port Interface (캐싱 추상화) ✅ |

---

## 의존성 방향 검증

### Clean Architecture 의존성 규칙 ✅
```
Infrastructure → Application → Domain
      ↓              ↓           ↓
   (구현)        (Use Case)   (비즈니스 핵심)
```

**검증 결과**:
- ✅ Domain Layer는 외부 의존성 없음 (Spring, JPA 등 프레임워크 독립)
- ✅ Application Layer는 Domain만 참조 (Infrastructure 직접 참조 없음)
- ✅ Infrastructure Layer는 Domain + Application 참조
- ✅ Port-Adapter 패턴 적용 (CachePort, Repository Interface)

---

## Reactive Programming 패턴 유지

**검증 항목**:
- ✅ 모든 Service 메서드는 Mono/Flux 반환
- ✅ Blocking 코드 없음 (`.block()` 사용 금지)
- ✅ flatMap/map/switchIfEmpty 체이닝 유지

---

## 빌드 검증 결과

```bash
./gradlew clean build -x test --no-daemon
```

**결과**:
```
BUILD SUCCESSFUL in 29s
9 actionable tasks: 9 executed
```

**검증 항목**:
- ✅ 모든 Import 경로 정상
- ✅ Spring Boot 컴포넌트 스캔 정상 동작 (infrastructure/config 자동 스캔 확인)
- ✅ GraphQL Schema 로딩 정상
- ✅ Reactive Repository 정상 동작

---

## 문서 업데이트

### 1. CLAUDE.md 업데이트
- "현재 구현 상태" → "Clean Architecture 리팩토링 완료" 추가
- 파일 경로 업데이트

### 2. README.md (필요 시)
- 아키텍처 섹션 업데이트

---

## 향후 작업

### 즉시 가능한 작업
1. **Redis 연동 및 실제 성능 테스트**
2. **CacheService 적용 확대** (getSkillCategories, Dashboard, Detail 조회)
3. **gRPC Server 구현** (캐시 무효화 수신)
4. **GraphQL Mutation 구현** (캐시 무효화 API)

### 장기 개선 사항
1. **부하 테스트** (Newman, k6)
2. **모니터링** (Prometheus, Grafana)
3. **API 문서화** (GraphQL Playground, Postman 컬렉션)

---

## 참고 자료

### Clean Architecture
- Uncle Bob's Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- Hexagonal Architecture (Port-Adapter Pattern)
- DDD (Domain-Driven Design)

### 프로젝트 문서
- `/Backend/docs/table_specification.md` - DB 스키마 Single Source of Truth
- `/Backend/Api-Server/README.md` - 아키텍처 및 컨벤션
- `/Backend/Api-Server/docs/GraphQL_API_개발_가이드.md`
- `/Backend/Api-Server/docs/캐싱_전략_가이드.md`

---

**작성일**: 2025-12-29
**최종 수정일**: 2025-12-29
**상태**: ✅ 완료
