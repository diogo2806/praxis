ALTER TABLE simulations
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE simulations
    ADD COLUMN deleted_by VARCHAR(160);

ALTER TABLE simulations
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_simulations_active
    ON simulations (archived, deleted_at);
