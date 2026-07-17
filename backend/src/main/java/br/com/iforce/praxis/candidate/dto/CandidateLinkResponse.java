package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;

@Schema(description = "Link de candidato gerado para uma tentativa de simulacao.")
public record CandidateLinkResponse(
        @Schema(example = "att_abc123def456") String attemptId,
        @Schema(example = "https://praxis.example.com/candidato/eyJhbGciOiJIUzI1NiJ9...") String candidateUrl,
        @Schema(example = "Maria Silva") String candidateName,
        @Schema(example = "maria@example.com") String candidateEmail,
        @Schema(example = "sim-atendimento-n2") String simulationId,
        @Schema(example = "Simulacao Atendimento N2") String simulationName,
        AttemptStatus status,
        Instant createdAt,
        Instant linkIssuedAt,
        Instant linkExpiresAt,
        @Schema(example = "7") long remainingDays,
        @Schema(allowableValues = {"active", "expiringSoon", "expired"}) String linkStatus
) {

    private static final long LEGACY_DEFAULT_TTL_HOURS = 168;

    public CandidateLinkResponse(
            String attemptId,
            String candidateUrl,
            String candidateName,
            String candidateEmail,
            String simulationId,
            String simulationName,
            AttemptStatus status,
            Instant createdAt
    ) {
        this(
                attemptId,
                candidateUrl,
                candidateName,
                candidateEmail,
                simulationId,
                simulationName,
                status,
                createdAt,
                createdAt,
                createdAt.plusSeconds(LEGACY_DEFAULT_TTL_HOURS * 60L * 60L),
                remainingDays(Instant.now(), createdAt.plusSeconds(LEGACY_DEFAULT_TTL_HOURS * 60L * 60L)),
                linkStatus(Instant.now(), createdAt.plusSeconds(LEGACY_DEFAULT_TTL_HOURS * 60L * 60L))
        );
    }

    public static long remainingDays(Instant now, Instant expiresAt) {
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            return 0;
        }
        long seconds = Duration.between(now, expiresAt).getSeconds();
        return Math.max(1, (seconds + 86_399L) / 86_400L);
    }

    public static String linkStatus(Instant now, Instant expiresAt) {
        long days = remainingDays(now, expiresAt);
        if (days == 0) {
            return "expired";
        }
        return days <= 3 ? "expiringSoon" : "active";
    }
}
