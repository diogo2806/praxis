package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingStatus;

import java.util.List;

public record ProfessionalDashboardResponse(
        long totalRevenueCents,
        long pendingEscrowCents,
        long releasedCents,
        long salesCount,
        List<ListingDashboardItemResponse> listings,
        List<ReviewResponse> recentReviews,
        List<PayoutSummaryResponse> payouts
) {
    public record ListingDashboardItemResponse(
            Long id,
            String title,
            ListingStatus status,
            long salesCount
    ) {
    }
}
