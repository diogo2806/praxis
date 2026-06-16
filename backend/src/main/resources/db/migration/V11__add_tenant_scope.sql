-- Tenant scoping for core aggregates (multi-tenant isolation).
-- Compatible with PostgreSQL (prod) and H2 in PostgreSQL mode (tests).

-- Default tenant for existing/seeded data.
INSERT INTO tenants (id, name, company_id)
SELECT 'tenant-1', 'Acme S.A.', 'empresa-123'
WHERE NOT EXISTS (
    SELECT 1 FROM tenants WHERE id = 'tenant-1'
);

-- Per-tenant Gupy integration token (stored as hash).
ALTER TABLE tenants
    ADD COLUMN integration_token_hash VARCHAR(120);

CREATE UNIQUE INDEX uk_tenants_company_id
    ON tenants (company_id);

CREATE UNIQUE INDEX uk_tenants_integration_token_hash
    ON tenants (integration_token_hash);

-- simulations -----------------------------------------------------------------
ALTER TABLE simulations
    ADD COLUMN tenant_id VARCHAR(120);

UPDATE simulations
SET tenant_id = 'tenant-1'
WHERE tenant_id IS NULL;

ALTER TABLE simulations
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE simulations
    ADD CONSTRAINT fk_simulations_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id);

CREATE INDEX idx_simulations_tenant_active
    ON simulations (tenant_id, archived, deleted_at);

-- candidate_attempts ----------------------------------------------------------
ALTER TABLE candidate_attempts
    ADD COLUMN tenant_id VARCHAR(120);

ALTER TABLE candidate_attempts
    ADD COLUMN company_id VARCHAR(120);

UPDATE candidate_attempts
SET tenant_id = 'tenant-1'
WHERE tenant_id IS NULL;

-- Backfill company_id from the legacy idempotency_key (companyId|documentId|testId).
UPDATE candidate_attempts
SET company_id = SUBSTRING(idempotency_key, 1, POSITION('|' IN idempotency_key) - 1)
WHERE company_id IS NULL
  AND POSITION('|' IN idempotency_key) > 1;

UPDATE candidate_attempts
SET company_id = 'empresa-123'
WHERE company_id IS NULL;

ALTER TABLE candidate_attempts
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE candidate_attempts
    ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE candidate_attempts
    ADD CONSTRAINT fk_candidate_attempts_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id);

CREATE INDEX idx_candidate_attempts_tenant_result
    ON candidate_attempts (tenant_id, result_id);

CREATE INDEX idx_candidate_attempts_tenant_idempotency
    ON candidate_attempts (tenant_id, idempotency_key);

CREATE INDEX idx_candidate_attempts_tenant_simulation_version
    ON candidate_attempts (tenant_id, simulation_version_id);

-- audit_events ----------------------------------------------------------------
ALTER TABLE audit_events
    ADD COLUMN tenant_id VARCHAR(120);

UPDATE audit_events
SET tenant_id = 'tenant-1'
WHERE tenant_id IS NULL;

ALTER TABLE audit_events
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE audit_events
    ADD CONSTRAINT fk_audit_events_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id);

CREATE INDEX idx_audit_events_tenant_aggregate
    ON audit_events (tenant_id, aggregate_type, aggregate_id, created_at);

-- result_deliveries -----------------------------------------------------------
ALTER TABLE result_deliveries
    ADD COLUMN tenant_id VARCHAR(120);

UPDATE result_deliveries
SET tenant_id = (
    SELECT candidate_attempts.tenant_id
    FROM candidate_attempts
    WHERE candidate_attempts.id = result_deliveries.candidate_attempt_id
)
WHERE tenant_id IS NULL;

UPDATE result_deliveries
SET tenant_id = 'tenant-1'
WHERE tenant_id IS NULL;

ALTER TABLE result_deliveries
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE result_deliveries
    ADD CONSTRAINT fk_result_deliveries_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id);

CREATE INDEX idx_result_deliveries_tenant_status_next_attempt
    ON result_deliveries (tenant_id, status, next_attempt_at);
