import type { MarketplacePayoutSummary } from "@/lib/api/marketplace";
import { formatMarketplaceDate, formatMarketplacePrice, payoutStatusLabels } from "@/lib/marketplace";

export function PayoutTable({ payouts }: { payouts: MarketplacePayoutSummary[] }) {
  if (payouts.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum repasse registrado.</p>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[680px] text-sm">
        <thead>
          <tr className="border-b border-border text-left text-xs uppercase text-muted-foreground">
            <th className="py-2 pr-3">Pedido</th>
            <th className="py-2 pr-3">Avaliação</th>
            <th className="py-2 pr-3">Valor</th>
            <th className="py-2 pr-3">Status</th>
            <th className="py-2">Liberacao</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {payouts.map((payout) => (
            <tr key={payout.id}>
              <td className="py-3 pr-3">#{payout.orderId}</td>
              <td className="py-3 pr-3">{payout.listingTitle ?? "-"}</td>
              <td className="py-3 pr-3 font-medium">{formatMarketplacePrice(payout.amountCents)}</td>
              <td className="py-3 pr-3">{payoutStatusLabels[payout.status]}</td>
              <td className="py-3">{formatMarketplaceDate(payout.releasedAt ?? payout.escrowReleaseAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
