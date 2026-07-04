package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;


/**
 * Pedido enviado pela tela quando o usuário confirma o aceite de um termo.
 *
 * <p>Na visão do processo, a versão informa exatamente qual texto estava vigente na
 * confirmação. Assim, se o termo mudar, o sistema pode solicitar que a nova versão seja
 * lida e aceita antes de seguir.</p>
 *
 * @param version versão do termo exibida ao usuário e confirmada no aceite
 */
@Schema(description = "Aceite do termo: a versão deve ser a corrente exibida ao usuário.")
public record AcceptTermRequest(
        @NotBlank
        @Schema(example = "2026-06-01", description = "Versão do termo que está sendo aceita.")
        String version
) {
}
