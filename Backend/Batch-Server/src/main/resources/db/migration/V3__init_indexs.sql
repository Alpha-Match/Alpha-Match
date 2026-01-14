-- ============================================================================
-- Alpha-Match Batch Server - Database Schema
-- ============================================================================
-- Version: 3.0
-- Date: 2026-01-06
-- Description: 조회 성능 최적화를 위한 Base Index 정의
    -- Application Layer의 SELECT 성능 향상을 위한 보조 인덱스
    -- 단일 컬럼 / 저비용 인덱스로 구성
    -- 트랜잭션 기반 생성으로 정합성 보장
-- ============================================================================

-- ============================================================================
-- Section 8: Indexes (Domain Base Indexx)
-- ============================================================================
-- 8.1 Skill Embedding Dictionary Indexes
CREATE INDEX idx_skill_category ON skill_embedding_dic(category_id);

-- 8.2 Candidate Domain Indexes
CREATE INDEX idx_candidate_position_category ON candidate(position_category);
CREATE INDEX idx_candidate_experience_years ON candidate(experience_years);
CREATE INDEX idx_candidate_created_at ON candidate(created_at);

-- 8.3 Recruit Domain Indexes
CREATE INDEX idx_recruit_position ON recruit(position);
CREATE INDEX idx_recruit_primary_keyword ON recruit(primary_keyword);
CREATE INDEX idx_recruit_experience_years ON recruit(experience_years);
CREATE INDEX idx_recruit_published_at ON recruit(published_at);