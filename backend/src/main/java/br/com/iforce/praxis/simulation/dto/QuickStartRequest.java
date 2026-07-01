package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.QuickStartCategory;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;


/** Pedido para criar um rascunho a partir de um modelo pronto. */
@Schema(description = "Categoria do modelo pronto a usar como base.")
public record QuickStartRequest(
        @Schema(example = "ATENDIMENTO")
        @NotNull(message = "Informe a categoria do modelo.")
        QuickStartCategory category
) {
}
