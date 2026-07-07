-- Bancos provisionados por versões antigas do projeto semearam a empresa padrão
-- com outro id (nomenclatura legada "tenant"), então 'empresa-1' pode não existir
-- mesmo com a V16 já aplicada. Sem essa linha, qualquer escrita com a segurança
-- desativada falha na FK de empresa (ex.: fk_simulations_tenant).
-- Reafirma o seed de forma idempotente na tabela já renomeada pela V66.
INSERT INTO empresas (id, name, company_id)
SELECT 'empresa-1',
       'Acme S.A.',
       CASE
           WHEN EXISTS (SELECT 1 FROM empresas WHERE company_id = 'empresa-123')
               THEN 'empresa-1'
           ELSE 'empresa-123'
       END
WHERE NOT EXISTS (
    SELECT 1
    FROM empresas
    WHERE id = 'empresa-1'
);