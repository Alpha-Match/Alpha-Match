-- Candidate v2 데이터 검증 쿼리

-- 1. 전체 데이터 건수 확인
SELECT
    'candidate' as table_name,
    COUNT(*) as count
FROM candidate
UNION ALL
SELECT
    'candidate_skill',
    COUNT(*)
FROM candidate_skill
UNION ALL
SELECT
    'candidate_description',
    COUNT(*)
FROM candidate_description
UNION ALL
SELECT
    'candidate_skills_embedding',
    COUNT(*)
FROM candidate_skills_embedding;

-- 2. 벡터 차원 확인
SELECT
    vector_dims(skills_vector) AS vector_dimension,
    COUNT(*) as count
FROM candidate_skills_embedding
GROUP BY vector_dimension;

-- 3. 새 컬럼 확인
SELECT
    COUNT(*) AS total,
    COUNT(resume_lang) AS resume_lang_count,
    COUNT(moreinfo) AS moreinfo_count,
    COUNT(looking_for) AS looking_for_count
FROM candidate_description;

-- 4. experience_years NULL 확인
SELECT
    CASE
        WHEN experience_years IS NULL THEN 'NULL'
        ELSE experience_years::text
    END AS exp_years,
    COUNT(*) as count
FROM candidate
GROUP BY experience_years
ORDER BY experience_years NULLS FIRST
LIMIT 10;

-- 5. FK 무결성 검증
SELECT
    COUNT(*) AS orphan_skill_count
FROM candidate_skill cs
LEFT JOIN candidate c ON cs.candidate_id = c.candidate_id
WHERE c.candidate_id IS NULL;

SELECT
    COUNT(*) AS orphan_description_count
FROM candidate_description cd
LEFT JOIN candidate c ON cd.candidate_id = c.candidate_id
WHERE c.candidate_id IS NULL;

SELECT
    COUNT(*) AS orphan_embedding_count
FROM candidate_skills_embedding ce
LEFT JOIN candidate c ON ce.candidate_id = c.candidate_id
WHERE c.candidate_id IS NULL;
