package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Pedido para criar uma nova tentativa por link direto.")
public record CreateDirectCandidateLinkRequest(
        @NotBlank
        @Schema(example = "sim-atendimento-n2")
        String simulationId,

        @NotBlank
        @Schema(example = "Maria Silva")
        String candidateName,

        @Email
        @NotBlank
        @Schema(example = "maria@example.com")
        String candidateEmail,

        @Schema(example = "1.50", description = "Multiplicador de tempo para acomodacoes de acessibilidade.")
        BigDecimal accommodationTimeMultiplier,

        @Size(max = 120)
        @Schema(
                example = "vaga-123-aplicacao-2026-07",
                description = "Identificador idempotente do ciclo/vaga. O mesmo valor reaproveita a mesma criacao; um novo valor cria outra tentativa."
        )
        String applicationCycleId
) {
}
