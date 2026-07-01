ALTER TABLE in_app_notifications
    ALTER COLUMN candidate_attempt_id DROP NOT NULL;

ALTER TABLE in_app_notifications
    ALTER COLUMN candidate_name DROP NOT NULL;

ALTER TABLE in_app_notifications
    ALTER COLUMN candidate_email DROP NOT NULL;

ALTER TABLE in_app_notifications
    ALTER COLUMN outbox_event_id DROP NOT NULL;
