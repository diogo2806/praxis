package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Consentimento do participante (paciente) para tratamento de dado sensível de saúde na vertical
 * educativa (Minuta A — LGPD, arts. 11 e 14). Registrado na trilha de auditoria da tentativa.
 */
@Schema(description = "Consentimento do participante para tratamento de dado sensível de saúde.")
public record HealthConsentRequest(
        @NotBlank
        @Size(max = 40)
        @Schema(example = "2026-06-01", description = "Versão do aviso de consentimento exibido ao participante.")
        String version,

        @Schema(
                example = "false",
                description = "Verdadeiro quando o consentimento é dado por responsável legal (menor/vulnerável)."
        )
        boolean onBehalfOfMinor
) {
}
