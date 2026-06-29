package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;

import java.time.Instant;

/** Linha da listagem de clientes do painel ADMIN. */
public record TenantAdminSummaryResponse(
        String tenantId,
        String name,
        String tradeName,
        String taxId,
        String corporateEmail,
        CommercialPlanType commercialPlanType,
        TenantStatus status,
        long completedAttemptsInPeriod,
        Instant createdAt
) {
}
