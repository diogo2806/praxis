-- Agenda a confirmação servidor-servidor do callback na mesma transação que conclui a tentativa.
-- A chave inclui a URL porque a Gupy pode renovar o callback de uma tentativa já concluída.
-- IF NOT EXISTS mantém compatibilidade com bancos que executaram este mesmo conteúdo
-- quando o arquivo ainda usava a versão V1001.
CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_gupy_callback_confirmation
    ON outbox_events (
        empresa_id,
        event_type,
        aggregate_id,
        md5((payload::jsonb ->> 'callbackUrl'))
    )
    WHERE event_type = 'GUPY_CALLBACK_CONFIRMATION';

CREATE OR REPLACE FUNCTION enqueue_gupy_callback_confirmation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    should_enqueue BOOLEAN := FALSE;
BEGIN
    IF NEW.status = 'COMPLETED'
       AND NULLIF(BTRIM(NEW.callback_url), '') IS NOT NULL
    THEN
        IF TG_OP = 'INSERT' THEN
            should_enqueue := TRUE;
        ELSIF OLD.status IS DISTINCT FROM NEW.status
           OR OLD.callback_url IS DISTINCT FROM NEW.callback_url
        THEN
            should_enqueue := TRUE;
        END IF;
    END IF;

    IF should_enqueue THEN
        INSERT INTO outbox_events (
            empresa_id,
            event_type,
            aggregate_type,
            aggregate_id,
            payload,
            status,
            attempts,
            next_attempt_at,
            created_at
        )
        VALUES (
            NEW.empresa_id,
            'GUPY_CALLBACK_CONFIRMATION',
            'CandidateAttempt',
            NEW.id,
            jsonb_build_object(
                'callbackUrl', NEW.callback_url,
                'deliveryState', jsonb_build_object(
                    'GUPY_CALLBACK', jsonb_build_object(
                        'status', 'PENDING',
                        'attempts', 0,
                        'httpStatus', NULL,
                        'lastError', NULL,
                        'confirmedAt', NULL
                    )
                )
            )::text,
            'PENDING',
            0,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_candidate_attempt_gupy_callback_confirmation ON candidate_attempts;

CREATE TRIGGER trg_candidate_attempt_gupy_callback_confirmation
AFTER INSERT OR UPDATE OF status, callback_url
ON candidate_attempts
FOR EACH ROW
EXECUTE FUNCTION enqueue_gupy_callback_confirmation();
