import type { ListingStatus } from "@/lib/api/praxis";
import { listingStatusLabels } from "@/lib/marketplace";
import { cn } from "@/lib/utils";

const statusClass: Record<ListingStatus, string> = {
  DRAFT: "border-slate-200 bg-slate-50 text-slate-700",
  PENDING_REVIEW: "border-amber-200 bg-amber-50 text-amber-800",
  APPROVED: "border-emerald-200 bg-emerald-50 text-emerald-800",
  REJECTED: "border-red-200 bg-red-50 text-red-700",
  SUSPENDED: "border-zinc-300 bg-zinc-100 text-zinc-700",
};

export function ListingStatusBadge({ status, className }: { status: ListingStatus; className?: string }) {
  return (
    <span className={cn("inline-flex rounded-md border px-2 py-1 text-xs font-medium", statusClass[status], className)}>
      {listingStatusLabels[status]}
    </span>
  );
}
