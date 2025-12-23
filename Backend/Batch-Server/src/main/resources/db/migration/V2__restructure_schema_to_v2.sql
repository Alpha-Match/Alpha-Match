-- ============================================================================
-- Alpha-Match Batch Server - Database Schema V2
-- ============================================================================
-- Version: 2.0
-- Date: 2025-12-21
-- Description: 스키마 재구조화 (벡터 차원 변경 768→384, 테이블 구조 개선)
-- ============================================================================

-- ============================================================================
-- Section 1: Drop Existing Tables (역순)
-- ============================================================================

-- Drop Indexes
DROP INDEX IF EXISTS idx_candidate_skills_vector;
DROP INDEX IF EXISTS idx_skill_vector;
DROP INDEX IF EXISTS idx_recruit_vector;
DROP INDEX IF EXISTS idx_candidate_position_category;
DROP INDEX IF EXISTS idx_candidate_experience_years;
DROP INDEX IF EXISTS idx_recruit_primary_keyword;
DROP INDEX IF EXISTS idx_recruit_exp_years;

-- Drop Domain Tables
DROP TABLE IF EXISTS candidate_skills_embedding CASCADE;
DROP TABLE IF EXISTS candidate_skill CASCADE;
DROP TABLE IF EXISTS candidate CASCADE;

DROP TABLE IF EXISTS recruit_embedding CASCADE;
DROP TABLE IF EXISTS recruit_metadata CASCADE;

DROP TABLE IF EXISTS skill_embedding_dic CASCADE;

-- ============================================================================
-- Section 2: Skill Embedding Dictionary Domain (먼저 생성)
-- ============================================================================

-- 2.1 skill_category_dic (직종 카테고리 사전)
CREATE TABLE skill_category_dic (
    category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE skill_category_dic IS '직종 카테고리 사전 (Backend, Frontend 등)';
COMMENT ON COLUMN skill_category_dic.category_id IS '카테고리 UUID (자동 생성)';
COMMENT ON COLUMN skill_category_dic.category IS '직종명';

-- 2.2 skill_embedding_dic (기술 스택 임베딩 사전)
CREATE TABLE skill_embedding_dic (
    skill_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    skill TEXT NOT NULL UNIQUE,
    skill_vector VECTOR(384) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_skill_category
        FOREIGN KEY (category_id)
        REFERENCES skill_category_dic(category_id)
        ON DELETE RESTRICT
);

COMMENT ON TABLE skill_embedding_dic IS '기술 스택 임베딩 사전';
COMMENT ON COLUMN skill_embedding_dic.skill_id IS '스킬 UUID (자동 생성)';
COMMENT ON COLUMN skill_embedding_dic.category_id IS '카테고리 FK';
COMMENT ON COLUMN skill_embedding_dic.skill IS '스킬명 (유니크)';
COMMENT ON COLUMN skill_embedding_dic.skill_vector IS '스킬 벡터 (384d)';

-- ============================================================================
-- Section 3: Candidate Domain
-- ============================================================================

-- 3.1 candidate
CREATE TABLE candidate (
    candidate_id UUID NOT NULL,
    position_category TEXT NOT NULL,
    experience_years INTEGER CHECK (experience_years > 0),
    original_resume TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (candidate_id)
);

COMMENT ON TABLE candidate IS '지원자 이력서 메타 데이터';
COMMENT ON COLUMN candidate.candidate_id IS '지원자 아이디 (UUID)';
COMMENT ON COLUMN candidate.position_category IS '직종 카테고리';
COMMENT ON COLUMN candidate.experience_years IS '경력 (연), NULL=신입/경력무관';
COMMENT ON COLUMN candidate.original_resume IS '이력서 원문';

-- 3.2 candidate_description (이력서 상세 원문)
CREATE TABLE candidate_description (
    candidate_id UUID NOT NULL,
    original_resume TEXT NOT NULL,
    resume_lang TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (candidate_id),
    CONSTRAINT fk_candidate_description
        FOREIGN KEY (candidate_id)
        REFERENCES candidate(candidate_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE candidate_description IS '이력서 상세 원문 (Markdown)';
COMMENT ON COLUMN candidate_description.candidate_id IS 'candidate.candidate_id';
COMMENT ON COLUMN candidate_description.original_resume IS '이력서 원문';
COMMENT ON COLUMN candidate_description.resume_lang IS '원문 언어';

-- 3.3 candidate_skill (기술 스택 상세, 복합 PK)
CREATE TABLE candidate_skill (
    candidate_id UUID NOT NULL,
    skill TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (candidate_id, skill),
    CONSTRAINT fk_candidate_skill_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidate(candidate_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE candidate_skill IS '지원자 기술 스택 상세 (1:N)';
COMMENT ON COLUMN candidate_skill.candidate_id IS 'candidate.candidate_id';
COMMENT ON COLUMN candidate_skill.skill IS '보유 스킬명';

-- 3.4 candidate_skills_embedding (기술 스택 뭉치 벡터)
CREATE TABLE candidate_skills_embedding (
    candidate_id UUID NOT NULL,
    skills TEXT[] NOT NULL,
    skills_vector VECTOR(384) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (candidate_id),
    CONSTRAINT fk_candidate_skills_embedding
        FOREIGN KEY (candidate_id)
        REFERENCES candidate(candidate_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE candidate_skills_embedding IS '지원자 기술 스택 뭉치 벡터';
COMMENT ON COLUMN candidate_skills_embedding.candidate_id IS 'candidate.candidate_id';
COMMENT ON COLUMN candidate_skills_embedding.skills IS '보유 스킬명 배열';
COMMENT ON COLUMN candidate_skills_embedding.skills_vector IS '기술 스택 벡터 (384d)';

-- ============================================================================
-- Section 4: Recruit Domain
-- ============================================================================

-- 4.1 recruit
CREATE TABLE recruit (
    recruit_id UUID NOT NULL,
    position TEXT NOT NULL,
    company_name TEXT NOT NULL,
    experience_years INTEGER CHECK (experience_years > 0),
    primary_keyword TEXT,
    english_level TEXT,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (recruit_id)
);

COMMENT ON TABLE recruit IS '채용 공고 메타 데이터';
COMMENT ON COLUMN recruit.recruit_id IS '채용 공고 ID (UUID)';
COMMENT ON COLUMN recruit.position IS '포지션 원문';
COMMENT ON COLUMN recruit.company_name IS '회사명';
COMMENT ON COLUMN recruit.experience_years IS '요구 경력 (연), NULL=신입/경력무관';
COMMENT ON COLUMN recruit.primary_keyword IS '주요 키워드';
COMMENT ON COLUMN recruit.english_level IS '영어 수준';
COMMENT ON COLUMN recruit.published_at IS '게시 날짜';

-- 4.2 recruit_description (채용 공고 상세 원문)
CREATE TABLE recruit_description (
    recruit_id UUID NOT NULL,
    long_description TEXT NOT NULL,
    description_lang TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (recruit_id),
    CONSTRAINT fk_recruit_description
        FOREIGN KEY (recruit_id)
        REFERENCES recruit(recruit_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE recruit_description IS '채용 공고 상세 원문 (Markdown)';
COMMENT ON COLUMN recruit_description.recruit_id IS 'recruit.recruit_id';
COMMENT ON COLUMN recruit_description.long_description IS '채용 공고 원문';
COMMENT ON COLUMN recruit_description.description_lang IS '원문 언어';

-- 4.3 recruit_skill (기술 스택 상세, 복합 PK)
CREATE TABLE recruit_skill (
    recruit_id UUID NOT NULL,
    skill TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (recruit_id, skill),
    CONSTRAINT fk_recruit_skill_recruit
        FOREIGN KEY (recruit_id)
        REFERENCES recruit(recruit_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE recruit_skill IS '채용 공고 기술 스택 상세 (1:N)';
COMMENT ON COLUMN recruit_skill.recruit_id IS 'recruit.recruit_id';
COMMENT ON COLUMN recruit_skill.skill IS '요구 기술 스택명';

-- 4.4 recruit_skills_embedding (기술 스택 뭉치 벡터)
CREATE TABLE recruit_skills_embedding (
    recruit_id UUID NOT NULL,
    skills TEXT[] NOT NULL,
    skills_vector VECTOR(384) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (recruit_id),
    CONSTRAINT fk_recruit_skills_embedding
        FOREIGN KEY (recruit_id)
        REFERENCES recruit(recruit_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE recruit_skills_embedding IS '채용 공고 기술 스택 뭉치 벡터';
COMMENT ON COLUMN recruit_skills_embedding.recruit_id IS 'recruit.recruit_id';
COMMENT ON COLUMN recruit_skills_embedding.skills IS '요구 기술 스택 배열';
COMMENT ON COLUMN recruit_skills_embedding.skills_vector IS '기술 스택 벡터 (384d)';

-- ============================================================================
-- Section 5: Indexes (Performance Optimization)
-- ============================================================================

-- 5.1 Skill Embedding Dictionary Indexes
CREATE INDEX idx_skill_vector ON skill_embedding_dic
    USING ivfflat (skill_vector vector_cosine_ops) WITH (lists = 100);

CREATE INDEX idx_skill_category ON skill_embedding_dic(category_id);

-- 5.2 Candidate Domain Indexes
CREATE INDEX idx_candidate_position_category ON candidate(position_category);
CREATE INDEX idx_candidate_experience_years ON candidate(experience_years);

CREATE INDEX idx_candidate_skills_vector ON candidate_skills_embedding
    USING ivfflat (skills_vector vector_cosine_ops) WITH (lists = 100);

-- 5.3 Recruit Domain Indexes
CREATE INDEX idx_recruit_position ON recruit(position);
CREATE INDEX idx_recruit_primary_keyword ON recruit(primary_keyword);
CREATE INDEX idx_recruit_experience_years ON recruit(experience_years);
CREATE INDEX idx_recruit_published_at ON recruit(published_at);

CREATE INDEX idx_recruit_skills_vector ON recruit_skills_embedding
    USING ivfflat (skills_vector vector_cosine_ops) WITH (lists = 100);

-- ============================================================================
-- End of Schema V2
-- ============================================================================
