package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;


import java.time.Instant;

import java.util.List;


/** Visão consolidada de cobrança de um cliente (abas Assinatura/Pagamentos da Parte B). */
public record EmpresaBillingOverviewResponse(
        String empresaId,
        CommercialPlanType commercialPlanType,
        EmpresaStatus status,
        int creditBalance,
        SubscriptionInfo subscription,
        List<BillingEventResponse> events
) {

    public record SubscriptionInfo(
            Long id,
            SubscriptionStatus status,
            String mpPreapprovalId,
            String initPoint,
            Instant currentPeriodEnd,
            Instant lastPaymentAt,
            Instant graceUntil
    ) {
    }
}
