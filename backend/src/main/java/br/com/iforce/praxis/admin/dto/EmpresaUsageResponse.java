package br.com.iforce.praxis.admin.dto;

import java.time.Instant;


/** Uso do cliente no período (avaliações concluídas). */
public record EmpresaUsageResponse(
        String empresaId,
        Instant periodStart,
        Instant periodEnd,
        long completedAttempts,
        long completedAttemptsLast7Days,
        long completedAttemptsLast30Days,
        long completedAttemptsAllTime,
        Instant lastCompletedAttemptAt
) {
}
