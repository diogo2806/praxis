package br.com.iforce.praxis.shared.integration.dto;

import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;

import io.swagger.v3.oas.annotations.media.Schema;


import java.time.Instant;

import java.util.List;


/** Estado atual do webhook personalizado do cliente. */
@Schema(description = "Estado do webhook personalizado configurado.")
public record GenericWebhookConfigResponse(
        @Schema(example = "https://meu-ats.com/webhooks/praxis")
        String webhookUrl,

        @Schema(example = "whsec_••••3f2a")
        String secretPreview,

        @Schema(example = "[\"RESULT_READY\"]")
        List<String> events,

        IntegrationStatus status,

        Instant lastDeliveryAt,

        String lastError
) {
}
