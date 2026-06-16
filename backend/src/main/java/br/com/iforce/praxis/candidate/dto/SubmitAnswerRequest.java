package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Resposta escolhida pelo candidato em um turno (ou marcação de timeout do turno).")
public record SubmitAnswerRequest(
        @NotBlank
        @Schema(example = "turno-1")
        String nodeId,

        @Schema(example = "opcao-equilibrada", nullable = true,
                description = "Alternativa escolhida. Obrigatória quando timedOut=false; ignorada no timeout.")
        String optionId,

        @Schema(example = "false",
                description = "Quando true, o turno estourou o tempo: registra resposta nível 0 e encerra a tentativa.")
        boolean timedOut
) {
}
