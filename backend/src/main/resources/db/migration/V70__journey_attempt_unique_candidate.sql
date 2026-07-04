-- Moved from V69 to V70 to avoid duplicate Flyway migration version.
CREATE UNIQUE INDEX IF NOT EXISTS uk_journey_attempt_candidate_sequence_active
    ON assessment_journey_attempts (empresa_id, journey_id, lower(candidate_email), sequence_key)
    WHERE status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED');
