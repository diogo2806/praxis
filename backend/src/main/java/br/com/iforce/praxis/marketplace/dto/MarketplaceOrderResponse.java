package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.OrderStatus;

import java.time.Instant;

public record MarketplaceOrderResponse(
        Long id,
        OrderStatus status,
        String listingTitle,
        long priceCents,
        String clonedSimulationId,
        Instant paidAt
) {
}
