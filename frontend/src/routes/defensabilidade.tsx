import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ClipboardCheck, Scale, Shield } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Termo } from "@/components/glossario";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  getSimulationVersion,
  listSimulationVersionAuditEvents,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/defensabilidade")({
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
      { title: "Defensabilidade - Praxis" },
      { name: "description", content: "Base técnica e jurídica do SJT determinístico." },
    ],
  }),
  component: DefensibilityPage,
});

const pillars = [
  {
    icon: ClipboardCheck,
    term: "construto" as const,
    title: "Construto definido",
    text: "O blueprint (modelo base) fixa cargo, situação crítica e comportamento observável.",
  },
  {
    icon: Scale,
    term: "score-auditavel" as const,
    title: "Score auditável",
    text: "Rubrica, peso e caminho explicam cada ponto do resultado.",
  },
  {
    icon: Shield,
    term: "pontuacao-deterministica" as const,
    title: "Pontuação determinística",
    text: "Cálculo por rubrica, peso e regra declarada.",
  },
];

function DefensibilityPage() {
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

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="promessa comercial indefensavel precisa ser removida" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Conformidade</div>
          <h1 className="mt-1 text-3xl font-semibold">Defensabilidade</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            O produto mede <Termo id="julgamento-situacional">julgamento situacional</Termo>,{" "}
            <Termo id="decisao-contexto">decisão em contexto</Termo> e{" "}
            <Termo id="evidencia-comportamental">evidência comportamental</Termo> estruturada.
          </p>
        </div>
        {versionQuery.data && <StatusBadge status={versionQuery.data.status} />}
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {pillars.map(({ icon: Icon, term, title, text }) => (
          <div key={title} className="rounded-md border border-border bg-card p-5">
            <Icon className="h-5 w-5 text-primary" />
            <h2 className="mt-3 text-sm font-semibold">
              <Termo id={term}>{title}</Termo>
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">{text}</p>
          </div>
        ))}
      </div>

      <div className="mt-5">
        <StateBanner tone="danger" title="Promessa proibida">
          Não vender como conversa real, resposta aberta automática ou decisão sem revisão humana.
        </StateBanner>
      </div>

      <div className="mt-6">
        {!hasContext ? (
          <EmptyState
            title="Selecione uma simulação para ver a sustentação"
            description="Escolha uma simulação abaixo para entender como ela se sustenta tecnicamente e juridicamente. Você verá o planejamento, as competências medidas e o histórico de aprovações."
            actions={<SimulationLinks loading={simulationsQuery.isLoading} simulations={simulationsQuery.data ?? []} />}
          />
        ) : versionQuery.isLoading || auditQuery.isLoading ? (
          <StateBanner tone="info" title="Carregando evidências">
            Buscando blueprint e trilha de auditoria da simulação {search.simulationId} v
            {search.versionNumber}.
          </StateBanner>
        ) : versionQuery.isError || auditQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível carregar evidências">
            {versionQuery.error instanceof Error
              ? versionQuery.error.message
              : auditQuery.error instanceof Error
                ? auditQuery.error.message
                : "Verifique a API."}
          </StateBanner>
        ) : versionQuery.data ? (
          <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Blueprint defensável
              </div>
              <h2 className="mt-2 text-xl font-semibold">{versionQuery.data.name}</h2>
              <p className="mt-2 text-sm text-muted-foreground">{versionQuery.data.description}</p>
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <Metric label="Nós" value={versionQuery.data.nodes.length} />
                <Metric
                  label="Alternativas"
                  value={versionQuery.data.nodes.reduce(
                    (total, node) => total + node.options.length,
                    0,
                  )}
                />
                <Metric
                  label="Competências"
                  value={versionQuery.data.blueprint.competencies.length}
                />
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                {versionQuery.data.blueprint.competencies.map((competency) => (
                  <span
                    key={competency.name}
                    className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                  >
                    {competency.name}: {(competency.weight * 100).toFixed(0)}%
                  </span>
                ))}
              </div>
            </section>
            <aside className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Eventos auditaveis
              </div>
              <div className="mt-3 space-y-3">
                {(auditQuery.data ?? []).slice(0, 5).map((event) => (
                  <div key={event.id} className="rounded-md border border-border bg-background p-3 text-sm">
                    <div className="font-medium">{event.message}</div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      {event.eventType} - {new Date(event.createdAt).toLocaleString("pt-BR")}
                    </div>
                  </div>
                ))}
                {(auditQuery.data ?? []).length === 0 && (
                  <p className="text-sm text-muted-foreground">
                    Nenhum evento registrado para esta versao.
                  </p>
                )}
              </div>
            </aside>
          </div>
        ) : null}
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
          to="/defensabilidade"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
