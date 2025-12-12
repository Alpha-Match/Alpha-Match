-- V2: Candidate 도메인 스키마 추가
-- 작성일: 2025-12-12
-- 목적: recruit 외 candidate 도메인 추가

-- 1. Candidate Metadata 테이블
CREATE TABLE candidate_metadata (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    skills TEXT[],                        -- PostgreSQL 배열 타입
    experience_years INTEGER NOT NULL,
    education_level VARCHAR(100),
    preferred_location VARCHAR(255),
    expected_salary INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 2. Candidate Embedding 테이블 (pgvector)
-- NOTE: candidate는 768 차원 벡터 사용 (recruit는 384)
CREATE TABLE candidate_embedding (
    id UUID PRIMARY KEY REFERENCES candidate_metadata(id) ON DELETE CASCADE,
    vector VECTOR(768) NOT NULL,          -- 차원 수 확인 필요
    updated_at TIMESTAMP DEFAULT NOW()
);

-- pgvector IVFFlat 인덱스 생성
CREATE INDEX candidate_embedding_ivfflat
ON candidate_embedding USING ivfflat (vector vector_l2_ops)
WITH (lists = 100);

-- 성능 최적화 인덱스
CREATE INDEX idx_candidate_metadata_updated_at ON candidate_metadata(updated_at);
CREATE INDEX idx_candidate_embedding_updated_at ON candidate_embedding(updated_at);
CREATE INDEX idx_candidate_metadata_name ON candidate_metadata(name);

-- 통계 정보 갱신 (쿼리 플래너 최적화)
ANALYZE candidate_metadata;
ANALYZE candidate_embedding;
