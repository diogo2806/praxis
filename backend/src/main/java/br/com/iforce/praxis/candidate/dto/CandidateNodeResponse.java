package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Turno atual da simulação visto pelo candidato.")
public record CandidateNodeResponse(
        @Schema(example = "turno-1")
        String id,

        @Schema(example = "1")
        int turnIndex,

        @Schema(example = "Cliente fictício")
        String speaker,

        @Schema(example = "Chegou quebrado. Quero meu dinheiro de volta agora.")
        String message,

        @Schema(example = "45", nullable = true)
        Integer timeLimitSeconds,

        List<CandidateOptionResponse> options
) {
}
