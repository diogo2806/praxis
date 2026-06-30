-- Empresa scoping for core aggregates (multi-empresa isolation).
-- Compatible with PostgreSQL (prod) and H2 in PostgreSQL mode (tests).

-- Per-empresa Gupy integration token (stored as hash).
ALTER TABLE tenants
    ADD COLUMN integration_token_hash VARCHAR(120);

CREATE UNIQUE INDEX uk_tenants_company_id
    ON tenants (company_id);

CREATE UNIQUE INDEX uk_tenants_integration_token_hash
    ON tenants (integration_token_hash);

-- simulations -----------------------------------------------------------------
ALTER TABLE simulations
    ADD COLUMN tenant_id VARCHAR(120);

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

ALTER TABLE audit_events
    ADD CONSTRAINT fk_audit_events_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id);

CREATE INDEX idx_audit_events_tenant_aggregate
    ON audit_events (tenant_id, aggregate_type, aggregate_id, created_at);

-- result_deliveries -----------------------------------------------------------
ALTER TABLE result_deliveries
    ADD COLUMN tenant_id VARCHAR(120);

ALTER TABLE result_deliveries
    ADD CONSTRAINT fk_result_deliveries_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id);

CREATE INDEX idx_result_deliveries_tenant_status_next_attempt
    ON result_deliveries (tenant_id, status, next_attempt_at);
