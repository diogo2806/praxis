ALTER TABLE simulation_nodes
    ADD COLUMN position_x DOUBLE PRECISION;

ALTER TABLE simulation_nodes
    ADD COLUMN position_y DOUBLE PRECISION;

ALTER TABLE simulation_nodes
    ADD COLUMN is_final BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE simulation_nodes
    ADD COLUMN report_text VARCHAR(2000);
