@echo off
set PGPASSWORD=season@heaven!2
psql -h localhost -p 5433 -U postgres -d alpha_match -f verify_candidate_data.sql
