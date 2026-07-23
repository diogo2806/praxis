ALTER TABLE simulation_nodes ADD COLUMN IF NOT EXISTS media_transcript VARCHAR(8000);
ALTER TABLE simulation_nodes ADD COLUMN IF NOT EXISTS media_captions_url VARCHAR(1000);
ALTER TABLE simulation_nodes ADD COLUMN IF NOT EXISTS media_version VARCHAR(120);

ALTER TABLE simulation_options ADD COLUMN IF NOT EXISTS media_transcript VARCHAR(8000);
ALTER TABLE simulation_options ADD COLUMN IF NOT EXISTS media_captions_url VARCHAR(1000);
ALTER TABLE simulation_options ADD COLUMN IF NOT EXISTS media_version VARCHAR(120);

ALTER TABLE attempt_node_serves ADD COLUMN IF NOT EXISTS media_type VARCHAR(16);
ALTER TABLE attempt_node_serves ADD COLUMN IF NOT EXISTS media_version VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_attempt_node_serves_media_quality
    ON attempt_node_serves (media_type, media_version);
