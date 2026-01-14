-- ============================================================================
-- Alpha-Match Batch Server - Database Schema
-- ============================================================================
-- Version: 5.0
-- Date: 2026-01-07
-- Description: HNSW 인덱스 정확도 최적화
--    - 기존 HNSW 인덱스 (m=16, ef_construction=64) 제거
--    - 고정확도 HNSW 인덱스 (m=32, ef_construction=128) 재생성
--    - 검색 정확도 개선을 위한 파라미터 튜닝
--    - Root Cause: HNSW approximation으로 인한 mid-range 유사도 결과 누락
-- ============================================================================

-- flyway: transactional=false

-- ============================================================================
-- Section 1: Drop Existing Low-Accuracy HNSW Indexes
-- ============================================================================

-- 1.1 skill_embedding_dic HNSW 인덱스 제거
DROP INDEX CONCURRENTLY IF EXISTS skill_embedding_dic_hnsw_idx;

-- 1.2 candidate_skills_embedding HNSW 인덱스 제거
DROP INDEX CONCURRENTLY IF EXISTS candidate_skills_embedding_hnsw_idx;

-- 1.3 recruit_skills_embedding HNSW 인덱스 제거
DROP INDEX CONCURRENTLY IF EXISTS recruit_skills_embedding_hnsw_idx;

-- ============================================================================
-- Section 2: Create Optimized High-Accuracy HNSW Indexes
-- ============================================================================

-- 2.1 skill_embedding_dic HNSW 인덱스 (최적화)
-- m=32: 레이어당 연결 수 증가 (정확도 향상)
-- ef_construction=128: 인덱스 빌드 시 탐색 깊이 증가 (품질 향상)
CREATE INDEX CONCURRENTLY skill_embedding_dic_hnsw_idx
    ON skill_embedding_dic
    USING hnsw (skill_vector vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);

-- 2.2 candidate_skills_embedding HNSW 인덱스 (최적화)
CREATE INDEX CONCURRENTLY candidate_skills_embedding_hnsw_idx
    ON candidate_skills_embedding
    USING hnsw (skills_vector vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);

-- 2.3 recruit_skills_embedding HNSW 인덱스 (최적화)
CREATE INDEX CONCURRENTLY recruit_skills_embedding_hnsw_idx
    ON recruit_skills_embedding
    USING hnsw (skills_vector vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);

-- ============================================================================
-- Section 3: Performance vs Accuracy Trade-off Analysis
-- ============================================================================
--
-- BEFORE (m=16, ef_construction=64):
-- - 속도: 매우 빠름 (~10-20ms per query)
-- - 정확도: 낮음 (mid-range 유사도 64-66% 결과 누락)
-- - 문제: ProCoders (66.08%), Softengi (65.51%), AGILENIX (64.66%) 검색 누락
--
-- AFTER (m=32, ef_construction=128):
-- - 속도: 약간 느림 (~20-40ms per query, 예상 10-20ms 증가)
-- - 정확도: 높음 (mid-range 결과 포함)
-- - 효과: 60% 이상 유사도 결과 정확하게 반환
--
-- Trade-off: 약간의 속도 희생으로 검색 품질 크게 향상
-- IVFFlat 인덱스는 유지 (더 정확한 검색이 필요한 경우 대안)
-- ============================================================================
