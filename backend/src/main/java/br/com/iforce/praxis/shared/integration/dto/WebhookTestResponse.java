package br.com.iforce.praxis.shared.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/** Resultado do envio de um evento de teste ao webhook do cliente. */
@Schema(description = "Resultado de um envio de teste ao webhook.")
public record WebhookTestResponse(
        @Schema(example = "true")
        boolean delivered,

        @Schema(example = "200")
        Integer httpStatus,

        @Schema(example = "OK")
        String responseSnippet
) {
}
