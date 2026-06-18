-- Data migration: Move existing result deliveries to outbox events
-- This migration preserves pending/retrying deliveries by converting them to RESULT_READY outbox events.
-- Completed (SENT) deliveries are skipped since they've already been delivered.

INSERT INTO outbox_events (
    tenant_id,
    event_type,
    aggregate_type,
    aggregate_id,
    payload,
    status,
    attempts,
    next_attempt_at,
    created_at
)
SELECT
    rd.tenant_id,
    'RESULT_READY',
    'CandidateAttempt',
    ca.id,
    jsonb_build_object(
        'webhookUrl', rd.webhook_url,
        'attemptId', ca.id,
        'migratedFromResultDeliveryId', rd.id,
        'originalAttemptCount', rd.attempt_count
    )::text,
    CASE
        WHEN rd.status = 'PENDING' THEN 'PENDING'
        WHEN rd.status = 'RETRYING' THEN 'RETRYING'
        WHEN rd.status = 'SENT' THEN 'SENT'
        ELSE 'DLQ'
    END,
    rd.attempt_count,
    CASE
        WHEN rd.status IN ('PENDING', 'RETRYING') THEN rd.next_attempt_at
        ELSE NULL
    END,
    COALESCE(rd.created_at, NOW())
FROM result_deliveries rd
JOIN candidate_attempts ca ON rd.candidate_attempt_id = ca.id
WHERE rd.status IN ('PENDING', 'RETRYING', 'SENT')
  AND NOT EXISTS (
    SELECT 1 FROM outbox_events oe
    WHERE oe.aggregate_id = ca.id
      AND oe.aggregate_type = 'CandidateAttempt'
      AND oe.event_type = 'RESULT_READY'
  )
ON CONFLICT DO NOTHING;
