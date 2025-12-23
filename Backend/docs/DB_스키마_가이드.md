# DB 스키마 가이드 v2

**작성일**: 2025-12-21
**대상**: Backend 전체 (Batch-Server, API-Server, Demo-Python)
**DB**: PostgreSQL + pgvector
**버전**: 2.0 (2025-12-21 스키마 재구조화)

> **중요**: 이 문서는 Backend 전체 프로젝트의 DB 스키마 단일 소스입니다.
> API Server, Batch Server, Demo-Python 작업 시 반드시 이 문서를 참조하세요.

---

## 개요

Alpha-Match 프로젝트의 PostgreSQL 데이터베이스 스키마 가이드입니다. 도메인별 테이블 구조, 관계, 인덱스 전략을 정의합니다.

### v2 주요 변경사항 (2025-12-21)

- **벡터 차원 통일**: 모든 벡터를 384차원으로 통일
- **타임스탬프**: `TIMESTAMP` → `TIMESTAMPTZ`
- **문자열 타입**: `VARCHAR(n)` → `TEXT`
- **테이블명 변경**: `recruit_metadata` → `recruit`, `recruit_embedding` → `recruit_skills_embedding`
- **새 테이블 추가**: `skill_category_dic`, `candidate_description`, `recruit_description`, `recruit_skill`
- **UUID 자동 생성**: `skill_category_dic`, `skill_embedding_dic`

### 도메인 구조

- **recruit**: 채용 공고 (384차원 벡터, 4-table 구조)
- **candidate**: 후보자 (384차원 벡터, 4-table 구조)
- **skill_embedding_dic**: 스킬 사전 (384차원 벡터, 2-table 구조)
- **공통**: DLQ, Checkpoint, Spring Batch, Quartz

---

## ERD (Entity Relationship Diagram)

### Skill Embedding Dictionary Domain (2-table, 사전 도메인)

```
┌──────────────────────────────┐
│  skill_category_dic          │
├──────────────────────────────┤
│ category_id (UUID) PK        │ ← 자동 생성
│ category (TEXT) UNIQUE       │
│ created_at (TIMESTAMPTZ)     │
│ updated_at (TIMESTAMPTZ)     │
└────────┬─────────────────────┘
         │ 1:N
         ▼
┌──────────────────────────────┐
│  skill_embedding_dic         │
├──────────────────────────────┤
│ skill_id (UUID) PK           │ ← 자동 생성
│ category_id (UUID) FK        │
│ skill (TEXT) UNIQUE          │
│ skill_vector (VECTOR(384))   │
│ created_at (TIMESTAMPTZ)     │
│ updated_at (TIMESTAMPTZ)     │
└──────────────────────────────┘
  IVFFlat Index on skill_vector
```

### Candidate Domain (4-table, DDD Aggregate)

```
┌──────────────────────────────┐
│  candidate (Aggregate Root)  │
├──────────────────────────────┤
│ candidate_id (UUID) PK       │
│ position_category (TEXT)     │
│ experience_years (INT) NULL  │
│ original_resume (TEXT)       │
│ created_at (TIMESTAMPTZ)     │
│ updated_at (TIMESTAMPTZ)     │
└────┬───────────┬─────────┬───┘
     │ 1:1       │ 1:N     │ 1:1
     │ CASCADE   │ CASCADE │ CASCADE
     ▼           ▼         ▼
┌──────────────────────────────┐   ┌──────────────────────────────┐   ┌──────────────────────────────────┐
│  candidate_description       │   │  candidate_skill             │   │  candidate_skills_embedding      │
├──────────────────────────────┤   ├──────────────────────────────┤   ├──────────────────────────────────┤
│ candidate_id (UUID) PK/FK    │   │ candidate_id (UUID) PK/FK    │   │ candidate_id (UUID) PK/FK        │
│ original_resume (TEXT)       │   │ skill (TEXT) PK              │   │ skills (TEXT[])                  │
│ resume_lang (TEXT)           │   │ created_at (TIMESTAMPTZ)     │   │ skills_vector (VECTOR(384))      │
│ created_at (TIMESTAMPTZ)     │   │ updated_at (TIMESTAMPTZ)     │   │ created_at (TIMESTAMPTZ)         │
│ updated_at (TIMESTAMPTZ)     │   └──────────────────────────────┘   │ updated_at (TIMESTAMPTZ)         │
└──────────────────────────────┘      Composite PK:                   └──────────────────────────────────┘
                                      (candidate_id, skill)                IVFFlat Index on skills_vector
```

### Recruit Domain (4-table)

```
┌──────────────────────────────┐
│  recruit                     │
├──────────────────────────────┤
│ recruit_id (UUID) PK         │
│ position (TEXT)              │
│ company_name (TEXT)          │
│ experience_years (INT) NULL  │
│ primary_keyword (TEXT)       │
│ english_level (TEXT)         │
│ published_at (TIMESTAMPTZ)   │
│ created_at (TIMESTAMPTZ)     │
│ updated_at (TIMESTAMPTZ)     │
└────┬───────────┬─────────┬───┘
     │ 1:1       │ 1:N     │ 1:1
     │ CASCADE   │ CASCADE │ CASCADE
     ▼           ▼         ▼
┌──────────────────────────────┐   ┌──────────────────────────────┐   ┌──────────────────────────────────┐
│  recruit_description         │   │  recruit_skill               │   │  recruit_skills_embedding        │
├──────────────────────────────┤   ├──────────────────────────────┤   ├──────────────────────────────────┤
│ recruit_id (UUID) PK/FK      │   │ recruit_id (UUID) PK/FK      │   │ recruit_id (UUID) PK/FK          │
│ long_description (TEXT)      │   │ skill (TEXT) PK              │   │ skills (TEXT[])                  │
│ description_lang (TEXT)      │   │ created_at (TIMESTAMPTZ)     │   │ skills_vector (VECTOR(384))      │
│ created_at (TIMESTAMPTZ)     │   │ updated_at (TIMESTAMPTZ)     │   │ created_at (TIMESTAMPTZ)         │
│ updated_at (TIMESTAMPTZ)     │   └──────────────────────────────┘   │ updated_at (TIMESTAMPTZ)         │
└──────────────────────────────┘      Composite PK:                   └──────────────────────────────────┘
                                      (recruit_id, skill)                  IVFFlat Index on skills_vector
```

### Common Tables

```
┌─────────────────────────────┐
│   dlq (공통)                │
├─────────────────────────────┤
│ id (BIGSERIAL) PK           │
│ domain (VARCHAR) [IDX]      │
│ failed_id (UUID)            │
│ error_message (TEXT)        │
│ payload (TEXT)              │
│ created_at (TIMESTAMPTZ)    │
└─────────────────────────────┘

┌─────────────────────────────┐
│   checkpoint (공통)         │
├─────────────────────────────┤
│ id (BIGSERIAL) PK           │
│ domain (VARCHAR) UNIQUE     │
│ last_processed_uuid (UUID)  │
│ processed_count (BIGINT)    │
│ updated_at (TIMESTAMPTZ)    │
└─────────────────────────────┘
```

> **상세 ERD**: `/Backend/docs/ERD_다이어그램.md` 참조
> **상세 명세**: `/Backend/docs/table_specification.md` 참조

---

## 벡터 차원 전략

| 도메인 | 차원 | 모델 |
|--------|------|------|
| recruit_skills_embedding | 384 | sentence-transformers/all-MiniLM-L6-v2 |
| candidate_skills_embedding | 384 | sentence-transformers/all-MiniLM-L6-v2 |
| skill_embedding_dic | 384 | sentence-transformers/all-MiniLM-L6-v2 |

**통일 이유:**
- 단일 임베딩 모델 사용으로 일관성 확보
- 메모리 효율성 향상
- 도메인 간 벡터 유사도 비교 가능

---

## 인덱스 전략

### 1. 벡터 인덱스 (IVFFlat)

```sql
-- Recruit Skills Embedding
CREATE INDEX idx_recruit_skills_vector ON recruit_skills_embedding
USING ivfflat (skills_vector vector_cosine_ops) WITH (lists = 100);

-- Candidate Skills Embedding
CREATE INDEX idx_candidate_skills_vector ON candidate_skills_embedding
USING ivfflat (skills_vector vector_cosine_ops) WITH (lists = 100);

-- Skill Embedding Dictionary
CREATE INDEX idx_skill_vector ON skill_embedding_dic
USING ivfflat (skill_vector vector_cosine_ops) WITH (lists = 100);
```

### 2. 조회 최적화 인덱스

```sql
-- Recruit
CREATE INDEX idx_recruit_position ON recruit(position);
CREATE INDEX idx_recruit_published_at ON recruit(published_at);
CREATE INDEX idx_recruit_experience_years ON recruit(experience_years);

-- Candidate
CREATE INDEX idx_candidate_position_category ON candidate(position_category);
CREATE INDEX idx_candidate_experience_years ON candidate(experience_years);

-- Skill Category
CREATE INDEX idx_skill_category ON skill_embedding_dic(category_id);
```

---

## 마이그레이션 버전

| 버전 | 파일명 | 설명 |
|------|--------|------|
| V1 | V1__init_database_schema.sql | 초기 스키마 (v1) |
| V2 | V2__restructure_schema_to_v2.sql | 스키마 재구조화 (v2, 2025-12-21) |

**V2 변경사항:**
- DROP & CREATE 방식 (벡터 차원 변경으로 인한 데이터 호환성 문제)
- 새 테이블 4개 추가
- 벡터 차원 통일 (384d)
- TIMESTAMPTZ 적용
- TEXT 타입 적용

**마이그레이션 정책**: `/Backend/docs/Flyway_마이그레이션_가이드.md` 참조

---

## 새 도메인 추가 방법

새 도메인을 추가하려면 `/Backend/Batch-Server/docs/도메인_확장_가이드.md`를 참조하세요.

---

## 트러블슈팅

### 1. Vector 차원 불일치

**증상**: `ERROR: vector dimension mismatch`

**원인**: Entity의 VECTOR_DIMENSION과 실제 데이터 차원 불일치

**해결**:
```java
// v2부터 모든 벡터는 384차원
public static final int VECTOR_DIMENSION = 384;
```

### 2. FK 제약조건 위반

**증상**: `ERROR: violates foreign key constraint`

**원인**: 부모 테이블 저장 전에 자식 테이블 저장 시도

**해결**:
```java
// 순서 보장: recruit → recruit_description, recruit_skill, recruit_skills_embedding
recruitRepository.upsertAll(recruitList);
recruitDescriptionRepository.upsertAll(descriptionList);
recruitSkillRepository.upsertAll(skillList);
recruitSkillsEmbeddingRepository.upsertAll(embeddingList);
```

### 3. UUID 자동 생성 미작동

**증상**: skill_id, category_id가 NULL

**원인**: @GeneratedValue 누락 또는 수동 설정

**해결**:
```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(name = "skill_id")
private UUID skillId;
```

---

## 참조

- **Flyway 마이그레이션 가이드**: `/Backend/docs/Flyway_마이그레이션_가이드.md`
- **테이블 명세서**: `/Backend/docs/table_specification.md`
- **도메인 확장 가이드**: `/Backend/Batch-Server/docs/도메인_확장_가이드.md`
- **pgvector 공식 문서**: https://github.com/pgvector/pgvector

---

**최종 수정일**: 2025-12-21
