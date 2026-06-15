package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alternativa visível ao candidato, sem gabarito, pesos ou marcadores internos.")
public record CandidateOptionResponse(
        @Schema(example = "opcao-equilibrada")
        String id,

        @Schema(example = "Acolho a frustração, peço os dados mínimos e explico o próximo passo.")
        String text
) {
}
