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
        ExternalEvidence externalEvidence,
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
            long resultEndpointQueries,
            long validPercentageResults,
            Instant lastGupyAttemptAt,
            Instant lastAuthenticatedRequestAt
    ) {
    }

    public record ExternalEvidence(
            boolean callbackConfirmed,
            Instant callbackConfirmedAt,
            String callbackConfirmedBy,
            boolean resultPagesConfirmed,
            Instant resultPagesConfirmedAt,
            String resultPagesConfirmedBy,
            boolean gupyApproved,
            Instant gupyApprovedAt,
            String gupyApprovedBy,
            boolean clientApproved,
            Instant clientApprovedAt,
            String clientApprovedBy,
            String notes
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
