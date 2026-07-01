import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, RotateCcw, Store } from "lucide-react";
import { useState } from "react";

import { AdminShell } from "@/components/admin-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { listAdminMarketplaceDisputes, refundAdminMarketplaceOrder } from "@/lib/api/praxis";
import { formatMarketplaceDate, formatMarketplacePrice, orderStatusLabels } from "@/lib/marketplace";

export const Route = createFileRoute("/admin/marketplace/disputes")({
  head: () => ({
    meta: [{ title: "Disputas do marketplace - Admin Praxis" }],
  }),
  component: AdminMarketplaceDisputesPage,
});

function AdminMarketplaceDisputesPage() {
  const queryClient = useQueryClient();
  const [reasonByOrder, setReasonByOrder] = useState<Record<number, string>>({});
  const disputes = useQuery({
    queryKey: ["admin-marketplace-disputes"],
    queryFn: listAdminMarketplaceDisputes,
  });
  const refund = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => refundAdminMarketplaceOrder(id, reason),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-marketplace-disputes"] });
      await queryClient.invalidateQueries({ queryKey: ["admin-marketplace-dashboard"] });
    },
  });

  return (
    <AdminShell>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-primary">
            <Store className="h-4 w-4" />
            Marketplace
          </div>
          <h1 className="mt-1 text-2xl font-semibold">Disputas e reembolsos</h1>
          <p className="mt-1 text-sm text-slate-500">Revise pedidos contestados antes de reembolsar.</p>
        </div>
        <Button asChild variant="outline">
          <Link to="/admin/marketplace/listings">Anuncios</Link>
        </Button>
      </div>

      {disputes.isError && (
        <div className="mb-4">
          <StateBanner tone="danger" title="Nao foi possivel carregar disputas">
            Verifique sua sessao de administrador e tente novamente.
          </StateBanner>
        </div>
      )}

      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="font-semibold">Pedidos em disputa</h2>
        {disputes.isLoading && (
          <div className="mt-3 flex items-center gap-2 text-sm text-slate-500">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando
          </div>
        )}
        <div className="mt-3 divide-y divide-slate-100">
          {(disputes.data ?? []).map((order) => (
            <div key={order.id} className="grid gap-3 py-4 lg:grid-cols-[1fr_280px_auto] lg:items-center">
              <div>
                <div className="font-medium">{order.listingTitle}</div>
                <div className="mt-1 text-xs text-slate-500">
                  Pedido #{order.id} · {orderStatusLabels[order.status]} · {formatMarketplacePrice(order.priceCents)} ·{" "}
                  {formatMarketplaceDate(order.paidAt)}
                </div>
              </div>
              <Input
                value={reasonByOrder[order.id] ?? ""}
                onChange={(event) => setReasonByOrder({ ...reasonByOrder, [order.id]: event.target.value })}
                placeholder="Motivo do reembolso"
              />
              <Button
                size="sm"
                disabled={refund.isPending}
                onClick={() => refund.mutate({ id: order.id, reason: reasonByOrder[order.id] })}
              >
                {refund.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <RotateCcw className="h-4 w-4" />}
                Reembolsar
              </Button>
            </div>
          ))}
          {disputes.data?.length === 0 && <p className="py-3 text-sm text-slate-500">Sem disputas abertas.</p>}
        </div>
      </section>
    </AdminShell>
  );
}
