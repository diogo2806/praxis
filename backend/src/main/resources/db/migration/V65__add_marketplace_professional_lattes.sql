-- REQ-063: curriculo Lattes verificado por prova de posse (codigo-desafio).
ALTER TABLE marketplace_professionals
    ADD COLUMN lattes_id                VARCHAR(16),
    ADD COLUMN lattes_verification_code VARCHAR(20),
    ADD COLUMN lattes_verified_at       TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX uq_marketplace_professionals_lattes
    ON marketplace_professionals (lattes_id);
