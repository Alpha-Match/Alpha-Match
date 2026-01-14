# Api-Server 구현 상세 문서

**프로젝트**: Alpha-Match (Headhunter-Recruit Matching System)
**작성일**: 2026-01-12
**버전**: 1.0

---

## 1. 시스템 개요

### 1.1 기술 스택

| 구분 | 기술 | 버전 | 용도 |
|------|------|------|------|
| Framework | Spring Boot | 4.0.1 | 애플리케이션 프레임워크 |
| Reactive | Spring WebFlux | 7.0.2 | Non-blocking I/O |
| API | GraphQL (Spring GraphQL) | 1.3.3 | 유연한 API 쿼리 |
| Database | R2DBC PostgreSQL | 1.0.7 | Reactive DB 연동 |
| Vector DB | pgvector | 0.5.1 | 벡터 유사도 검색 |
| Cache L1 | Caffeine | 3.1.8 | In-memory 캐시 |
| Cache L2 | Redis | 7.x | 분산 캐시 |
| Build | Gradle | 8.x | 빌드 도구 |
| Java | OpenJDK | 21 | 런타임 |

### 1.2 아키텍처 패턴

```
┌─────────────────────────────────────────────────────────────────┐
│                    4-Layer Clean Architecture                    │
├─────────────────────────────────────────────────────────────────┤
│  Presentation Layer (Input Adapters)                            │
│  ├── GraphQL Resolver (QueryResolver)                           │
│  └── gRPC Server (예정)                                         │
├─────────────────────────────────────────────────────────────────┤
│  Application Layer (Use Cases)                                  │
│  ├── SearchService (검색 통합)                                  │
│  ├── DashboardService (통계 생성)                               │
│  └── CacheService (캐시 관리)                                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer (Business Core)                                   │
│  ├── Entities (Recruit, Candidate, SkillDic)                    │
│  ├── Repository Ports (Interface)                               │
│  └── Domain Services (SkillNormalizationService)                │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer (Output Adapters)                         │
│  ├── R2DBC Repositories (PostgreSQL + pgvector)                 │
│  ├── Cache Adapters (Caffeine, Redis)                           │
│  └── Configuration (CORS, R2DBC, Cache)                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 정량적 지표

### 2.1 코드베이스 규모

| 계층 | 파일 수 | 주요 컴포넌트 |
|------|--------|--------------|
| Presentation | 11 | 1 Resolver, 10 GraphQL Types |
| Application | 5 | 3 Services, 2 DTOs |
| Domain | 15 | 9 Entities, 4 Repositories, 2 Services |
| Infrastructure | 10 | 5 Repositories, 2 Cache Adapters, 3 Configs |
| **Total** | **41** | - |

### 2.2 GraphQL Schema 규모

| 구분 | 수량 | 목록 |
|------|------|------|
| Query | 9 | searchMatches, skillCategories, dashboardData, getRecruit, getCandidate, getCategoryDistribution, getSkillCompetencyMatch, topCompanies, searchStatistics |
| Mutation | 3 | invalidateRecruitCache, invalidateCandidateCache, invalidateAllCaches |
| Type | 16 | UserMode, MatchItem, SkillMatch, SearchMatchesResult, SkillCategory, DashboardData, RecruitDetail, CandidateDetail, etc. |
| Input | 0 | (Arguments로 처리) |

### 2.3 데이터 규모

| 테이블 | 레코드 수 | 비고 |
|--------|----------|------|
| recruit | 87,488 | 채용 공고 |
| recruit_skill | ~612,416 | 공고당 평균 7개 스킬 |
| recruit_skills_embedding | 87,488 | 1536차원 벡터 |
| candidate | 118,741 | 후보자 |
| candidate_skill | ~593,705 | 후보자당 평균 5개 스킬 |
| candidate_skills_embedding | 118,741 | 1536차원 벡터 |
| skill_embedding_dic | 105 | 스킬 사전 |
| skill_category_dic | 6 | 카테고리 |
| **Total** | **~1,500,000+** | - |

---

## 3. 핵심 기능 구현

### 3.1 벡터 유사도 검색

**알고리즘**: Cosine Similarity (pgvector `<=>` 연산자)

```sql
-- 벡터 유사도 검색 쿼리
SELECT
    r.recruit_id,
    r.position,
    r.company_name,
    (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) AS similarity_score
FROM recruit r
INNER JOIN recruit_skills_embedding rse ON r.recruit_id = rse.recruit_id
WHERE rse.skills_vector IS NOT NULL
  AND (1 - (rse.skills_vector <=> CAST(:queryVector AS vector))) >= :threshold
ORDER BY similarity_score DESC
LIMIT :limit
```

**성능 지표**:
| 지표 | 값 |
|------|-----|
| 벡터 차원 | 1536 (OpenAI text-embedding-3-small) |
| 인덱스 | HNSW (m=32, ef_construction=128) |
| 유사도 임계값 | 0.6 (60%) |
| 검색 응답 시간 | 200-600ms (Cold), 50-100ms (Cached) |

### 3.2 스킬 정규화 플로우

```
입력: ["Java", "Spring Boot", "PostgreSQL"]
         │
         ▼
┌─────────────────────────────────┐
│  SkillNormalizationService      │
│  1. skill_embedding_dic 조회    │
│  2. 각 스킬 벡터 추출           │
│  3. 벡터 평균 계산              │
└─────────────────────────────────┘
         │
         ▼
쿼리 벡터: [0.012, -0.034, 0.087, ...]  (1536차원)
         │
         ▼
┌─────────────────────────────────┐
│  pgvector 유사도 검색           │
│  - Cosine Distance 계산         │
│  - 상위 N개 결과 반환           │
└─────────────────────────────────┘
         │
         ▼
출력: 유사도 60% 이상 매칭 결과
```

### 3.3 Multi-Layer Caching

```
┌────────────────────────────────────────────────────────┐
│                    Request Flow                        │
└────────────────────────────────────────────────────────┘
                          │
                          ▼
              ┌───────────────────┐
              │   L1 Cache Hit?   │
              │   (Caffeine)      │
              └───────────────────┘
                    │         │
                   Yes        No
                    │         │
                    ▼         ▼
              ┌─────────┐  ┌───────────────────┐
              │ Return  │  │   L2 Cache Hit?   │
              │ Data    │  │   (Redis)         │
              └─────────┘  └───────────────────┘
                               │         │
                              Yes        No
                               │         │
                               ▼         ▼
                         ┌─────────┐  ┌───────────────┐
                         │ Update  │  │  DB Query     │
                         │ L1 +    │  │  + Update     │
                         │ Return  │  │  L1 + L2      │
                         └─────────┘  └───────────────┘
```

**캐시 설정**:
| 캐시 레벨 | 저장소 | TTL | 최대 크기 |
|----------|--------|-----|----------|
| L1 | Caffeine | 30초 | 10,000 entries |
| L2 | Redis | 10분 | 무제한 |

---

## 4. 성능 최적화 결과

### 4.1 캐싱 성능

| API | Cold Start | Cache Hit | 개선율 |
|-----|-----------|-----------|--------|
| searchMatches | 610ms | 65ms | **9.4x** |
| searchStatistics | 8,260ms | 8ms | **1,027x** |
| skillCategories | 230ms | 20ms | **11.5x** |

### 4.2 SQL 최적화 (searchStatistics)

| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| 쿼리 수 | 52,501 | 1 | **52,501x** |
| 응답 시간 | 30-60초 | 8초 | **3.6-7.3x** |
| 메모리 사용 | ~500MB | ~10MB | **50x** |

### 4.3 동시 처리 성능

| 지표 | 값 |
|------|-----|
| R2DBC Connection Pool | 20 connections |
| Netty Event Loop | 8 threads (CPU cores) |
| 이론적 동시 처리량 | ~1,000 req/s |

---

## 5. API 명세

### 5.1 주요 Query

#### searchMatches
```graphql
searchMatches(
  mode: UserMode!           # CANDIDATE | RECRUITER
  skills: [String!]!        # 검색 스킬 목록
  experience: String        # "0-2 Years" | "3-5 Years" | "6-9 Years" | "10+ Years"
  limit: Int                # 결과 수 제한 (기본: 20)
  offset: Int               # 페이지네이션 오프셋
  sortBy: String            # "score DESC" | "experience ASC"
): SearchMatchesResult!
```

**응답 예시**:
```json
{
  "matches": [
    {
      "id": "uuid",
      "title": "Senior Java Developer",
      "company": "TechCorp",
      "score": 0.847,
      "skills": ["Java", "Spring", "PostgreSQL"],
      "experience": 5
    }
  ],
  "vectorVisualization": [
    { "skill": "Java", "isCore": true, "x": 12.5, "y": 8.3 }
  ]
}
```

#### searchStatistics
```graphql
searchStatistics(
  mode: UserMode!
  skills: [String!]!
  limit: Int
): SearchStatisticsResult!
```

**응답 예시**:
```json
{
  "totalCount": 52487,
  "topSkills": [
    { "skill": "Java", "count": 15234, "percentage": 29.0 },
    { "skill": "Python", "count": 12456, "percentage": 23.7 }
  ]
}
```

---

## 6. 의존성 그래프

```
QueryResolver
    │
    ├── SearchService
    │       ├── SkillNormalizationService
    │       │       └── SkillEmbeddingDicRepository
    │       ├── RecruitSearchRepository
    │       ├── CandidateSearchRepository
    │       ├── RecruitSkillRepository
    │       └── CandidateSkillRepository
    │
    ├── DashboardService
    │       ├── RecruitRepository
    │       └── CandidateRepository
    │
    └── CacheService
            ├── CaffeineCacheAdapter (L1)
            └── RedisCacheAdapter (L2)
```

---

## 7. 설정 파일

### 7.1 application.yml 주요 설정

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5433/alpha_match
    pool:
      initial-size: 5
      max-size: 20
      max-idle-time: 30m

  graphql:
    graphiql:
      enabled: true
    path: /graphql

  data:
    redis:
      host: localhost
      port: 6379

cache:
  caffeine:
    spec: maximumSize=10000,expireAfterWrite=30s
```

---

## 8. 테스트 커버리지

| 테스트 유형 | 파일 수 | 테스트 케이스 |
|------------|--------|--------------|
| Unit Test | 6 | 45 |
| Integration Test | 2 | 12 |
| **Total** | **8** | **57** |

### 주요 테스트 파일
- `QueryResolverTest.java` - GraphQL 리졸버 테스트
- `SearchServiceTest.java` - 검색 서비스 테스트
- `SkillNormalizationServiceTest.java` - 스킬 정규화 테스트
- `CandidateRepositoryTest.java` - 레포지토리 테스트
- `RecruitRepositoryTest.java` - 레포지토리 테스트

---

## 9. 배포 정보

| 항목 | 값 |
|------|-----|
| 포트 | 8080 (HTTP), 50052 (gRPC) |
| JVM 옵션 | `-Xms512m -Xmx2g -XX:+UseG1GC` |
| 빌드 시간 | ~25초 |
| JAR 크기 | ~85MB |
| 기동 시간 | ~8초 |

---

**문서 끝**
