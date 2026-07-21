-- Registra, junto à ciência do aviso de privacidade, o aceite versionado dos
-- Termos de Uso antes do início da avaliação.

ALTER TABLE candidate_notice_acceptances
    ADD COLUMN terms_version VARCHAR(80),
    ADD COLUMN terms_hash VARCHAR(64),
    ADD COLUMN terms_accepted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE candidate_notice_acceptances
    DROP CONSTRAINT uk_candidate_notice_acceptance;

ALTER TABLE candidate_notice_acceptances
    ADD CONSTRAINT uk_candidate_legal_acceptance
        UNIQUE (attempt_id, notice_version, terms_version),
    ADD CONSTRAINT ck_candidate_terms_acceptance_complete
        CHECK (
            (terms_version IS NULL AND terms_hash IS NULL AND terms_accepted_at IS NULL)
            OR
            (terms_version IS NOT NULL AND terms_hash IS NOT NULL AND terms_accepted_at IS NOT NULL)
        );
