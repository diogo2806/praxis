-- Permite localizar diretamente o usuário associado a um token de recuperação sem
-- percorrer todas as solicitações pendentes e executar BCrypt para cada registro.
--
-- O hash BCrypt existente continua sendo a prova final do token. O SHA-256 abaixo é
-- apenas um índice de localização seguro porque o token possui 256 bits aleatórios.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token_lookup_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_password_reset_token_lookup_hash
    ON users (password_reset_token_lookup_hash)
    WHERE password_reset_token_lookup_hash IS NOT NULL;
