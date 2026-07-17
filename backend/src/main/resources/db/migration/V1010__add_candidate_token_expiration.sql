ALTER TABLE candidate_attempts
    ADD COLUMN candidate_token_expires_at TIMESTAMPTZ;

UPDATE candidate_attempts
SET candidate_token_expires_at = candidate_token_issued_at + INTERVAL '168 hours'
WHERE candidate_token_expires_at IS NULL;

ALTER TABLE candidate_attempts
    ALTER COLUMN candidate_token_expires_at SET NOT NULL;

CREATE INDEX idx_candidate_attempts_token_expires_at
    ON candidate_attempts (empresa_id, candidate_token_expires_at);
