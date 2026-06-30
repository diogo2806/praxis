package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.audit.model.HumanDecision;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;


@Schema(description = "Decisão de uma pessoa sobre o candidato. A pontuação é apenas apoio; a decisão é humana.")
public record RegisterDispositionRequest(
        @NotNull
        @Schema(example = "advanced", description = "Disposição registrada pelo recrutador: advanced, rejected, hired ou onHold.")
        HumanDecision decision,

        @Size(max = 1000)
        @Schema(nullable = true, description = "Justificativa livre da decisão. Opcional, porém muito valiosa em litígio.")
        String reason
) {
}
