package br.com.iforce.praxis.marketplace.dto;

public record AdminMarketplaceDashboardResponse(
        long pendingProfessionals,
        long verifiedProfessionals,
        long pendingListings,
        long approvedListings,
        long paidOrders
) {
}
