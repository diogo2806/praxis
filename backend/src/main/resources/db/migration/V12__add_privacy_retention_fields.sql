ALTER TABLE candidate_attempts
    ADD COLUMN anonymized_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_candidate_attempts_retention
    ON candidate_attempts (status, finished_at, anonymized_at);
