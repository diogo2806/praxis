package br.com.iforce.praxis.billing.dto;

/** Resultado da criação de uma cobrança no Mercado Pago (link de pagamento/assinatura). */
public record CheckoutResponse(
        String kind,
        String mpResourceId,
        String initPoint,
        String externalReference
) {
}
