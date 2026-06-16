ALTER TABLE simulation_versions
    ADD COLUMN gupy_integration_activated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE simulation_versions
    ADD COLUMN gupy_integration_activated_by VARCHAR(160);
