-- Add tracking columns to outbox_events for better event delivery management
ALTER TABLE outbox_events
ADD COLUMN last_attempt_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE outbox_events
ADD COLUMN sent_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE outbox_events
ADD COLUMN last_error TEXT;
