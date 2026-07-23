CREATE TABLE talent_normative_groups (
    id BIGSERIAL PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    simulation_id VARCHAR(120) NOT NULL,
    simulation_version_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    name VARCHAR(160) NOT NULL,
    job_title VARCHAR(160) NOT NULL,
    seniority VARCHAR(100),
    gupy_job_id BIGINT,
    population_description VARCHAR(1000) NOT NULL,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    minimum_sample INTEGER NOT NULL DEFAULT 30,
    path_compatibility_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_talent_normative_group_version
        FOREIGN KEY (simulation_version_id) REFERENCES simulation_versions(id),
    CONSTRAINT ck_talent_normative_period CHECK (period_end > period_start),
    CONSTRAINT ck_talent_normative_minimum_sample CHECK (minimum_sample >= 30),
    CONSTRAINT ck_talent_normative_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INELIGIBLE', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_talent_normative_active_version
    ON talent_normative_groups (empresa_id, simulation_version_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_talent_normative_lookup
    ON talent_normative_groups (empresa_id, simulation_id, version_number, status);

CREATE TABLE talent_cut_score_policies (
    id BIGSERIAL PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    simulation_id VARCHAR(120) NOT NULL,
    simulation_version_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    score INTEGER NOT NULL,
    population_description VARCHAR(1000) NOT NULL,
    justification VARCHAR(2000) NOT NULL,
    evidence VARCHAR(2000) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    approved_by VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_talent_cut_score_version
        FOREIGN KEY (simulation_version_id) REFERENCES simulation_versions(id),
    CONSTRAINT ck_talent_cut_score_range CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT ck_talent_cut_score_period CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT ck_talent_cut_score_status CHECK (status IN ('DRAFT', 'APPROVED', 'REVOKED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uk_talent_cut_score_approved_version
    ON talent_cut_score_policies (empresa_id, simulation_version_id)
    WHERE status = 'APPROVED';

CREATE INDEX idx_talent_cut_score_lookup
    ON talent_cut_score_policies (empresa_id, simulation_id, version_number, status);

CREATE TABLE talent_reference_snapshots (
    id BIGSERIAL PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    attempt_id VARCHAR(80) NOT NULL,
    simulation_id VARCHAR(120) NOT NULL,
    simulation_version_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    target_profile_json TEXT NOT NULL,
    normative_reference_json TEXT,
    cut_score_policy_json TEXT,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_talent_snapshot_attempt
        FOREIGN KEY (attempt_id) REFERENCES candidate_attempts(id),
    CONSTRAINT fk_talent_snapshot_version
        FOREIGN KEY (simulation_version_id) REFERENCES simulation_versions(id),
    CONSTRAINT uk_talent_snapshot_attempt UNIQUE (empresa_id, attempt_id)
);

CREATE INDEX idx_talent_snapshot_version
    ON talent_reference_snapshots (empresa_id, simulation_version_id, captured_at);
