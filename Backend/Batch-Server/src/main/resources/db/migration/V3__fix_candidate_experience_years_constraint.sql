-- V3: Fix candidate.experience_years constraint to allow 0 (신입)
-- 기존: CHECK (experience_years > 0)
-- 변경: CHECK (experience_years >= 0)

-- 기존 제약 조건 삭제
ALTER TABLE candidate
DROP CONSTRAINT IF EXISTS candidate_experience_years_check;

-- 새 제약 조건 추가 (>= 0, 신입 허용)
ALTER TABLE candidate
ADD CONSTRAINT candidate_experience_years_check
CHECK (experience_years >= 0);

COMMENT ON COLUMN candidate.experience_years IS '경력 (연), 0=신입';
