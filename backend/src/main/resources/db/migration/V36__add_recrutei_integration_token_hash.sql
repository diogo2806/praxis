CREATE TABLE integration_tokens (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  VARCHAR(120)  NOT NULL REFERENCES tenants(id),
    provider   VARCHAR(60)   NOT NULL,
    token_hash VARCHAR(120)  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_integration_tokens_provider_hash UNIQUE (provider, token_hash)
);

CREATE INDEX idx_integration_tokens_tenant ON integration_tokens(tenant_id);

INSERT INTO integration_tokens (tenant_id, provider, token_hash)
SELECT id, 'gupy', integration_token_hash
FROM tenants
WHERE integration_token_hash IS NOT NULL;
