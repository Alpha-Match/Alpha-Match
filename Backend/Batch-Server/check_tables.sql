-- Check v2 tables
SELECT tablename FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
