import type {
  MarketplaceProfessionalProfile,
  MarketplaceProfessionalDashboard,
  MarketplaceMessageThread,
} from "@/lib/api/praxis";

/**
 * Fallback professional profile when /api/v1/marketplace/professionals/me fails.
 */
export const marketplaceProfessionalMeFallback: MarketplaceProfessionalProfile = {
  id: 0,
  displayName: "Profissional",
  bio: null,
  specialties: [],
  linkedinUrl: null,
  lattesUrl: null,
  lattesVerified: false,
  lattesVerificationCode: null,
  verificationStatus: "PENDING_VERIFICATION",
  averageRating: null,
  totalReviews: 0,
  totalSales: 0,
  mercadoPagoConnected: false,
};

/**
 * Fallback dashboard when /api/v1/marketplace/professionals/me/dashboard fails.
 */
export const marketplaceProfessionalDashboardFallback: MarketplaceProfessionalDashboard = {
  totalRevenueCents: 0,
  pendingEscrowCents: 0,
  releasedCents: 0,
  salesCount: 0,
  listings: [],
  recentReviews: [],
  payouts: [],
};

/**
 * Fallback message threads when /api/v1/marketplace/messages fails.
 */
export const marketplaceMessageThreadsFallback: MarketplaceMessageThread[] = [];
