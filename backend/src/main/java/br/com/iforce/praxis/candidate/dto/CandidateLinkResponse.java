package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import io.swagger.v3.oas.annotations.media.Schema;


import java.time.Instant;


@Schema(description = "Link de candidato gerado para uma tentativa de simulacao.")
public record CandidateLinkResponse(
        @Schema(example = "att_abc123def456")
        String attemptId,

        @Schema(example = "https://praxis.example.com/candidato/eyJhbGciOiJIUzI1NiJ9...")
        String candidateUrl,

        @Schema(example = "Maria Silva")
        String candidateName,

        @Schema(example = "maria@example.com")
        String candidateEmail,

        @Schema(example = "sim-atendimento-n2")
        String simulationId,

        @Schema(example = "Simulacao Atendimento N2")
        String simulationName,

        AttemptStatus status,

        Instant createdAt
) {
}
