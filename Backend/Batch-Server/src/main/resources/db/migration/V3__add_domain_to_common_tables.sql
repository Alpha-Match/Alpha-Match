-- V3: 공통 테이블에 domain 컬럼 추가 및 통합
-- 작성일: 2025-12-12
-- 목적: DLQ, Checkpoint를 도메인별로 확장 가능하게 변경

-- 1. DLQ 테이블 통합 및 도메인 컬럼 추가
-- 기존 recruit_embedding_dlq를 dlq로 재구성
ALTER TABLE recruit_embedding_dlq RENAME TO dlq;

-- domain 컬럼 추가 (기본값: recruit)
ALTER TABLE dlq ADD COLUMN domain VARCHAR(50) NOT NULL DEFAULT 'recruit';

-- recruit_id를 entity_id로 변경 (범용화)
ALTER TABLE dlq RENAME COLUMN recruit_id TO entity_id;

-- 인덱스 추가
CREATE INDEX idx_dlq_domain ON dlq(domain);
CREATE INDEX idx_dlq_entity_id ON dlq(entity_id);
CREATE INDEX idx_dlq_domain_created_at ON dlq(domain, created_at);

-- 2. Checkpoint 테이블 통합 및 도메인 컬럼 추가
-- 기존 embedding_batch_checkpoint를 checkpoint로 재구성
ALTER TABLE embedding_batch_checkpoint RENAME TO checkpoint;

-- domain 컬럼 추가 (기본값: recruit)
ALTER TABLE checkpoint ADD COLUMN domain VARCHAR(50) NOT NULL DEFAULT 'recruit';

-- 기존 PK 제거 및 새로운 복합 PK 생성
ALTER TABLE checkpoint DROP CONSTRAINT IF EXISTS embedding_batch_checkpoint_pkey;
ALTER TABLE checkpoint ADD PRIMARY KEY (domain);

-- candidate 도메인용 초기 레코드 삽입
INSERT INTO checkpoint (domain, last_processed_uuid, updated_at)
VALUES ('candidate', NULL, NOW())
ON CONFLICT (domain) DO NOTHING;

-- 3. 기존 recruit 도메인 데이터에 created_at 추가 (누락된 컬럼)
ALTER TABLE recruit_metadata ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

-- 기존 데이터에 대해 created_at을 updated_at과 동일하게 설정
UPDATE recruit_metadata
SET created_at = updated_at
WHERE created_at IS NULL;

-- 통계 정보 갱신
ANALYZE dlq;
ANALYZE checkpoint;
ANALYZE recruit_metadata;
