ALTER TABLE candidate_attempts
    ADD COLUMN IF NOT EXISTS gupy_job_id BIGINT;

ALTER TABLE candidate_attempts
    ADD COLUMN IF NOT EXISTS callback_url VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_candidate_attempts_gupy_job
    ON candidate_attempts (empresa_id, company_id, gupy_job_id);
