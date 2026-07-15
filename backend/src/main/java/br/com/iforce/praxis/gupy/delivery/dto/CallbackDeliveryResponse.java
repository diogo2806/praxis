package br.com.iforce.praxis.gupy.delivery.dto;

import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;

import java.time.Instant;

public record CallbackDeliveryResponse(
        Long deliveryId,
        String attemptId,
        String callbackUrl,
        ResultDeliveryStatus status,
        int attempts,
        Integer httpStatus,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Instant confirmedAt,
        String lastError,
        Instant createdAt
) {
}
