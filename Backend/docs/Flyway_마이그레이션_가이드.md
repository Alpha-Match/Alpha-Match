# Flyway 마이그레이션 가이드

**작성일**: 2025-12-12
**대상**: Backend 전체 (Batch-Server, API-Server)
**Flyway 버전**: 최신 (Spring Boot 4.0 기본 제공)

---

## 개요

Flyway는 데이터베이스 스키마 버전 관리 도구입니다. SQL 마이그레이션 파일을 통해 데이터베이스 변경 이력을 추적하고, 자동으로 적용합니다.

### 핵심 원칙

1. **절대 수정 금지**: 이미 적용된 마이그레이션 파일은 절대 수정하지 않습니다.
2. **순차 적용**: 마이그레이션은 버전 순서대로 자동 적용됩니다.
3. **멱등성**: 여러 번 실행해도 안전해야 합니다.
4. **Rollback 불가**: Flyway는 rollback을 지원하지 않습니다. 복구는 새 마이그레이션으로 수행합니다.

---

## 디렉토리 구조

```
Backend/Batch-Server/src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_candidate_schema.sql
├── V3__add_domain_to_common_tables.sql
├── V4__add_performance_indexes.sql
├── V5__add_constraints_and_functions.sql
└── V6__add_new_domain.sql (예시)
```

---

## 네이밍 규칙

### 파일명 형식

```
V{버전}__{설명}.sql
```

**구성 요소:**
- `V`: 버전을 의미하는 접두사 (필수, 대문자)
- `{버전}`: 정수 버전 번호 (1, 2, 3, ...) 또는 시맨틱 버전 (1.0, 1.1, 2.0)
- `__`: 두 개의 언더스코어 (필수)
- `{설명}`: 영문 설명 (snake_case)
- `.sql`: 파일 확장자

### 예시

**올바른 예시:**
```
V1__init_schema.sql
V2__add_candidate_schema.sql
V3__add_domain_to_common_tables.sql
V4__add_performance_indexes.sql
V5__add_constraints_and_functions.sql
V6__alter_dlq_add_domain.sql
V7__create_index_metadata_uuid.sql
```

**잘못된 예시:**
```
v1__init_schema.sql              # 소문자 v
V1_init_schema.sql               # 언더스코어 1개
V1 init schema.sql               # 공백
V1__init-schema.sql              # 하이픈 (snake_case 권장)
1__init_schema.sql               # V 접두사 없음
V1.1__init_schema.sql            # 시맨틱 버전 (가능하지만 정수 권장)
```

---

## 버전 관리 정책

### 1. 버전 번호 할당

**규칙:**
- 정수 버전 사용 (V1, V2, V3, ...)
- 연속된 번호 사용 (건너뛰지 않음)
- 팀 내 조율 필요 (동시 작업 시 충돌 방지)

**팀 협업 시:**
- Git 브랜치별로 버전 예약
- Merge 전 버전 번호 재조정
- 또는 시맨틱 버전 사용 (V1.1, V1.2, V2.1)

### 2. 마이그레이션 적용

**자동 적용 (권장):**
```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

**수동 적용:**
```bash
./gradlew flywayMigrate
```

### 3. 마이그레이션 검증

```bash
# 현재 상태 확인
./gradlew flywayInfo

# 마이그레이션 검증
./gradlew flywayValidate

# 마이그레이션 정리 (개발 환경만)
./gradlew flywayClean
```

---

## 마이그레이션 작성 가이드

### 1. 기본 템플릿

```sql
-- V{버전}__{설명}.sql
-- 작성일: YYYY-MM-DD
-- 목적: 변경 사항 설명

-- 1. 테이블 생성
CREATE TABLE example_table (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 2. 인덱스 생성
CREATE INDEX idx_example_name ON example_table(name);

-- 3. 통계 정보 갱신
ANALYZE example_table;
```

### 2. 멱등성 보장

**IF NOT EXISTS 사용:**
```sql
-- 테이블
CREATE TABLE IF NOT EXISTS example_table (...);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_example_name ON example_table(name);

-- Extension
CREATE EXTENSION IF NOT EXISTS vector;
```

**조건부 ALTER:**
```sql
-- 컬럼 추가 (PostgreSQL 9.6+)
ALTER TABLE example_table
ADD COLUMN IF NOT EXISTS new_column VARCHAR(100);

-- 제약조건 추가
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_example') THEN
        ALTER TABLE example_table ADD CONSTRAINT chk_example CHECK (value > 0);
    END IF;
END $$;
```

### 3. 트랜잭션 관리

**기본 트랜잭션 (자동):**
```sql
-- 모든 DDL이 하나의 트랜잭션으로 실행됨
CREATE TABLE ...;
CREATE INDEX ...;
-- 오류 발생 시 전체 롤백
```

**명시적 트랜잭션 (비권장):**
```sql
BEGIN;
CREATE TABLE ...;
COMMIT;
```

---

## 주요 마이그레이션 패턴

### 1. 테이블 생성

```sql
-- V2__add_candidate_schema.sql
CREATE TABLE candidate_metadata (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_candidate_metadata_updated_at ON candidate_metadata(updated_at);

ANALYZE candidate_metadata;
```

### 2. 컬럼 추가

```sql
-- V6__add_column_to_recruit_metadata.sql
ALTER TABLE recruit_metadata
ADD COLUMN IF NOT EXISTS salary_range INTEGER;

-- 기본값 설정 (기존 데이터)
UPDATE recruit_metadata SET salary_range = 0 WHERE salary_range IS NULL;
```

### 3. 제약조건 추가

```sql
-- V7__add_constraints.sql
ALTER TABLE recruit_metadata
ADD CONSTRAINT chk_recruit_exp_years_positive
CHECK (exp_years >= 0);

ALTER TABLE recruit_metadata
ADD CONSTRAINT chk_recruit_english_level
CHECK (english_level IN ('NONE', 'BASIC', 'INTERMEDIATE', 'ADVANCED', 'NATIVE'));
```

### 4. 인덱스 추가/삭제

```sql
-- V8__optimize_indexes.sql

-- 기존 인덱스 삭제
DROP INDEX IF EXISTS old_idx_name;

-- 새 인덱스 생성
CREATE INDEX idx_recruit_metadata_company_exp
ON recruit_metadata(company_name, exp_years);

-- Partial 인덱스
CREATE INDEX idx_recruit_metadata_recent_updates
ON recruit_metadata(updated_at DESC)
WHERE updated_at > NOW() - INTERVAL '7 days';
```

### 5. 트리거 및 함수

```sql
-- V9__add_triggers.sql

-- 트리거 함수 생성
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER trigger_recruit_metadata_updated_at
BEFORE UPDATE ON recruit_metadata
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

### 6. 데이터 마이그레이션

```sql
-- V10__migrate_old_data.sql

-- 기존 데이터 변환
UPDATE recruit_metadata
SET primary_keyword = UPPER(primary_keyword)
WHERE primary_keyword IS NOT NULL;

-- 새 테이블로 데이터 이동
INSERT INTO new_table (id, name, created_at)
SELECT id, name, NOW()
FROM old_table
WHERE migrated = false;
```

### 7. 벡터 인덱스

```sql
-- V11__add_vector_index.sql

-- pgvector extension (이미 설치됨)
CREATE EXTENSION IF NOT EXISTS vector;

-- IVFFlat 인덱스
CREATE INDEX candidate_embedding_ivfflat
ON candidate_embedding USING ivfflat (vector vector_l2_ops)
WITH (lists = 100);

-- HNSW 인덱스 (프로덕션 옵션)
CREATE INDEX candidate_embedding_hnsw
ON candidate_embedding USING hnsw (vector vector_l2_ops)
WITH (m = 16, ef_construction = 64);
```

---

## 롤백 및 복구

### 1. 롤백 전략

Flyway는 자동 롤백을 지원하지 않습니다. 복구는 새 마이그레이션으로 수행합니다.

**잘못된 마이그레이션 복구:**

1. **문제 파악**: `./gradlew flywayInfo`
2. **복구 마이그레이션 작성**:

```sql
-- V12__rollback_v11_changes.sql

-- V11에서 추가한 인덱스 삭제
DROP INDEX IF EXISTS candidate_embedding_hnsw;

-- V11에서 추가한 컬럼 삭제
ALTER TABLE candidate_metadata DROP COLUMN IF EXISTS new_column;
```

3. **적용**: `./gradlew flywayMigrate`

### 2. 개발 환경 초기화

**주의: 프로덕션에서는 절대 사용 금지**

```bash
# 모든 마이그레이션 제거 (데이터 삭제)
./gradlew flywayClean

# 재적용
./gradlew flywayMigrate
```

---

## 팀 협업 전략

### 1. 브랜치별 버전 예약

**예시:**
- `feat/user-auth`: V10~V19
- `feat/payment`: V20~V29
- `feat/notification`: V30~V39

### 2. 시맨틱 버전 사용

**예시:**
- `main`: V1.0, V2.0, V3.0
- `feat/user-auth`: V1.1, V1.2, V1.3
- `feat/payment`: V2.1, V2.2, V2.3

### 3. Merge 시 버전 재조정

**Merge 전:**
```
feature-branch:
  V10__add_user_table.sql
  V11__add_user_index.sql

main:
  V1~V9 (기존)
```

**Merge 후:**
```
main:
  V1~V9 (기존)
  V10__add_user_table.sql
  V11__add_user_index.sql
```

**충돌 발생 시:**
- 파일명 변경 (V10 → V12)
- 또는 시맨틱 버전 사용

---

## 테스트 전략

### 1. 로컬 테스트

```bash
# Docker로 PostgreSQL 실행
docker run -d \
  --name postgres-test \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  pgvector/pgvector:pg16

# 마이그레이션 적용
./gradlew flywayMigrate

# 검증
./gradlew flywayInfo
./gradlew flywayValidate
```

### 2. CI/CD 테스트

```yaml
# .github/workflows/ci.yml
jobs:
  test:
    services:
      postgres:
        image: pgvector/pgvector:pg16
        env:
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432

    steps:
      - name: Run Flyway Migration
        run: ./gradlew flywayMigrate

      - name: Validate Migration
        run: ./gradlew flywayValidate
```

---

## 트러블슈팅

### 1. Checksum Mismatch

**증상:**
```
ERROR: Validate failed: Migration checksum mismatch
```

**원인**: 이미 적용된 마이그레이션 파일을 수정함

**해결:**
```bash
# 개발 환경: 초기화 후 재적용
./gradlew flywayClean
./gradlew flywayMigrate

# 프로덕션: 체크섬 재계산 (신중히)
./gradlew flywayRepair
```

### 2. Migration Failed

**증상:**
```
ERROR: Migration V5__xxx failed
```

**원인**: SQL 구문 오류 또는 제약조건 위반

**해결:**
1. 오류 확인: 로그 분석
2. 수동 복구:
```sql
-- flyway_schema_history 테이블 확인
SELECT * FROM flyway_schema_history WHERE success = false;

-- 실패한 마이그레이션 제거
DELETE FROM flyway_schema_history WHERE version = '5';
```
3. 마이그레이션 파일 수정
4. 재적용

### 3. Out of Order

**증상:**
```
ERROR: Detected out of order migration
```

**원인**: V10 적용 후 V9 추가

**해결:**
```yaml
# application.yml
spring:
  flyway:
    out-of-order: true  # 경고만 (비권장)
```

**권장:** 버전 번호 재조정

---

## 모니터링

### 1. flyway_schema_history 테이블

Flyway는 적용된 마이그레이션 이력을 저장합니다.

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

**컬럼:**
- `installed_rank`: 적용 순서
- `version`: 버전 번호
- `description`: 설명
- `type`: 타입 (SQL)
- `script`: 파일명
- `checksum`: 체크섬
- `installed_on`: 적용 시간
- `execution_time`: 실행 시간 (ms)
- `success`: 성공 여부

### 2. 마이그레이션 상태 확인

```bash
./gradlew flywayInfo
```

**출력 예시:**
```
+-----------+---------+---------------------+------+---------------------+
| Category  | Version | Description         | Type | Installed On        |
+-----------+---------+---------------------+------+---------------------+
| Success   | 1       | init schema         | SQL  | 2025-12-12 10:00:00 |
| Success   | 2       | add candidate       | SQL  | 2025-12-12 10:01:00 |
| Success   | 3       | add domain          | SQL  | 2025-12-12 10:02:00 |
| Pending   | 4       | add indexes         | SQL  |                     |
+-----------+---------+---------------------+------+---------------------+
```

---

## 참조

- **DB 스키마 가이드**: `/Backend/docs/DB_스키마_가이드.md`
- **도메인 확장 가이드**: `/Backend/Batch-Server/docs/도메인_확장_가이드.md`
- **Flyway 공식 문서**: https://flywaydb.org/documentation/

---

**최종 수정일**: 2025-12-12
