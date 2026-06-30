package br.com.iforce.praxis.gupy.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Resultado do reprocessamento manual de uma entrega.")
public record ReprocessDeliveryResponse(
        ResultDeliveryResponse delivery
) {
}
