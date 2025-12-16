-- ============================================================================
-- Alpha-Match Batch Server - Database Schema
-- ============================================================================
-- Version: 1.0
-- Date: 2025-12-16
-- Description: 통합 데이터베이스 스키마 (pgvector + Spring Batch + Quartz)
-- ============================================================================

-- ============================================================================
-- Section 1: Extensions
-- ============================================================================

-- Enable pgvector extension for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable uuid-ossp for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Section 2: Candidate Domain
-- ============================================================================

-- 2.1 skill_embedding_dic (FK 참조 대상, 먼저 생성)
CREATE TABLE skill_embedding_dic (
    position_category VARCHAR(50) NOT NULL,
    skill VARCHAR(50) NOT NULL,
    skill_vector VECTOR(768) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (skill)
);

COMMENT ON TABLE skill_embedding_dic IS '기술 스택 사전 + 벡터 테이블';
COMMENT ON COLUMN skill_embedding_dic.position_category IS '이미 정규화된 직종명';
COMMENT ON COLUMN skill_embedding_dic.skill IS '기술 스택 스킬명';
COMMENT ON COLUMN skill_embedding_dic.skill_vector IS '기술 스택 벡터 정보 (768d)';

-- 2.2 candidate
CREATE TABLE candidate (
    candidate_id UUID NOT NULL,
    position_category VARCHAR(50) NOT NULL,
    experience_years INTEGER NOT NULL DEFAULT 0,
    original_resume TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (candidate_id)
);

COMMENT ON TABLE candidate IS '지원자 이력서';
COMMENT ON COLUMN candidate.candidate_id IS '아이디 (UUID, 구분용)';
COMMENT ON COLUMN candidate.position_category IS '직종 카테고리';
COMMENT ON COLUMN candidate.experience_years IS '경력 (연)';
COMMENT ON COLUMN candidate.original_resume IS '기존 원문';

-- 2.3 candidate_skill (복합 PK)
CREATE TABLE candidate_skill (
    candidate_id UUID NOT NULL,
    skill VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (candidate_id, skill),
    CONSTRAINT fk_candidate_skill_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidate(candidate_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_candidate_skill_skill
        FOREIGN KEY (skill)
        REFERENCES skill_embedding_dic(skill)
        ON DELETE RESTRICT
);

COMMENT ON TABLE candidate_skill IS '기술 스택 상세 (복합 PK - DDD Aggregate 패턴)';
COMMENT ON COLUMN candidate_skill.candidate_id IS '아이디 (UUID, 구분용)';
COMMENT ON COLUMN candidate_skill.skill IS '보유 스킬명';

-- 2.4 candidate_skills_embedding
CREATE TABLE candidate_skills_embedding (
    candidate_id UUID NOT NULL,
    skills VARCHAR(50)[] NOT NULL,
    skills_vector VECTOR(768) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (candidate_id),
    CONSTRAINT fk_candidate_skills_embedding
        FOREIGN KEY (candidate_id)
        REFERENCES candidate(candidate_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE candidate_skills_embedding IS '기술 스택 뭉치 벡터 테이블';
COMMENT ON COLUMN candidate_skills_embedding.candidate_id IS '아이디 (UUID, 구분용)';
COMMENT ON COLUMN candidate_skills_embedding.skills IS '보유 스킬명 배열';
COMMENT ON COLUMN candidate_skills_embedding.skills_vector IS '기술 스택 벡터 정보 (768d)';

-- ============================================================================
-- Section 3: Recruit Domain
-- ============================================================================

-- 3.1 recruit_metadata
CREATE TABLE recruit_metadata (
    id UUID NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    exp_years INTEGER NOT NULL DEFAULT 0,
    english_level VARCHAR(20),
    primary_keyword VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
);

COMMENT ON TABLE recruit_metadata IS '채용 공고 메타데이터';
COMMENT ON COLUMN recruit_metadata.id IS '채용 공고 ID';
COMMENT ON COLUMN recruit_metadata.company_name IS '회사명';
COMMENT ON COLUMN recruit_metadata.exp_years IS '요구 경력 (연)';
COMMENT ON COLUMN recruit_metadata.english_level IS '영어 레벨';
COMMENT ON COLUMN recruit_metadata.primary_keyword IS '주요 키워드';

-- 3.2 recruit_embedding
CREATE TABLE recruit_embedding (
    id UUID NOT NULL,
    vector VECTOR(384) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_recruit_embedding
        FOREIGN KEY (id)
        REFERENCES recruit_metadata(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE recruit_embedding IS '채용 공고 임베딩';
COMMENT ON COLUMN recruit_embedding.id IS '채용 공고 ID';
COMMENT ON COLUMN recruit_embedding.vector IS '임베딩 벡터 (384d)';

-- ============================================================================
-- Section 4: Common Tables (Batch Management)
-- ============================================================================

-- 4.1 dlq (Dead Letter Queue)
CREATE TABLE dlq (
    id BIGSERIAL NOT NULL,
    domain VARCHAR(50) NOT NULL,
    failed_id UUID,
    error_message TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
);

COMMENT ON TABLE dlq IS 'Dead Letter Queue (실패한 레코드 저장)';
COMMENT ON COLUMN dlq.id IS 'DLQ ID (자동 증가)';
COMMENT ON COLUMN dlq.domain IS '도메인명 (recruit, candidate)';
COMMENT ON COLUMN dlq.failed_id IS '실패한 레코드 ID';
COMMENT ON COLUMN dlq.error_message IS '에러 메시지';
COMMENT ON COLUMN dlq.payload IS '원본 데이터 (JSON)';

-- 4.2 checkpoint
CREATE TABLE checkpoint (
    id BIGSERIAL NOT NULL,
    domain VARCHAR(50) NOT NULL UNIQUE,
    last_processed_uuid UUID,
    processed_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
);

COMMENT ON TABLE checkpoint IS '배치 처리 체크포인트 관리 (재시작 지원)';
COMMENT ON COLUMN checkpoint.id IS 'Checkpoint ID';
COMMENT ON COLUMN checkpoint.domain IS '도메인명 (recruit, candidate)';
COMMENT ON COLUMN checkpoint.last_processed_uuid IS '마지막 처리 UUID';
COMMENT ON COLUMN checkpoint.processed_count IS '처리된 레코드 수';

-- ============================================================================
-- Section 5: Spring Batch Metadata Tables (v6.0)
-- ============================================================================
-- Reference: https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-postgresql.sql

CREATE TABLE BATCH_JOB_INSTANCE  (
	JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT ,
	JOB_NAME VARCHAR(100) NOT NULL,
	JOB_KEY VARCHAR(32) NOT NULL,
	constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
) ;

CREATE TABLE BATCH_JOB_EXECUTION  (
	JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT  ,
	JOB_INSTANCE_ID BIGINT NOT NULL,
	CREATE_TIME TIMESTAMP NOT NULL,
	START_TIME TIMESTAMP DEFAULT NULL ,
	END_TIME TIMESTAMP DEFAULT NULL ,
	STATUS VARCHAR(10) ,
	EXIT_CODE VARCHAR(2500) ,
	EXIT_MESSAGE VARCHAR(2500) ,
	LAST_UPDATED TIMESTAMP,
	constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
	references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
	JOB_EXECUTION_ID BIGINT NOT NULL ,
	PARAMETER_NAME VARCHAR(100) NOT NULL ,
	PARAMETER_TYPE VARCHAR(100) NOT NULL ,
	PARAMETER_VALUE VARCHAR(2500) ,
	IDENTIFYING CHAR(1) NOT NULL ,
	constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION  (
	STEP_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT NOT NULL,
	STEP_NAME VARCHAR(100) NOT NULL,
	JOB_EXECUTION_ID BIGINT NOT NULL,
	CREATE_TIME TIMESTAMP NOT NULL,
	START_TIME TIMESTAMP DEFAULT NULL ,
	END_TIME TIMESTAMP DEFAULT NULL ,
	STATUS VARCHAR(10) ,
	COMMIT_COUNT BIGINT ,
	READ_COUNT BIGINT ,
	FILTER_COUNT BIGINT ,
	WRITE_COUNT BIGINT ,
	READ_SKIP_COUNT BIGINT ,
	WRITE_SKIP_COUNT BIGINT ,
	PROCESS_SKIP_COUNT BIGINT ,
	ROLLBACK_COUNT BIGINT ,
	EXIT_CODE VARCHAR(2500) ,
	EXIT_MESSAGE VARCHAR(2500) ,
	LAST_UPDATED TIMESTAMP,
	constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
	STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT ,
	constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
	references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
) ;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
	JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT ,
	constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ;

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_SEQ MAXVALUE 9223372036854775807 NO CYCLE;

-- ============================================================================
-- Section 6: Quartz Scheduler Tables (v2.3.2)
-- ============================================================================
-- Reference: http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-09.html

CREATE TABLE qrtz_job_details
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME   VARCHAR(250) NOT NULL,
    IS_DURABLE BOOL NOT NULL,
    IS_NONCONCURRENT BOOL NOT NULL,
    IS_UPDATE_DATA BOOL NOT NULL,
    REQUESTS_RECOVERY BOOL NOT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(200) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_simprop_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 BOOL NULL,
    BOOL_PROP_2 BOOL NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_calendars
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME  VARCHAR(200) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);


CREATE TABLE qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_fired_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(200) NULL,
    JOB_GROUP VARCHAR(200) NULL,
    IS_NONCONCURRENT BOOL NULL,
    REQUESTS_RECOVERY BOOL NULL,
    PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE qrtz_locks
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

CREATE INDEX idx_qrtz_j_req_recovery ON qrtz_job_details(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX idx_qrtz_j_grp ON qrtz_job_details(SCHED_NAME,JOB_GROUP);

CREATE INDEX idx_qrtz_t_j ON qrtz_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX idx_qrtz_t_jg ON qrtz_triggers(SCHED_NAME,JOB_GROUP);
CREATE INDEX idx_qrtz_t_c ON qrtz_triggers(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX idx_qrtz_t_g ON qrtz_triggers(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX idx_qrtz_t_state ON qrtz_triggers(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX idx_qrtz_t_n_state ON qrtz_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX idx_qrtz_t_n_g_state ON qrtz_triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX idx_qrtz_t_next_fire_time ON qrtz_triggers(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX idx_qrtz_t_nft_st ON qrtz_triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX idx_qrtz_t_nft_misfire ON qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX idx_qrtz_t_nft_st_misfire ON qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX idx_qrtz_t_nft_st_misfire_grp ON qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX idx_qrtz_ft_trig_inst_name ON qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX idx_qrtz_ft_j_g ON qrtz_fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX idx_qrtz_ft_jg ON qrtz_fired_triggers(SCHED_NAME,JOB_GROUP);
CREATE INDEX idx_qrtz_ft_t_g ON qrtz_fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX idx_qrtz_ft_tg ON qrtz_fired_triggers(SCHED_NAME,TRIGGER_GROUP);

-- ============================================================================
-- Section 7: Indexes (Performance Optimization)
-- ============================================================================

-- 7.1 Candidate Domain Indexes
CREATE INDEX idx_skill_vector ON skill_embedding_dic
    USING ivfflat (skill_vector vector_cosine_ops) WITH (lists = 100);

CREATE INDEX idx_candidate_position_category ON candidate(position_category);
CREATE INDEX idx_candidate_experience_years ON candidate(experience_years);

CREATE INDEX idx_candidate_skills_vector ON candidate_skills_embedding
    USING ivfflat (skills_vector vector_cosine_ops) WITH (lists = 100);

-- 7.2 Recruit Domain Indexes
CREATE INDEX idx_recruit_primary_keyword ON recruit_metadata(primary_keyword);
CREATE INDEX idx_recruit_exp_years ON recruit_metadata(exp_years);

CREATE INDEX idx_recruit_vector ON recruit_embedding
    USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);

-- 7.3 Common Table Indexes
CREATE INDEX idx_dlq_domain ON dlq(domain);
CREATE INDEX idx_dlq_created_at ON dlq(created_at);

CREATE UNIQUE INDEX idx_checkpoint_domain ON checkpoint(domain);

-- ============================================================================
-- End of Schema
-- ============================================================================
