ALTER TABLE simulations
    ADD COLUMN critical_situation VARCHAR(1200);

ALTER TABLE simulations
    ADD COLUMN result_use VARCHAR(120);
