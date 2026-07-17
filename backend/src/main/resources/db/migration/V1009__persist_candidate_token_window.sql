ALTER TABLE candidate_attempts ADD COLUMN candidate_token_issued_at TIMESTAMP WITH TIME ZONE;
UPDATE candidate_attempts SET candidate_token_issued_at = created_at WHERE candidate_token_issued_at IS NULL;
ALTER TABLE candidate_attempts ALTER COLUMN candidate_token_issued_at SET NOT NULL;
