import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ClipboardCheck, Scale, Shield } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ComplianceScope } from "@/components/compliance-scope";
import { Termo } from "@/components/glossario";
import { ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useLanguage } from "@/lib/language-context";
import {
  getSimulationVersion,
  listSimulationVersionAuditEvents,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";

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
      { title: "Confiabilidade e segurança técnica - Praxis" },
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
    text: "O plano da avaliação fixa cargo, situação crítica e comportamento observável.",
  },
  {
    icon: Scale,
    term: "score-auditavel" as const,
    title: "Score auditável",
    text: "Critérios de pontuação, peso e caminho explicam cada ponto do resultado.",
  },
  {
    icon: Shield,
    term: "pontuacao-deterministica" as const,
    title: "Pontuação determinística",
    text: "Cálculo por critérios de pontuação, peso e regra declarada.",
  },
];

function DefensibilityPage() {
  const { t } = useLanguage();
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
  const auditEvents = auditQuery.data ?? [];

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="promessa comercial indefensável precisa ser removida" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">{t.common.compliance}</div>
          <h1 className="mt-1 text-3xl font-semibold">{t.defensability.heading}</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Esta tela responde por que o resultado e tecnicamente sustentavel. O produto mede{" "}
            <Termo id="julgamento-situacional">julgamento situacional</Termo>,{" "}
            <Termo id="decisao-contexto">escolha baseada na situação</Termo> e{" "}
            <Termo id="evidencia-comportamental">evidência comportamental</Termo> estruturada.
          </p>
        </div>
        {version && <StatusBadge status={version.status} />}
      </div>

      <ComplianceScope current="defensabilidade" />

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
          <section className="rounded-md border border-border bg-card p-5">
            <div className="mb-4">
              <h2 className="text-lg font-semibold">
                Selecione uma simulação para ver a sustentação
              </h2>
              <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
                Escolha uma versão para revisar a base técnica, pesos e evidências que sustentam o
                resultado.
              </p>
            </div>
            <SimulationLinks
              error={simulationsQuery.isError}
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          </section>
        ) : versionQuery.isLoading || auditQuery.isLoading ? (
          <StateBanner tone="info" title="Carregando evidências">
            Buscando plano da avaliação e trilha de auditoria da simulação {search.simulationId} v
            {search.versionNumber}.
          </StateBanner>
        ) : versionQuery.isError || auditQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível carregar evidências">
            {versionQuery.error instanceof Error
              ? versionQuery.error.message
              : auditQuery.error instanceof Error
                ? auditQuery.error.message
                : "Verifique sua conexão e tente novamente."}
          </StateBanner>
        ) : version ? (
          <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Plano defensável
              </div>
              <h2 className="mt-2 text-xl font-semibold">{version.name}</h2>
              <p className="mt-2 text-sm text-muted-foreground">{version.description}</p>
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <Metric label="Nos" value={version.nodes.length} />
                <Metric
                  label="Alternativas"
                  value={version.nodes.reduce((total, node) => total + node.options.length, 0)}
                />
                <Metric label="Competencias" value={version.blueprint.competencies.length} />
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                {version.blueprint.competencies.map((competency) => (
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
                Eventos de auditoria
              </div>
              <div className="mt-3 space-y-3">
                {auditEvents.slice(0, 5).map((event) => (
                  <div
                    key={event.id}
                    className="rounded-md border border-border bg-background p-3 text-sm"
                  >
                    <div className="font-medium">{event.message}</div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      {new Date(event.createdAt).toLocaleString("pt-BR")}
                    </div>
                  </div>
                ))}
                {auditEvents.length === 0 && (
                  <p className="text-sm text-muted-foreground">Nenhum evento registrado.</p>
                )}
              </div>
            </aside>
          </div>
        ) : null}
      </div>
    </AppShell>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-1 text-2xl font-semibold tabular-nums">{value}</div>
    </div>
  );
}

function SimulationLinks({
  error,
  loading,
  simulations,
}: {
  error: boolean;
  loading: boolean;
  simulations: SimulationSummaryResponse[];
}) {
  if (loading) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm">
        Carregando simulações...
      </div>
    );
  }
  if (error) {
    return (
      <StateBanner tone="danger" title="Não foi possível carregar as simulações">
        Verifique se o sistema está disponível e tente novamente.
      </StateBanner>
    );
  }
  if (simulations.length === 0) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhuma simulação encontrada.
      </div>
    );
  }
  return (
    <div className="rounded-md border border-border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Simulação</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Competências</TableHead>
            <TableHead className="text-right">Versão</TableHead>
            <TableHead className="text-right">Tentativas</TableHead>
            <TableHead className="text-right">Ação</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {simulations.map((simulation) => (
            <TableRow key={`${simulation.id}-${simulation.versionNumber}`}>
              <TableCell className="min-w-[220px]">
                <div className="font-medium">{simulation.name}</div>
                <div className="mt-1 max-w-[360px] truncate text-xs text-muted-foreground">
                  {simulation.description}
                </div>
              </TableCell>
              <TableCell className="min-w-[150px]">
                <StatusBadge
                  status={simulation.status}
                  maturity={maturityForStatus(simulation.status)}
                />
              </TableCell>
              <TableCell className="min-w-[220px]">
                <div className="flex flex-wrap gap-1">
                  {simulation.competencies.slice(0, 3).map((competency) => (
                    <span
                      key={competency}
                      className="rounded-md border border-border bg-card px-2 py-0.5 text-[10px] text-muted-foreground"
                    >
                      {competency}
                    </span>
                  ))}
                  {simulation.competencies.length > 3 && (
                    <span className="rounded-md border border-border bg-card px-2 py-0.5 text-[10px] text-muted-foreground">
                      +{simulation.competencies.length - 3}
                    </span>
                  )}
                </div>
              </TableCell>
              <TableCell className="text-right tabular-nums">v{simulation.versionNumber}</TableCell>
              <TableCell className="text-right tabular-nums">
                {simulation.attemptsCreated.toLocaleString("pt-BR")}
              </TableCell>
              <TableCell className="text-right">
                <Link
                  to="/defensabilidade"
                  search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
                  className="inline-flex items-center justify-center rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                >
                  Ver sustentação
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
