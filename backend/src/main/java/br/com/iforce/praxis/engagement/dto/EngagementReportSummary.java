package br.com.iforce.praxis.engagement.dto;

import java.time.Instant;


/**
 * Métricas agregadas de engajamento de um cliente no período, base do relatório mensal enviado
 * ao e-mail corporativo dos administradores da empresa (papel {@code EMPRESA}).
 *
 * <p>A estimativa de tempo poupado é opcional e sempre carrega os elementos necessários para
 * interpretação responsável: período, fórmula, hipótese configurada, fonte metodológica e ressalva
 * de que o valor não representa economia observada sem uma comparação real do processo.</p>
 */
public record EngagementReportSummary(
        String empresaId,
        String name,
        String corporateEmail,
        Instant periodStart,
        Instant periodEnd,
        long completedEvaluations,
        boolean timeSavingEstimateEnabled,
        Double estimatedHoursSaved,
        Double assumedHoursPerCompletedEvaluation,
        String estimationFormula,
        String estimationMethodologySource,
        String estimationCaveat
) {

    public static final String TIME_SAVING_ESTIMATION_FORMULA =
            "completedEvaluations * assumedHoursPerCompletedEvaluation";

    public static final String TIME_SAVING_ESTIMATION_CAVEAT =
            "Estimativa baseada em hipotese configurada; nao representa economia observada "
                    + "nem efeito causal sem comparacao com dados reais do processo manual.";
}
