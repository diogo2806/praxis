package br.com.iforce.praxis.shared.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;


import java.util.List;


/** Configuração do webhook genérico assinado (integração CUSTOM_API). */
@Schema(description = "Configuração do webhook personalizado do cliente.")
public record ConfigureGenericWebhookRequest(
        @Schema(example = "https://meu-ats.com/webhooks/praxis")
        @NotBlank(message = "Informe a URL de destino do webhook.")
        String webhookUrl,

        @Schema(example = "[\"RESULT_READY\"]")
        List<String> events
) {
}
