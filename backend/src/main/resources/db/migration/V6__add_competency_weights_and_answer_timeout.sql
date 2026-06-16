-- Pesos por competência (Σ = 1.0 por versão) exigidos pelo cálculo de score normalizado.
ALTER TABLE simulation_competencies
    ADD COLUMN weight DOUBLE PRECISION NOT NULL DEFAULT 0;

-- Pesos da versão semente (O Dia do Caos): Empatia 0.4, Resolução de conflito 0.4, Aderência à política 0.2.
UPDATE simulation_competencies SET weight = 0.4
    WHERE simulation_version_id = 1 AND name = 'Empatia';
UPDATE simulation_competencies SET weight = 0.4
    WHERE simulation_version_id = 1 AND name = 'Resolução de conflito';
UPDATE simulation_competencies SET weight = 0.2
    WHERE simulation_version_id = 1 AND name = 'Aderência à política';

-- Timeout de turno: registra "sem resposta" (nível 0) sem opção escolhida.
ALTER TABLE attempt_answers
    ADD COLUMN timed_out BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE attempt_answers
    ALTER COLUMN option_id DROP NOT NULL;
