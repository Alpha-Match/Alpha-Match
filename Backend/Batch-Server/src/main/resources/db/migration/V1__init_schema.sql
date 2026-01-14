-- ============================================================================
-- Alpha-Match Batch Server - Database Schema
-- ============================================================================
-- Version: 1.0
-- Date: 2026-01-06
-- Description: 통합 데이터베이스 스키마 (pgvector + Spring Batch + Quartz)
-- 도메인 스키마의 Source of Truth
    -- 비즈니스 핵심 테이블 정의
    -- “이 DB가 무엇을 담는지”를 설명
-- ============================================================================

-- ============================================================================
-- Section 1: Extensions
-- ============================================================================

-- Enable pgvector extension for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable uuid-ossp for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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
                                     skill_vector VECTOR(1536) NOT NULL,
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
COMMENT ON COLUMN skill_embedding_dic.skill_vector IS '스킬 벡터 (1536d)';

-- ============================================================================
-- Section 3: Candidate Domain
-- ============================================================================

-- 3.1 candidate
CREATE TABLE candidate (
                           candidate_id UUID NOT NULL,
                           position_category TEXT NOT NULL,
                           experience_years INTEGER CHECK (experience_years >= 0),
                           original_resume TEXT NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           PRIMARY KEY (candidate_id)
);

COMMENT ON TABLE candidate IS '지원자 이력서 메타 데이터';
COMMENT ON COLUMN candidate.candidate_id IS '지원자 아이디 (UUID)';
COMMENT ON COLUMN candidate.position_category IS '직종 카테고리';
COMMENT ON COLUMN candidate.experience_years IS '경력 (연), 0=신입';
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
                                            skills_vector VECTOR(1536) NOT NULL,
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
COMMENT ON COLUMN candidate_skills_embedding.skills_vector IS '기술 스택 벡터 (1536d)';

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
                                          skills_vector VECTOR(1536) NOT NULL,
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
COMMENT ON COLUMN recruit_skills_embedding.skills_vector IS '기술 스택 벡터 (1536d)';