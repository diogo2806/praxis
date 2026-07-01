package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.QuickStartCategory;

import io.swagger.v3.oas.annotations.media.Schema;


/** Resumo de um modelo pronto exibido na grade do "começar rápido". */
@Schema(description = "Resumo de um modelo pronto do começar rápido.")
public record QuickStartTemplateSummaryResponse(
        @Schema(example = "ATENDIMENTO")
        QuickStartCategory category,

        @Schema(example = "Atendimento sob pressão")
        String title,

        @Schema(example = "3 cenários prontos de escalonamento e SLA")
        String description,

        @Schema(example = "3")
        int nodeCount
) {
}
