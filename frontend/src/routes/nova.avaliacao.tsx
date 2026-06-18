import { useEffect } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";

export const Route = createFileRoute("/nova/avaliacao")({
  validateSearch: parseWizardSearch,
  head: () => ({
    meta: [
      { title: "Avaliação - Práxis" },
      { name: "description", content: "Modelo base e objetivo da avaliação." },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const navigate = useNavigate();

  useEffect(() => {
    void navigate({ to: "/nova/blueprint", search, replace: true });
  }, [navigate, search]);

  return (
    <AppShell>
      <StateBanner tone="info" title="Abrindo avaliação">
        Redirecionando para o modelo base da avaliação.
      </StateBanner>
      <div className="mt-4">
        <Link
          to="/nova/blueprint"
          search={search}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
        >
          Abrir avaliação
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
