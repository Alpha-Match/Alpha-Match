@echo off
echo ============================================================================
echo HNSW Index Upgrade Script
echo ============================================================================
echo This will upgrade HNSW indexes from (m=16, ef=64) to (m=32, ef=128)
echo Expected time: 15-30 minutes for 116K candidates + 89K recruits (1536d)
echo ============================================================================
echo.
set PGPASSWORD=season@heaven!2
psql -h localhost -p 5433 -U postgres -d alpha_match -f upgrade_hnsw_indexes.sql
echo.
echo ============================================================================
echo Upgrade completed! Check the output above for any errors.
echo ============================================================================
pause
