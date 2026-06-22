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

        @Schema(example = "CONTINUAR_TESTE", allowableValues = {"INICIAR", "CONTINUAR_TESTE", "VER_RESULTADOS"})
        String acaoSugeridaFrontend,

        ProgressoResponse progresso,

        EtapaAtualResponse etapaAtual,

        @Schema(
                example = "false",
                description = "Quando verdadeiro, o tenant opera na vertical de saúde: o fluxo deve coletar "
                        + "consentimento do participante para tratamento de dado sensível (LGPD)."
        )
        boolean verticalSaude
) {
    @Schema(description = "Resumo narrativo do progresso do candidato, sem expor gabarito ou proximas escolhas.")
    public record ProgressoResponse(
            @Schema(example = "2")
            int passoAtual,

            @Schema(example = "5")
            int passosEstimados,

            @Schema(example = "40")
            int percentual
    ) {
    }
}
