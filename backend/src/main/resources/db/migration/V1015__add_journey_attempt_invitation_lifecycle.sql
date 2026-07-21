ALTER TABLE assessment_journey_attempts
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS invitation_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMPTZ;

UPDATE assessment_journey_attempts
SET expires_at = created_at + INTERVAL '7 days'
WHERE expires_at IS NULL;

ALTER TABLE assessment_journey_attempts
    ALTER COLUMN expires_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_journey_attempts_empresa_created
    ON assessment_journey_attempts (empresa_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_journey_attempts_empresa_expiration
    ON assessment_journey_attempts (empresa_id, expires_at)
    WHERE status IN ('CREATED', 'IN_PROGRESS', 'EXPIRED');
