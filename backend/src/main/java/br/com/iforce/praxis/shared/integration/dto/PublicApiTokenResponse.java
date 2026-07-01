package br.com.iforce.praxis.shared.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Token de API pública gerado para o cliente consumir a API da Práxis.
 * O valor completo só é exibido nesta resposta (na geração/rotação); depois,
 * apenas o preview fica disponível.
 */
@Schema(description = "Token de API pública (exibido apenas na criação/rotação).")
public record PublicApiTokenResponse(
        @Schema(example = "prx_live_xxxxxxxxxxxx")
        String token,

        @Schema(example = "prx_live_••••ab12")
        String tokenPreview
) {
}
