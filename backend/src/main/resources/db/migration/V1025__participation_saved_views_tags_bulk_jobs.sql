CREATE TABLE participation_saved_views (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    owner_user_id VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    shared BOOLEAN NOT NULL DEFAULT FALSE,
    filters_json TEXT NOT NULL,
    sort_json TEXT,
    columns_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_participation_saved_view_owner_name
        UNIQUE (empresa_id, owner_user_id, name)
);

CREATE INDEX idx_participation_saved_views_lookup
    ON participation_saved_views (empresa_id, owner_user_id, shared, name);

CREATE TABLE participation_tags (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    name VARCHAR(80) NOT NULL,
    color VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_participation_tag_empresa_name UNIQUE (empresa_id, name),
    CONSTRAINT ck_participation_tag_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE INDEX idx_participation_tags_empresa
    ON participation_tags (empresa_id, name);

CREATE TABLE participation_tag_assignments (
    id BIGSERIAL PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    participation_type VARCHAR(20) NOT NULL,
    participation_id VARCHAR(120) NOT NULL,
    tag_id VARCHAR(36) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_participation_tag_assignment_tag
        FOREIGN KEY (tag_id) REFERENCES participation_tags(id) ON DELETE CASCADE,
    CONSTRAINT ck_participation_tag_assignment_type
        CHECK (participation_type IN ('individual', 'journey')),
    CONSTRAINT uk_participation_tag_assignment
        UNIQUE (empresa_id, participation_type, participation_id, tag_id)
);

CREATE INDEX idx_participation_tag_assignments_lookup
    ON participation_tag_assignments (empresa_id, participation_type, participation_id);

CREATE TABLE participation_bulk_jobs (
    id VARCHAR(36) PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    requested_by VARCHAR(120) NOT NULL,
    action VARCHAR(30) NOT NULL,
    selection_mode VARCHAR(20) NOT NULL,
    filter_json TEXT,
    payload_json TEXT,
    idempotency_key VARCHAR(120) NOT NULL,
    justification VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    total_items INTEGER NOT NULL DEFAULT 0,
    processed_items INTEGER NOT NULL DEFAULT 0,
    succeeded_items INTEGER NOT NULL DEFAULT 0,
    skipped_items INTEGER NOT NULL DEFAULT 0,
    failed_items INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_participation_bulk_job_idempotency UNIQUE (empresa_id, idempotency_key),
    CONSTRAINT ck_participation_bulk_job_action
        CHECK (action IN ('RESEND', 'EXTEND', 'CANCEL', 'ADD_TAG', 'REMOVE_TAG', 'EXPORT')),
    CONSTRAINT ck_participation_bulk_job_selection
        CHECK (selection_mode IN ('EXPLICIT', 'FILTER')),
    CONSTRAINT ck_participation_bulk_job_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED')),
    CONSTRAINT ck_participation_bulk_job_counts CHECK (
        total_items >= 0
        AND processed_items >= 0
        AND succeeded_items >= 0
        AND skipped_items >= 0
        AND failed_items >= 0
        AND processed_items <= total_items
    )
);

CREATE INDEX idx_participation_bulk_jobs_lookup
    ON participation_bulk_jobs (empresa_id, created_at DESC, status);

CREATE TABLE participation_bulk_items (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    participation_type VARCHAR(20) NOT NULL,
    participation_id VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(1000),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_participation_bulk_item_job
        FOREIGN KEY (job_id) REFERENCES participation_bulk_jobs(id) ON DELETE CASCADE,
    CONSTRAINT ck_participation_bulk_item_type
        CHECK (participation_type IN ('individual', 'journey')),
    CONSTRAINT ck_participation_bulk_item_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'SKIPPED', 'FAILED')),
    CONSTRAINT uk_participation_bulk_item
        UNIQUE (job_id, participation_type, participation_id)
);

CREATE INDEX idx_participation_bulk_items_job
    ON participation_bulk_items (job_id, status, id);
