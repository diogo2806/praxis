package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;


import java.time.Instant;

import java.util.List;


/** Visão consolidada de cobrança exibida ao próprio cliente (empresa). */
public record ClientBillingResponse(
        String empresaId,
        CommercialPlanType plan,
        EmpresaStatus empresaStatus,
        String financialStatus,
        int creditBalance,
        UsageSummary usage,
        SubscriptionInfo subscription,
        List<String> availableActions,
        List<BillingEventResponse> events
) {

    public record UsageSummary(
            long completedLast7Days,
            long completedLast30Days,
            long completedAllTime
    ) {
    }

    public record SubscriptionInfo(
            SubscriptionStatus status,
            String initPoint,
            Instant currentPeriodEnd,
            Instant lastPaymentAt,
            Instant graceUntil
    ) {
    }
}
