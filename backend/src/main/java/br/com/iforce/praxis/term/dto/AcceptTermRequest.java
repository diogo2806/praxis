package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Aceite do termo: a versão deve ser a corrente exibida ao usuário.")
public record AcceptTermRequest(
        @NotBlank
        @Schema(example = "2026-06-01", description = "Versão do termo que está sendo aceita.")
        String version
) {
}
