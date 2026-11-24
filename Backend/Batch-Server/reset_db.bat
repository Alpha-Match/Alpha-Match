@echo off
if not defined PGPASSWORD set PGPASSWORD=%DB_PASSWORD%
psql -h localhost -p 5433 -U postgres -d postgres -f reset_db.sql
