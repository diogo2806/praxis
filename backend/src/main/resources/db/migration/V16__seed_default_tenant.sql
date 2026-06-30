-- Ensure the default empresa used when admin security is disabled exists in
-- empty production databases. Without this row, inserts into empresa-scoped
-- tables fail on the empresa foreign key while read endpoints still return [].
INSERT INTO tenants (id, name, company_id, integration_token_hash)
SELECT 'empresa-1', 'Acme S.A.', 'empresa-123', NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM tenants
    WHERE id = 'empresa-1'
);
