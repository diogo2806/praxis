package br.com.iforce.praxis.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Opção de enum para selects e comboboxes do front.")
public record EnumOptionResponse(
        @Schema(example = "COMPLETED")
        String value,

        @Schema(example = "completed")
        String label
) {
}
