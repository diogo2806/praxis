package br.com.iforce.praxis.billing.model;

/**
 * Tipo de evento financeiro append-only registrado em {@code tenant_billing_events}.
 *
 * <p>Os eventos refletem fatos confirmados junto ao Mercado Pago (ou ajustes manuais),
 * nunca uma marcação manual de "pago".</p>
 */
public enum BillingEventType {
    CREDIT_CHECKOUT_CREATED,
    CREDIT_PURCHASE_APPROVED,
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_AUTHORIZED,
    SUBSCRIPTION_PAYMENT_APPROVED,
    SUBSCRIPTION_PAYMENT_REJECTED,
    SUBSCRIPTION_CANCELLED,
    PAYMENT_PENDING,
    PAYMENT_REFUNDED,
    PAYMENT_CHARGEBACK
}
