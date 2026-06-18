package br.com.iforce.praxis.tenantconfig.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Catalogos configuraveis por tenant para as telas de autoria.")
public record TenantConfigResponse(
        @Schema(description = "Competencias avaliaveis no plano da avaliacao.")
        List<ConfigOptionDto> competencies,

        @Schema(description = "Niveis de senioridade reconhecidos pela empresa.")
        List<ConfigOptionDto> seniorityLevels,

        @Schema(description = "Itens do checklist de linguagem inclusiva.")
        List<ConfigOptionDto> languageChecklist,

        @Schema(description = "Usos possiveis do resultado no funil de selecao.")
        List<ConfigOptionDto> resultUses,

        @Schema(description = "Limites de tempo de resposta oferecidos no editor.")
        List<ConfigOptionDto> answerTimeLimits
) {
}
