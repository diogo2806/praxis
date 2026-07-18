package br.com.iforce.praxis.gupy.dto;

import java.time.Instant;
import java.util.List;

/** Visão consolidada da prontidão técnica e das evidências da homologação Gupy. */
public record GupyHomologationResponse(
        String status,
        int readinessPercent,
        boolean externalApprovalRequired,
        String publicBaseUrl,
        Instant generatedAt,
        Metrics metrics,
        List<Endpoint> endpoints,
        List<Check> checks
) {

    public record Metrics(
            long publishedTests,
            long gupyAttempts,
            long completedGupyAttempts,
            long attemptsWithResultWebhook,
            long sentResultWebhooks,
            long resultWebhooksInDlq,
            Instant lastGupyAttemptAt,
            Instant lastAuthenticatedRequestAt
    ) {
    }

    public record Endpoint(String method, String url, String purpose) {
    }

    public record Check(
            String code,
            String title,
            String status,
            String detail,
            boolean external
    ) {
    }
}
