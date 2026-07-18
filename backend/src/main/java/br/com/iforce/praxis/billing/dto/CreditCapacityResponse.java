package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

/** Capacidade atual para iniciar novas avaliações sem confundir saldo com reservas em andamento. */
public record CreditCapacityResponse(
        CommercialPlanType plan,
        boolean metered,
        int creditBalance,
        int reservedCredits,
        int availableCredits
) {
}
