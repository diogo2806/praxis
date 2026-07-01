import { Link } from "@tanstack/react-router";
import { ShieldCheck } from "lucide-react";

import type { MarketplaceListingSummary } from "@/lib/api/praxis";
import { categoryLabel, formatMarketplacePrice } from "@/lib/marketplace";
import { cn } from "@/lib/utils";

import { StarRating } from "./star-rating";

export function ListingCard({ listing, className }: { listing: MarketplaceListingSummary; className?: string }) {
  return (
    <Link
      to="/marketplace/$listingId"
      params={{ listingId: String(listing.id) }}
      className={cn("rounded-md border border-border bg-card p-4 transition hover:border-primary/50 hover:shadow-sm", className)}
    >
      <div className="flex items-start justify-between gap-3">
        <span className="rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
          {categoryLabel(listing.category)}
        </span>
        <span className="text-sm font-semibold">{formatMarketplacePrice(listing.priceCents)}</span>
      </div>
      <h2 className="mt-3 line-clamp-2 text-lg font-semibold">{listing.title}</h2>
      <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-1">
          <ShieldCheck className="h-3.5 w-3.5" />
          {listing.professional.displayName}
        </span>
        <span className="inline-flex items-center gap-1">
          <StarRating value={Math.round(Number(listing.averageRating ?? 0))} readOnly />
          {listing.averageRating ? Number(listing.averageRating).toFixed(1) : "Sem notas"} - {listing.totalReviews}
        </span>
      </div>
    </Link>
  );
}
