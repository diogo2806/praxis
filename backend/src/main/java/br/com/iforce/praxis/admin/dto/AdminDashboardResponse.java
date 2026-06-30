package br.com.iforce.praxis.admin.dto;

import java.time.Instant;

import java.util.List;


/** Visão consolidada do dashboard administrativo (rota {@code /admin}). */
public record AdminDashboardResponse(
        Instant periodStart,
        Instant periodEnd,
        long totalEmpresas,
        long activeEmpresas,
        long trialEmpresas,
        long suspendedEmpresas,
        long canceledEmpresas,
        long totalCompletedAttempts,
        List<TopUsageEmpresa> topUsageEmpresas,
        List<EmpresaAdminSummaryResponse> recentEmpresas,
        List<EmpresaAdminSummaryResponse> attentionEmpresas
) {

    /** Linha do ranking de uso por empresa no período. */
    public record TopUsageEmpresa(
            String empresaId,
            String name,
            long completedAttempts
    ) {
    }
}
