-- Add missing columns to candidate_description
ALTER TABLE candidate_description
    ADD COLUMN IF NOT EXISTS moreinfo TEXT,
    ADD COLUMN IF NOT EXISTS looking_for TEXT;

-- Add comments
COMMENT ON COLUMN candidate_description.moreinfo IS '추가 정보 (프로젝트, 성과, 자격증 등)';
COMMENT ON COLUMN candidate_description.looking_for IS '구직 희망사항 (희망 역할, 급여, 근무 조건 등)';

-- Verify columns were added
SELECT column_name FROM information_schema.columns
WHERE table_name = 'candidate_description'
  AND column_name IN ('moreinfo', 'looking_for');
