DROP INDEX IF EXISTS idx_candidate_attempts_tenant_idempotency;

CREATE UNIQUE INDEX idx_candidate_attempts_tenant_idempotency
    ON candidate_attempts(tenant_id, idempotency_key);
