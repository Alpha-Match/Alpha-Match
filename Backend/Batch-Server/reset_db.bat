@echo off
set PGPASSWORD=season@heaven!2
psql -h localhost -p 5433 -U postgres -d postgres -f reset_db.sql
