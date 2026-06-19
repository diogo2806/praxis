ALTER TABLE simulation_competencies
    ADD COLUMN target_score INTEGER NOT NULL DEFAULT 70;

ALTER TABLE simulation_competencies
    ADD CONSTRAINT ck_simulation_competencies_target_score
        CHECK (target_score >= 0 AND target_score <= 100);
