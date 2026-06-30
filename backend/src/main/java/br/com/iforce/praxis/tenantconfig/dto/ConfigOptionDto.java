package br.com.iforce.praxis.tenantconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Opção configurável por empresa exibida no frontend.")
public record ConfigOptionDto(
        @Schema(example = "Empatia", description = "Valor estavel usado pela aplicacao.")
        String value,

        @Schema(example = "Empatia", description = "Rotulo exibido ao usuario.")
        String label,

        @Schema(example = "false", description = "Quando true, a opção aparece travada e não selecionável.")
        boolean locked,

        @Schema(example = "false", description = "Quando true a opcao ja vem marcada por padrao.")
        boolean selectedByDefault
) {
}
