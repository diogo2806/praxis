ALTER TABLE candidate_attempts
    ADD COLUMN accommodation_time_multiplier NUMERIC(3,2) DEFAULT 1.00 NOT NULL;

ALTER TABLE candidate_attempts
    ADD CONSTRAINT ck_candidate_attempts_accommodation_time_multiplier
        CHECK (accommodation_time_multiplier >= 1.00 AND accommodation_time_multiplier <= 9.99);

ALTER TABLE simulation_nodes
    ADD COLUMN plain_text_description VARCHAR(1500);

ALTER TABLE simulation_nodes
    ADD COLUMN audio_description_url VARCHAR(1000);

ALTER TABLE simulation_options
    ADD COLUMN plain_text_description VARCHAR(1500);

ALTER TABLE simulation_options
    ADD COLUMN audio_description_url VARCHAR(1000);
