package br.com.iforce.praxis.recrutei.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * O resultado de uma única competência dentro do boletim da prova.
 *
 * <p>Na visão do processo, é cada "linha" do boletim: o nome da competência,
 * uma frase descritiva, a nota obtida, o tipo de nota (percentual), o nível de
 * desempenho alcançado e a data da avaliação. Várias dessas linhas juntas
 * formam o resultado completo entregue à Recrutei.</p>
 */
@Schema(description = "Item de resultado de competência.")
public record RecruteiTestResultItemResponse(
        String title,
        String description,
        int score,
        String type,
        String tier,
        String date
) {
}
