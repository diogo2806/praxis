ALTER TABLE tenant_integrations
    ADD COLUMN IF NOT EXISTS token_preview VARCHAR(40);
