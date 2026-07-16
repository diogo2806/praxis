package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado numérico seguro exibido à pessoa candidata.")
public record CandidateResultItemResponse(
        @Schema(example = "Comunicação")
        String titulo,

        @Schema(example = "73", minimum = "0", maximum = "100")
        int pontuacao,

        @Schema(example = "73%")
        String resultado
) {
}
