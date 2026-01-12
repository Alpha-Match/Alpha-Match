-- Check candidate loading progress
SELECT COUNT(*) as loaded_candidates FROM candidate;
SELECT COUNT(*) as loaded_skills FROM candidate_skill;
SELECT COUNT(*) as loaded_descriptions FROM candidate_description;
SELECT COUNT(*) as loaded_embeddings FROM candidate_skills_embedding;
