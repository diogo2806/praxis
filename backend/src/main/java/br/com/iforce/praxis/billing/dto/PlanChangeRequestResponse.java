package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.billing.model.PlanChangeRequestType;

import java.time.Instant;

/** Acompanhamento de uma solicitação de mudança ou cancelamento de contrato Enterprise. */
public record PlanChangeRequestResponse(
        Long id,
        PlanChangeRequestType requestType,
        CommercialPlanType currentPlan,
        CommercialPlanType requestedPlan,
        String status,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
