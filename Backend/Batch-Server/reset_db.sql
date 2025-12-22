-- DB 초기화 스크립트
-- 기존 연결 종료
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'alpha_match'
  AND pid <> pg_backend_pid();

-- 데이터베이스 삭제 및 재생성
DROP DATABASE IF EXISTS alpha_match;
CREATE DATABASE alpha_match;

-- pgvector 확장 설치
\c alpha_match
CREATE EXTENSION IF NOT EXISTS vector;
