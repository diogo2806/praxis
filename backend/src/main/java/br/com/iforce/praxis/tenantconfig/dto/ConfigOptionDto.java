package br.com.iforce.praxis.tenantconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Opcao configuravel por tenant exibida no frontend.")
public record ConfigOptionDto(
        @Schema(example = "Empatia", description = "Valor estavel usado pela aplicacao.")
        String value,

        @Schema(example = "Empatia", description = "Rotulo exibido ao usuario.")
        String label,

        @Schema(example = "false", description = "Quando true a opcao aparece travada e nao selecionavel.")
        boolean locked,

        @Schema(example = "false", description = "Quando true a opcao ja vem marcada por padrao.")
        boolean selectedByDefault,

        @Schema(example = "true", description = "Quando false a opcao esta desativada e nao aparece em fluxos normais.")
        boolean active
) {
}
