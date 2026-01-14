-- ============================================================================
-- Alpha-Match Batch Server - Database Schema
-- ============================================================================
-- Version: 4.0
-- Date: 2026-01-06
-- Description: 대용량 Embedding 검색을 위한 인덱스 정의
    -- pgvector 기반 벡터 유사도 검색 성능 최적화
    -- Data Layer 적재 중에도 Application Layer SELECT 무중단 보장
    -- CREATE INDEX CONCURRENTLY 기반 비트랜잭션 마이그레이션
-- ============================================================================

-- flyway: transactional=false

-- ============================================================================
-- Section 9: Indexes (Performance Optimization)
-- ============================================================================
-- 9.1-1 skill_embedding_dic ivfflat 인덱스
CREATE INDEX CONCURRENTLY idx_skill_vector ON skill_embedding_dic
    USING ivfflat (skill_vector vector_cosine_ops) WITH (lists = 100);

-- 9.1-2 skill_embedding_dic HNSW 인덱스
CREATE INDEX CONCURRENTLY IF NOT EXISTS skill_embedding_dic_hnsw_idx
    ON skill_embedding_dic
    USING hnsw (skill_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 9.2-1 candidate_skills_embedding ivfflat 인덱스
CREATE INDEX CONCURRENTLY idx_candidate_skills_vector ON candidate_skills_embedding
    USING ivfflat (skills_vector vector_cosine_ops) WITH (lists = 100);

-- 9.2-2 candidate_skills_embedding HNSW 인덱스
CREATE INDEX CONCURRENTLY IF NOT EXISTS candidate_skills_embedding_hnsw_idx
    ON candidate_skills_embedding
    USING hnsw (skills_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 9.3-1 recruit_skills_embedding ivfflat 인덱스
CREATE INDEX CONCURRENTLY idx_recruit_skills_vector ON recruit_skills_embedding
    USING ivfflat (skills_vector vector_cosine_ops) WITH (lists = 100);

-- 9.3-2. recruit_skills_embedding HNSW 인덱스
CREATE INDEX CONCURRENTLY IF NOT EXISTS recruit_skills_embedding_hnsw_idx
    ON recruit_skills_embedding
    USING hnsw (skills_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);