package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Resposta usada para exibir um termo na interface.
 *
 * @param type tipo do termo dentro do processo
 * @param version versão vigente do termo
 * @param text conteúdo mostrado ao usuário
 */
@Schema(description = "Termo exibido ao recrutador para aceite.")
public record TermResponse(
        String type,
        String version,
        String text
) {
}
