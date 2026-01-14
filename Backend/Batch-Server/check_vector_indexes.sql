-- 현재 벡터 인덱스 상태 확인

-- 1. 모든 인덱스 조회 (벡터 관련)
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename IN ('candidate_skills_embedding', 'recruit_skills_embedding', 'skill_embedding_dic')
ORDER BY tablename, indexname;

-- 2. 인덱스 타입 상세 확인 (HNSW vs IVFFlat)
SELECT
    c.relname AS table_name,
    i.relname AS index_name,
    am.amname AS index_type,
    pg_size_pretty(pg_relation_size(i.oid)) AS index_size
FROM pg_class c
JOIN pg_index idx ON c.oid = idx.indrelid
JOIN pg_class i ON i.oid = idx.indexrelid
JOIN pg_am am ON i.relam = am.oid
WHERE c.relname IN ('candidate_skills_embedding', 'recruit_skills_embedding', 'skill_embedding_dic')
  AND am.amname IN ('hnsw', 'ivfflat')
ORDER BY c.relname, i.relname;

-- 3. 테이블별 레코드 수
SELECT 'candidate_skills_embedding' as table_name, COUNT(*) as row_count FROM candidate_skills_embedding
UNION ALL
SELECT 'recruit_skills_embedding', COUNT(*) FROM recruit_skills_embedding
UNION ALL
SELECT 'skill_embedding_dic', COUNT(*) FROM skill_embedding_dic;
