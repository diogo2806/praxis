-- Permite anexar uma imagem ou áudio (além do texto) a cada turno e a cada alternativa do teste.
ALTER TABLE simulation_nodes
    ADD COLUMN media_url VARCHAR(1000);

ALTER TABLE simulation_nodes
    ADD COLUMN media_type VARCHAR(16);

ALTER TABLE simulation_options
    ADD COLUMN media_url VARCHAR(1000);

ALTER TABLE simulation_options
    ADD COLUMN media_type VARCHAR(16);

ALTER TABLE simulation_nodes
    ADD CONSTRAINT ck_simulation_node_media_type
        CHECK (media_type IS NULL OR media_type IN ('IMAGE', 'AUDIO'));

ALTER TABLE simulation_options
    ADD CONSTRAINT ck_simulation_option_media_type
        CHECK (media_type IS NULL OR media_type IN ('IMAGE', 'AUDIO'));
