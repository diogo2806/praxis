package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import java.util.List;

/** Dados complementares para os controles de troca e cancelamento de plano. */
public record PlanManagementResponse(
        CommercialPlanType currentPlan,
        List<PlanChangeRequestResponse> enterpriseRequests
) {
}
