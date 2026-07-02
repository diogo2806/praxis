-- Garante a existência da empresa padrão ('empresa-1') usada por CurrentEmpresaService
-- quando praxis.security.enabled=false (ambientes de teste/homologação sem login).
-- V16__seed_default_tenant.sql já fazia esse seed contra a tabela "tenants"; esta
-- migration repete a garantia após o rename para "empresas" (V66), cobrindo bancos
-- onde a linha não existe (ex.: schema criado após V16 via baseline, ou linha removida).

INSERT INTO empresas (id, name, company_id, integration_token_hash)
SELECT 'empresa-1', 'Acme S.A.', 'empresa-123', NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM empresas
    WHERE id = 'empresa-1'
);
