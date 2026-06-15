ALTER TABLE candidate_attempts
    ADD COLUMN simulation_version_id BIGINT;

ALTER TABLE candidate_attempts
    ADD COLUMN simulation_version_number INTEGER;

ALTER TABLE candidate_attempts
    ADD CONSTRAINT fk_candidate_attempts_simulation_version
        FOREIGN KEY (simulation_version_id)
        REFERENCES simulation_versions (id);

CREATE INDEX idx_candidate_attempts_simulation_version_id
    ON candidate_attempts (simulation_version_id);
