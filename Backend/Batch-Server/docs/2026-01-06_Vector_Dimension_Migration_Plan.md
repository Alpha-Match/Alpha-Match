# 벡터 차원 변경 마이그레이션 계획
**일시:** 2026-01-06
**변경:** 358차원 → 1000차원 이상

---

## 📋 1. 변경 사항

### 1.1 벡터 차원 증가

| 항목 | Before | After | 증가율 |
|------|--------|-------|--------|
| **차원** | 358d | 1000d+ | 2.8배 |
| **벡터 크기** | 358 × 4 bytes = 1,432 bytes | 1000 × 4 bytes = 4,000 bytes | 2.8배 |
| **87K 레코드** | 125 MB | 349 MB | 2.8배 |
| **인덱스 크기** | ~1.2 GB | ~3.4 GB | 2.8배 |

### 1.2 영향 분석

**메모리:**
```
Chunk 100건 처리 시:
Before: 100 × 1.4KB = 140 KB
After:  100 × 4.0KB = 400 KB (2.8배 증가)

HNSW 인덱스:
Before: 1.2 GB
After:  3.4 GB (2.8배 증가)
→ 총 메모리: 2.7GB → 7.6GB (+4.9GB)
```

**네트워크:**
```
gRPC 전송 크기:
Before: 471 MB (87K 레코드)
After:  1,318 MB (87K 레코드, 2.8배)
→ 전송 시간: 약 2-3배 증가 예상
```

**DB 저장:**
```
PostgreSQL vector 타입:
Before: vector(358)
After:  vector(1000+)
→ 테이블 크기: 631 MB → 1,767 MB
```

---

## 🔧 2. 필수 변경 작업

### 2.1 Flyway 마이그레이션 (V5)

**파일:** `src/main/resources/db/migration/V5__update_vector_dimension_to_1000.sql`

```sql
-- 1. HNSW 인덱스 제거 (재구축 필요)
DROP INDEX IF EXISTS recruit_skills_embedding_hnsw_idx;
DROP INDEX IF EXISTS candidate_skills_embedding_hnsw_idx;
DROP INDEX IF EXISTS skill_embedding_dic_hnsw_idx;

-- 2. 벡터 차원 변경
ALTER TABLE recruit_skills_embedding
ALTER COLUMN skills_vector TYPE vector(1000);

ALTER TABLE candidate_skills_embedding
ALTER COLUMN skills_vector TYPE vector(1000);

ALTER TABLE skill_embedding_dic
ALTER COLUMN skill_vector TYPE vector(1000);

-- 3. HNSW 인덱스 재생성 (CONCURRENTLY로 서비스 중단 방지)
CREATE INDEX CONCURRENTLY recruit_skills_embedding_hnsw_idx
ON recruit_skills_embedding
USING hnsw (skills_vector vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

CREATE INDEX CONCURRENTLY candidate_skills_embedding_hnsw_idx
ON candidate_skills_embedding
USING hnsw (skills_vector vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

CREATE INDEX CONCURRENTLY skill_embedding_dic_hnsw_idx
ON skill_embedding_dic
USING hnsw (skill_vector vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
```

**예상 소요 시간:**
- 인덱스 제거: ~1초
- ALTER TABLE: ~5-10초 (테이블당)
- 인덱스 재생성: ~5-10분 (테이블당, HNSW 빌드)

### 2.2 application.yml 설정 변경

```yaml
# application-batch.yml
batch:
  embedding:
    domains:
      recruit:
        vector-dimension: 1000  # 358 → 1000
      candidate:
        vector-dimension: 1000  # 358 → 1000
```

### 2.3 Python Proto 변경

**파일:** `Demo-Python/src/proto/embedding_stream.proto`

```protobuf
message RecruitRow {
  string id = 1;
  // ...
  repeated float skills_vector = 10;  // 크기 검증: 1000
}
```

---

## ⚡ 3. 성능 최적화 (Spring Batch 활용)

### 3.1 upsertAll() 배치 쿼리 최적화

**현재 문제:**
```java
// RecruitJpaRepository.java
default void upsertAll(List<RecruitEntity> entities) {
    entities.forEach(this::upsert);  // ❌ N번 실행
}
```

**개선 방안:**
```java
@Transactional
default void upsertAll(List<RecruitEntity> entities) {
    if (entities.isEmpty()) return;

    // 배치 크기로 분할 (JDBC batch_size = 300)
    int batchSize = 300;
    for (int i = 0; i < entities.size(); i += batchSize) {
        int end = Math.min(i + batchSize, entities.size());
        List<RecruitEntity> batch = entities.subList(i, end);

        // Native Batch Upsert (단일 쿼리)
        upsertBatch(batch);
    }
}

@Modifying
@Query(value = """
    INSERT INTO recruit (...)
    VALUES :batchValues
    ON CONFLICT (recruit_id) DO UPDATE SET ...
    """, nativeQuery = true)
void upsertBatch(@Param("batchValues") List<RecruitEntity> entities);
```

**성능 예상:**
```
Before: 300건 × 4 테이블 = 1,200 쿼리
After:  4 쿼리 (테이블당 1개 배치 쿼리)
→ 300배 향상 ✅
```

### 3.2 ItemWriter에 EntityManager flush/clear 추가

**현재 문제:**
```java
// RecruitItemWriter.java
@Override
@Transactional
public void write(Chunk<? extends RecruitItem> chunk) {
    // ...
    recruitRepository.upsertAll(recruits);
    recruitSkillRepository.upsertAll(allSkills);
    // ❌ EntityManager 메모리 누적
}
```

**개선 방안:**
```java
@Slf4j
@RequiredArgsConstructor
public class RecruitItemWriter implements ItemWriter<RecruitItem> {

    private final RecruitJpaRepository recruitRepository;
    private final EntityManager entityManager;  // ← 추가

    @Override
    @Transactional
    public void write(Chunk<? extends RecruitItem> chunk) {
        // ... (기존 upsert 로직)

        // EntityManager 플러시 및 클리어 (메모리 해제)
        entityManager.flush();
        entityManager.clear();

        log.debug("[Recruit Writer] EntityManager cleared, memory released");
    }
}
```

**메모리 효과:**
```
Before: Chunk 누적 → OOM 위험
After:  Chunk마다 해제 → 메모리 안정 ✅
```

### 3.3 Virtual Thread 병렬 쓰기 유지

**현재 DataProcessor 방식:**
```java
// RecruitDataProcessor.java (line 105-131)
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // 3개 테이블 병렬 쓰기
    Future<?> skillFuture = executor.submit(() ->
        recruitSkillRepository.upsertAll(finalSkillEntities));
    Future<?> descFuture = executor.submit(() ->
        recruitDescriptionRepository.upsertAll(finalDescriptionEntities));
    Future<?> embeddingFuture = executor.submit(() ->
        recruitSkillsEmbeddingRepository.upsertAll(finalEmbeddingEntities));

    // 모든 병렬 작업 완료 대기
    skillFuture.get();
    descFuture.get();
    embeddingFuture.get();
}
```

**ItemWriter에도 적용:**
```java
// RecruitItemWriter.java (개선)
@Override
@Transactional
public void write(Chunk<? extends RecruitItem> chunk) {
    // ...

    // 1. recruit 먼저 저장 (FK)
    recruitRepository.upsertAll(recruits);

    // 2. 나머지 3개 테이블 병렬 쓰기
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        Future<?> skillFuture = executor.submit(() ->
            recruitSkillRepository.upsertAll(allSkills));
        Future<?> descFuture = executor.submit(() ->
            recruitDescriptionRepository.upsertAll(descriptions));
        Future<?> embeddingFuture = executor.submit(() ->
            recruitSkillsEmbeddingRepository.upsertAll(embeddings));

        skillFuture.get();
        descFuture.get();
        embeddingFuture.get();
    }

    entityManager.flush();
    entityManager.clear();
}
```

---

## 🧪 4. 성능 테스트 계획

### 4.1 테스트 시나리오

**시나리오 1: Pattern 2 (현재 방식)**
```yaml
# DataProcessor (gRPC Server)
- 벡터 차원: 1000d
- Chunk 크기: 100
- 테이블: 4개
- 병렬 쓰기: Virtual Thread (3개)
```

**시나리오 2: Pattern 1 (Spring Batch)**
```yaml
# ItemReader/Processor/Writer
- 벡터 차원: 1000d
- Chunk 크기: 300
- 테이블: 4개
- 병렬 쓰기: Virtual Thread (3개)
- EntityManager: flush/clear
```

**시나리오 3: 배치 쿼리 최적화**
```yaml
# Pattern 1 + upsertBatch()
- 벡터 차원: 1000d
- Chunk 크기: 300
- 배치 쿼리: 단일 쿼리로 300건 처리
- 병렬 쓰기: Virtual Thread (3개)
```

### 4.2 측정 지표

| 지표 | 목표 |
|-----|------|
| **처리 시간** | < 15분 (87K 레코드) |
| **처리량 (RPS)** | > 100 RPS |
| **메모리 사용량** | < 4GB (Heap) |
| **네트워크 전송** | 측정 및 분석 |
| **DB 쿼리 수** | 최소화 (배치 쿼리) |

### 4.3 예상 결과

```
┌─────────────────────────────────────────────────────┐
│  시나리오별 성능 예상                                │
├─────────────────────────────────────────────────────┤
│  1. Pattern 2 (현재)                                │
│     ├─ 처리 시간: ~12분 (358d 기준: 8m38s × 1.4)   │
│     ├─ RPS: ~120                                     │
│     └─ 쿼리 수: 1,200 (Chunk당)                     │
│                                                      │
│  2. Pattern 1 (Spring Batch)                        │
│     ├─ 처리 시간: ~10분 (Chunk 300 최적화)         │
│     ├─ RPS: ~145                                     │
│     └─ 쿼리 수: 1,200 (Chunk당)                     │
│                                                      │
│  3. 배치 쿼리 최적화                                 │
│     ├─ 처리 시간: ~4-5분 ✅                         │
│     ├─ RPS: ~290 ✅                                  │
│     └─ 쿼리 수: 4 (Chunk당, 300배 감소) ✅         │
└─────────────────────────────────────────────────────┘
```

---

## ✅ 5. 체크리스트

### 5.1 마이그레이션 전

- [ ] Flyway V5 스크립트 작성
- [ ] application.yml 벡터 차원 설정 변경
- [ ] Python Proto 1000d 변경 및 검증
- [ ] DB 백업 (현재 358d 데이터)

### 5.2 최적화 구현

- [ ] upsertAll() 배치 쿼리 구현
- [ ] ItemWriter에 EntityManager flush/clear 추가
- [ ] Virtual Thread 병렬 쓰기 ItemWriter 적용
- [ ] Chunk 크기 튜닝 (100 vs 300 vs 500)

### 5.3 성능 테스트

- [ ] Pattern 2 (현재) 성능 측정
- [ ] Pattern 1 (Spring Batch) 성능 측정
- [ ] 배치 쿼리 최적화 성능 측정
- [ ] 메모리 사용량 모니터링 (jconsole/VisualVM)
- [ ] 결과 비교 및 분석

### 5.4 배포

- [ ] 최적화 버전 선택
- [ ] Flyway 마이그레이션 실행
- [ ] HNSW 인덱스 재구축 (5-10분)
- [ ] 성능 검증 (프로덕션)

---

## 📊 6. 리스크 및 대응

### 리스크 1: 인덱스 재구축 시간 (10분+)

**대응:**
- `CREATE INDEX CONCURRENTLY` 사용 (서비스 중단 방지)
- 야간 시간대 작업
- 기존 인덱스 유지 후 새 인덱스 생성 완료 시 교체

### 리스크 2: 메모리 부족 (7.6GB 인덱스)

**대응:**
- JVM Heap: 8GB 유지 (충분)
- PostgreSQL shared_buffers 증가 (4GB → 8GB)
- HNSW 파라미터 조정 (m=16 → m=12, 메모리 절감)

### 리스크 3: 네트워크 전송 시간 증가

**대응:**
- gRPC max-inbound-message-size 증가 (100MB → 200MB)
- Chunk 크기 조정 (100 → 50, 전송 빈도 증가)

---

**작성일:** 2026-01-06
**담당자:** Batch-Server Team
