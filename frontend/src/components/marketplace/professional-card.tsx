import { Link } from "@tanstack/react-router";
import { ShieldCheck } from "lucide-react";

import type { MarketplaceProfessionalSummary } from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

import { StarRating } from "./star-rating";

export function ProfessionalCard({
  professional,
  averageRating,
  totalReviews,
  className,
}: {
  professional: MarketplaceProfessionalSummary;
  averageRating?: number | null;
  totalReviews?: number;
  className?: string;
}) {
  return (
    <Link
      to="/marketplace/professionals/$professionalId"
      params={{ professionalId: String(professional.id) }}
      className={cn("block rounded-md border border-border bg-card p-4 transition hover:border-primary/50", className)}
    >
      <div className="flex items-center justify-between gap-3">
        <div>
          <div className="font-semibold">{professional.displayName}</div>
          {professional.verified && (
            <div className="mt-1 inline-flex items-center gap-1 text-xs text-primary">
              <ShieldCheck className="h-3.5 w-3.5" />
              Verificado
            </div>
          )}
        </div>
      </div>
      <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
        <StarRating value={Math.round(Number(averageRating ?? 0))} readOnly />
        <span>{averageRating ? Number(averageRating).toFixed(1) : "Sem notas"}</span>
        {totalReviews != null && <span>· {totalReviews}</span>}
      </div>
    </Link>
  );
}
