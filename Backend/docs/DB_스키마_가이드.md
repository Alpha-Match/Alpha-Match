# DB 스키마 가이드

**작성일**: 2025-12-12
**대상**: Backend 전체 (Batch-Server, API-Server)
**DB**: PostgreSQL + pgvector

---

## 개요

Alpha-Match 프로젝트의 PostgreSQL 데이터베이스 스키마 가이드입니다. 도메인별 테이블 구조, 관계, 인덱스 전략을 정의합니다.

### 도메인 구조

- **recruit**: 채용 공고 (384차원 벡터)
- **candidate**: 후보자 (768차원 벡터)
- **공통**: DLQ (Dead Letter Queue), Checkpoint

---

## ERD (Entity Relationship Diagram)

```
┌─────────────────────────┐
│   recruit_metadata      │
├─────────────────────────┤
│ id (UUID) PK            │
│ company_name (TEXT)     │
│ exp_years (INT)         │
│ english_level (TEXT)    │
│ primary_keyword (TEXT)  │
│ created_at (TIMESTAMP)  │
│ updated_at (TIMESTAMP)  │
└─────────┬───────────────┘
          │ 1:1
          │ (FK)
          ▼
┌─────────────────────────┐
│   recruit_embedding     │
├─────────────────────────┤
│ id (UUID) PK/FK         │
│ vector (VECTOR(384))    │
│ updated_at (TIMESTAMP)  │
└─────────────────────────┘

┌─────────────────────────┐
│   candidate_metadata    │
├─────────────────────────┤
│ id (UUID) PK            │
│ name (VARCHAR)          │
│ skills (TEXT[])         │
│ experience_years (INT)  │
│ education_level (VAR)   │
│ preferred_location (VAR)│
│ expected_salary (INT)   │
│ created_at (TIMESTAMP)  │
│ updated_at (TIMESTAMP)  │
└─────────┬───────────────┘
          │ 1:1
          │ (FK)
          ▼
┌─────────────────────────┐
│   candidate_embedding   │
├─────────────────────────┤
│ id (UUID) PK/FK         │
│ vector (VECTOR(768))    │
│ updated_at (TIMESTAMP)  │
└─────────────────────────┘

┌─────────────────────────┐
│   dlq (공통)            │
├─────────────────────────┤
│ id (BIGSERIAL) PK       │
│ domain (VARCHAR) [IDX]  │
│ entity_id (UUID)        │
│ error_message (TEXT)    │
│ payload (JSONB)         │
│ created_at (TIMESTAMP)  │
└─────────────────────────┘

┌─────────────────────────┐
│   checkpoint (공통)     │
├─────────────────────────┤
│ domain (VARCHAR) PK     │
│ last_processed_uuid (U) │
│ updated_at (TIMESTAMP)  │
└─────────────────────────┘
```

---

## 테이블 상세

### 1. recruit_metadata

채용 공고의 메타데이터를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | UUID | PK | 공고 고유 ID |
| company_name | TEXT | NOT NULL | 회사명 |
| exp_years | INTEGER | NOT NULL, CHECK(>=0) | 필요 경력 연수 |
| english_level | TEXT | CHECK(ENUM) | 영어 수준 (NONE/BASIC/INTERMEDIATE/ADVANCED/NATIVE) |
| primary_keyword | TEXT | | 주요 키워드 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성 시간 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정 시간 (자동 갱신) |

**인덱스:**
- `idx_recruit_metadata_updated_at` (updated_at)
- `idx_recruit_metadata_company_exp` (company_name, exp_years)
- `idx_recruit_metadata_recent_updates` (updated_at DESC) WHERE updated_at > NOW() - 7일

**트리거:**
- `trigger_recruit_metadata_updated_at`: UPDATE 시 updated_at 자동 갱신

---

### 2. recruit_embedding

채용 공고의 임베딩 벡터 (384차원)를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | UUID | PK, FK → recruit_metadata(id) | 공고 ID (CASCADE DELETE) |
| vector | VECTOR(384) | NOT NULL | 384차원 임베딩 벡터 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정 시간 |

**인덱스:**
- `recruit_embedding_ivfflat` (vector) USING ivfflat (lists=100)
- `idx_recruit_embedding_updated_at` (updated_at)

**벡터 검색:**
```sql
-- L2 Distance (유사도 검색)
SELECT id, vector <-> '[0.1, 0.2, ...]'::vector AS distance
FROM recruit_embedding
ORDER BY distance
LIMIT 10;

-- Cosine Similarity (헬퍼 함수)
SELECT id, cosine_similarity(vector, '[0.1, 0.2, ...]'::vector) AS similarity
FROM recruit_embedding
ORDER BY similarity DESC
LIMIT 10;
```

---

### 3. candidate_metadata

후보자의 메타데이터를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | UUID | PK | 후보자 고유 ID |
| name | VARCHAR(255) | NOT NULL | 이름 |
| skills | TEXT[] | | 보유 기술 (PostgreSQL 배열) |
| experience_years | INTEGER | NOT NULL, CHECK(>=0) | 경력 연수 |
| education_level | VARCHAR(100) | | 학력 수준 |
| preferred_location | VARCHAR(255) | | 선호 지역 |
| expected_salary | INTEGER | CHECK(>0 OR NULL) | 희망 연봉 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성 시간 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정 시간 |

**인덱스:**
- `idx_candidate_metadata_updated_at` (updated_at)
- `idx_candidate_metadata_name` (name)
- `idx_candidate_metadata_exp_edu` (experience_years, education_level)
- `idx_candidate_metadata_skills_gin` (skills) USING GIN
- `idx_candidate_metadata_recent_updates` (updated_at DESC) WHERE updated_at > NOW() - 7일

**배열 검색:**
```sql
-- skills 배열에 'Java' 포함된 후보자
SELECT * FROM candidate_metadata WHERE 'Java' = ANY(skills);

-- skills 배열에 'Java' 또는 'Python' 포함
SELECT * FROM candidate_metadata WHERE skills && ARRAY['Java', 'Python'];
```

---

### 4. candidate_embedding

후보자의 임베딩 벡터 (768차원)를 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | UUID | PK, FK → candidate_metadata(id) | 후보자 ID (CASCADE DELETE) |
| vector | VECTOR(768) | NOT NULL | 768차원 임베딩 벡터 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정 시간 |

**인덱스:**
- `candidate_embedding_ivfflat` (vector) USING ivfflat (lists=100)
- `idx_candidate_embedding_updated_at` (updated_at)

---

### 5. dlq (Dead Letter Queue)

처리 실패한 레코드를 도메인별로 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | DLQ 레코드 ID (자동 증가) |
| domain | VARCHAR(50) | NOT NULL, CHECK(IN 'recruit','candidate') | 도메인 구분 |
| entity_id | UUID | | 실패한 엔티티의 UUID |
| error_message | TEXT | NOT NULL | 에러 메시지 |
| payload | JSONB | | 실패한 데이터 (JSON) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 실패 시간 |

**인덱스:**
- `idx_dlq_domain` (domain)
- `idx_dlq_entity_id` (entity_id)
- `idx_dlq_domain_created_at` (domain, created_at)

**조회 예시:**
```sql
-- recruit 도메인의 최근 실패 레코드
SELECT * FROM dlq
WHERE domain = 'recruit'
ORDER BY created_at DESC
LIMIT 100;

-- 특정 엔티티의 실패 이력
SELECT * FROM dlq
WHERE entity_id = 'uuid-here'
ORDER BY created_at DESC;
```

---

### 6. checkpoint (Batch Checkpoint)

배치 처리의 마지막 처리 UUID를 도메인별로 저장합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| domain | VARCHAR(50) | PK, CHECK(IN 'recruit','candidate') | 도메인 구분 (PK) |
| last_processed_uuid | UUID | | 마지막 처리된 UUID |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 마지막 업데이트 시간 |

**초기 데이터:**
```sql
INSERT INTO checkpoint (domain, last_processed_uuid, updated_at)
VALUES
    ('recruit', NULL, NOW()),
    ('candidate', NULL, NOW());
```

**사용 예시:**
```sql
-- recruit 도메인의 마지막 체크포인트 조회
SELECT last_processed_uuid FROM checkpoint WHERE domain = 'recruit';

-- 체크포인트 업데이트
UPDATE checkpoint
SET last_processed_uuid = 'new-uuid-here'
WHERE domain = 'recruit';
```

---

## 헬퍼 함수 및 뷰

### 함수

#### 1. cosine_similarity(a vector, b vector) → float

코사인 유사도를 계산합니다.

```sql
SELECT cosine_similarity(
    (SELECT vector FROM recruit_embedding WHERE id = 'uuid1'),
    (SELECT vector FROM recruit_embedding WHERE id = 'uuid2')
);
```

#### 2. get_domain_stats(domain_name VARCHAR) → TABLE

도메인별 통계를 조회합니다.

```sql
SELECT * FROM get_domain_stats('recruit');

-- 결과:
-- metadata_count | embedding_count | dlq_count | last_processed_uuid | last_checkpoint_time
```

### 뷰

#### v_all_domain_stats

전체 도메인 통계를 조회합니다 (모니터링용).

```sql
SELECT * FROM v_all_domain_stats;

-- 결과:
-- domain    | metadata_count | embedding_count | dlq_count | last_processed_uuid | last_checkpoint_time
-- recruit   | 141897         | 141897          | 5         | uuid-...            | 2025-12-12 10:30:00
-- candidate | 0              | 0               | 0         | NULL                | 2025-12-12 10:00:00
```

---

## 인덱스 전략

### 1. 기본 인덱스
- **PK**: 자동 생성 (B-Tree)
- **FK**: 명시적으로 생성하지 않음 (조회 패턴에 따라)

### 2. 조회 최적화 인덱스
- `updated_at`: 배치 처리 시 증분 조회
- 복합 인덱스: 자주 함께 필터링되는 컬럼 조합

### 3. 벡터 인덱스
- **IVFFlat**: 빠른 근사 검색, 적은 메모리 (기본)
- **HNSW**: 더 빠른 검색, 더 많은 메모리 (프로덕션 옵션)

### 4. Partial 인덱스
- 최근 7일 데이터: 배치 처리 최적화

### 5. GIN 인덱스
- PostgreSQL 배열 타입: skills 검색

---

## 마이그레이션 버전

| 버전 | 파일명 | 설명 |
|------|--------|------|
| V1 | V1__init_schema.sql | 초기 스키마 (recruit 도메인) |
| V2 | V2__add_candidate_schema.sql | candidate 도메인 추가 |
| V3 | V3__add_domain_to_common_tables.sql | DLQ, Checkpoint 도메인 확장 |
| V4 | V4__add_performance_indexes.sql | 성능 최적화 인덱스 |
| V5 | V5__add_constraints_and_functions.sql | 제약조건, 트리거, 함수 |

**마이그레이션 정책**: `/Backend/docs/Flyway_마이그레이션_가이드.md` 참조

---

## 새 도메인 추가 방법

새 도메인(예: `company`)을 추가하려면:

### 1. Flyway 마이그레이션 파일 작성

**V6__add_company_schema.sql**:
```sql
-- Company Metadata
CREATE TABLE company_metadata (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    industry VARCHAR(100),
    employee_count INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Company Embedding (512차원 예시)
CREATE TABLE company_embedding (
    id UUID PRIMARY KEY REFERENCES company_metadata(id) ON DELETE CASCADE,
    vector VECTOR(512) NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_company_metadata_updated_at ON company_metadata(updated_at);
CREATE INDEX company_embedding_ivfflat ON company_embedding USING ivfflat (vector vector_l2_ops) WITH (lists = 100);
```

**V7__add_company_domain_support.sql**:
```sql
-- DLQ 제약조건 업데이트
ALTER TABLE dlq DROP CONSTRAINT IF EXISTS chk_dlq_domain;
ALTER TABLE dlq ADD CONSTRAINT chk_dlq_domain CHECK (domain IN ('recruit', 'candidate', 'company'));

-- Checkpoint 제약조건 업데이트
ALTER TABLE checkpoint DROP CONSTRAINT IF EXISTS chk_checkpoint_domain;
ALTER TABLE checkpoint ADD CONSTRAINT chk_checkpoint_domain CHECK (domain IN ('recruit', 'candidate', 'company'));

-- Checkpoint 초기 레코드
INSERT INTO checkpoint (domain, last_processed_uuid, updated_at)
VALUES ('company', NULL, NOW());
```

### 2. Entity 클래스 작성

```java
// CompanyMetadataEntity.java
@Entity
@Table(name = "company_metadata")
public class CompanyMetadataEntity extends BaseMetadataEntity {
    private String name;
    private String industry;
    private Integer employeeCount;

    @Override
    public String getDomainType() {
        return "company";
    }
}

// CompanyEmbeddingEntity.java
@Entity
@Table(name = "company_embedding")
public class CompanyEmbeddingEntity extends BaseEmbeddingEntity {
    public static final int VECTOR_DIMENSION = 512;

    @Override
    public String getDomainType() {
        return "company";
    }

    @Override
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }
}
```

### 3. Repository 작성

```java
public interface CompanyMetadataRepository extends JpaRepository<CompanyMetadataEntity, UUID> {
    // Native query for upsert
}

public interface CompanyEmbeddingRepository extends JpaRepository<CompanyEmbeddingEntity, UUID> {
    // Native query for upsert
}
```

### 4. 헬퍼 함수 업데이트

**V8__update_helper_functions.sql**:
```sql
-- get_domain_stats 함수 업데이트
CREATE OR REPLACE FUNCTION get_domain_stats(domain_name VARCHAR)
RETURNS TABLE(...) AS $$
BEGIN
    -- 'company' 케이스 추가
END;
$$ LANGUAGE plpgsql;

-- v_all_domain_stats 뷰 업데이트
CREATE OR REPLACE VIEW v_all_domain_stats AS
SELECT ... FROM company_metadata ...
UNION ALL ...;
```

---

## 트러블슈팅

### 1. Vector 차원 불일치

**증상**: `ERROR: vector dimension mismatch`

**원인**: Entity의 VECTOR_DIMENSION과 실제 데이터 차원 불일치

**해결**:
```java
// Entity에서 차원 검증
if (vectorArray.length != VECTOR_DIMENSION) {
    throw new IllegalArgumentException("Vector dimension mismatch");
}
```

### 2. FK 제약조건 위반

**증상**: `ERROR: violates foreign key constraint`

**원인**: metadata 저장 전에 embedding 저장 시도

**해결**:
```java
// metadata → embedding 순서 보장
metadataRepository.upsertAll(metadataList);
embeddingRepository.upsertAll(embeddingList);
```

### 3. Flyway 마이그레이션 실패

**증상**: `FlywayException: Validate failed`

**원인**: 이미 적용된 마이그레이션 파일 수정

**해결**:
- 절대 수정하지 말 것
- 새 버전 파일로 변경사항 추가

### 4. 벡터 인덱스 성능 저하

**증상**: 유사도 검색이 느림

**해결**:
- IVFFlat lists 파라미터 조정 (데이터 크기의 √n)
- HNSW 인덱스로 전환 (V4 참조)

---

## 참조

- **Flyway 마이그레이션 가이드**: `/Backend/docs/Flyway_마이그레이션_가이드.md`
- **도메인 확장 가이드**: `/Backend/Batch-Server/docs/도메인_확장_가이드.md`
- **pgvector 공식 문서**: https://github.com/pgvector/pgvector

---

**최종 수정일**: 2025-12-12
