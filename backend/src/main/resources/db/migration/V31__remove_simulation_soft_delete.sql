DROP INDEX IF EXISTS idx_simulations_tenant_active;

DROP INDEX IF EXISTS idx_simulations_active;

ALTER TABLE simulations
    DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE simulations
    DROP COLUMN IF EXISTS deleted_by;

ALTER TABLE simulations
    DROP COLUMN IF EXISTS archived;

CREATE INDEX idx_simulations_tenant
    ON simulations (tenant_id);
