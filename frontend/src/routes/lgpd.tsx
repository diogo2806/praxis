import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { FileSearch, ShieldCheck } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  getSimulationVersion,
  listSimulationVersionAuditEvents,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/lgpd")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string"
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "LGPD & Explicabilidade - Praxis" },
      { name: "description", content: "Explicacao de score e revisao humana." },
    ],
  }),
  component: LgpdPage,
});

function LgpdPage() {
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasContext,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const auditQuery = useQuery({
    queryKey: ["simulation-audit", search.simulationId, search.versionNumber],
    queryFn: () => listSimulationVersionAuditEvents(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const version = versionQuery.data;
  const optionsCount =
    version?.nodes.reduce((total, node) => total + node.options.length, 0) ?? 0;
  const criticalOptionsCount =
    version?.nodes.reduce(
      (total, node) => total + node.options.filter((option) => option.isCritical).length,
      0,
    ) ?? 0;

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="canal de revisao humana nao configurado" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Conformidade</div>
          <h1 className="mt-1 text-3xl font-semibold">LGPD e explicabilidade</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            O candidato pode pedir revisao. A explicacao usa rubrica, escolha e caminho, sem
            caixa-preta.
          </p>
        </div>
        {version && <StatusBadge status={version.status} />}
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        {[
          "Pontuacao normalizada por caminho",
          "Erro critico gera revisao humana",
          "Sem IA julgando candidato",
        ].map((item) => (
          <div key={item} className="rounded-md border border-border bg-card p-5">
            <ShieldCheck className="h-5 w-5 text-success" />
            <div className="mt-3 text-sm font-semibold">{item}</div>
            <p className="mt-1 text-sm text-muted-foreground">
              Evidencia rastreavel por versao, tentativa, turno e alternativa escolhida.
            </p>
          </div>
        ))}
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <FileSearch className="h-4 w-4" />
            Explicacao baseada em contrato real
          </div>
          {!hasContext ? (
            <EmptyState
              title="Selecione uma versao para explicar"
              description="A tela usa blueprint, opcoes e eventos reais quando a URL tem simulationId e versionNumber."
              actions={<SimulationLinks loading={simulationsQuery.isLoading} simulations={simulationsQuery.data ?? []} />}
            />
          ) : versionQuery.isLoading || auditQuery.isLoading ? (
            <StateBanner tone="info" title="Carregando explicabilidade">
              Buscando versao {search.simulationId} v{search.versionNumber}.
            </StateBanner>
          ) : versionQuery.isError || auditQuery.isError ? (
            <StateBanner tone="danger" title="Nao foi possivel carregar dados LGPD">
              {versionQuery.error instanceof Error
                ? versionQuery.error.message
                : auditQuery.error instanceof Error
                  ? auditQuery.error.message
                  : "Verifique a API."}
            </StateBanner>
          ) : version ? (
            <div>
              <h2 className="text-xl font-semibold">{version.name}</h2>
              <p className="mt-2 text-sm text-muted-foreground">{version.description}</p>
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <Metric label="Turnos" value={version.nodes.length} />
                <Metric label="Alternativas" value={optionsCount} />
                <Metric label="Criticas" value={criticalOptionsCount} />
              </div>
              <div className="mt-5 space-y-3">
                {version.nodes.slice(0, 3).map((node) => (
                  <div key={node.id} className="rounded-md border border-border bg-background p-3">
                    <div className="text-xs font-semibold uppercase text-muted-foreground">
                      {node.id} - turno {node.turnIndex}
                    </div>
                    <p className="mt-1 text-sm">{node.clientMessage}</p>
                    <div className="mt-2 flex flex-wrap gap-1.5 text-[11px] text-muted-foreground">
                      {node.options.slice(0, 3).map((option) => (
                        <span key={option.id} className="rounded border border-border px-2 py-1">
                          {option.id}: {Object.keys(option.competencyLevels).join(", ") || "sem rubrica"}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </section>

        <aside className="space-y-4">
          <StateBanner tone="warn" title="Canal de revisao obrigatorio">
            Uso eliminatorio so e permitido para simulacao validada, com aprovacao e revisao humana.
          </StateBanner>
          {hasContext && (
            <div className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Evidencias auditaveis
              </div>
              <div className="mt-3 space-y-3">
                {(auditQuery.data ?? []).slice(0, 4).map((event) => (
                  <div key={event.id} className="rounded-md border border-border bg-background p-3 text-sm">
                    <div className="font-medium">{event.message}</div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      {new Date(event.createdAt).toLocaleString("pt-BR")}
                    </div>
                  </div>
                ))}
                {(auditQuery.data ?? []).length === 0 && (
                  <p className="text-sm text-muted-foreground">Nenhum evento registrado.</p>
                )}
              </div>
            </div>
          )}
        </aside>
      </div>

      <div className="mt-6">
        <Link
          to="/"
          className="inline-flex rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>
    </AppShell>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 text-2xl font-semibold tabular-nums">{value}</div>
    </div>
  );
}

function SimulationLinks({
  loading,
  simulations,
}: {
  loading: boolean;
  simulations: SimulationSummaryResponse[];
}) {
  if (loading) return <span className="text-sm text-muted-foreground">Carregando...</span>;
  return (
    <div className="flex flex-wrap gap-2">
      {simulations.map((simulation) => (
        <Link
          key={`${simulation.id}-${simulation.versionNumber}`}
          to="/lgpd"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
