CREATE TABLE feature_flags (
    id VARCHAR(36) PRIMARY KEY,
    flag_key VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(1000) NOT NULL,
    owner VARCHAR(120) NOT NULL,
    default_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    global_override BOOLEAN,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    kill_switch BOOLEAN NOT NULL DEFAULT FALSE,
    frontend_exposed BOOLEAN NOT NULL DEFAULT FALSE,
    temporary BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE,
    removal_plan VARCHAR(2000),
    environment_targets VARCHAR(2000),
    company_targets VARCHAR(8000),
    plan_targets VARCHAR(2000),
    user_targets VARCHAR(8000),
    role_targets VARCHAR(4000),
    rollout_percentage INTEGER NOT NULL DEFAULT 0,
    affects_scoring BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(120) NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_feature_flag_rollout CHECK (rollout_percentage BETWEEN 0 AND 100),
    CONSTRAINT ck_feature_flag_scoring CHECK (affects_scoring = FALSE),
    CONSTRAINT ck_feature_flag_expiration CHECK (
        temporary = FALSE OR (expires_at IS NOT NULL AND removal_plan IS NOT NULL)
    )
);

CREATE INDEX idx_feature_flags_governance
    ON feature_flags (active, kill_switch, expires_at, flag_key);

CREATE TABLE feature_flag_metrics (
    id BIGSERIAL PRIMARY KEY,
    flag_key VARCHAR(120) NOT NULL,
    variant VARCHAR(20) NOT NULL,
    metric VARCHAR(60) NOT NULL,
    sample_count BIGINT NOT NULL DEFAULT 0,
    total_value DOUBLE PRECISION NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_feature_flag_metric_flag
        FOREIGN KEY (flag_key) REFERENCES feature_flags(flag_key) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_feature_flag_metric_variant CHECK (variant IN ('ON', 'OFF')),
    CONSTRAINT uk_feature_flag_metric_variant UNIQUE (flag_key, variant, metric)
);

CREATE INDEX idx_feature_flag_metrics_lookup
    ON feature_flag_metrics (flag_key, metric, variant);
