package br.com.iforce.praxis.candidate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resposta escolhida pelo candidato na etapa atual.")
public record RegistrarRespostaRequest(
        @JsonAlias("nodeId")
        @Schema(example = "turno-1", description = "Campo opcional para compatibilidade com clientes antigos.")
        String etapaId,

        @JsonAlias("optionId")
        @Schema(example = "A", nullable = true,
                description = "Resposta escolhida. Obrigatória quando tempoEsgotado=false; ignorada quando o tempo se esgota.")
        String respostaId,

        @JsonAlias({"nodeNumber", "stepNumber"})
        @Schema(example = "1", nullable = true,
                description = "Numero publico da etapa usado para conciliar respostas atrasadas.")
        Integer etapaNumero,

        @JsonAlias({"answeredAt", "clientAnsweredAt", "respondedAt"})
        @Schema(example = "2026-06-19T12:30:45Z", nullable = true,
                description = "Horário em que o front-end gerou a resposta.")
        Instant respondidaEm,

        @JsonAlias("timedOut")
        @Schema(example = "false",
                description = "Quando true, registra a etapa sem resposta e encerra a participacao.")
        boolean tempoEsgotado
) {
}
