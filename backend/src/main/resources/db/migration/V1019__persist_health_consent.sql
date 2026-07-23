ALTER TABLE candidate_attempts
    ADD COLUMN IF NOT EXISTS health_consent_recorded_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS health_consent_version VARCHAR(80),
    ADD COLUMN IF NOT EXISTS health_consent_subject_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS health_consent_revoked_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_candidate_attempts_health_consent
    ON candidate_attempts (empresa_id, health_consent_recorded_at)
    WHERE health_consent_recorded_at IS NOT NULL;
