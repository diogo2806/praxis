package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pedido da empresa para gerar link de simulacao para um candidato.")
public record CreateCandidateLinkRequest(
        @NotBlank
        @Schema(example = "sim-atendimento-n2")
        String simulationId,

        @NotBlank
        @Schema(example = "Maria Silva")
        String candidateName,

        @Email
        @NotBlank
        @Schema(example = "maria@example.com")
        String candidateEmail
) {
}
