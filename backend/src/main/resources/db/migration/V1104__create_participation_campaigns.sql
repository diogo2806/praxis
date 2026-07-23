CREATE TABLE participation_campaigns (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    name VARCHAR(180) NOT NULL,
    simulation_id VARCHAR(120) NOT NULL,
    application_cycle_id VARCHAR(120) NOT NULL,
    application_context VARCHAR(200),
    idempotency_key VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL,
    initial_send_at TIMESTAMPTZ NOT NULL,
    link_validity_days INTEGER NOT NULL,
    consent_required BOOLEAN NOT NULL DEFAULT FALSE,
    allow_existing_active BOOLEAN NOT NULL DEFAULT FALSE,
    subject_template VARCHAR(240) NOT NULL,
    body_template TEXT NOT NULL,
    retention_until TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    paused_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_participation_campaign_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'RUNNING', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_participation_campaign_validity CHECK (link_validity_days BETWEEN 1 AND 90),
    CONSTRAINT uk_participation_campaign_cycle UNIQUE (empresa_id, simulation_id, application_cycle_id),
    CONSTRAINT uk_participation_campaign_idempotency UNIQUE (empresa_id, idempotency_key)
);

CREATE TABLE participation_campaign_reminders (
    id BIGSERIAL PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES participation_campaigns(id) ON DELETE CASCADE,
    reminder_index INTEGER NOT NULL,
    send_after_hours INTEGER NOT NULL,
    target_state VARCHAR(24) NOT NULL,
    subject_template VARCHAR(240) NOT NULL,
    body_template TEXT NOT NULL,
    CONSTRAINT ck_campaign_reminder_index CHECK (reminder_index BETWEEN 1 AND 3),
    CONSTRAINT ck_campaign_reminder_hours CHECK (send_after_hours BETWEEN 1 AND 2160),
    CONSTRAINT ck_campaign_reminder_target CHECK (target_state IN ('NOT_OPENED', 'NOT_STARTED', 'IN_PROGRESS')),
    CONSTRAINT uk_campaign_reminder UNIQUE (campaign_id, reminder_index)
);

CREATE TABLE participation_campaign_participants (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES participation_campaigns(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,
    candidate_name VARCHAR(180),
    candidate_email VARCHAR(320),
    candidate_email_hash VARCHAR(64) NOT NULL,
    consent_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    accommodation_multiplier NUMERIC(5,2),
    attempt_id VARCHAR(120),
    candidate_url TEXT,
    link_expires_at TIMESTAMPTZ,
    participation_status VARCHAR(32) NOT NULL,
    communication_status VARCHAR(32) NOT NULL,
    last_error TEXT,
    opened_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_campaign_participant_status CHECK (participation_status IN ('PENDING', 'LINK_CREATED', 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'EXPIRED', 'CANCELLED', 'FAILED')),
    CONSTRAINT ck_campaign_communication_status CHECK (communication_status IN ('PENDING', 'QUEUED', 'DELIVERED', 'FAILED', 'BOUNCED', 'OPENED', 'SKIPPED')),
    CONSTRAINT uk_campaign_participant_email UNIQUE (campaign_id, candidate_email_hash)
);

CREATE TABLE participation_campaign_outbox (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    campaign_id UUID NOT NULL REFERENCES participation_campaigns(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES participation_campaign_participants(id) ON DELETE CASCADE,
    message_type VARCHAR(24) NOT NULL,
    reminder_index INTEGER NOT NULL DEFAULT 0,
    target_state VARCHAR(24),
    scheduled_at TIMESTAMPTZ NOT NULL,
    recipient_email VARCHAR(320),
    subject_text VARCHAR(240),
    body_text TEXT,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    provider_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    CONSTRAINT ck_campaign_outbox_type CHECK (message_type IN ('INITIAL', 'REMINDER')),
    CONSTRAINT ck_campaign_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'SKIPPED', 'FAILED', 'DEAD_LETTER', 'CANCELLED')),
    CONSTRAINT uk_campaign_outbox_idempotency UNIQUE (empresa_id, idempotency_key),
    CONSTRAINT uk_campaign_outbox_message UNIQUE (campaign_id, participant_id, message_type, reminder_index)
);

CREATE INDEX ix_campaign_company_status ON participation_campaigns (empresa_id, status, created_at DESC);
CREATE INDEX ix_campaign_participant_campaign ON participation_campaign_participants (campaign_id, participation_status, communication_status);
CREATE INDEX ix_campaign_participant_attempt ON participation_campaign_participants (attempt_id);
CREATE INDEX ix_campaign_outbox_due ON participation_campaign_outbox (status, next_attempt_at, scheduled_at);
CREATE INDEX ix_campaign_outbox_campaign ON participation_campaign_outbox (campaign_id, participant_id);
