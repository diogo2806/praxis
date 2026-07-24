CREATE TABLE simulation_version_locales (
    id UUID PRIMARY KEY,
    simulation_version_id BIGINT NOT NULL REFERENCES simulation_versions(id) ON DELETE CASCADE,
    locale VARCHAR(16) NOT NULL,
    is_base_locale BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    revision INTEGER NOT NULL DEFAULT 1,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(180) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    reviewed_by VARCHAR(180),
    reviewed_at TIMESTAMPTZ,
    approved_by VARCHAR(180),
    approved_at TIMESTAMPTZ,
    CONSTRAINT ck_simulation_locale_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED')),
    CONSTRAINT uk_simulation_version_locale UNIQUE (simulation_version_id, locale)
);

CREATE UNIQUE INDEX uk_simulation_version_base_locale
    ON simulation_version_locales (simulation_version_id)
    WHERE is_base_locale = TRUE;

CREATE TABLE simulation_locale_contents (
    locale_id UUID PRIMARY KEY REFERENCES simulation_version_locales(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    instructions TEXT NOT NULL,
    report_introduction TEXT NOT NULL
);

CREATE TABLE simulation_node_translations (
    locale_id UUID NOT NULL REFERENCES simulation_version_locales(id) ON DELETE CASCADE,
    node_id VARCHAR(120) NOT NULL,
    speaker VARCHAR(120) NOT NULL,
    message VARCHAR(1200) NOT NULL,
    report_text VARCHAR(2000),
    plain_text_description VARCHAR(1500),
    media_transcript TEXT,
    PRIMARY KEY (locale_id, node_id)
);

CREATE TABLE simulation_option_translations (
    locale_id UUID NOT NULL REFERENCES simulation_version_locales(id) ON DELETE CASCADE,
    node_id VARCHAR(120) NOT NULL,
    option_id VARCHAR(120) NOT NULL,
    text VARCHAR(800) NOT NULL,
    plain_text_description VARCHAR(1500),
    media_transcript TEXT,
    PRIMARY KEY (locale_id, node_id, option_id)
);

CREATE TABLE simulation_competency_translations (
    locale_id UUID NOT NULL REFERENCES simulation_version_locales(id) ON DELETE CASCADE,
    competency_name VARCHAR(140) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    report_text TEXT NOT NULL,
    PRIMARY KEY (locale_id, competency_name)
);

ALTER TABLE candidate_attempts
    ADD COLUMN selected_locale VARCHAR(16),
    ADD COLUMN locale_source VARCHAR(24),
    ADD COLUMN locale_selected_at TIMESTAMPTZ;

ALTER TABLE candidate_attempts
    ADD CONSTRAINT ck_candidate_attempt_locale_source
    CHECK (locale_source IS NULL OR locale_source IN ('INVITATION', 'ATS', 'CANDIDATE', 'BASE_FALLBACK'));

CREATE INDEX ix_simulation_version_locale_status
    ON simulation_version_locales (simulation_version_id, status, locale);
CREATE INDEX ix_candidate_attempt_selected_locale
    ON candidate_attempts (empresa_id, simulation_version_id, selected_locale);
