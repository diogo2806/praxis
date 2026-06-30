package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;


@Schema(description = "Pagina de testes publicada no formato esperado pela Gupy.")
public record TestItemsResponse(
        @Schema(example = "50")
        int limit,

        @Schema(example = "0")
        int offset,

        @Schema(example = "1")
        int total_tests,

        List<GupyTestResponse> payload
) {
}
