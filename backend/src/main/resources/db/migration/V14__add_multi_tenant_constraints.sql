-- Enforce tenant_id NOT NULL em tabelas tenant-aware.
-- Bancos ja populados podem ter recebido tenant_id nullable na V11; por isso
-- garantimos tenant default e fazemos backfill antes dos ALTER COLUMN.
INSERT INTO tenants (id, name, company_id, integration_token_hash)
SELECT 'tenant-1', 'Acme S.A.', 'empresa-123', NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM tenants
    WHERE id = 'tenant-1'
);

UPDATE candidate_attempts
SET tenant_id = 'tenant-1'
WHERE tenant_id IS NULL;

UPDATE candidate_attempts
SET company_id = 'empresa-123'
WHERE company_id IS NULL;

UPDATE result_deliveries
SET tenant_id = 'tenant-1'
WHERE tenant_id IS NULL;

UPDATE audit_events
SET tenant_id = 'tenant-1'
WHERE tenant_id IS NULL;

ALTER TABLE candidate_attempts ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE result_deliveries ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE audit_events ALTER COLUMN tenant_id SET NOT NULL;

-- Criar indice unico composto para idempotencia. A V11 criava um indice
-- nao-unico com este nome; removemos antes para nao mascarar a constraint.
DROP INDEX IF EXISTS idx_candidate_attempts_tenant_idempotency;

CREATE UNIQUE INDEX idx_candidate_attempts_tenant_idempotency
    ON candidate_attempts(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Criar indices para isolamento multi-tenant em queries criticas.
CREATE INDEX IF NOT EXISTS idx_candidate_attempts_tenant_simulation
    ON candidate_attempts(tenant_id, simulation_version_id);

CREATE INDEX IF NOT EXISTS idx_result_deliveries_tenant_status
    ON result_deliveries(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_aggregate
    ON audit_events(tenant_id, aggregate_type, aggregate_id);

COMMENT ON TABLE audit_events IS 'Append-only audit log. Deve permitir apenas INSERT.';
