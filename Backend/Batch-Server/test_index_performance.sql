-- Index Performance Comparison Test (HNSW vs IVFFlat)
-- 1536d Vector Dimension

-- ======================================
-- 1. Table and Index Sizes
-- ======================================

\echo '=== Table Sizes ==='
SELECT tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size
FROM pg_tables
WHERE schemaname = 'public'
  AND (tablename LIKE '%embedding%' OR tablename LIKE '%skill%')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

\echo ''
\echo '=== Index Sizes ==='
SELECT indexname,
       tablename,
       pg_size_pretty(pg_relation_size(schemaname||'.'||indexname)) AS index_size
FROM pg_indexes
WHERE schemaname = 'public'
  AND (indexname LIKE '%embedding%' OR indexname LIKE '%skill%' OR indexname LIKE '%hnsw%' OR indexname LIKE '%ivf%')
ORDER BY pg_relation_size(schemaname||'.'||indexname) DESC;

-- ======================================
-- 2. Get Sample Vector for Testing
-- ======================================

\echo ''
\echo '=== Sample Vector ==='
-- Get a random recruit skills vector for testing
SELECT recruit_id,
       substring(skills_vector::text, 1, 100) || '...' AS vector_preview
FROM recruit_skills_embedding
ORDER BY RANDOM()
LIMIT 1;

-- Store vector in variable (manual copy required)
-- \set test_vector `SELECT skills_vector FROM recruit_skills_embedding ORDER BY RANDOM() LIMIT 1`

-- ======================================
-- 3. Performance Test Queries
-- ======================================

\echo ''
\echo '=== Performance Test Instructions ==='
\echo 'Run the following queries manually with a sample vector:'
\echo ''
\echo '-- Test 1: HNSW Index Performance'
\echo 'EXPLAIN ANALYZE'
\echo 'SELECT recruit_id,'
\echo '       skills_vector <=> ''[YOUR_VECTOR_HERE]'' AS distance'
\echo 'FROM recruit_skills_embedding'
\echo 'ORDER BY skills_vector <=> ''[YOUR_VECTOR_HERE]'''
\echo 'LIMIT 10;'
\echo ''
\echo '-- Test 2: IVFFlat Index Performance (if exists)'
\echo 'EXPLAIN ANALYZE'
\echo 'SELECT recruit_id,'
\echo '       skills_vector <=> ''[YOUR_VECTOR_HERE]'' AS distance'
\echo 'FROM recruit_skills_embedding'
\echo 'ORDER BY skills_vector <=> ''[YOUR_VECTOR_HERE]'''
\echo 'LIMIT 10;'
\echo ''

-- ======================================
-- 4. Index Statistics
-- ======================================

\echo '=== Index Statistics ==='
SELECT indexrelname AS index_name,
       idx_scan AS index_scans,
       idx_tup_read AS tuples_read,
       idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
  AND (indexrelname LIKE '%embedding%' OR indexrelname LIKE '%skill%' OR indexrelname LIKE '%hnsw%' OR indexrelname LIKE '%ivf%')
ORDER BY idx_scan DESC;

\echo ''
\echo '=== HNSW Index Parameters ==='
SELECT indexname,
       indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname LIKE '%hnsw%';

\echo ''
\echo 'Test completed. Please run manual performance tests with actual vectors.'
