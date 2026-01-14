-- ============================================================================
-- HNSW 인덱스 수동 업그레이드 스크립트
-- ============================================================================
-- Purpose: V5 마이그레이션이 실패한 경우 수동으로 HNSW 인덱스 업그레이드
-- From: m=16, ef_construction=64 (낮은 정확도)
-- To: m=32, ef_construction=128 (고정확도)
--
-- Expected Impact:
-- - 검색 정확도 향상 (60%+ 유사도 결과 포함)
-- - 쿼리 시간 10-20ms 증가
-- - ProCoders (66%), Softengi (65%), AGILENIX (64%) 검색 가능
-- ============================================================================

\timing on

-- ============================================================================
-- Step 1: Drop existing low-accuracy HNSW indexes
-- ============================================================================

\echo 'Dropping skill_embedding_dic HNSW index...'
DROP INDEX CONCURRENTLY IF EXISTS skill_embedding_dic_hnsw_idx;

\echo 'Dropping candidate_skills_embedding HNSW index...'
DROP INDEX CONCURRENTLY IF EXISTS candidate_skills_embedding_hnsw_idx;

\echo 'Dropping recruit_skills_embedding HNSW index...'
DROP INDEX CONCURRENTLY IF EXISTS recruit_skills_embedding_hnsw_idx;

-- ============================================================================
-- Step 2: Create optimized high-accuracy HNSW indexes
-- ============================================================================

\echo 'Creating skill_embedding_dic HNSW index (m=32, ef_construction=128)...'
CREATE INDEX CONCURRENTLY skill_embedding_dic_hnsw_idx
    ON skill_embedding_dic
    USING hnsw (skill_vector vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);

\echo 'Creating candidate_skills_embedding HNSW index (m=32, ef_construction=128)...'
CREATE INDEX CONCURRENTLY candidate_skills_embedding_hnsw_idx
    ON candidate_skills_embedding
    USING hnsw (skills_vector vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);

\echo 'Creating recruit_skills_embedding HNSW index (m=32, ef_construction=128)...'
CREATE INDEX CONCURRENTLY recruit_skills_embedding_hnsw_idx
    ON recruit_skills_embedding
    USING hnsw (skills_vector vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);

-- ============================================================================
-- Step 3: Verification
-- ============================================================================

\echo 'Verifying upgraded HNSW indexes...'
SELECT
    c.relname AS table_name,
    i.relname AS index_name,
    am.amname AS index_type,
    pg_get_indexdef(i.oid) AS index_definition,
    pg_size_pretty(pg_relation_size(i.oid)) AS index_size
FROM pg_class c
JOIN pg_index idx ON c.oid = idx.indrelid
JOIN pg_class i ON i.oid = idx.indexrelid
JOIN pg_am am ON i.relam = am.oid
WHERE c.relname IN ('candidate_skills_embedding', 'recruit_skills_embedding', 'skill_embedding_dic')
  AND i.relname LIKE '%hnsw%'
ORDER BY c.relname;

\echo 'HNSW index upgrade completed successfully!'
