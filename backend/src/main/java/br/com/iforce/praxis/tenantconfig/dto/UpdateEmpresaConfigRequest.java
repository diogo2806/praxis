package br.com.iforce.praxis.tenantconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;


import java.util.List;


@Schema(description = "Substitui o catalogo de um tipo de configuracao para a empresa.")
public record UpdateEmpresaConfigRequest(
        @NotNull
        @Valid
        @Schema(description = "Lista ordenada de opcoes. A ordem enviada e preservada na exibicao.")
        List<OptionInput> options
) {

    @Schema(description = "Opção a persistir para a empresa.")
    public record OptionInput(
            @NotBlank
            @Schema(example = "Pensamento Critico")
            String value,

            @Schema(example = "Pensamento Critico", description = "Quando vazio, o value e reutilizado como rotulo.")
            String label,

            boolean locked,

            boolean selectedByDefault
    ) {
    }
}
