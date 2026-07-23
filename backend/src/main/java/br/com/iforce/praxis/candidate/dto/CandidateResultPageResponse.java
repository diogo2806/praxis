package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Dados seguros para a página final da pessoa candidata.")
public record CandidateResultPageResponse(
        @Schema(example = "Atendimento em situação crítica")
        String avaliacaoNome,

        @Schema(example = "concluida", allowableValues = {
                "nao_iniciada", "em_andamento", "concluida", "abandonada", "expirada"
        })
        String status,

        @Schema(example = "true")
        boolean finalizado,

        @Schema(example = "https://cliente.gupy.io/candidate-return")
        String redirectUrl,

        @Schema(example = "2026-07-15T14:30:00Z")
        Instant concluidoEm,

        @Schema(description = "Resultados major permitidos pelo contrato Gupy para exibição à pessoa candidata.")
        List<CandidateResultItemResponse> resultados,

        @Schema(example = "240", description = "Pontos obtidos antes da normalização pelo caminho.")
        Integer pontuacaoBruta,

        @Schema(example = "320", description = "Teto bruto disponível no caminho efetivamente percorrido.")
        Integer pontuacaoMaximaCaminho,

        @Schema(example = "75", description = "Nota comparável de 0 a 100 após normalização pelo caminho.")
        Integer pontuacaoNormalizada,

        @Schema(example = "path-normalized-v2")
        String versaoAlgoritmoPontuacao
) {

    public CandidateResultPageResponse(
            String avaliacaoNome,
            String status,
            boolean finalizado,
            String redirectUrl,
            Instant concluidoEm,
            List<CandidateResultItemResponse> resultados
    ) {
        this(
                avaliacaoNome,
                status,
                finalizado,
                redirectUrl,
                concluidoEm,
                resultados,
                null,
                null,
                null,
                null
        );
    }
}
