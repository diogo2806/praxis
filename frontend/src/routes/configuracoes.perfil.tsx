import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Building2 } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { SkeletonRows, StateBanner } from "@/components/praxis-ui";
import { getCompanyProfile } from "@/lib/api/praxis";

export const Route = createFileRoute("/configuracoes/perfil")({
  head: () => ({
    meta: [
      { title: "Perfil da empresa - Praxis" },
      {
        name: "description",
        content: "Dados cadastrais da empresa no Praxis.",
      },
    ],
  }),
  component: CompanyProfilePage,
});

function CompanyProfilePage() {
  const profileQuery = useQuery({
    queryKey: ["company-profile"],
    queryFn: getCompanyProfile,
  });

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <div className="text-xs uppercase text-primary">Configurações</div>
          <h1 className="mt-1 text-3xl font-semibold">Perfil da empresa</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Dados cadastrais da empresa exibidos para consulta. Para alterar dados cadastrais da
            empresa, fale com o administrador da plataforma.
          </p>
        </div>

        <section className="rounded-md border border-border bg-card">
          <div className="border-b border-border px-5 py-4">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <Building2 className="h-4 w-4 text-primary" />
              Cadastro empresarial
            </div>
          </div>
          <div className="p-5">
            {profileQuery.isLoading && <SkeletonRows rows={4} />}
            {profileQuery.isError && (
              <StateBanner tone="danger" title="Não foi possível carregar o perfil">
                {profileQuery.error instanceof Error ? profileQuery.error.message : "Tente novamente."}
              </StateBanner>
            )}
            {profileQuery.data && (
              <div className="grid gap-4 md:grid-cols-2">
                <ReadOnlyField label="Nome fantasia" value={profileQuery.data.tradeName} />
                <ReadOnlyField label="Razão social" value={profileQuery.data.legalName} />
                <ReadOnlyField label="CNPJ" value={profileQuery.data.taxId} />
                <ReadOnlyField label="E-mail corporativo" value={profileQuery.data.corporateEmail} />
                <ReadOnlyField label="Telefone" value={profileQuery.data.phone} />
                <ReadOnlyField label="Site" value={profileQuery.data.website} />
              </div>
            )}
          </div>
        </section>
      </div>
    </AppShell>
  );
}

function ReadOnlyField({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="rounded-md border border-border bg-background p-4">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 min-h-5 text-sm font-medium text-foreground">{value?.trim() || "-"}</div>
    </div>
  );
}
