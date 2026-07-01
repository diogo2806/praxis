package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.model.ListingStatus;

import java.util.List;
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
        List<PreviewNodeResponse> previewNodes,
        ListingSummaryResponse.ProfessionalSummaryResponse professional
) {
    public record PreviewNodeResponse(
            Long id,
            String nodeId,
            int turnIndex,
            String speaker,
            String message,
            List<PreviewOptionResponse> options
    ) {
    }

    public record PreviewOptionResponse(
            Long id,
            String optionId,
            String text
    ) {
    }
}
