-- Modify the idempotency index to allow NULL values
-- Remove the WHERE clause to allow composite keys where idempotency_key is NULL
DROP INDEX IF EXISTS idx_candidate_attempts_tenant_idempotency;

CREATE UNIQUE INDEX idx_candidate_attempts_tenant_idempotency
    ON candidate_attempts(tenant_id, idempotency_key);
