package br.com.iforce.praxis.recrutei.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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
