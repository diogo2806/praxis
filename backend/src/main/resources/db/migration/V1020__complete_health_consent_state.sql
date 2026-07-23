ALTER TABLE candidate_attempts
    ADD COLUMN IF NOT EXISTS health_consent_source VARCHAR(40);

UPDATE candidate_attempts
SET health_consent_source = 'CANDIDATE_PORTAL'
WHERE health_consent_recorded_at IS NOT NULL
  AND health_consent_source IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_candidate_attempt_health_consent_complete'
    ) THEN
        ALTER TABLE candidate_attempts
            ADD CONSTRAINT ck_candidate_attempt_health_consent_complete
                CHECK (
                    health_consent_recorded_at IS NULL
                    OR (
                        NULLIF(BTRIM(health_consent_version), '') IS NOT NULL
                        AND health_consent_subject_type IS NOT NULL
                        AND health_consent_source IS NOT NULL
                    )
                );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_candidate_attempt_health_consent_revoked'
    ) THEN
        ALTER TABLE candidate_attempts
            ADD CONSTRAINT ck_candidate_attempt_health_consent_revoked
                CHECK (health_consent_revoked_at IS NULL OR health_consent_recorded_at IS NOT NULL);
    END IF;
END $$;
