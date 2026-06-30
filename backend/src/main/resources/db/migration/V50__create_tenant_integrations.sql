CREATE TABLE tenant_integrations (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             VARCHAR(120) NOT NULL REFERENCES tenants(id),
    provider              VARCHAR(60)  NOT NULL,
    type                  VARCHAR(40)  NOT NULL,
    status                VARCHAR(40)  NOT NULL,
    credentials_hash      VARCHAR(120),
    credentials_encrypted TEXT,
    settings_json         TEXT,
    last_sync_at          TIMESTAMP WITH TIME ZONE,
    configured_at         TIMESTAMP WITH TIME ZONE,
    disabled_at           TIMESTAMP WITH TIME ZONE,
    last_error_message    VARCHAR(600),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_integrations_tenant_provider UNIQUE (tenant_id, provider)
);

CREATE INDEX idx_tenant_integrations_tenant ON tenant_integrations(tenant_id);

INSERT INTO tenant_integrations (
    tenant_id,
    provider,
    type,
    status,
    credentials_hash,
    configured_at,
    created_at,
    updated_at
)
SELECT tenant_id,
       UPPER(provider),
       'ATS',
       'CONECTADA',
       token_hash,
       created_at,
       created_at,
       created_at
FROM integration_tokens
ON CONFLICT (tenant_id, provider) DO NOTHING;
