package br.com.iforce.praxis.marketplace.dto;

public record CheckoutResponse(
        Long orderId,
        String checkoutUrl,
        String mpPreferenceId
) {
}
