@echo off
set PGPASSWORD=season@heaven!2
psql -h localhost -p 5433 -U postgres -d alpha_match -f add_columns.sql
pause
