# Api-Server Postman Collection

GraphQL API 테스트 및 성능 측정을 위한 Postman 컬렉션입니다.

## 파일 구성

- `Api-Server.postman_collection.json` - GraphQL API 테스트 컬렉션
- `performance-test.js` - 캐싱 성능 테스트 스크립트
- `README.md` - 이 문서

## 사전 요구사항

1. **Api-Server 실행**
   ```bash
   cd Backend/Api-Server
   ./gradlew bootRun
   ```

2. **데이터 적재 (Batch-Server)**
   - PostgreSQL에 데이터가 이미 적재되어 있어야 함
   - skill_category_dic, skill_embedding_dic 테이블 필수

3. **Redis 실행** (캐싱 테스트 시)
   ```bash
   redis-server
   ```

## 테스트 항목

### 1. Get Skill Categories (Caching Test)
**목적:** 스킬 카테고리 캐싱 성능 측정

**기대 결과:**
- 첫 호출: ~100-500ms (DB 조회)
- 두 번째 호출: ~1-10ms (L1 캐시 히트)
- 10초 후 호출: ~10-50ms (L2 캐시 히트)
- 10분 후 호출: ~100-500ms (캐시 만료, DB 재조회)

**수동 테스트 방법:**
1. Postman에서 "1. Get Skill Categories (Caching Test)" 선택
2. Send 버튼 클릭
3. Response Time 확인 (하단 상태바)
4. 10번 반복하여 평균 시간 측정

**자동 테스트 방법 (Newman):**
```bash
npm install -g newman
newman run Api-Server.postman_collection.json \
  --folder "1. Get Skill Categories (Caching Test)" \
  --iteration-count 10 \
  --delay-request 100
```

### 2. Search Matches - CANDIDATE Mode
**목적:** 구직자가 채용 공고 검색

**변수:**
- `mode`: "CANDIDATE"
- `skills`: ["Java", "Spring", "React"]
- `experience`: "3-5 Years"

**기대 응답:**
```json
{
  "data": {
    "searchMatches": {
      "matches": [
        {
          "id": "uuid",
          "title": "포지션명",
          "company": "회사명",
          "score": 0.85,
          "skills": ["Java", "Spring"],
          "experience": 3
        }
      ],
      "vectorVisualization": [...]
    }
  }
}
```

### 3. Search Matches - RECRUITER Mode
**목적:** 구인자가 지원자 검색

**변수:**
- `mode`: "RECRUITER"
- `skills`: ["Python", "Django", "PostgreSQL"]
- `experience`: "5-9 Years"

### 4. Search Matches - Experience Filters Test
**목적:** 경력 필터링 테스트

**테스트 케이스:**
- `experience: "0-2 Years"` - 신입 (0-2년)
- `experience: "3-5 Years"` - 중급 (3-5년)
- `experience: "6-9 Years"` - 시니어 (6-9년)
- `experience: "10+ Years"` - 전문가 (10년 이상)
- `experience: ""` - 필터 없음

### 5. Get Recruit Detail
**목적:** 채용 공고 상세 조회

**사용법:**
1. Search Matches 쿼리 실행
2. 결과에서 `id` 복사
3. Variables 탭에서 `id` 값 변경
4. Send

### 6. Get Candidate Detail
**목적:** 지원자 상세 조회

**사용법:**
1. Search Matches (RECRUITER Mode) 쿼리 실행
2. 결과에서 `id` 복사
3. Variables 탭에서 `id` 값 변경
4. Send

### 7. Dashboard Candidate Stats
**목적:** 대시보드 통계 조회

**변수:**
- `userMode: "CANDIDATE"` - 채용 공고 통계
- `userMode: "RECRUITER"` - 지원자 통계

## 캐싱 성능 테스트

### 테스트 시나리오

#### Scenario 1: L1 Cache (Caffeine) 성능 측정
```bash
# 10초 내 연속 호출 (L1 캐시 활용)
newman run Api-Server.postman_collection.json \
  --folder "1. Get Skill Categories (Caching Test)" \
  --iteration-count 20 \
  --delay-request 100
```

**기대 결과:**
- Iteration 0: ~100-500ms (DB)
- Iteration 1-19: ~1-10ms (L1 cache)

#### Scenario 2: L2 Cache (Redis) 성능 측정
```bash
# L1 캐시 만료 후 (10초 대기)
# 수동으로 10초 대기 후 재호출
```

**기대 결과:**
- 첫 호출: ~10-50ms (L2 cache)
- 이후 호출: ~1-10ms (L1 cache 재생성)

#### Scenario 3: Cache Miss (전체 만료)
```bash
# L2 캐시 만료 후 (10분 대기)
# 수동으로 10분 대기 후 재호출
```

**기대 결과:**
- 첫 호출: ~100-500ms (DB)
- 이후 호출: ~1-10ms (L1 cache)

### 성능 지표 분석

**Good Performance:**
- L1 cache hit: < 10ms
- L2 cache hit: < 50ms
- DB query: < 500ms
- Cache speedup: > 10x

**Poor Performance (문제 가능성):**
- L1 cache hit: > 50ms → Caffeine 설정 확인
- L2 cache hit: > 200ms → Redis 연결 확인
- DB query: > 1000ms → DB 인덱스 확인

## 문제 해결 (Troubleshooting)

### 1. Connection refused (8088)
```
Error: connect ECONNREFUSED 127.0.0.1:8088
```

**해결:**
- Api-Server 실행 여부 확인
- `./gradlew bootRun`으로 서버 시작
- `application.yml`에서 포트 확인

### 2. Empty skill categories
```json
{
  "data": {
    "skillCategories": []
  }
}
```

**해결:**
- Batch-Server로 데이터 적재 확인
- PostgreSQL에서 `skill_category_dic` 테이블 확인
  ```sql
  SELECT COUNT(*) FROM skill_category_dic;
  SELECT COUNT(*) FROM skill_embedding_dic;
  ```

### 3. Cache not working (slow response)
```
Response Time: 500ms (every call)
```

**해결:**
- Redis 실행 확인: `redis-cli ping` → PONG
- `application.yml`에서 Redis 설정 확인
- CacheService 로그 확인 (DEBUG 레벨)

### 4. GraphQL errors
```json
{
  "errors": [
    {
      "message": "Validation error...",
      "extensions": {
        "classification": "ValidationError"
      }
    }
  ]
}
```

**해결:**
- GraphQL Schema 확인 (`schema.graphqls`)
- Query 변수 타입 확인 (String, Int, Enum 등)
- Resolver 로그 확인

## 로그 분석

### 1. 캐싱 로그 확인
```bash
# Api-Server 로그에서 캐시 히트/미스 확인
tail -f logs/api-server.log | grep -i cache
```

**예상 로그:**
```
[INFO] Cache lookup: key=skill:categories
[DEBUG] L1 cache MISS: key=skill:categories
[DEBUG] L2 cache MISS: key=skill:categories
[DEBUG] Caffeine cache PUT: key=skill:categories
[DEBUG] Redis cache PUT: key=skill:categories
[DEBUG] L1 cache HIT: key=skill:categories
```

### 2. 쿼리 성능 로그 확인
```bash
# PostgreSQL slow query 확인
tail -f /var/log/postgresql/postgresql.log | grep -i "duration"
```

## 성능 비교 리포트 예시

```
=== Skill Categories Caching Performance ===

Test Environment:
- Machine: MacBook Pro M1
- PostgreSQL: 15.3
- Redis: 7.0.11
- Api-Server: Spring Boot 4.0, WebFlux

Results (10 iterations):
┌─────────────┬──────────────┬─────────┬─────────┬─────────┐
│ Iteration   │ Cache Layer  │ Time    │ Speedup │ Status  │
├─────────────┼──────────────┼─────────┼─────────┼─────────┤
│ 0 (First)   │ None (DB)    │ 324ms   │ 1.0x    │ ✅      │
│ 1           │ L1 (Caffeine)│ 3ms     │ 108.0x  │ ✅      │
│ 2           │ L1 (Caffeine)│ 2ms     │ 162.0x  │ ✅      │
│ 3-9         │ L1 (Caffeine)│ 2-4ms   │ 81-162x │ ✅      │
│ After 10s   │ L2 (Redis)   │ 18ms    │ 18.0x   │ ✅      │
│ After 10m   │ None (DB)    │ 310ms   │ 1.0x    │ ✅      │
└─────────────┴──────────────┴─────────┴─────────┴─────────┘

Average Response Time: 38ms
Cache Hit Rate: 90%
Overall Speedup: 8.5x

Conclusion: ✅ Caching working as expected
```

## 추가 테스트 시나리오

### Concurrent Request Test
```bash
# Apache Bench로 동시 요청 테스트
ab -n 100 -c 10 -p request.json -T "application/json" \
  http://localhost:8088/graphql
```

### Stress Test
```bash
# k6로 부하 테스트
k6 run --vus 50 --duration 30s stress-test.js
```

## 참고 문서

- GraphQL API 가이드: `/Backend/Api-Server/docs/GraphQL_API_개발_가이드.md`
- 캐싱 전략 가이드: `/Backend/Api-Server/docs/캐싱_전략_가이드.md`
- 프로젝트 문서: `/CLAUDE.md`
