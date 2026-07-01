ALTER TABLE marketplace_professionals
    ADD COLUMN IF NOT EXISTS anonymized_at TIMESTAMP WITH TIME ZONE;
