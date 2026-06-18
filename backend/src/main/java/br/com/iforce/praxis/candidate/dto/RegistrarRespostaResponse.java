package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do registro de resposta do candidato.")
public record RegistrarRespostaResponse(
        @Schema(example = "att_123")
        String participacaoId,

        @Schema(example = "concluida")
        String status,

        @Schema(example = "true")
        boolean repetida,

        @Schema(example = "true")
        boolean finalizado,

        EtapaAtualResponse etapaAtual
) {
}
