-- ============================================================================
-- Alpha-Match Batch Server - Database Schema
-- ============================================================================
-- Version: 6.0
-- Date: 2026-01-08
-- Description: candidate_description 테이블 확장
--    - moreinfo 컬럼 추가 (추가 정보: 프로젝트, 성과 등)
--    - looking_for 컬럼 추가 (구직 희망사항: 희망 역할, 급여 등)
-- ============================================================================

-- Section 1: Add new columns to candidate_description
ALTER TABLE candidate_description
    ADD COLUMN IF NOT EXISTS moreinfo TEXT,
    ADD COLUMN IF NOT EXISTS looking_for TEXT;

-- Section 2: Add column comments
COMMENT ON COLUMN candidate_description.moreinfo IS '추가 정보 (프로젝트, 성과, 자격증 등)';
COMMENT ON COLUMN candidate_description.looking_for IS '구직 희망사항 (희망 역할, 급여, 근무 조건 등)';
