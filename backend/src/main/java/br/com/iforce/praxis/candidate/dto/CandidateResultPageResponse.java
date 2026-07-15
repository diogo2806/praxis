package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados seguros para a página final da pessoa candidata.")
public record CandidateResultPageResponse(
        @Schema(example = "Atendimento em situação crítica")
        String avaliacaoNome,

        @Schema(example = "concluida", allowableValues = {
                "nao_iniciada", "em_andamento", "concluida", "abandonada", "expirada"
        })
        String status,

        @Schema(example = "true")
        boolean finalizado,

        @Schema(example = "https://cliente.gupy.io/candidate-return")
        String redirectUrl,

        @Schema(example = "2026-07-15T14:30:00Z")
        Instant concluidoEm
) {
}
