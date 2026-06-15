package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Resposta escolhida pelo candidato em um turno.")
public record SubmitAnswerRequest(
        @NotBlank
        @Schema(example = "turno-1")
        String nodeId,

        @NotBlank
        @Schema(example = "opcao-equilibrada")
        String optionId
) {
}
