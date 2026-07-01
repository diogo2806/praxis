import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ExternalLink, Loader2, PlugZap, Trash2 } from "lucide-react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getMarketplaceProfessionalDashboard,
  disconnectMarketplaceMercadoPago,
  getMarketplaceProfessionalMe,
} from "@/lib/api/praxis";
import { formatMarketplaceDate, formatMarketplacePrice, payoutStatusLabels } from "@/lib/marketplace";
import { getApiBaseUrl } from "@/lib/runtime-config";

export const Route = createFileRoute("/profissional/financeiro")({
  head: () => ({
    meta: [{ title: "Financeiro profissional - Marketplace Práxis" }],
  }),
  component: ProfessionalFinancePage,
});

function ProfessionalFinancePage() {
  const queryClient = useQueryClient();
  const profile = useQuery({
    queryKey: ["marketplace-professional-me"],
    queryFn: getMarketplaceProfessionalMe,
  });
  const dashboard = useQuery({
    queryKey: ["marketplace-professional-dashboard"],
    queryFn: getMarketplaceProfessionalDashboard,
    retry: false,
  });
  const disconnect = useMutation({
    mutationFn: disconnectMarketplaceMercadoPago,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["marketplace-professional-me"] });
    },
  });

  const connectUrl = `${getApiBaseUrl()}/api/v1/marketplace/professionals/me/mercadopago/connect`;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/profissional">
            <ArrowLeft className="h-4 w-4" />
            Área do profissional
          </Link>
        </Button>

        <section className="rounded-md border border-border bg-card p-5">
          <div className="flex items-center gap-2 text-xs uppercase text-primary">
            <PlugZap className="h-4 w-4" />
            Financeiro
          </div>
          <h1 className="mt-2 text-2xl font-semibold">Mercado Pago Connect</h1>

          {profile.isLoading && (
            <div className="mt-5 flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              Carregando conexão
            </div>
          )}
          {profile.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível carregar a conexão">
                {profile.error instanceof Error ? profile.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          {disconnect.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível desconectar">
                {disconnect.error instanceof Error ? disconnect.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          {profile.data && (
            <div className="mt-5 rounded-md border border-border bg-background p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="text-sm font-semibold">
                    {profile.data.mercadoPagoConnected ? "Conta conectada" : "Conta não conectada"}
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">
                    A conexão permite split de pagamento e repasse ao profissional.
                  </p>
                </div>
                {profile.data.mercadoPagoConnected ? (
                  <Button
                    variant="outline"
                    disabled={disconnect.isPending}
                    onClick={() => disconnect.mutate()}
                  >
                    {disconnect.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                    Desconectar
                  </Button>
                ) : (
                  <Button asChild>
                    <a href={connectUrl}>
                      <ExternalLink className="h-4 w-4" />
                      Conectar Mercado Pago
                    </a>
                  </Button>
                )}
              </div>
            </div>
          )}

          {dashboard.data && (
            <section className="mt-5 rounded-md border border-border bg-background p-4">
              <h2 className="text-sm font-semibold">Extrato de repasses</h2>
              <div className="mt-3 grid gap-3 sm:grid-cols-3">
                <Metric label="Receita total" value={formatMarketplacePrice(dashboard.data.totalRevenueCents)} />
                <Metric label="Em escrow" value={formatMarketplacePrice(dashboard.data.pendingEscrowCents)} />
                <Metric label="Liberado" value={formatMarketplacePrice(dashboard.data.releasedCents)} />
              </div>
              {dashboard.data.payouts.length === 0 ? (
                <p className="mt-4 text-sm text-muted-foreground">Nenhum repasse registrado ainda.</p>
              ) : (
                <div className="mt-4 overflow-x-auto">
                  <table className="w-full min-w-[620px] text-left text-sm">
                    <thead className="border-b border-border text-xs uppercase text-muted-foreground">
                      <tr>
                        <th className="py-2 pr-3 font-medium">Pedido</th>
                        <th className="py-2 pr-3 font-medium">Teste</th>
                        <th className="py-2 pr-3 font-medium">Valor</th>
                        <th className="py-2 pr-3 font-medium">Status</th>
                        <th className="py-2 font-medium">Data</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {dashboard.data.payouts.map((payout) => (
                        <tr key={payout.id}>
                          <td className="py-2 pr-3">#{payout.orderId}</td>
                          <td className="py-2 pr-3">{payout.listingTitle ?? "Listing removido"}</td>
                          <td className="py-2 pr-3 font-medium">{formatMarketplacePrice(payout.amountCents)}</td>
                          <td className="py-2 pr-3">{payoutStatusLabels[payout.status]}</td>
                          <td className="py-2">{formatMarketplaceDate(payout.releasedAt ?? payout.escrowReleaseAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}
        </section>
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-card p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 text-lg font-semibold">{value}</div>
    </div>
  );
}
