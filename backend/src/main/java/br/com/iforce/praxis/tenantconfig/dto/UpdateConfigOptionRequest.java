package br.com.iforce.praxis.tenantconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Atualiza uma opcao especifica de um catalogo.")
public record UpdateConfigOptionRequest(
        @NotBlank
        @Schema(example = "Pensamento Critico")
        String label,

        boolean locked,

        boolean selectedByDefault,

        @Schema(example = "true", description = "Quando false a opcao esta desativada.")
        Boolean active
) {
    public UpdateConfigOptionRequest {
        if (active == null) {
            active = true;
        }
    }
}
