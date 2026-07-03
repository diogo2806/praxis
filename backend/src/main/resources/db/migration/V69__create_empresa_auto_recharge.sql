-- Parte B — Auto-Recarga (recarga automática) para o plano pré-pago AVULSO.
-- Guarda a preferência de recarga automática de cada cliente: quando o saldo de créditos
-- cai abaixo de um nível crítico, a plataforma cobra automaticamente o cartão salvo no
-- Mercado Pago e libera um novo lote de créditos, sem intervenção humana.
--
-- O cartão em si NUNCA é armazenado aqui: guardamos apenas as referências opacas do
-- Mercado Pago (customer + card), obtidas num cadastro prévio de meio de pagamento.

CREATE TABLE IF NOT EXISTS empresa_auto_recharge_config (
    empresa_id VARCHAR(120) PRIMARY KEY,
    -- Liga/desliga a recarga automática para o cliente.
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    -- Nível crítico de saldo: ao cair ABAIXO deste valor, dispara a recarga.
    threshold_credits INTEGER NOT NULL DEFAULT 5,
    -- Pacote de créditos AVULSO usado na recarga (subscription_plans.id).
    plan_id BIGINT,
    -- Referências do meio de pagamento salvo no Mercado Pago (nunca o cartão em claro).
    mp_customer_id VARCHAR(120),
    mp_card_id VARCHAR(120),
    -- Estado do ciclo de recarga: IDLE (ocioso) ou PENDING (cobrança em andamento).
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    -- external_reference da cobrança em andamento (idempotência com o Mercado Pago).
    pending_reference VARCHAR(200),
    -- Última tentativa de recarga (throttle/cooldown e observabilidade).
    last_triggered_at TIMESTAMP WITH TIME ZONE,
    -- Resumo legível do último resultado (aprovada, recusada, falha de comunicação...).
    last_outcome VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Varredura eficiente por clientes com recarga ligada e/ou cobrança em andamento.
CREATE INDEX IF NOT EXISTS idx_auto_recharge_enabled ON empresa_auto_recharge_config (enabled, status);
