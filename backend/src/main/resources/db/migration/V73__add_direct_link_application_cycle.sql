ALTER TABLE candidate_attempts
    ADD COLUMN IF NOT EXISTS application_cycle_id VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_candidate_attempts_application_cycle
    ON candidate_attempts (empresa_id, application_cycle_id);
