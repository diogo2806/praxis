package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.billing.model.BillingEventType;

import java.time.Instant;

/** Evento financeiro exibido no painel ADMIN (append-only, somente leitura). */
public record BillingEventResponse(
        Long id,
        BillingEventType eventType,
        String mpResourceType,
        String mpResourceId,
        String mpStatus,
        Long amountCents,
        String currency,
        Instant createdAt
) {
}
