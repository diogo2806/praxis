package br.com.iforce.praxis.recrutei.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;


/**
 * Uma página da lista de provas publicadas, no formato da Recrutei.
 *
 * <p>Na visão do processo, é a "vitrine paginada": como a lista de provas pode
 * ser grande, ela é entregue em partes. Este registro informa quantos itens
 * vieram nesta página (limit), a partir de qual posição (offset), o total
 * existente e a lista de provas em si. Assim a Recrutei sabe se há mais páginas
 * a buscar.</p>
 */
@Schema(description = "Página de testes publicada no formato esperado pela Recrutei.")
public record RecruteiTestListResponse(
        @Schema(example = "50")
        int limit,

        @Schema(example = "0")
        int offset,

        @Schema(example = "1")
        int total,

        List<RecruteiTestResponse> data
) {
}
