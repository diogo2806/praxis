-- Compatibilidade exclusiva do ambiente de testes.
-- Alguns fixtures usam INSERT direto em candidate_attempts para montar cenários
-- históricos e não passam pelo mapper que inicializa a janela do token.
-- Os defaults existem apenas no classpath de testes e não alteram o schema de produção.

ALTER TABLE candidate_attempts
    ALTER COLUMN candidate_token_issued_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE candidate_attempts
    ALTER COLUMN candidate_token_expires_at
        SET DEFAULT (CURRENT_TIMESTAMP + INTERVAL '168 hours');
