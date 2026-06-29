package br.com.iforce.praxis.billing.model;

/**
 * Estado de uma assinatura recorrente (plano PROFISSIONAL) espelhando o preapproval do
 * Mercado Pago, mais o tratamento de inadimplência local.
 */
public enum SubscriptionStatus {
    /** Criada no Mercado Pago, aguardando o primeiro pagamento/autorização. */
    PENDING,
    /** Autorizada e em dia. */
    AUTHORIZED,
    /** Pagamento recusado; em período de carência (inadimplência). */
    DELINQUENT,
    /** Pausada no Mercado Pago. */
    PAUSED,
    /** Cancelada. */
    CANCELLED
}
