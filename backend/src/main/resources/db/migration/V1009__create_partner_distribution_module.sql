CREATE TABLE partner_clients (
    id                  VARCHAR(120) PRIMARY KEY,
    empresa_id          VARCHAR(120) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    name                VARCHAR(180) NOT NULL,
    external_company_id VARCHAR(120) NOT NULL,
    provider            VARCHAR(40) NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_partner_client_external_company UNIQUE (empresa_id, provider, external_company_id),
    CONSTRAINT ck_partner_client_provider CHECK (provider IN ('GUPY', 'RECRUTEI', 'CUSTOM_API'))
);

CREATE INDEX idx_partner_clients_empresa_active
    ON partner_clients(empresa_id, active, name);

CREATE TABLE partner_catalog_access (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        VARCHAR(120) NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    partner_client_id VARCHAR(120) NOT NULL REFERENCES partner_clients(id) ON DELETE CASCADE,
    simulation_id     VARCHAR(120) NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_partner_catalog_client_simulation UNIQUE (partner_client_id, simulation_id)
);

CREATE INDEX idx_partner_catalog_client_active
    ON partner_catalog_access(empresa_id, partner_client_id, active);

ALTER TABLE integration_tokens
    ADD COLUMN partner_client_id VARCHAR(120) REFERENCES partner_clients(id) ON DELETE CASCADE;

ALTER TABLE integration_tokens
    ADD COLUMN client_company_id VARCHAR(120);

CREATE UNIQUE INDEX uq_integration_tokens_partner_client_provider
    ON integration_tokens(partner_client_id, provider)
    WHERE partner_client_id IS NOT NULL;

CREATE INDEX idx_integration_tokens_partner_client
    ON integration_tokens(partner_client_id);
