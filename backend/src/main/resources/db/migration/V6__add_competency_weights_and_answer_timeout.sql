-- Pesos por competência (Σ = 1.0 por versão) exigidos pelo cálculo de score normalizado.
ALTER TABLE simulation_competencies
    ADD COLUMN weight DOUBLE PRECISION NOT NULL DEFAULT 0;

-- Timeout de turno: registra "sem resposta" (nível 0) sem opção escolhida.
ALTER TABLE attempt_answers
    ADD COLUMN timed_out BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE attempt_answers
    ALTER COLUMN option_id DROP NOT NULL;
