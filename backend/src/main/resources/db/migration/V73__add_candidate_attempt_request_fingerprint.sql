ALTER TABLE candidate_attempts
    ADD COLUMN request_fingerprint VARCHAR(64),
    ADD COLUMN request_fingerprint_version INTEGER;

COMMENT ON COLUMN candidate_attempts.request_fingerprint IS
    'SHA-256 da representação canônica versionada da solicitação de criação idempotente.';

COMMENT ON COLUMN candidate_attempts.request_fingerprint_version IS
    'Versão do algoritmo de canonicalização usado em request_fingerprint.';