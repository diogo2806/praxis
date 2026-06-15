ALTER TABLE candidate_attempts
    ADD COLUMN started_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE candidate_attempts
    ADD COLUMN finished_at TIMESTAMP WITH TIME ZONE;
