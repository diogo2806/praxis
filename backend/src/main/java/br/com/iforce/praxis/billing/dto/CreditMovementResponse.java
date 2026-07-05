package br.com.iforce.praxis.billing.dto;

import java.time.Instant;

/** Linha do extrato de créditos exibida para a própria empresa. */
public record CreditMovementResponse(
        Long id,
        int delta,
        String reason,
        int balanceAfter,
        String note,
        Instant createdAt
) {
}
