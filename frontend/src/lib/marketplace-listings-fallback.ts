import type { MarketplacePageResponse, MarketplaceListingSummary } from "@/lib/api/praxis";

/**
 * Empty fallback response for marketplace listings when API fails.
 * Used when /api/v1/marketplace/listings endpoint is unavailable.
 */
export const marketplaceListingsFallback: MarketplacePageResponse<MarketplaceListingSummary> = {
  content: [],
  page: 0,
  totalPages: 0,
  totalElements: 0,
};
