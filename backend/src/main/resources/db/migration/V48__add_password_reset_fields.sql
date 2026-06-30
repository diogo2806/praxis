-- Campos de recuperação de senha reutilizando a própria tabela de usuários.
-- Apenas o hash BCrypt do token é armazenado; o token puro nunca é persistido.
-- password_reset_expires_at controla a expiração automática (TTL padrão de 2 horas).
-- last_password_reset_at preserva o histórico da última redefinição concluída.

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_password_reset_at TIMESTAMP WITH TIME ZONE;
