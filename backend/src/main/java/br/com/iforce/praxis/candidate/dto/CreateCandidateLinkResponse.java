package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Link gerado para o candidato acessar a simulacao.")
public record CreateCandidateLinkResponse(
        @Schema(example = "att_abc123def456")
        String attemptId,

        @Schema(example = "https://praxis.example.com/candidato/att_abc123def456")
        String candidateUrl,

        @Schema(example = "Simulacao Atendimento N2")
        String simulationName
) {
}
