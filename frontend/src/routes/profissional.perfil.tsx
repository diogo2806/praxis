import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Loader2, Save } from "lucide-react";
import { useEffect, useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getMarketplaceProfessionalMe, updateMarketplaceProfessionalMe } from "@/lib/api/praxis";
import { professionalStatusLabels, splitList } from "@/lib/marketplace";

export const Route = createFileRoute("/profissional/perfil")({
  head: () => ({
    meta: [{ title: "Perfil profissional - Marketplace Praxis" }],
  }),
  component: ProfessionalProfilePage,
});

function ProfessionalProfilePage() {
  const queryClient = useQueryClient();
  const profile = useQuery({
    queryKey: ["marketplace-professional-me"],
    queryFn: getMarketplaceProfessionalMe,
  });
  const [form, setForm] = useState({ displayName: "", bio: "", specialties: "", linkedinUrl: "", pixKey: "" });

  useEffect(() => {
    if (!profile.data) return;
    setForm({
      displayName: profile.data.displayName,
      bio: profile.data.bio ?? "",
      specialties: profile.data.specialties.join(", "),
      linkedinUrl: profile.data.linkedinUrl ?? "",
      pixKey: "",
    });
  }, [profile.data]);

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

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-4xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/profissional">
            <ArrowLeft className="h-4 w-4" />
            Area do profissional
          </Link>
        </Button>

        <section className="rounded-md border border-border bg-card p-5">
          <div className="text-xs uppercase text-primary">Perfil publico</div>
          <h1 className="mt-1 text-2xl font-semibold">Dados profissionais</h1>
          {profile.data && (
            <div className="mt-2 text-sm text-muted-foreground">
              {professionalStatusLabels[profile.data.verificationStatus]}
            </div>
          )}

          {profile.isLoading && (
            <div className="mt-5 flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              Carregando perfil
            </div>
          )}
          {profile.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Nao foi possivel carregar seu perfil">
                {profile.error instanceof Error ? profile.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          {update.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Nao foi possivel salvar o perfil">
                {update.error instanceof Error ? update.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          {update.isSuccess && (
            <div className="mt-5">
              <StateBanner tone="ok" title="Perfil salvo" />
            </div>
          )}

          {profile.data && (
            <form
              className="mt-5 grid gap-4"
              onSubmit={(event) => {
                event.preventDefault();
                update.mutate();
              }}
            >
              <Field label="Nome publico" value={form.displayName} onChange={(value) => setForm({ ...form, displayName: value })} />
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
          )}
        </section>
      </div>
    </main>
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
