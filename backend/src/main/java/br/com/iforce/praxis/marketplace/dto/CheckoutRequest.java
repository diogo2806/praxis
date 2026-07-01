package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long listingId
) {
}
