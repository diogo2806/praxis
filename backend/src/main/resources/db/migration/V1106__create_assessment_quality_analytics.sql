CREATE TABLE assessment_external_criteria (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    candidate_attempt_id VARCHAR(80) NOT NULL REFERENCES candidate_attempts(id) ON DELETE CASCADE,
    criterion_code VARCHAR(80) NOT NULL,
    criterion_label VARCHAR(180) NOT NULL,
    criterion_type VARCHAR(16) NOT NULL,
    numeric_value NUMERIC(14,4),
    category_value VARCHAR(180),
    observed_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_assessment_external_criterion_type CHECK (criterion_type IN ('NUMERIC', 'CATEGORY')),
    CONSTRAINT ck_assessment_external_criterion_value CHECK (
        (criterion_type = 'NUMERIC' AND numeric_value IS NOT NULL AND category_value IS NULL)
        OR (criterion_type = 'CATEGORY' AND numeric_value IS NULL AND category_value IS NOT NULL)
    ),
    CONSTRAINT uk_assessment_external_criterion UNIQUE (empresa_id, candidate_attempt_id, criterion_code)
);

CREATE TABLE assessment_quality_sensitive_audits (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    simulation_id VARCHAR(120) NOT NULL,
    simulation_version_number INTEGER,
    gupy_job_id BIGINT,
    group_criterion_code VARCHAR(80) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    legal_basis VARCHAR(300) NOT NULL,
    minimum_sample INTEGER NOT NULL,
    visible_groups INTEGER NOT NULL,
    suppressed_groups INTEGER NOT NULL,
    requested_by VARCHAR(180) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_assessment_quality_minimum_sample CHECK (minimum_sample >= 10)
);

CREATE INDEX ix_assessment_external_criterion_scope
    ON assessment_external_criteria (empresa_id, criterion_code, observed_at DESC);
CREATE INDEX ix_assessment_external_criterion_attempt
    ON assessment_external_criteria (candidate_attempt_id);
CREATE INDEX ix_assessment_quality_sensitive_audit_scope
    ON assessment_quality_sensitive_audits (empresa_id, simulation_id, requested_at DESC);
