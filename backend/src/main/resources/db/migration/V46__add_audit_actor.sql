-- Ator das ações de auditoria. As ações do ADMIN registram explicitamente o operador
-- que as executou (actor_user_id) e o empresa alvo (empresa_id). Eventos antigos não
-- possuem ator, portanto a coluna é anulável.

ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS actor_user_id VARCHAR(120);
