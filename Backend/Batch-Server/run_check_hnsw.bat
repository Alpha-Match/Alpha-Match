@echo off
if not defined PGPASSWORD set PGPASSWORD=%DB_PASSWORD%
psql -h localhost -p 5433 -U postgres -d alpha_match -f check_hnsw_params.sql
