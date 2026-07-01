import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Loader2, ShieldAlert, Store, XCircle } from "lucide-react";

import { AdminShell } from "@/components/admin-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getAdminMarketplaceDashboard,
  listAdminPendingMarketplaceListings,
  listAdminPendingMarketplaceProfessionals,
  moderateMarketplaceListing,
  moderateMarketplaceProfessional,
} from "@/lib/api/praxis";
import { professionalStatusLabels } from "@/lib/marketplace";

export const Route = createFileRoute("/admin/marketplace/professionals")({
  head: () => ({
    meta: [{ title: "Marketplace - Admin Práxis" }],
  }),
  component: AdminMarketplacePage,
});

function AdminMarketplacePage() {
  const queryClient = useQueryClient();
  const dashboard = useQuery({
    queryKey: ["admin-marketplace-dashboard"],
    queryFn: getAdminMarketplaceDashboard,
  });
  const professionals = useQuery({
    queryKey: ["admin-marketplace-professionals-pending"],
    queryFn: listAdminPendingMarketplaceProfessionals,
  });
  const listings = useQuery({
    queryKey: ["admin-marketplace-listings-pending"],
    queryFn: listAdminPendingMarketplaceListings,
  });

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["admin-marketplace-dashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["admin-marketplace-professionals-pending"] }),
      queryClient.invalidateQueries({ queryKey: ["admin-marketplace-listings-pending"] }),
    ]);
  };

  const professionalAction = useMutation({
    mutationFn: ({ id, action }: { id: number; action: "approve" | "reject" | "suspend" }) =>
      moderateMarketplaceProfessional(id, action),
    onSuccess: refresh,
  });
  const listingAction = useMutation({
    mutationFn: ({ id, action }: { id: number; action: "approve" | "reject" | "suspend" }) =>
      moderateMarketplaceListing(id, action),
    onSuccess: refresh,
  });

  return (
    <AdminShell>
      <div className="mb-6">
        <div className="flex items-center gap-2 text-xs uppercase text-primary">
          <Store className="h-4 w-4" />
          Marketplace
        </div>
        <h1 className="mt-1 text-2xl font-semibold">Moderação de psicometria</h1>
        <p className="mt-1 text-sm text-slate-500">
          Profissionais e anúncios precisam de aprovação manual antes de aparecerem na vitrine.
        </p>
      </div>

      {(dashboard.isError || professionals.isError || listings.isError) && (
        <div className="mb-4">
          <StateBanner tone="danger" title="Não foi possível carregar a moderação">
            Verifique sua sessão de administrador e tente novamente.
          </StateBanner>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-5">
        <AdminMetric label="Profissionais pendentes" value={dashboard.data?.pendingProfessionals} />
        <AdminMetric label="Profissionais verificados" value={dashboard.data?.verifiedProfessionals} />
        <AdminMetric label="Anúncios pendentes" value={dashboard.data?.pendingListings} />
        <AdminMetric label="Anúncios publicados" value={dashboard.data?.approvedListings} />
        <AdminMetric label="Pedidos pagos" value={dashboard.data?.paidOrders} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold">Profissionais pendentes</h2>
          {professionals.isLoading && <LoadingLine />}
          <div className="mt-3 divide-y divide-slate-100">
            {(professionals.data ?? []).map((professional) => (
              <div key={professional.id} className="py-3">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="font-medium">{professional.displayName}</div>
                    <div className="mt-1 text-xs text-slate-500">
                      {professionalStatusLabels[professional.verificationStatus]} ·{" "}
                      {professional.specialties.join(", ") || "Sem especialidades"}
                    </div>
                  </div>
                <ModerationButtons
                  pending={professionalAction.isPending}
                  onApprove={() => professionalAction.mutate({ id: professional.id, action: "approve" })}
                  onReject={() => professionalAction.mutate({ id: professional.id, action: "reject" })}
                  onSuspend={() => professionalAction.mutate({ id: professional.id, action: "suspend" })}
                />
                </div>
                {professional.bio && <p className="mt-2 line-clamp-3 text-sm text-slate-600">{professional.bio}</p>}
              </div>
            ))}
            {professionals.data?.length === 0 && <p className="py-3 text-sm text-slate-500">Sem profissionais pendentes.</p>}
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold">Anúncios pendentes</h2>
          {listings.isLoading && <LoadingLine />}
          <div className="mt-3 divide-y divide-slate-100">
            {(listings.data ?? []).map((listing) => (
              <div key={listing.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <div className="font-medium">Anúncio #{listing.id}</div>
                  <div className="mt-1 text-xs text-slate-500">{listing.status}</div>
                </div>
                <ModerationButtons
                  pending={listingAction.isPending}
                  onApprove={() => listingAction.mutate({ id: listing.id, action: "approve" })}
                  onReject={() => listingAction.mutate({ id: listing.id, action: "reject" })}
                  onSuspend={() => listingAction.mutate({ id: listing.id, action: "suspend" })}
                />
              </div>
            ))}
            {listings.data?.length === 0 && <p className="py-3 text-sm text-slate-500">Sem anúncios pendentes.</p>}
          </div>
        </section>
      </div>
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

function LoadingLine() {
  return (
    <div className="mt-3 flex items-center gap-2 text-sm text-slate-500">
      <Loader2 className="h-4 w-4 animate-spin" />
      Carregando
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
