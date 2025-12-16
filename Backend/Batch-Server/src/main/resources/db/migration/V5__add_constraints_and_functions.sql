-- V5: 제약조건 및 헬퍼 함수 추가
-- 작성일: 2025-12-12
-- 목적: 데이터 무결성 강화 및 유틸리티 함수 제공

-- 1. CHECK 제약조건 추가

-- Recruit Metadata
ALTER TABLE recruit_metadata
ADD CONSTRAINT chk_recruit_exp_years_positive
CHECK (exp_years >= 0);

ALTER TABLE recruit_metadata
ADD CONSTRAINT chk_recruit_english_level
CHECK (english_level IN ('NONE', 'BASIC', 'INTERMEDIATE', 'ADVANCED', 'NATIVE'));

-- Candidate Metadata
ALTER TABLE candidate_metadata
ADD CONSTRAINT chk_candidate_experience_positive
CHECK (experience_years >= 0);

ALTER TABLE candidate_metadata
ADD CONSTRAINT chk_candidate_salary_positive
CHECK (expected_salary IS NULL OR expected_salary > 0);

-- DLQ
ALTER TABLE dlq
ADD CONSTRAINT chk_dlq_domain
CHECK (domain IN ('recruit', 'candidate'));

-- Checkpoint
ALTER TABLE checkpoint
ADD CONSTRAINT chk_checkpoint_domain
CHECK (domain IN ('recruit', 'candidate'));

-- 2. 트리거 함수: updated_at 자동 갱신
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Recruit Metadata Trigger
CREATE TRIGGER trigger_recruit_metadata_updated_at
BEFORE UPDATE ON recruit_metadata
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Recruit Embedding Trigger
CREATE TRIGGER trigger_recruit_embedding_updated_at
BEFORE UPDATE ON recruit_embedding
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Candidate Metadata Trigger
CREATE TRIGGER trigger_candidate_metadata_updated_at
BEFORE UPDATE ON candidate_metadata
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Candidate Embedding Trigger
CREATE TRIGGER trigger_candidate_embedding_updated_at
BEFORE UPDATE ON candidate_embedding
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Checkpoint Trigger
CREATE TRIGGER trigger_checkpoint_updated_at
BEFORE UPDATE ON checkpoint
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 3. 헬퍼 함수: 벡터 유사도 검색 (Cosine Similarity)
-- NOTE: pgvector는 L2 distance를 기본 제공
-- Cosine Similarity = 1 - Cosine Distance

CREATE OR REPLACE FUNCTION cosine_similarity(a vector, b vector)
RETURNS float AS $$
BEGIN
    RETURN 1 - (a <=> b);  -- <=> 는 cosine distance operator
END;
$$ LANGUAGE plpgsql IMMUTABLE STRICT PARALLEL SAFE;

-- 4. 헬퍼 함수: 도메인별 통계 조회
CREATE OR REPLACE FUNCTION get_domain_stats(domain_name VARCHAR)
RETURNS TABLE(
    metadata_count BIGINT,
    embedding_count BIGINT,
    dlq_count BIGINT,
    last_processed_uuid UUID,
    last_checkpoint_time TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        CASE
            WHEN domain_name = 'recruit' THEN (SELECT COUNT(*) FROM recruit_metadata)
            WHEN domain_name = 'candidate' THEN (SELECT COUNT(*) FROM candidate_metadata)
            ELSE 0
        END AS metadata_count,
        CASE
            WHEN domain_name = 'recruit' THEN (SELECT COUNT(*) FROM recruit_embedding)
            WHEN domain_name = 'candidate' THEN (SELECT COUNT(*) FROM candidate_embedding)
            ELSE 0
        END AS embedding_count,
        (SELECT COUNT(*) FROM dlq WHERE domain = domain_name) AS dlq_count,
        (SELECT last_processed_uuid FROM checkpoint WHERE domain = domain_name) AS last_processed_uuid,
        (SELECT updated_at FROM checkpoint WHERE domain = domain_name) AS last_checkpoint_time;
END;
$$ LANGUAGE plpgsql;

-- 5. View: 전체 도메인 통계 (모니터링용)
CREATE OR REPLACE VIEW v_all_domain_stats AS
SELECT
    'recruit' AS domain,
    (SELECT COUNT(*) FROM recruit_metadata) AS metadata_count,
    (SELECT COUNT(*) FROM recruit_embedding) AS embedding_count,
    (SELECT COUNT(*) FROM dlq WHERE domain = 'recruit') AS dlq_count,
    (SELECT last_processed_uuid FROM checkpoint WHERE domain = 'recruit') AS last_processed_uuid,
    (SELECT updated_at FROM checkpoint WHERE domain = 'recruit') AS last_checkpoint_time
UNION ALL
SELECT
    'candidate' AS domain,
    (SELECT COUNT(*) FROM candidate_metadata) AS metadata_count,
    (SELECT COUNT(*) FROM candidate_embedding) AS embedding_count,
    (SELECT COUNT(*) FROM dlq WHERE domain = 'candidate') AS dlq_count,
    (SELECT last_processed_uuid FROM checkpoint WHERE domain = 'candidate') AS last_processed_uuid,
    (SELECT updated_at FROM checkpoint WHERE domain = 'candidate') AS last_checkpoint_time;

-- 6. 코멘트 추가 (문서화)
COMMENT ON TABLE recruit_metadata IS '채용 공고 메타데이터';
COMMENT ON TABLE recruit_embedding IS '채용 공고 임베딩 벡터 (384차원)';
COMMENT ON TABLE candidate_metadata IS '후보자 메타데이터';
COMMENT ON TABLE candidate_embedding IS '후보자 임베딩 벡터 (768차원)';
COMMENT ON TABLE dlq IS '데드 레터 큐 (도메인별 실패 레코드)';
COMMENT ON TABLE checkpoint IS '배치 체크포인트 (도메인별 마지막 처리 UUID)';

COMMENT ON FUNCTION cosine_similarity(vector, vector) IS '코사인 유사도 계산 (1 - cosine_distance)';
COMMENT ON FUNCTION get_domain_stats(VARCHAR) IS '도메인별 통계 조회 함수';
COMMENT ON VIEW v_all_domain_stats IS '전체 도메인 통계 뷰 (모니터링용)';
