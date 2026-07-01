import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Edit3, Loader2, MessageSquare, Plus, Save, ShieldCheck, Star } from "lucide-react";
import { useEffect, useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  getMarketplaceProfessionalDashboard,
  getMarketplaceProfessionalMe,
  listMarketplaceMessageThreads,
  updateMarketplaceProfessionalMe,
} from "@/lib/api/praxis";
import {
  formatMarketplaceDate,
  formatMarketplacePrice,
  listingStatusLabels,
  professionalStatusLabels,
  splitList,
} from "@/lib/marketplace";

export const Route = createFileRoute("/profissional")({
  head: () => ({
    meta: [{ title: "Área do profissional - Marketplace Práxis" }],
  }),
  component: ProfessionalHomePage,
});

function ProfessionalHomePage() {
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
  const threads = useQuery({
    queryKey: ["marketplace-message-threads", "professional"],
    queryFn: () => listMarketplaceMessageThreads("professional"),
    retry: false,
  });
  const [form, setForm] = useState({ displayName: "", bio: "", specialties: "", linkedinUrl: "", pixKey: "" });

  const update = useMutation({
    mutationFn: () =>
      updateMarketplaceProfessionalMe({
        displayName: form.displayName || undefined,
        bio: form.bio,
        specialties: splitList(form.specialties),
        linkedinUrl: form.linkedinUrl,
        pixKey: form.pixKey,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["marketplace-professional-me"] });
      await queryClient.invalidateQueries({ queryKey: ["marketplace-professional-dashboard"] });
    },
  });

  const loadForm = () => {
    if (!profile.data) return;
    setForm({
      displayName: profile.data.displayName,
      bio: profile.data.bio ?? "",
      specialties: profile.data.specialties.join(", "),
      linkedinUrl: profile.data.linkedinUrl ?? "",
      pixKey: "",
    });
  };

  useEffect(() => {
    loadForm();
  }, [profile.data]);

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-6xl px-5 py-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-xs uppercase text-primary">Marketplace</div>
            <h1 className="mt-1 text-3xl font-semibold">Área do profissional</h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline">
              <Link to="/profissional/financeiro">Financeiro</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/profissional/listings">Meus testes</Link>
            </Button>
            <Button asChild>
              <Link to="/profissional/listings/novo">
                <Plus className="h-4 w-4" />
                Novo anúncio
              </Link>
            </Button>
          </div>
        </div>

        {profile.isLoading && (
          <div className="flex items-center gap-2 rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Carregando perfil
          </div>
        )}
        {profile.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar seu perfil profissional">
            {profile.error instanceof Error ? profile.error.message : "Faça login com uma conta profissional."}
          </StateBanner>
        )}
        {update.isError && (
          <div className="mb-4">
            <StateBanner tone="danger" title="Não foi possível salvar o perfil">
              {update.error instanceof Error ? update.error.message : "Tente novamente."}
            </StateBanner>
          </div>
        )}
        {profile.data && (
          <div className="grid gap-5 lg:grid-cols-[360px_1fr]">
            <aside className="space-y-4">
              <section className="rounded-md border border-border bg-card p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h2 className="text-xl font-semibold">{profile.data.displayName}</h2>
                    <div className="mt-2 inline-flex items-center gap-1 rounded-md bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
                      <ShieldCheck className="h-3.5 w-3.5" />
                      {professionalStatusLabels[profile.data.verificationStatus]}
                    </div>
                  </div>
                  <Button variant="outline" size="icon" onClick={loadForm} aria-label="Editar perfil">
                    <Edit3 className="h-4 w-4" />
                  </Button>
                </div>
                <div className="mt-4 grid grid-cols-3 gap-2 text-center">
                  <Metric
                    label="Receita"
                    value={dashboard.data ? formatMarketplacePrice(dashboard.data.totalRevenueCents) : "-"}
                  />
                  <Metric
                    label="Escrow"
                    value={dashboard.data ? formatMarketplacePrice(dashboard.data.pendingEscrowCents) : "-"}
                  />
                  <Metric label="Vendas" value={String(dashboard.data?.salesCount ?? profile.data.totalSales)} />
                </div>
              </section>
              <section className="rounded-md border border-border bg-card p-5">
                <h2 className="flex items-center gap-2 text-sm font-semibold">
                  <MessageSquare className="h-4 w-4" />
                  Conversas
                </h2>
                <div className="mt-3 space-y-2">
                  {(threads.data ?? []).slice(0, 4).map((thread) => (
                    <div key={thread.id} className="rounded-md border border-border bg-background p-3 text-sm">
                      <div className="font-medium">Anúncio #{thread.listingId}</div>
                      <div className="text-xs text-muted-foreground">{thread.messages.at(-1)?.body ?? "Sem mensagens"}</div>
                    </div>
                  ))}
                  {threads.data?.length === 0 && <p className="text-sm text-muted-foreground">Nenhuma conversa aberta.</p>}
                </div>
              </section>
            </aside>

            <section className="rounded-md border border-border bg-card p-5">
              <h2 className="text-lg font-semibold">Perfil público</h2>
              <form
                className="mt-4 grid gap-4"
                onSubmit={(event) => {
                  event.preventDefault();
                  update.mutate();
                }}
              >
                <Field label="Nome público" value={form.displayName} onChange={(value) => setForm({ ...form, displayName: value })} />
                <Field label="Especialidades" value={form.specialties} onChange={(value) => setForm({ ...form, specialties: value })} />
                <Field label="LinkedIn" value={form.linkedinUrl} onChange={(value) => setForm({ ...form, linkedinUrl: value })} />
                <Field label="Chave Pix" value={form.pixKey} onChange={(value) => setForm({ ...form, pixKey: value })} />
                <label className="grid gap-1 text-sm">
                  <span className="font-medium">Bio</span>
                  <textarea
                    value={form.bio}
                    onChange={(event) => setForm({ ...form, bio: event.target.value })}
                    rows={7}
                    className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                  />
                </label>
                <div className="flex justify-end">
                  <Button type="submit" disabled={update.isPending}>
                    {update.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                    Salvar perfil
                  </Button>
                </div>
              </form>
            </section>

            <section className="rounded-md border border-border bg-card p-5 lg:col-start-2">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 className="text-lg font-semibold">Meus testes</h2>
                <Button asChild size="sm" variant="outline">
                  <Link to="/profissional/listings">
                    Ver todos
                  </Link>
                </Button>
              </div>
              {dashboard.isLoading && (
                <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Carregando listagens
                </div>
              )}
              {dashboard.data?.listings.length === 0 && (
                <p className="mt-3 text-sm text-muted-foreground">Nenhum teste publicado ainda.</p>
              )}
              {(dashboard.data?.listings ?? []).length > 0 && (
                <div className="mt-3 divide-y divide-border">
                  {dashboard.data!.listings.map((listing) => (
                    <div key={listing.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                      <div>
                        <div className="font-medium">{listing.title}</div>
                        <div className="mt-1 text-xs text-muted-foreground">
                          {listingStatusLabels[listing.status]}
                        </div>
                      </div>
                      <div className="text-sm font-semibold">{listing.salesCount} venda(s)</div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="rounded-md border border-border bg-card p-5 lg:col-start-2">
              <h2 className="text-lg font-semibold">Avaliações recentes</h2>
              {dashboard.data?.recentReviews.length === 0 && (
                <p className="mt-3 text-sm text-muted-foreground">Nenhuma avaliação recebida ainda.</p>
              )}
              {(dashboard.data?.recentReviews ?? []).length > 0 && (
                <div className="mt-3 divide-y divide-border">
                  {dashboard.data!.recentReviews.map((review) => (
                    <div key={review.id} className="py-3">
                      <div className="flex flex-wrap items-center gap-2 text-sm">
                        <span className="inline-flex items-center gap-1 font-semibold">
                          <Star className="h-4 w-4 text-warning" />
                          {review.rating}/5
                        </span>
                        <span className="text-muted-foreground">{formatMarketplaceDate(review.createdAt)}</span>
                      </div>
                      {review.comment && <p className="mt-1 text-sm text-muted-foreground">{review.comment}</p>}
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-background p-2">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 flex items-center justify-center gap-1 font-semibold">
        {value}
      </div>
    </div>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="grid gap-1 text-sm">
      <span className="font-medium">{label}</span>
      <Input value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}
