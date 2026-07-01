import type { MarketplaceReviewResponse } from "@/lib/api/marketplace";
import { formatMarketplaceDate } from "@/lib/marketplace";

import { StarRating } from "./star-rating";

export function ReviewList({ reviews }: { reviews: MarketplaceReviewResponse[] }) {
  if (reviews.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma avaliacao publicada ainda.</p>;
  }

  return (
    <div className="divide-y divide-border">
      {reviews.map((review) => (
        <article key={review.id} className="py-3">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <StarRating value={review.rating} readOnly />
            <span className="font-semibold">{review.rating}/5</span>
            <span className="text-muted-foreground">{formatMarketplaceDate(review.createdAt)}</span>
          </div>
          {review.comment && <p className="mt-1 text-sm text-muted-foreground">{review.comment}</p>}
        </article>
      ))}
    </div>
  );
}
