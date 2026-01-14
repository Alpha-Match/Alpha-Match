# Alpha-Match 프로젝트 발표 참조 자료

**프로젝트**: Alpha-Match (Headhunter-Recruit Matching System)
**작성일**: 2026-01-12

---

## 슬라이드 1: 프로젝트 개요

### 프로젝트 목표
> **"대규모 벡터 기반 추천 시스템의 파이프라인을 작은 단위로 직접 구축"**

### 3가지 핵심 실험
1. **Reactive API 서버** - WebFlux + GraphQL
2. **gRPC Streaming** - Python ↔ Java 양방향 통신
3. **Vector Search + Caching** - pgvector + Caffeine/Redis

### 시스템 규모
| 지표 | 값 |
|------|-----|
| 총 데이터 | 206,334건 |
| 벡터 차원 | 1536d |
| API 응답 시간 | 8ms (캐시) |

---

## 슬라이드 2: 시스템 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                        Alpha-Match System                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌─────────┐      ┌─────────────┐      ┌─────────────┐        │
│   │ Frontend│─────▶│  Api-Server │◀────▶│   Redis     │        │
│   │ Next.js │      │  (GraphQL)  │      │   (L2 Cache)│        │
│   └─────────┘      └──────┬──────┘      └─────────────┘        │
│                           │                                     │
│                           ▼                                     │
│                    ┌──────────────┐                             │
│                    │  PostgreSQL  │                             │
│                    │  + pgvector  │                             │
│                    └──────┬───────┘                             │
│                           │                                     │
│                           ▼                                     │
│   ┌─────────────┐  ┌─────────────┐                             │
│   │   Python    │◀─│Batch-Server │                             │
│   │(Embedding)  │  │(Spring Batch)│                             │
│   └─────────────┘  └─────────────┘                             │
│        gRPC Streaming                                           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 슬라이드 3: 기술 스택 비교

### Backend Servers

| 구분 | Api-Server | Batch-Server |
|------|-----------|--------------|
| Framework | Spring WebFlux | Spring Batch 6.0 |
| 통신 | GraphQL + gRPC | gRPC Client |
| DB Access | R2DBC (Reactive) | JPA + Hibernate |
| 캐싱 | Caffeine + Redis | - |
| 스케줄링 | - | Quartz |

### 주요 라이브러리
- **Vector DB**: pgvector 0.5.1
- **gRPC**: grpc-java 1.60.0
- **GraphQL**: Spring GraphQL 1.3.3
- **Cache**: Caffeine 3.1.8

---

## 슬라이드 4: 데이터 처리 파이프라인

### 데이터 흐름
```
Raw Data (CSV/JSON)
       │
       ▼
┌─────────────────┐
│  Python Server  │  ← 임베딩 생성 (text-embedding-3-small)
│  - Preprocessing│
│  - Embedding    │
└────────┬────────┘
         │ gRPC Stream
         ▼
┌─────────────────┐
│  Batch Server   │  ← 청크 단위 처리 (100건)
│  - Reader       │
│  - Processor    │
│  - Writer       │
└────────┬────────┘
         │ JPA/Native Query
         ▼
┌─────────────────┐
│   PostgreSQL    │  ← pgvector 저장
│   + pgvector    │
└─────────────────┘
```

### 처리 성능
| 도메인 | 건수 | 시간 | 처리량 |
|--------|------|------|--------|
| Recruit | 87,488 | 12m 55s | 113 rps |
| Candidate | 118,741 | 30m 50s | 64 rps |
| Skill Dic | 105 | 1.7s | 62 rps |
| **합계** | **206,334** | **44m 47s** | **77 rps** |

---

## 슬라이드 5: 벡터 유사도 검색

### 알고리즘
- **Distance Metric**: Cosine Similarity
- **Index**: HNSW (Hierarchical Navigable Small World)
- **Parameters**: m=32, ef_construction=128

### 검색 쿼리
```sql
SELECT recruit_id, position, company_name,
       (1 - (skills_vector <=> :queryVector)) AS similarity
FROM recruit r
JOIN recruit_skills_embedding rse ON r.recruit_id = rse.recruit_id
WHERE (1 - (skills_vector <=> :queryVector)) >= 0.6
ORDER BY similarity DESC
LIMIT 20;
```

### 성능
| 지표 | 값 |
|------|-----|
| 벡터 차원 | 1536 |
| 유사도 임계값 | 60% |
| 검색 시간 | 200-600ms |

---

## 슬라이드 6: 캐싱 아키텍처

### Multi-Layer Cache
```
Request → L1 (Caffeine) → L2 (Redis) → Database
              ↓               ↓
           30초 TTL        10분 TTL
           In-memory       Distributed
```

### 성능 개선 결과

| API | Cold Start | Cache Hit | 개선율 |
|-----|-----------|-----------|--------|
| searchMatches | 610ms | 65ms | **9.4x** |
| searchStatistics | 8,260ms | 8ms | **1,027x** |
| skillCategories | 230ms | 20ms | **11.5x** |

---

## 슬라이드 7: SQL 최적화

### N+1 문제 해결

**Before (52,501 쿼리)**:
```
1. 매칭 결과 조회 (1 query)
2. 각 결과별 스킬 조회 (52,500 queries) ← N+1!
3. Java에서 집계
```

**After (1 쿼리)**:
```sql
WITH matched AS (
    SELECT recruit_id FROM recruit r
    JOIN recruit_skills_embedding rse ON ...
    WHERE similarity >= 0.6
),
skill_counts AS (
    SELECT skill, COUNT(*) as count
    FROM matched m
    JOIN recruit_skill rs ON m.recruit_id = rs.recruit_id
    GROUP BY skill
)
SELECT * FROM skill_counts ORDER BY count DESC;
```

### 최적화 결과
| 지표 | Before | After | 개선 |
|------|--------|-------|------|
| 쿼리 수 | 52,501 | 1 | 52,501x |
| 응답 시간 | 30-60s | 8s | 7x |
| 메모리 | 500MB | 10MB | 50x |

---

## 슬라이드 8: Clean Architecture

### 4-Layer Structure
```
┌────────────────────────────────────┐
│  Presentation (GraphQL Resolver)   │  ← Input Adapter
├────────────────────────────────────┤
│  Application (Services)            │  ← Use Cases
├────────────────────────────────────┤
│  Domain (Entities, Ports)          │  ← Business Core
├────────────────────────────────────┤
│  Infrastructure (Repositories)     │  ← Output Adapters
└────────────────────────────────────┘
```

### 의존성 규칙
- 상위 → 하위 참조 금지
- Domain은 외부 의존성 없음
- Port-Adapter 패턴으로 분리

---

## 슬라이드 9: gRPC 스트리밍

### Client Streaming Pattern
```
Python Server                    Batch Server
     │                                │
     │◀── StreamRequest(domain) ──────│
     │                                │
     │──── DataRow (chunk 1) ────────▶│
     │──── DataRow (chunk 2) ────────▶│
     │──── ...                        │
     │──── DataRow (chunk N) ────────▶│
     │──── StreamComplete ───────────▶│
     │                                │
```

### 설정
| 항목 | 값 |
|------|-----|
| Chunk Size | 100 |
| Max Message | 100MB |
| Timeout | 10분 |

---

## 슬라이드 10: GraphQL API

### 주요 Query (9개)
```graphql
# 매칭 검색
searchMatches(mode, skills, experience, limit, offset, sortBy)

# 통계
searchStatistics(mode, skills, limit)
dashboardData(userMode)
topCompanies(limit)

# 상세 조회
getRecruit(id)
getCandidate(id)

# 분석
getCategoryDistribution(skills)
getSkillCompetencyMatch(mode, targetId, searchedSkills)

# 스킬 카테고리
skillCategories
```

### 응답 예시
```json
{
  "matches": [{
    "id": "uuid",
    "title": "Senior Java Developer",
    "company": "TechCorp",
    "score": 0.847,
    "skills": ["Java", "Spring"]
  }]
}
```

---

## 슬라이드 11: 정량적 성과

### 데이터베이스
| 테이블 | 레코드 | 용량 |
|--------|--------|------|
| recruit | 87,488 | 50MB |
| recruit_skills_embedding | 87,488 | 530MB |
| candidate | 118,741 | 60MB |
| candidate_skills_embedding | 118,741 | 720MB |
| **Total** | **~1.5M** | **~1.9GB** |

### 코드베이스
| 서버 | 파일 수 | 테스트 케이스 |
|------|--------|--------------|
| Api-Server | 41 | 57 |
| Batch-Server | 45+ | 40 |
| **Total** | **86+** | **97** |

---

## 슬라이드 12: 기술적 도전과 해결

### 1. pgvector 직렬화 문제
- **문제**: JPA에서 float[] → vector 변환 실패
- **해결**: Native Query + CAST(:vector AS vector)

### 2. N+1 쿼리 문제
- **문제**: 52,501개 쿼리 → 30-60초 응답
- **해결**: CTE 기반 단일 쿼리 → 8초

### 3. 메모리 부족 (OOM)
- **문제**: 대용량 데이터 처리 시 OOM
- **해결**: JVM 튜닝 (-Xmx8g) + Chunk 처리

### 4. CORS 에러
- **문제**: SSR 환경에서 403 Forbidden
- **해결**: Origin Pattern 설정 (localhost:*)

---

## 슬라이드 13: 학습 포인트

### Reactive Programming
- Mono/Flux 체이닝
- Non-blocking I/O
- R2DBC Connection Pool

### gRPC Streaming
- Client Streaming 패턴
- Proto 메시지 설계
- Error Handling

### Vector Database
- pgvector 인덱스 (HNSW)
- Cosine Similarity
- 벡터 정규화

### Caching Strategy
- Multi-layer (L1/L2)
- TTL 설계
- Cache Invalidation

---

## 슬라이드 14: 향후 계획

### 단기 (1-2주)
- [ ] Redis L2 캐시 연동
- [ ] 캐시 무효화 gRPC 구현
- [ ] 부하 테스트 (1000 concurrent)

### 중기 (1개월)
- [ ] Kubernetes 배포
- [ ] 모니터링 대시보드 (Grafana)
- [ ] A/B 테스트 인프라

### 장기 (3개월)
- [ ] 실시간 추천
- [ ] 사용자 피드백 학습
- [ ] 다국어 지원

---

## 부록: 주요 수치 요약

| 카테고리 | 항목 | 값 |
|----------|------|-----|
| **데이터** | 총 레코드 | 206,334 |
| | 벡터 차원 | 1536 |
| | DB 용량 | 1.9GB |
| **성능** | 검색 응답 (캐시) | 65ms |
| | 통계 응답 (캐시) | 8ms |
| | 최대 개선율 | 1,027x |
| **처리량** | Recruit | 113 rps |
| | Candidate | 64 rps |
| | 평균 | 77 rps |
| **코드** | 총 파일 | 86+ |
| | 테스트 | 97 cases |
| | 커밋 | 50+ |

---

**발표 자료 끝**
