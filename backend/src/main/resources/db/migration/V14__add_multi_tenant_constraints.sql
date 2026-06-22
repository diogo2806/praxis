-- Enforce tenant_id NOT NULL em todas as tabelas tenant-aware
ALTER TABLE candidate_attempts ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE result_deliveries ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE audit_events ALTER COLUMN tenant_id SET NOT NULL;

-- Criar índices compostos para idempotência (tenant_id + idempotency_key)
CREATE UNIQUE INDEX IF NOT EXISTS idx_candidate_attempts_tenant_idempotency
    ON candidate_attempts(tenant_id, idempotency_key);

-- Criar índices para isolamento multi-tenant em queries críticas
CREATE INDEX IF NOT EXISTS idx_candidate_attempts_tenant_simulation
    ON candidate_attempts(tenant_id, simulation_version_id);

CREATE INDEX IF NOT EXISTS idx_result_deliveries_tenant_status
    ON result_deliveries(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_aggregate
    ON audit_events(tenant_id, aggregate_type, aggregate_id);

-- Verificar constraint para audit_events (append-only)
-- Nota: Triggers para enforçar append-only serão implementados no aplicativo
COMMENT ON TABLE audit_events IS 'Append-only audit log. Deve permitir apenas INSERT.';
