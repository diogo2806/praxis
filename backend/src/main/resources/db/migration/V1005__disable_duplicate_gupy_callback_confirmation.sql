-- callback_url é a URL de retorno da pessoa candidata no navegador.
-- O frontend já executa esse GET após a conclusão; o trigger servidor-servidor
-- provocava uma segunda chamada para a mesma URL.
DROP TRIGGER IF EXISTS trg_candidate_attempt_gupy_callback_confirmation ON candidate_attempts;
DROP FUNCTION IF EXISTS enqueue_gupy_callback_confirmation();

-- Preserva o histórico e impede que confirmações antigas ainda pendentes sejam
-- executadas depois da correção.
UPDATE outbox_events
SET status = 'DLQ',
    next_attempt_at = NULL,
    last_error = 'Entrega desativada: callback_url é executada pelo navegador da pessoa candidata.'
WHERE event_type = 'GUPY_CALLBACK_CONFIRMATION'
  AND status IN ('PENDING', 'PROCESSING', 'RETRYING');
