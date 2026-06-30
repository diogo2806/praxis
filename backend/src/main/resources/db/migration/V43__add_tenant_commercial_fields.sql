-- Campos comerciais do cliente (empresa) usados pelo painel administrativo (Parte A).
-- status: situação operacional (ATIVO, EM_TESTE, SUSPENSO, CANCELADO).
-- commercial_plan_type: rótulo comercial (AVULSO, PROFISSIONAL, ENTERPRISE).
-- commercial_condition: condição comercial livre, relevante sobretudo no ENTERPRISE.
-- Empresas existentes assumem ATIVO/ENTERPRISE para preservar o acesso atual.

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS commercial_plan_type VARCHAR(40) NOT NULL DEFAULT 'ENTERPRISE';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS commercial_condition VARCHAR(2000);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants (status);
CREATE INDEX IF NOT EXISTS idx_tenants_commercial_plan_type ON tenants (commercial_plan_type);
