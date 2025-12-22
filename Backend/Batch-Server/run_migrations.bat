@echo off
set PGPASSWORD=season@heaven!2
psql -h localhost -p 5433 -U postgres -d alpha_match -f src/main/resources/db/migration/V1__init_database_schema.sql
psql -h localhost -p 5433 -U postgres -d alpha_match -f src/main/resources/db/migration/V2__restructure_schema_to_v2.sql
