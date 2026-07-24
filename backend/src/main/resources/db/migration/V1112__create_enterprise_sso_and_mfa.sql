CREATE TABLE enterprise_identity_providers (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    protocol VARCHAR(16) NOT NULL,
    issuer_uri VARCHAR(1000) NOT NULL,
    client_id VARCHAR(500) NOT NULL,
    client_secret_env_var VARCHAR(160) NOT NULL,
    redirect_uri VARCHAR(1000) NOT NULL,
    frontend_success_uri VARCHAR(1000) NOT NULL,
    scopes VARCHAR(500) NOT NULL DEFAULT 'openid profile email',
    allowed_email_domains TEXT NOT NULL,
    default_role VARCHAR(80) NOT NULL DEFAULT 'RESULTS_ANALYST',
    jit_provisioning_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    enforce_sso BOOLEAN NOT NULL DEFAULT FALSE,
    require_mfa BOOLEAN NOT NULL DEFAULT TRUE,
    accepted_mfa_amr_values VARCHAR(500) NOT NULL DEFAULT 'mfa,otp,hwk,sms',
    active BOOLEAN NOT NULL DEFAULT FALSE,
    last_test_status VARCHAR(24),
    last_test_message VARCHAR(1000),
    last_tested_at TIMESTAMPTZ,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(180) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_enterprise_idp_protocol CHECK (protocol IN ('OIDC')),
    CONSTRAINT ck_enterprise_idp_test_status CHECK (
        last_test_status IS NULL OR last_test_status IN ('SUCCESS', 'ERROR')
    ),
    CONSTRAINT uk_enterprise_idp_name UNIQUE (empresa_id, display_name)
);

CREATE TABLE enterprise_sso_login_requests (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES enterprise_identity_providers(id) ON DELETE CASCADE,
    empresa_id VARCHAR(120) NOT NULL,
    state_hash VARCHAR(64) NOT NULL UNIQUE,
    nonce_hash VARCHAR(64) NOT NULL,
    pkce_verifier VARCHAR(180) NOT NULL,
    return_uri VARCHAR(1000) NOT NULL,
    requested_email VARCHAR(320),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);

CREATE TABLE enterprise_sso_identities (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES enterprise_identity_providers(id) ON DELETE CASCADE,
    empresa_id VARCHAR(120) NOT NULL,
    subject_identifier VARCHAR(500) NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(320),
    assigned_role VARCHAR(80) NOT NULL,
    last_mfa_verified_at TIMESTAMPTZ,
    first_login_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ NOT NULL,
    disabled_at TIMESTAMPTZ,
    CONSTRAINT uk_enterprise_sso_subject UNIQUE (provider_id, subject_identifier),
    CONSTRAINT uk_enterprise_sso_email UNIQUE (provider_id, email)
);

CREATE TABLE enterprise_auth_audit_events (
    id UUID PRIMARY KEY,
    empresa_id VARCHAR(120),
    provider_id UUID,
    event_type VARCHAR(80) NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    actor_identifier VARCHAR(320),
    ip_address VARCHAR(64),
    user_agent VARCHAR(1000),
    detail VARCHAR(2000),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_enterprise_auth_audit_outcome CHECK (outcome IN ('SUCCESS', 'DENIED', 'ERROR'))
);

CREATE INDEX ix_enterprise_idp_domain_active
    ON enterprise_identity_providers (active, empresa_id);
CREATE INDEX ix_enterprise_sso_request_expiry
    ON enterprise_sso_login_requests (expires_at, consumed_at);
CREATE INDEX ix_enterprise_identity_email
    ON enterprise_sso_identities (empresa_id, email);
CREATE INDEX ix_enterprise_auth_audit_scope
    ON enterprise_auth_audit_events (empresa_id, occurred_at DESC);
