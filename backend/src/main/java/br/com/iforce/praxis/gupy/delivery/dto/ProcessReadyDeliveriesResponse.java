package br.com.iforce.praxis.gupy.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado do processamento em lote das entregas prontas.")
public record ProcessReadyDeliveriesResponse(
        @Schema(example = "3")
        int processedCount,

        List<ResultDeliveryResponse> deliveries
) {
}
