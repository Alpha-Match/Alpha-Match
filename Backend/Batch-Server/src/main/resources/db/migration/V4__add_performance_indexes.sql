-- V4: 성능 최적화 인덱스 추가
-- 작성일: 2025-12-12
-- 목적: 조회 성능 향상 및 벡터 검색 최적화

-- 1. Recruit 도메인 인덱스 (V1에서 추가된 것 외)
-- 복합 인덱스: company_name + exp_years (필터링 조합 쿼리 최적화)
CREATE INDEX idx_recruit_metadata_company_exp ON recruit_metadata(company_name, exp_years);

-- Partial 인덱스: 최근 업데이트된 데이터 (배치 처리 최적화)
CREATE INDEX idx_recruit_metadata_recent_updates
ON recruit_metadata(updated_at DESC)
WHERE updated_at > NOW() - INTERVAL '7 days';

-- 2. Candidate 도메인 인덱스
-- 복합 인덱스: experience_years + education_level
CREATE INDEX idx_candidate_metadata_exp_edu ON candidate_metadata(experience_years, education_level);

-- GIN 인덱스: skills 배열 검색 (배열 타입 전용)
CREATE INDEX idx_candidate_metadata_skills_gin ON candidate_metadata USING GIN(skills);

-- Partial 인덱스: 최근 업데이트된 데이터
CREATE INDEX idx_candidate_metadata_recent_updates
ON candidate_metadata(updated_at DESC)
WHERE updated_at > NOW() - INTERVAL '7 days';

-- 3. DLQ 인덱스 (에러 분석 최적화)
-- 에러 메시지 전문 검색 (GIN + pg_trgm 필요시 추가)
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX idx_dlq_error_message_trgm ON dlq USING GIN(error_message gin_trgm_ops);

-- 4. 벡터 검색 최적화 (HNSW 인덱스 - pgvector 0.5.0 이상)
-- NOTE: IVFFlat은 이미 V1, V2에서 생성됨
-- HNSW는 더 빠른 검색 속도, 더 많은 메모리 사용
-- 프로덕션 환경에서 데이터 규모에 따라 선택

-- HNSW 인덱스 (주석 처리: 필요시 활성화)
-- DROP INDEX IF EXISTS recruit_embedding_ivfflat;
-- CREATE INDEX recruit_embedding_hnsw
-- ON recruit_embedding USING hnsw (vector vector_l2_ops)
-- WITH (m = 16, ef_construction = 64);

-- DROP INDEX IF EXISTS candidate_embedding_ivfflat;
-- CREATE INDEX candidate_embedding_hnsw
-- ON candidate_embedding USING hnsw (vector vector_l2_ops)
-- WITH (m = 16, ef_construction = 64);

-- 통계 정보 갱신
ANALYZE recruit_metadata;
ANALYZE recruit_embedding;
ANALYZE candidate_metadata;
ANALYZE candidate_embedding;
ANALYZE dlq;
