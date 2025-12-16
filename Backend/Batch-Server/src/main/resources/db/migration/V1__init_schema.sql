-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Recruit Metadata 테이블
CREATE TABLE recruit_metadata (
    id UUID PRIMARY KEY,
    company_name TEXT NOT NULL,
    exp_years INT NOT NULL,
    english_level TEXT,
    primary_keyword TEXT,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 2. Recruit Embedding 테이블 (pgvector)
CREATE TABLE recruit_embedding (
    id UUID PRIMARY KEY REFERENCES recruit_metadata(id) ON DELETE CASCADE,
    vector VECTOR(384) NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- pgvector IVFFlat 인덱스 생성
CREATE INDEX recruit_embedding_ivfflat
ON recruit_embedding USING ivfflat (vector vector_l2_ops)
WITH (lists = 100);

-- 3. Dead Letter Queue 테이블
CREATE TABLE recruit_embedding_dlq (
    id BIGSERIAL PRIMARY KEY,
    recruit_id UUID,
    error_message TEXT NOT NULL,
    payload JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 4. Batch Checkpoint 테이블
CREATE TABLE embedding_batch_checkpoint (
    id BIGSERIAL PRIMARY KEY,
    last_processed_uuid UUID,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Checkpoint 초기 레코드 삽입 (최초 실행용)
INSERT INTO embedding_batch_checkpoint (last_processed_uuid, updated_at)
VALUES (NULL, NOW());

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_metadata_updated_at ON recruit_metadata(updated_at);
CREATE INDEX idx_embedding_updated_at ON recruit_embedding(updated_at);
CREATE INDEX idx_dlq_created_at ON recruit_embedding_dlq(created_at);
