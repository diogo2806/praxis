package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.model.ListingStatus;

import java.math.BigDecimal;

public record ListingSummaryResponse(
        Long id,
        String title,
        ListingCategory category,
        long priceCents,
        ListingStatus status,
        BigDecimal averageRating,
        int totalReviews,
        ProfessionalSummaryResponse professional
) {
    public record ProfessionalSummaryResponse(
            Long id,
            String displayName,
            boolean verified
    ) {
    }
}
