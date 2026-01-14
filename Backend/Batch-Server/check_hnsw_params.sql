-- HNSW 인덱스 파라미터 상세 확인

SELECT
    c.relname AS table_name,
    i.relname AS index_name,
    am.amname AS index_type,
    pg_get_indexdef(i.oid) AS index_definition,
    pg_size_pretty(pg_relation_size(i.oid)) AS index_size
FROM pg_class c
JOIN pg_index idx ON c.oid = idx.indrelid
JOIN pg_class i ON i.oid = idx.indexrelid
JOIN pg_am am ON i.relam = am.oid
WHERE c.relname IN ('candidate_skills_embedding', 'recruit_skills_embedding', 'skill_embedding_dic')
  AND i.relname LIKE '%hnsw%'
ORDER BY c.relname;
