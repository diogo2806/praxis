-- Permite anexar uma imagem ou áudio (além do texto) a cada turno e a cada alternativa do teste.
ALTER TABLE simulation_nodes
    ADD COLUMN IF NOT EXISTS media_url VARCHAR(1000);

ALTER TABLE simulation_nodes
    ADD COLUMN IF NOT EXISTS media_type VARCHAR(16);

ALTER TABLE simulation_options
    ADD COLUMN IF NOT EXISTS media_url VARCHAR(1000);

ALTER TABLE simulation_options
    ADD COLUMN IF NOT EXISTS media_type VARCHAR(16);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_simulation_node_media_type'
    ) THEN
        ALTER TABLE simulation_nodes
            ADD CONSTRAINT ck_simulation_node_media_type
                CHECK (media_type IS NULL OR media_type IN ('IMAGE', 'AUDIO'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_simulation_option_media_type'
    ) THEN
        ALTER TABLE simulation_options
            ADD CONSTRAINT ck_simulation_option_media_type
                CHECK (media_type IS NULL OR media_type IN ('IMAGE', 'AUDIO'));
    END IF;
END $$;
