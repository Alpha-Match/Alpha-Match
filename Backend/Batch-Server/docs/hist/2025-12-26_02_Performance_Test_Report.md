# Performance Test Report - Chunk Size 300 Baseline

**작성일:** 2025-12-26
**작성자:** Claude Opus 4.5
**버전:** 1.0

---

## 1. 테스트 개요

### 1.1 테스트 목적
- OOM 크래시 수정 후 전체 도메인 데이터 적재 검증
- JVM 힙 메모리 증가 (8GB) 및 로깅 최적화 효과 확인
- 청크 사이즈 300 기준 성능 Baseline 수립

### 1.2 테스트 환경

| 항목 | 값 |
|------|-----|
| **OS** | Windows 10/11 |
| **Java** | OpenJDK 21.0.9 |
| **JVM Heap** | `-Xms2g -Xmx8g` |
| **GC** | G1GC |
| **DB** | PostgreSQL 15.15 + pgvector |
| **Batch Server Port** | 9090 (gRPC) |
| **Python Server Port** | 8000 (HTTP/gRPC) |
| **청크 사이즈** | 300 (Batch Server), 100 (Python Streaming) |

---

## 2. 테스트 결과 요약

### 2.1 도메인별 성능 지표

| 도메인 | 레코드 수 | 청크 수 | 소요 시간 | 처리량 (rps) | 상태 |
|--------|----------|---------|-----------|--------------|------|
| **Skill_dic** | 105 | 2 | 1.69s | 62.2 | ✅ 성공 |
| **Recruit** | 87,488 | 875 | 12m 54.8s | 113.0 | ✅ 성공 |
| **Candidate** | 118,741 | 1,188 | 30m 50.1s | 64.2 | ✅ 성공 |
| **전체** | **206,334** | **2,065** | **44m 46.6s** | **76.8** | ✅ 성공 |

### 2.2 테이블별 저장 데이터

#### Skill Dictionary (2 tables)
- `skill_category_dic`: 카테고리 데이터
- `skill_embedding_dic`: 105 스킬 임베딩 (384d 벡터)

#### Recruit (4 tables)
- `recruit`: 87,488 채용공고 메타데이터
- `recruit_skill`: 채용공고별 스킬 목록 (다대다)
- `recruit_description`: 채용공고 상세 설명
- `recruit_skills_embedding`: 87,488 벡터 임베딩 (384d)

#### Candidate (4 tables)
- `candidate`: 118,741 후보자 메타데이터
- `candidate_skill`: 후보자별 스킬 목록 (다대다)
- `candidate_description`: 후보자 상세 정보
- `candidate_skills_embedding`: 118,741 벡터 임베딩 (384d)

---

## 3. 성능 분석

### 3.1 처리량 비교

```
Recruit:    ████████████████████████████████████████████████████████ 113.0 rps
Candidate:  ████████████████████████████████ 64.2 rps
Skill_dic:  ██████████████████████████████ 62.2 rps
평균:        ██████████████████████████████████████ 76.8 rps
```

### 3.2 Recruit vs Candidate 성능 차이 분석

Recruit (113 rps) > Candidate (64.2 rps) 원인:

1. **테이블 구조 차이**
   - Recruit: 4-table upsert (recruit → skill → description → embedding)
   - Candidate: 4-table upsert (동일 구조)

2. **데이터 특성**
   - Candidate 레코드가 약 36% 더 많음 (118,741 vs 87,488)
   - Candidate의 스킬 목록이 평균적으로 더 길 수 있음

3. **트랜잭션 부하**
   - 연속 처리 시 DB 캐시 워밍업 상태 차이
   - GC 압력 누적

### 3.3 메모리 사용 분석

JVM 힙 8GB 설정 후:
- OOM 크래시 없이 전체 206,334건 처리 완료
- 약 45분간 안정적인 처리 유지
- G1GC로 효율적인 메모리 관리

---

## 4. 테스트 타임라인

```
16:14:54 - Batch Server 시작 (PID 40356)
16:15:14 - gRPC Server 리스닝 시작 (Port 9090)
16:27:33 - Recruit 테스트 시작
16:40:27 - Recruit 테스트 완료 (12m 54.8s)
16:41:47 - Candidate 테스트 시작
17:12:37 - Candidate 테스트 완료 (30m 50.1s)
17:13:01 - Skill_dic 테스트 시작
17:13:03 - Skill_dic 테스트 완료 (1.69s)
```

---

## 5. 결론 및 권장사항

### 5.1 성공 요인
- JVM 힙 메모리 8GB 설정으로 OOM 방지
- 로깅 레벨 INFO로 조정하여 메모리/디스크 절약
- G1GC 가비지 컬렉터로 대용량 힙 효율적 관리

### 5.2 현재 성능 Baseline
- **평균 처리량**: 76.8 records/sec
- **총 처리량**: 206,334 records / 2686.6s

### 5.3 향후 최적화 방향

1. **청크 사이즈 튜닝**
   - 100, 500, 1000 청크 사이즈 비교 테스트 권장
   - 예상: 청크 사이즈 증가 시 throughput 향상 가능

2. **병렬 처리**
   - 멀티 스레드 DB 쓰기 검토
   - HikariCP 풀 사이즈 조정 (현재: 20)

3. **배치 인서트 최적화**
   - Hibernate JDBC Batch Size 조정 (현재: 300)
   - Native Bulk Insert 고려

4. **모니터링**
   - JMX/Micrometer 메트릭 추가
   - GC 로그 분석

---

## 6. 첨부 자료

### 테스트 결과 파일
- `recruit_test_result.json`
- `candidate_test_result.json`
- `skill_dic_test_result.json`

### 로그 파일
- `Backend/Batch-Server/startup_optimized.log`

---

**문서 끝**
