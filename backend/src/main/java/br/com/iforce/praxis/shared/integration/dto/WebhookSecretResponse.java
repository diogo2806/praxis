package br.com.iforce.praxis.shared.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Segredo HMAC do webhook, exibido por completo apenas ao gerar/rotacionar.
 * Depois disso só o preview fica disponível.
 */
@Schema(description = "Segredo HMAC do webhook (completo apenas na rotação).")
public record WebhookSecretResponse(
        @Schema(example = "whsec_3f2a...")
        String secret,

        @Schema(example = "whsec_••••3f2a")
        String secretPreview
) {
}
