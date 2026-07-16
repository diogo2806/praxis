-- Permite localizar diretamente o usuário associado a um convite sem percorrer
-- todos os usuários convidados e executar BCrypt para cada registro.
--
-- O BCrypt existente continua sendo a comprovação final do token. O SHA-256 é
-- usado somente como índice porque o convite possui 256 bits aleatórios.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS invite_token_lookup_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_invite_token_lookup_hash
    ON users (invite_token_lookup_hash)
    WHERE invite_token_lookup_hash IS NOT NULL;
