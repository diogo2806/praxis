import { useEffect } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";

export const Route = createFileRoute("/nova/cenario")({
  validateSearch: parseWizardSearch,
  head: () => ({
    meta: [
      { title: "Cenário - Práxis" },
      { name: "description", content: "Personagem, diálogo e critérios de pontuação do cenário." },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const navigate = useNavigate();

  useEffect(() => {
    void navigate({ to: "/nova/personagem", search, replace: true });
  }, [navigate, search]);

  return (
    <AppShell>
      <StateBanner tone="info" title="Abrindo cenário">
        Redirecionando para a configuração do personagem.
      </StateBanner>
      <div className="mt-4">
        <Link
          to="/nova/personagem"
          search={search}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
        >
          Abrir cenário
        </Link>
      </div>
    </AppShell>
  );
}

function parseWizardSearch(search: Record<string, unknown>) {
  return {
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  };
}
