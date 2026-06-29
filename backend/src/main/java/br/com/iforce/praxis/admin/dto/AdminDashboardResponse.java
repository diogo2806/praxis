package br.com.iforce.praxis.admin.dto;

import java.time.Instant;
import java.util.List;

/** Visão consolidada do dashboard administrativo (rota {@code /admin}). */
public record AdminDashboardResponse(
        Instant periodStart,
        Instant periodEnd,
        long totalTenants,
        long activeTenants,
        long trialTenants,
        long suspendedTenants,
        long canceledTenants,
        long totalCompletedAttempts,
        List<TopUsageTenant> topUsageTenants,
        List<TenantAdminSummaryResponse> recentTenants,
        List<TenantAdminSummaryResponse> attentionTenants
) {

    /** Linha do ranking de uso por tenant no período. */
    public record TopUsageTenant(
            String tenantId,
            String name,
            long completedAttempts
    ) {
    }
}
