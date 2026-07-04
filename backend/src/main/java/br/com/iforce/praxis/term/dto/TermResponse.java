package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Resposta com o termo que deve ser apresentado ao usuário para leitura e aceite.
 *
 * <p>Na visão do processo, reúne tudo que a interface precisa mostrar antes da
 * confirmação: qual termo é, qual versão está valendo e qual texto deve ser lido.</p>
 *
 * @param type tipo do termo exibido no processo
 * @param version versão vigente do termo
 * @param text texto apresentado ao usuário antes do aceite
 */
@Schema(description = "Termo exibido ao recrutador para aceite.")
public record TermResponse(
        String type,
        String version,
        String text
) {
}
