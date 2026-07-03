package br.com.iforce.praxis.engagement.dto;

import java.time.Instant;


/**
 * Métricas agregadas de engajamento de um cliente no período, base do relatório mensal enviado
 * ao e-mail corporativo dos administradores da empresa (papel {@code EMPRESA}).
 *
 * <p>O destaque é {@code hoursSaved} — as "Horas economizadas com avaliações Práxis" —, uma
 * tradução direta do volume de avaliações concluídas ({@code completedEvaluations}) em valor
 * percebido, reforçando o retorno contínuo do software e reduzindo a chance de evasão.</p>
 */
public record EngagementReportSummary(
        String empresaId,
        String name,
        String corporateEmail,
        Instant periodStart,
        Instant periodEnd,
        long completedEvaluations,
        double hoursSaved
) {
}
