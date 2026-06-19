ALTER TABLE simulation_versions
    DROP COLUMN IF EXISTS gupy_integration_activated_at;

ALTER TABLE simulation_versions
    DROP COLUMN IF EXISTS gupy_integration_activated_by;
