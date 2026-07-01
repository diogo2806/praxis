import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Loader2, ShieldAlert, Store, XCircle } from "lucide-react";

import { AdminShell } from "@/components/admin-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getAdminMarketplaceDashboard,
  listAdminPendingMarketplaceListings,
  moderateMarketplaceListing,
} from "@/lib/api/praxis";
import { listingStatusLabels } from "@/lib/marketplace";

export const Route = createFileRoute("/admin/marketplace/listings")({
  head: () => ({
    meta: [{ title: "Anuncios do marketplace - Admin Praxis" }],
  }),
  component: AdminMarketplaceListingsPage,
});

function AdminMarketplaceListingsPage() {
  const queryClient = useQueryClient();
  const dashboard = useQuery({
    queryKey: ["admin-marketplace-dashboard"],
    queryFn: getAdminMarketplaceDashboard,
  });
  const listings = useQuery({
    queryKey: ["admin-marketplace-listings-pending"],
    queryFn: listAdminPendingMarketplaceListings,
  });

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["admin-marketplace-dashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["admin-marketplace-listings-pending"] }),
    ]);
  };

  const listingAction = useMutation({
    mutationFn: ({ id, action }: { id: number; action: "approve" | "reject" | "suspend" }) =>
      moderateMarketplaceListing(id, action),
    onSuccess: refresh,
  });

  return (
    <AdminShell>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-primary">
            <Store className="h-4 w-4" />
            Marketplace
          </div>
          <h1 className="mt-1 text-2xl font-semibold">Anuncios pendentes</h1>
          <p className="mt-1 text-sm text-slate-500">Modere avaliações antes da publicação na vitrine.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button asChild variant="outline">
            <Link to="/admin/marketplace/professionals">Profissionais</Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/admin/marketplace/disputes">Disputas</Link>
          </Button>
        </div>
      </div>

      {(dashboard.isError || listings.isError) && (
        <div className="mb-4">
          <StateBanner tone="danger" title="Nao foi possivel carregar a fila de anuncios">
            Verifique sua sessao de administrador e tente novamente.
          </StateBanner>
        </div>
      )}

      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <AdminMetric label="Anuncios pendentes" value={dashboard.data?.pendingListings} />
        <AdminMetric label="Anuncios publicados" value={dashboard.data?.approvedListings} />
        <AdminMetric label="Pedidos pagos" value={dashboard.data?.paidOrders} />
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="font-semibold">Fila de moderacao</h2>
        {listings.isLoading && (
          <div className="mt-3 flex items-center gap-2 text-sm text-slate-500">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando
          </div>
        )}
        <div className="mt-3 divide-y divide-slate-100">
          {(listings.data ?? []).map((listing) => (
            <div key={listing.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
              <div>
                <div className="font-medium">Anuncio #{listing.id}</div>
                <div className="mt-1 text-xs text-slate-500">{listingStatusLabels[listing.status]}</div>
              </div>
              <ModerationButtons
                pending={listingAction.isPending}
                onApprove={() => listingAction.mutate({ id: listing.id, action: "approve" })}
                onReject={() => listingAction.mutate({ id: listing.id, action: "reject" })}
                onSuspend={() => listingAction.mutate({ id: listing.id, action: "suspend" })}
              />
            </div>
          ))}
          {listings.data?.length === 0 && <p className="py-3 text-sm text-slate-500">Sem anuncios pendentes.</p>}
        </div>
      </section>
    </AdminShell>
  );
}

function AdminMetric({ label, value }: { label: string; value?: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="text-xs text-slate-500">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-slate-900">{value ?? "-"}</div>
    </div>
  );
}

function ModerationButtons({
  pending,
  onApprove,
  onReject,
  onSuspend,
}: {
  pending: boolean;
  onApprove: () => void;
  onReject: () => void;
  onSuspend: () => void;
}) {
  return (
    <div className="flex gap-2">
      <Button size="sm" disabled={pending} onClick={onApprove}>
        {pending ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
        Aprovar
      </Button>
      <Button size="sm" variant="outline" disabled={pending} onClick={onReject}>
        <XCircle className="h-4 w-4" />
        Reprovar
      </Button>
      <Button size="icon" variant="outline" disabled={pending} aria-label="Suspender" onClick={onSuspend}>
        <ShieldAlert className="h-4 w-4" />
      </Button>
    </div>
  );
}
