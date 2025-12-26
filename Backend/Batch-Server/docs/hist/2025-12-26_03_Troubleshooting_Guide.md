# Troubleshooting Guide - 2025-12-26

**작성일:** 2025-12-26
**작성자:** Claude Opus 4.5
**버전:** 1.0

---

## 개요

이 문서는 Alpha-Match Batch Server 성능 테스트 과정에서 발생한 문제들과 해결 방법을 정리한 트러블슈팅 가이드입니다.

---

## 1. OOM (Out of Memory) 크래시

### 1.1 증상
- Recruit 도메인 테스트 시 약 42분 후 서버 크래시
- gRPC 연결 리셋 오류: `StatusCode.UNAVAILABLE, details = "Connection reset"`
- 0 bytes 전송됨 (데이터 수신 실패)

### 1.2 로그 분석
```
FAILURE: Build failed with an exception.
* What went wrong:
Java heap space
```

### 1.3 원인
1. **JVM 힙 메모리 부족**
   - 기본 힙 크기 (~1-2GB)로 87,488개 레코드 처리 시도
   - 각 레코드는 384차원 벡터 임베딩 포함

2. **과도한 DEBUG 로깅**
   ```yaml
   logging:
     level:
       org.hibernate.SQL: DEBUG
       org.hibernate.type.descriptor.sql.BasicBinder: TRACE
   ```
   - 모든 SQL 문과 파라미터 바인딩 로깅
   - **1.3GB 로그 파일** 생성으로 힙 메모리 추가 소모

### 1.4 해결 방법

#### Step 1: JVM 힙 메모리 증가
**파일:** `gradle.properties` (신규 생성)
```properties
org.gradle.jvmargs=-Xms2g -Xmx8g -XX:+UseG1GC -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

#### Step 2: 로깅 레벨 최적화
**파일:** `src/main/resources/application.yml`
```yaml
# 변경 전
logging:
  level:
    com.alpha.backend: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# 변경 후
logging:
  level:
    com.alpha.backend: INFO
    org.hibernate.SQL: INFO
    org.hibernate.type.descriptor.sql.BasicBinder: INFO
```

### 1.5 결과
- OOM 없이 206,334건 처리 완료
- 44분 46초 동안 안정적 처리

---

## 2. 데이터 파일 경로 오류

### 2.1 증상
```json
{"detail":"데이터 파일을 찾을 수 없습니다: .../data/recruit_v1.pkl"}
```

### 2.2 원인
- 파일명 불일치: `recruit_v1.pkl` vs `recruitment_v1.pkl`

### 2.3 해결 방법
```bash
# 올바른 파일명 확인
ls Demo-Python/data/*.pkl

# 올바른 API 호출
curl -X POST "http://localhost:8000/data/ingest/recruit?file_name=recruitment_v1.pkl"
```

### 2.4 사용 가능한 데이터 파일
| 파일명 | 크기 | 용도 |
|--------|------|------|
| `recruitment_v1.pkl` | 471MB | Recruit 도메인 |
| `candidate_v1.pkl` | 454MB | Candidate 도메인 |
| `skill_embeddings_from_json.pkl` | 366KB | Skill Dictionary |

---

## 3. gRPC 포트 충돌

### 3.1 증상
```
Port 9090 already in use
```

### 3.2 원인
- 이전 Batch Server 프로세스가 종료되지 않음
- 동일 포트에서 새 서버 시작 시도

### 3.3 해결 방법
```bash
# 포트 사용 프로세스 확인
netstat -ano | findstr :9090

# 프로세스 종료 (Windows)
taskkill /F /PID <PID>

# 또는 PowerShell
powershell -Command "Get-Process java | Stop-Process -Force"
```

### 3.4 예방 방법
서버 시작 전 항상 포트 상태 확인:
```bash
netstat -ano | findstr ":9090"
netstat -ano | findstr ":8000"
```

---

## 4. Flyway 마이그레이션 오류

### 4.1 증상
- 마이그레이션 스크립트 실행 실패
- 스키마 불일치 오류

### 4.2 원인
- 기존 테이블과 마이그레이션 스크립트 충돌
- 수동 스키마 변경 후 Flyway 히스토리 불일치

### 4.3 해결 방법
```sql
-- Flyway 히스토리 테이블 확인
SELECT * FROM flyway_schema_history;

-- 필요시 히스토리 정리 (주의: 개발 환경에서만)
DELETE FROM flyway_schema_history WHERE version = 'X';

-- 또는 DB 완전 초기화
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

---

## 5. 서버 상태 확인 명령어 모음

### 5.1 포트 확인
```bash
# Batch Server (gRPC)
netstat -ano | findstr ":9090"

# Python Server (HTTP)
netstat -ano | findstr ":8000"

# PostgreSQL
netstat -ano | findstr ":5433"
```

### 5.2 프로세스 확인
```bash
# Java 프로세스
tasklist | findstr java

# Python 프로세스
tasklist | findstr python
```

### 5.3 로그 확인
```bash
# Batch Server 로그 (최신)
tail -50 Backend/Batch-Server/startup_optimized.log

# 특정 패턴 검색
grep -E "ERROR|Exception" Backend/Batch-Server/*.log
```

---

## 6. 성능 문제 진단

### 6.1 처리량 저하 시 확인사항

1. **JVM 메모리 사용량**
   ```bash
   # PowerShell
   Get-Process java | Select-Object Id, WorkingSet, VirtualMemorySize
   ```

2. **DB 커넥션 풀**
   - HikariCP 설정 확인 (`application.yml`)
   - `maximum-pool-size: 20` (기본값)

3. **청크 사이즈**
   - 현재: 300 (Batch Server), 100 (Python Streaming)
   - 메모리 부족 시 감소, 처리량 향상 시 증가

### 6.2 DB 잠금 확인
```sql
-- PostgreSQL 활성 잠금 확인
SELECT * FROM pg_locks WHERE NOT granted;

-- 활성 쿼리 확인
SELECT * FROM pg_stat_activity WHERE state = 'active';
```

---

## 7. 권장 설정값

### 7.1 JVM 설정 (gradle.properties)
```properties
# 대용량 데이터 처리용
org.gradle.jvmargs=-Xms2g -Xmx8g -XX:+UseG1GC -XX:MaxMetaspaceSize=512m

# 중간 규모 (메모리 제한 환경)
org.gradle.jvmargs=-Xms1g -Xmx4g -XX:+UseG1GC
```

### 7.2 로깅 레벨 (application.yml)
```yaml
# 프로덕션/성능 테스트
logging:
  level:
    com.alpha.backend: INFO
    org.hibernate.SQL: INFO

# 디버깅 (개발 환경)
logging:
  level:
    com.alpha.backend: DEBUG
    org.hibernate.SQL: DEBUG  # 주의: 대용량 로그 생성
```

### 7.3 HikariCP 설정
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20    # 동시 커넥션 수
      minimum-idle: 5          # 최소 유휴 커넥션
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 8. 체크리스트

### 8.1 서버 시작 전
- [ ] 포트 9090, 8000 사용 가능 확인
- [ ] PostgreSQL 실행 확인 (포트 5433)
- [ ] gradle.properties JVM 설정 확인
- [ ] 로깅 레벨 확인 (성능 테스트 시 INFO)

### 8.2 테스트 실행 전
- [ ] 데이터 파일 경로 확인
- [ ] 서버 로그 모니터링 준비
- [ ] 충분한 디스크 공간 확인

### 8.3 오류 발생 시
- [ ] 서버 로그 확인 (`tail -100 *.log`)
- [ ] 포트 상태 확인 (`netstat -ano`)
- [ ] 프로세스 상태 확인 (`tasklist`)
- [ ] DB 잠금 확인 (`pg_locks`)

---

## 참고 문서

- OOM 크래시 분석: `docs/hist/2025-12-26_01_OOM_Crash_Analysis_Report.md`
- 성능 테스트 결과: `docs/hist/2025-12-26_02_Performance_Test_Report.md`

---

**문서 끝**
