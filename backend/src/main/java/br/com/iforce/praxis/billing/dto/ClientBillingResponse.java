package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import java.time.Instant;
import java.util.List;

/** Visão consolidada de cobrança exibida ao próprio cliente (tenant). */
public record ClientBillingResponse(
        String tenantId,
        CommercialPlanType plan,
        TenantStatus tenantStatus,
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
            Instant currentPeriodEnd,
            Instant lastPaymentAt,
            Instant graceUntil
    ) {
    }
}
