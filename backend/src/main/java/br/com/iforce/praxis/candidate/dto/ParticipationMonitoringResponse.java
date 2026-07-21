package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo operacional unificado de uma participação individual ou por jornada.")
public record ParticipationMonitoringResponse(
        @Schema(example = "att_abc123")
        String participationId,

        @Schema(example = "individual", allowableValues = {"individual", "journey"})
        String participationType,

        String candidateName,
        String candidateEmail,

        String simulationId,
        String simulationName,
        Integer versionNumber,

        String journeyId,
        String journeyName,
        String sequenceKey,

        @Schema(example = "inProgress")
        String status,

        int currentTurn,
        int estimatedTurns,
        int progressPercent,
        long elapsedSeconds,
        Instant lastSignalAt,
        boolean active,

        String candidateUrl,
        Instant expiresAt,

        @Schema(example = "active", allowableValues = {"active", "expiringSoon", "expired", "canceled"})
        String linkStatus,

        long remainingDays,
        boolean canResend,
        boolean canExtend,
        boolean canCancel,
        String resultAttemptId,
        Instant createdAt
) {
}
