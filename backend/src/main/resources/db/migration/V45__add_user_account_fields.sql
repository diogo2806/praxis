-- Campos de conta do usuário usados pela aba "Acessos" do painel administrativo.
-- status: ATIVO, CONVIDADO ou BLOQUEADO (bloqueio não apaga histórico).
-- last_login_at: alimentado a cada login bem-sucedido.
-- invite_*: suporte ao convite por link gerado no cadastro do cliente.

ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS invited_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS invite_token_hash VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS invite_expires_at TIMESTAMP WITH TIME ZONE;
