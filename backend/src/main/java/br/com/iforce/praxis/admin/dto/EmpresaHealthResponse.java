package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.CustomerHealthLevel;

import br.com.iforce.praxis.admin.model.EmpresaStatus;


import java.time.Instant;


/**
 * Saúde de retenção de um cliente para o painel de "Saúde do Cliente" e para a fila de atuação
 * de Customer Success ({@code GET /api/admin/empresas/at-risk}).
 *
 * <p>Compara o volume de avaliações concluídas no período atual ({@code completedCurrentPeriod},
 * janela {@code periodStart}–{@code periodEnd}) com o período imediatamente anterior de mesmo
 * tamanho ({@code completedPreviousPeriod}, janela {@code previousPeriodStart}–
 * {@code previousPeriodEnd}). {@code dropPercent} é a queda relativa (0.30 = caiu 30%; valores
 * negativos indicam crescimento) e {@code healthScore} é um índice de 0 a 100 (100 = manteve ou
 * cresceu). Ambos são {@code null} quando não há base suficiente no período anterior.</p>
 */
public record EmpresaHealthResponse(
        String empresaId,
        String name,
        String tradeName,
        String corporateEmail,
        CommercialPlanType commercialPlanType,
        EmpresaStatus status,
        CustomerHealthLevel level,
        long completedCurrentPeriod,
        long completedPreviousPeriod,
        Double dropPercent,
        Integer healthScore,
        Instant lastCompletedAt,
        Instant periodStart,
        Instant periodEnd,
        Instant previousPeriodStart,
        Instant previousPeriodEnd
) {
}
