package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.model.ListingStatus;

import java.util.Set;

public record ListingDetailResponse(
        Long id,
        String title,
        String description,
        ListingCategory category,
        long priceCents,
        ListingStatus status,
        String sourceSimulationId,
        Long sourceVersionId,
        Set<Long> previewNodeIds,
        ListingSummaryResponse.ProfessionalSummaryResponse professional
) {
}
