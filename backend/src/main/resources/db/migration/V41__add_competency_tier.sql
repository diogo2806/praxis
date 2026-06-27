ALTER TABLE simulation_competencies
    ADD COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'MAJOR';

UPDATE simulation_competencies
SET tier = 'MINOR'
WHERE LOWER(name) IN ('aderencia a politica', 'aderência à política');
