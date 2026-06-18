package br.com.iforce.praxis.candidate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta escolhida pelo candidato na etapa atual.")
public record RegistrarRespostaRequest(
        @JsonAlias("nodeId")
        @Schema(example = "turno-1", description = "Campo opcional para compatibilidade com clientes antigos.")
        String etapaId,

        @JsonAlias("optionId")
        @Schema(example = "A", nullable = true,
                description = "Alternativa escolhida. Obrigatoria quando tempoEsgotado=false; ignorada quando o tempo esgota.")
        String respostaId,

        @JsonAlias("timedOut")
        @Schema(example = "false",
                description = "Quando true, registra a etapa sem resposta e encerra a participacao.")
        boolean tempoEsgotado
) {
}
