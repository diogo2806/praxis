CREATE TABLE realistic_job_previews (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    scope_key VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    display_position VARCHAR(20) NOT NULL,
    acknowledgement_required BOOLEAN NOT NULL DEFAULT FALSE,
    active_version_number INTEGER,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(180) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_realistic_preview_scope CHECK (scope_type IN ('JOB', 'ROLE', 'JOURNEY')),
    CONSTRAINT ck_realistic_preview_position CHECK (display_position IN ('BEFORE', 'AFTER', 'BOTH')),
    CONSTRAINT uk_realistic_preview_scope UNIQUE (empresa_id, scope_type, scope_key)
);

CREATE TABLE realistic_job_preview_versions (
    id UUID PRIMARY KEY,
    preview_id UUID NOT NULL REFERENCES realistic_job_previews(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    responsibilities TEXT NOT NULL,
    autonomy TEXT NOT NULL,
    pressure_context TEXT NOT NULL,
    contact_frequency TEXT NOT NULL,
    critical_situations TEXT NOT NULL,
    routine_description TEXT NOT NULL,
    work_conditions TEXT NOT NULL,
    positive_aspects TEXT NOT NULL,
    media_json TEXT NOT NULL DEFAULT '[]',
    alternative_text TEXT NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_by VARCHAR(180),
    published_at TIMESTAMPTZ,
    CONSTRAINT ck_realistic_preview_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT uk_realistic_preview_version UNIQUE (preview_id, version_number)
);

CREATE TABLE realistic_job_preview_scenarios (
    preview_version_id UUID NOT NULL REFERENCES realistic_job_preview_versions(id) ON DELETE CASCADE,
    scenario_node_id VARCHAR(120) NOT NULL,
    PRIMARY KEY (preview_version_id, scenario_node_id)
);

CREATE TABLE attempt_realistic_preview_presentations (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    candidate_attempt_id VARCHAR(80) NOT NULL REFERENCES candidate_attempts(id) ON DELETE CASCADE,
    preview_version_id UUID NOT NULL REFERENCES realistic_job_preview_versions(id),
    display_stage VARCHAR(20) NOT NULL,
    presented_at TIMESTAMPTZ NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    voluntary_withdrawal BOOLEAN NOT NULL DEFAULT FALSE,
    clarity_score INTEGER,
    realism_score INTEGER,
    expectation_compatibility_score INTEGER,
    reaction_recorded_at TIMESTAMPTZ,
    CONSTRAINT ck_realistic_preview_stage CHECK (display_stage IN ('BEFORE', 'AFTER')),
    CONSTRAINT ck_realistic_preview_clarity CHECK (clarity_score IS NULL OR clarity_score BETWEEN 1 AND 5),
    CONSTRAINT ck_realistic_preview_realism CHECK (realism_score IS NULL OR realism_score BETWEEN 1 AND 5),
    CONSTRAINT ck_realistic_preview_compatibility CHECK (expectation_compatibility_score IS NULL OR expectation_compatibility_score BETWEEN 1 AND 5),
    CONSTRAINT uk_attempt_realistic_preview UNIQUE (candidate_attempt_id, preview_version_id, display_stage)
);

CREATE INDEX ix_realistic_preview_scope ON realistic_job_previews (empresa_id, scope_type, scope_key);
CREATE INDEX ix_realistic_preview_version_status ON realistic_job_preview_versions (preview_id, status, version_number DESC);
CREATE INDEX ix_attempt_realistic_preview_metrics ON attempt_realistic_preview_presentations (empresa_id, preview_version_id, presented_at DESC);
