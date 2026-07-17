package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Quantidade de dias a acrescentar à validade do link do candidato.")
public record ExtendCandidateLinkRequest(
        @Schema(example = "7", minimum = "1", maximum = "365")
        @Min(value = 1, message = "Informe pelo menos 1 dia adicional.")
        @Max(value = 365, message = "A extensão máxima por operação é de 365 dias.")
        int additionalDays
) {
}
