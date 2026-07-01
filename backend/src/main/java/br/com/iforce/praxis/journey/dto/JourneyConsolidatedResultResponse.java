package br.com.iforce.praxis.journey.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStepStatus;

import io.swagger.v3.oas.annotations.media.Schema;


import java.time.Instant;

import java.util.List;


/**
 * Visão consolidada do candidato em uma jornada: status geral e o resultado de
 * cada teste realizado, sem substituir os resultados individuais.
 *
 * <p>Conforme o MVP, os resultados são exibidos separados por teste; a nota
 * geral ponderada fica para uma etapa posterior.</p>
 */
@Schema(description = "Resultado consolidado de um candidato em uma jornada (separado por teste).")
public record JourneyConsolidatedResultResponse(
        @Schema(example = "jatt_abc123")
        String journeyAttemptId,

        @Schema(example = "processo-trainee-2026-abc12345")
        String journeyId,

        @Schema(example = "Processo Trainee 2026")
        String journeyName,

        @Schema(example = "Maria Silva")
        String candidateName,

        @Schema(example = "maria@example.com")
        String candidateEmail,

        @Schema(example = "principal")
        String sequenceKey,

        @Schema(example = "completed")
        AssessmentJourneyAttemptStatus status,

        @Schema(example = "2026-06-30T12:00:00Z")
        Instant startedAt,

        @Schema(example = "2026-06-30T13:00:00Z")
        Instant completedAt,

        @Schema(description = "Resultado de cada teste realizado dentro da jornada.")
        List<TestResult> tests
) {

    @Schema(description = "Resultado individual de um teste dentro da jornada.")
    public record TestResult(
            @Schema(example = "sim-atendimento-caos")
            String simulationId,

            @Schema(example = "Teste de Atendimento")
            String simulationName,

            @Schema(example = "3")
            int simulationVersionNumber,

            @Schema(example = "true")
            boolean required,

            @Schema(example = "completed")
            AssessmentJourneyStepStatus stepStatus,

            @Schema(example = "att_abc123")
            String candidateAttemptId,

            @Schema(example = "completed", description = "Status da tentativa individual do teste, quando existir.")
            AttemptStatus attemptStatus,

            @Schema(example = "82", description = "Pontuação geral do teste, quando concluído.")
            Integer score,

            @Schema(description = "Indicadores/competências do teste com sua pontuação.")
            List<CompetencyResult> competencies
    ) {
    }

    @Schema(description = "Pontuação de uma competência/indicador de um teste.")
    public record CompetencyResult(
            @Schema(example = "Comunicação")
            String name,

            @Schema(example = "80")
            int score
    ) {
    }
}
