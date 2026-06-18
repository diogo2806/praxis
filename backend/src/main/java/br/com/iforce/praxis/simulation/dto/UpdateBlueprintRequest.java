package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.validation.SumWeightsEqualsOne;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@SumWeightsEqualsOne
@Schema(description = "Payload para atualizar o plano estrutural de uma versao de simulacao.")
public record UpdateBlueprintRequest(
        @Size(max = 180)
        @Schema(example = "Analista de Atendimento N2")
        String name,

        @Size(max = 1200)
        @Schema(example = "Cliente exige solucao imediata para atraso recorrente.")
        String criticalSituation,

        @Size(max = 1000)
        @Schema(example = "Reconhece o impacto, aplica a politica e propoe proximos passos.")
        String highPerformance,

        @Size(max = 1000)
        @Schema(example = "Prometer excecao proibida ou culpar outra area.")
        String criticalError,

        @Size(max = 120)
        @Schema(example = "Triagem")
        String resultUse,

        @NotBlank
        @Size(max = 120)
        @Schema(example = "turno-1")
        String rootNodeId,

        @Valid
        @NotEmpty
        @Schema(description = "Competencias avaliadas e pesos normalizados.")
        List<CompetencyRequest> competencies
) {

    public record CompetencyRequest(
            @NotBlank
            @Size(max = 140)
            @Schema(example = "Empatia")
            String name,

            @NotNull
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            @Schema(example = "0.35")
            Double weight
    ) {
    }
}
