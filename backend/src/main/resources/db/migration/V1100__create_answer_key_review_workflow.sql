CREATE TABLE answer_key_review_rounds (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    simulation_id VARCHAR(180) NOT NULL,
    version_number INTEGER NOT NULL,
    round_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    minimum_experts INTEGER NOT NULL DEFAULT 2,
    minimum_consensus NUMERIC(5,4) NOT NULL DEFAULT 0.7000,
    content_fingerprint VARCHAR(64),
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    approved_by VARCHAR(180),
    approved_at TIMESTAMPTZ,
    CONSTRAINT ck_answer_key_round_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED')),
    CONSTRAINT ck_answer_key_round_min_experts CHECK (minimum_experts BETWEEN 2 AND 20),
    CONSTRAINT ck_answer_key_round_min_consensus CHECK (minimum_consensus BETWEEN 0.5000 AND 1.0000),
    CONSTRAINT uk_answer_key_round UNIQUE (empresa_id, simulation_id, version_number, round_number)
);

CREATE TABLE answer_key_review_assignments (
    id BIGSERIAL PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES answer_key_review_rounds(id) ON DELETE CASCADE,
    user_id VARCHAR(180) NOT NULL,
    assignment_role VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    invited_by VARCHAR(180) NOT NULL,
    invited_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    CONSTRAINT ck_answer_key_assignment_role CHECK (assignment_role IN ('EXPERT', 'APPROVER')),
    CONSTRAINT ck_answer_key_assignment_status CHECK (status IN ('INVITED', 'IN_PROGRESS', 'SUBMITTED', 'APPROVED')),
    CONSTRAINT uk_answer_key_assignment UNIQUE (round_id, user_id, assignment_role)
);

CREATE TABLE answer_key_evidence_links (
    id BIGSERIAL PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES answer_key_review_rounds(id) ON DELETE CASCADE,
    node_id VARCHAR(120) NOT NULL,
    task_text VARCHAR(1200) NOT NULL,
    risk_text VARCHAR(1200) NOT NULL,
    competency_name VARCHAR(180) NOT NULL,
    indicator VARCHAR(600) NOT NULL,
    evidence_weight NUMERIC(8,4) NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(180) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_answer_key_evidence_weight CHECK (evidence_weight > 0),
    CONSTRAINT uk_answer_key_evidence UNIQUE (round_id, node_id, competency_name, indicator)
);

CREATE TABLE answer_key_option_reviews (
    id BIGSERIAL PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES answer_key_review_rounds(id) ON DELETE CASCADE,
    reviewer_user_id VARCHAR(180) NOT NULL,
    node_id VARCHAR(120) NOT NULL,
    option_id VARCHAR(120) NOT NULL,
    effectiveness_score INTEGER NOT NULL,
    behavioral_justification VARCHAR(4000) NOT NULL,
    competency_scores_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_answer_key_effectiveness_score CHECK (effectiveness_score BETWEEN 0 AND 100),
    CONSTRAINT uk_answer_key_option_review UNIQUE (round_id, reviewer_user_id, node_id, option_id)
);

CREATE TABLE answer_key_review_events (
    id BIGSERIAL PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES answer_key_review_rounds(id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    actor_user_id VARCHAR(180) NOT NULL,
    event_data_json TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_answer_key_round_lookup
    ON answer_key_review_rounds (empresa_id, simulation_id, version_number, round_number DESC);
CREATE INDEX ix_answer_key_assignment_user
    ON answer_key_review_assignments (round_id, user_id, assignment_role, status);
CREATE INDEX ix_answer_key_option_review_option
    ON answer_key_option_reviews (round_id, node_id, option_id);
CREATE INDEX ix_answer_key_event_round
    ON answer_key_review_events (round_id, occurred_at);
