package br.com.iforce.praxis.admin.dto;

import java.time.Instant;

/** Uso do cliente no período (avaliações concluídas). */
public record TenantUsageResponse(
        String tenantId,
        Instant periodStart,
        Instant periodEnd,
        long completedAttempts,
        long completedAttemptsLast7Days,
        long completedAttemptsLast30Days,
        long completedAttemptsAllTime,
        Instant lastCompletedAttemptAt
) {
}
