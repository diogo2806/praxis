package br.com.iforce.praxis.gupy.delivery.dto;

import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Estado de entrega assíncrona do resultado para a Gupy.")
public record ResultDeliveryResponse(
        @Schema(example = "1")
        Long id,

        @Schema(example = "att_123")
        String attemptId,

        @Schema(example = "res_123")
        String resultId,

        @Schema(example = "https://cliente.gupy.io/result-webhook")
        String webhookUrl,

        @Schema(example = "retrying")
        ResultDeliveryStatus status,

        @Schema(example = "2")
        int attemptCount,

        @Schema(example = "2026-06-15T20:00:04Z")
        Instant nextAttemptAt,

        @Schema(example = "2026-06-15T20:00:00Z")
        Instant lastAttemptAt,

        @Schema(example = "2026-06-15T20:00:00Z")
        Instant sentAt,

        @Schema(example = "HTTP 500")
        String lastError,

        @Schema(example = "2026-06-15T20:00:00Z")
        Instant createdAt
) {
}
