package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;


/**
 * Dados enviados pela tela quando uma pessoa confirma um termo.
 *
 * <p>Na visão do processo, informa ao sistema qual versão estava sendo exibida
 * no momento da confirmação.</p>
 *
 * @param version versão apresentada na tela e confirmada pelo usuário
 */
@Schema(description = "Aceite do termo: a versão deve ser a corrente exibida ao usuário.")
public record AcceptTermRequest(
        @NotBlank
        @Schema(example = "2026-06-01", description = "Versão do termo que está sendo aceita.")
        String version
) {
}
