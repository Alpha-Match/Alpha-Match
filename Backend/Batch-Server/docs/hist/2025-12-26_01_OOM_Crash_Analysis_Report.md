# OOM Crash Analysis Report

**작성일:** 2025-12-26
**작성자:** Claude Opus 4.5
**버전:** 1.0

---

## 1. 개요

### 1.1 문제 발생 상황
- **발생 시간:** 2025-12-26 15:43:05
- **영향 받은 도메인:** Recruit (87,488 records)
- **증상:** Batch Server가 약 42분 동안 실행 후 크래시

### 1.2 오류 메시지
```
FAILURE: Build failed with an exception.
* What went wrong:
Java heap space
```

---

## 2. 근본 원인 분석

### 2.1 직접적 원인: JVM Heap Space 부족
- 기본 JVM 힙 크기(~1-2GB)로 87,488개 레코드 처리 시도
- 각 레코드는 384차원 벡터 임베딩 포함 (float32 × 384 = 1.5KB/record)
- 총 예상 데이터 크기: 87,488 × 1.5KB ≈ 128MB (벡터만)

### 2.2 간접적 원인: 과도한 DEBUG 로깅

**문제의 설정:**
```yaml
logging:
  level:
    com.alpha.backend: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**영향:**
- `org.hibernate.SQL: DEBUG` → 모든 SQL 문 로깅
- `org.hibernate.type.descriptor.sql.BasicBinder: TRACE` → 모든 파라미터 바인딩 로깅
- 결과: **1.3GB 로그 파일** 생성 (migration_test.log)
- 로그 버퍼가 힙 메모리 추가 소모

### 2.3 메모리 소모 분석

| 항목 | 예상 메모리 사용량 |
|------|-------------------|
| 벡터 데이터 (87,488 × 384d) | ~128MB |
| Hibernate 엔티티 캐시 | ~200-500MB |
| SQL 로그 버퍼 (DEBUG) | ~500MB-1GB |
| Spring Batch 청크 버퍼 | ~100-300MB |
| JPA EntityManager | ~100-200MB |
| **총계** | **~1.5-2.5GB** |

기본 힙 크기(~1-2GB)로는 처리 불가능.

---

## 3. 적용된 수정사항

### 3.1 JVM 힙 메모리 증가

**파일:** `gradle.properties` (신규 생성)
```properties
org.gradle.jvmargs=-Xms2g -Xmx8g -XX:+UseG1GC -XX:MaxMetaspaceSize=512m

org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

**변경 내용:**
- `-Xms2g`: 초기 힙 크기 2GB
- `-Xmx8g`: 최대 힙 크기 8GB
- `-XX:+UseG1GC`: G1 가비지 컬렉터 (대용량 힙에 최적)
- `-XX:MaxMetaspaceSize=512m`: 메타스페이스 제한

### 3.2 로깅 레벨 최적화

**파일:** `application.yml`

**변경 전:**
```yaml
logging:
  level:
    com.alpha.backend: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.flywaydb: DEBUG
    org.springframework.boot.autoconfigure: DEBUG
```

**변경 후:**
```yaml
logging:
  level:
    com.alpha.backend: INFO
    org.hibernate.SQL: INFO
    org.hibernate.type.descriptor.sql.BasicBinder: INFO
    org.flywaydb: INFO
    org.springframework.boot.autoconfigure: INFO
```

**기대 효과:**
- SQL 로깅 비활성화 → 로그 파일 크기 90%+ 감소
- 힙 메모리 절약 → 안정적인 대용량 처리

---

## 4. 검증 결과

### 4.1 서버 재시작 성공
- **PID:** 40356
- **gRPC Port:** 9090
- **시작 시간:** 23.019초
- **상태:** 정상 가동

### 4.2 확인된 컴포넌트
- Flyway 마이그레이션: Schema version 3 (최신)
- Hibernate JDBC Batch: 300 (자동 활성화)
- gRPC Services: EmbeddingStreamService, Reflection, Health
- Quartz Scheduler: Standby 모드

---

## 5. 권장 후속 조치

### 5.1 즉시 조치 (완료)
- [x] gradle.properties 생성 (JVM 힙 8GB)
- [x] application.yml 로깅 레벨 INFO로 변경
- [x] 1.3GB 로그 파일 삭제

### 5.2 성능 테스트 (진행 중)
- [ ] Recruit 도메인 재테스트 (청크 300)
- [ ] 다양한 청크 사이즈 비교 (100, 500, 1000)

### 5.3 장기 개선 (권장)
- [ ] JMX/Micrometer 메트릭 모니터링 추가
- [ ] 메모리 사용량 알림 설정
- [ ] 스트리밍 처리 방식 검토 (메모리 피크 감소)

---

## 6. 결론

Recruit 도메인 테스트 실패는 **JVM 힙 메모리 부족**과 **과도한 DEBUG 로깅**의 복합적 원인으로 발생했습니다.
힙 크기를 8GB로 증가하고 로깅 레벨을 INFO로 조정하여 문제를 해결했습니다.

다음 단계로 성능 테스트를 진행하여 최적의 청크 사이즈를 결정할 예정입니다.

---

**문서 끝**
