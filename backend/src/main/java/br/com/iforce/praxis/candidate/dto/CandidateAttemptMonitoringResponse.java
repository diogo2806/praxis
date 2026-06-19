package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resumo operacional de uma tentativa em andamento para monitoramento da empresa.")
public record CandidateAttemptMonitoringResponse(
        @Schema(example = "att_abc123def456")
        String attemptId,

        @Schema(example = "Maria Silva")
        String candidateName,

        @Schema(example = "maria@example.com")
        String candidateEmail,

        @Schema(example = "sim-atendimento-n2")
        String simulationId,

        @Schema(example = "Simulacao Atendimento N2")
        String simulationName,

        @Schema(example = "3")
        int versionNumber,

        AttemptStatus status,

        @Schema(example = "2")
        int currentTurn,

        @Schema(example = "5")
        int estimatedTurns,

        @Schema(example = "40")
        int progressPercent,

        @Schema(example = "522")
        long elapsedSeconds,

        Instant lastSignalAt,

        @Schema(example = "true")
        boolean active
) {
}
