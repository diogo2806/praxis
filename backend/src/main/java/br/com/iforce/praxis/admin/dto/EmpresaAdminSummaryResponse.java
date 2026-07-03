package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;


import java.time.Instant;


/** Linha da listagem de clientes do painel ADMIN. */
public record EmpresaAdminSummaryResponse(
        String empresaId,
        String name,
        String tradeName,
        String taxId,
        String corporateEmail,
        CommercialPlanType commercialPlanType,
        EmpresaStatus status,
        long completedAttemptsInPeriod,
        int creditBalance,
        Instant createdAt
) {
}
