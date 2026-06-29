package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

/** Plano de cobrança disponível para vincular a um cliente. */
public record SubscriptionPlanResponse(
        Long id,
        String code,
        String name,
        CommercialPlanType planType,
        long priceCents,
        String currency,
        Integer creditAmount
) {
}
