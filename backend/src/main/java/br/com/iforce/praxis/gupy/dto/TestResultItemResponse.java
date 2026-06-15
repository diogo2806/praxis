package br.com.iforce.praxis.gupy.dto;

import br.com.iforce.praxis.gupy.model.ResultTier;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nota por competência no formato esperado pela Gupy.")
public record TestResultItemResponse(
        @Schema(example = "Empatia")
        String name,

        @Schema(example = "86", minimum = "0", maximum = "100")
        Integer score,

        @Schema(example = "major")
        ResultTier tier
) {
}
