package br.com.iforce.praxis.tenantconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;


@Schema(description = "Catálogos configuráveis por empresa usados nas telas de autoria.")
public record EmpresaConfigResponse(
        @Schema(description = "Competencias avaliaveis no plano da avaliacao.")
        List<ConfigOptionDto> competencies,

        @Schema(description = "Limites de tempo de resposta oferecidos no editor.")
        List<ConfigOptionDto> answerTimeLimits
) {
}
