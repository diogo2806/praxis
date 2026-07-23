ALTER TABLE candidate_attempts
    ADD COLUMN IF NOT EXISTS raw_score INTEGER,
    ADD COLUMN IF NOT EXISTS path_maximum_score INTEGER,
    ADD COLUMN IF NOT EXISTS normalized_score INTEGER,
    ADD COLUMN IF NOT EXISTS scoring_algorithm_version VARCHAR(80);

UPDATE candidate_attempts
SET normalized_score = score,
    scoring_algorithm_version = CASE
        WHEN score IS NULL THEN scoring_algorithm_version
        ELSE COALESCE(scoring_algorithm_version, 'legacy-path-normalized-v1')
    END
WHERE normalized_score IS NULL
   OR (score IS NOT NULL AND scoring_algorithm_version IS NULL);
