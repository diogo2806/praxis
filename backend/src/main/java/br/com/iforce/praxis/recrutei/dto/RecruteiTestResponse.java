package br.com.iforce.praxis.recrutei.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uma prova publicada, apresentada à Recrutei como item de catálogo.
 *
 * <p>Na visão do processo, é o "cartão" de uma prova na vitrine que a Recrutei
 * consulta: traz o identificador, o nome, a categoria, uma descrição curta e o
 * nível da avaliação. É o que o recrutador vê ao escolher qual prova aplicar.</p>
 */
@Schema(description = "Teste Praxis publicado e visível para consumo pela Recrutei.")
public record RecruteiTestResponse(
        @Schema(example = "sim-atendimento-n2")
        String id,

        @Schema(example = "Atendimento N2")
        String name,

        @Schema(example = "Situational Judgment")
        String category,

        @Schema(example = "Avaliação situacional determinística para priorização, comunicação e decisão em contexto.")
        String description,

        @Schema(example = "advanced", allowableValues = {"advanced", "intermediate", "basic"})
        String level
) {
}
