package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;

import java.time.Instant;
import java.util.List;

/** Detalhe completo de um cliente (aba Geral + usuários de acesso). */
public record TenantAdminDetailResponse(
        String tenantId,
        String name,
        String tradeName,
        String legalName,
        String taxId,
        String corporateEmail,
        String phone,
        String website,
        boolean healthVertical,
        CommercialPlanType commercialPlanType,
        String commercialCondition,
        TenantStatus status,
        long completedAttemptsInPeriod,
        List<AdminUserResponse> users,
        Instant createdAt,
        Instant updatedAt
) {
}
