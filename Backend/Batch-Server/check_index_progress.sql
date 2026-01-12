-- 현재 진행 중인 인덱스 생성 작업 확인
SELECT
    a.pid,
    p.datname,
    p.relid::regclass AS table_name,
    p.index_relid::regclass AS index_name,
    p.phase,
    p.tuples_total,
    p.tuples_done,
    ROUND(100.0 * p.tuples_done / NULLIF(p.tuples_total, 0), 2) AS progress_pct,
    a.wait_event_type,
    a.wait_event,
    NOW() - a.query_start AS elapsed_time
FROM pg_stat_progress_create_index p
JOIN pg_stat_activity a ON p.pid = a.pid
WHERE p.command = 'CREATE INDEX';
