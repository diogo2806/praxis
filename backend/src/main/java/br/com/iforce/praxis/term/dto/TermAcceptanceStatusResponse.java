package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import java.time.Instant;


/**
 * Resposta que resume a situação do aceite de um termo para o usuário atual.
 *
 * <p>Na visão do processo, a tela usa estes dados para saber se a etapa do termo já
 * está concluída, qual versão está valendo agora e qual foi a última versão aceita.</p>
 *
 * @param type tipo do termo consultado no processo
 * @param currentVersion versão vigente que precisa estar aceita
 * @param accepted indica se a versão vigente já foi aceita pelo usuário atual
 * @param acceptedVersion última versão aceita pelo usuário, quando houver
 * @param acceptedAt momento em que o último aceite foi registrado, quando houver
 */
@Schema(description = "Situação de aceite do termo pelo usuário atual.")
public record TermAcceptanceStatusResponse(
        String type,
        String currentVersion,
        boolean accepted,
        String acceptedVersion,
        Instant acceptedAt
) {
}
