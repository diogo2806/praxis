package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado publico da participacao do candidato na avaliacao.")
public record ParticipacaoResponse(
        @Schema(example = "att_123")
        String participacaoId,

        @Schema(example = "Atendimento N2")
        String avaliacaoNome,

        @Schema(example = "em_andamento")
        String status,

        @Schema(example = "false")
        boolean finalizado,

        EtapaAtualResponse etapaAtual
) {
}
