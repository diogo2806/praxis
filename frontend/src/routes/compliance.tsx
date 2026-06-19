import { useMemo, useEffect } from "react";
import { useNavigate } from "@tanstack/react-router";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Link2, X } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Sheet, SheetClose, SheetContent, SheetTitle } from "@/components/ui/sheet";
import { useQuery } from "@tanstack/react-query";
import {
  getPrivacyCompliance,
  getSimulationVersion,
  listSimulationVersionAuditEvents,
  listSimulations,
  type AuditEventResponse,
  type SimulationSummaryResponse,
  type SimulationVersionDetailResponse,
  type SimulationVersionNodeResponse,
} from "@/lib/api/praxis";

const CUT_OFF_SCORE = 60;

type ComplianceRow = SimulationSummaryResponse & {
  reliability: number;
  complianceLabels: string[];
};

type PathCandidate = {
  sequence: string;
  total: number;
  byCriteria: Record<string, number>;
};

type MatrixItem = {
  criterio: string;
  peso: number;
  cobertura: number;
};

export const Route = createFileRoute("/compliance")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [{ title: "Compliance das vers�es de simula��o - Praxis" }],
  }),
  component: CompliancePage,
});

function CompliancePage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/compliance" });
  const hasContext = Boolean(search.simulationId && search.versionNumber);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
  });
  const privacyQuery = useQuery({
    queryKey: ["privacy-compliance"],
    queryFn: getPrivacyCompliance,
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

  const complianceRows = useMemo(() => {
    return (simulationsQuery.data ?? []).map((simulation) => ({
      ...simulation,
      reliability: Math.round(simulation.completionRatePercent),
      complianceLabels: buildComplianceLabels(simulation, privacyQuery.data),
    }));
  }, [simulationsQuery.data, privacyQuery.data]);

  const activeRow = useMemo(() => {
    if (!hasContext) return null;
    return (
      complianceRows.find(
        (row) => row.id === search.simulationId && row.versionNumber === search.versionNumber,
      ) ?? null
    );
  }, [hasContext, complianceRows, search.simulationId, search.versionNumber]);

  return (
    <AppShell>
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Conformidade</div>
        <h1 className="mt-1 text-3xl font-semibold">Compliance e confiabilidade</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Vis�o unificada da vers�o com status de compliance, crit�rios e trilha de auditoria.
        </p>
      </div>

      {privacyQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando pol�tica de compliance" />
      ) : privacyQuery.isError ? (
        <StateBanner tone="danger" title="N�o foi poss�vel carregar pol�tica de compliance">
          {privacyQuery.error instanceof Error ? privacyQuery.error.message : "Erro ao carregar."}
        </StateBanner>
      ) : null}

      {simulationsQuery.isLoading ? (
        <div className="rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
          Carregando vers�es...
        </div>
      ) : simulationsQuery.isError ? (
        <StateBanner tone="danger" title="N�o foi poss�vel carregar vers�es">
          {simulationsQuery.error instanceof Error ? simulationsQuery.error.message : "Verifique a conex�o."}
        </StateBanner>
      ) : (
        <div className="rounded-md border border-border bg-card">
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Identifica��o</TableHead>
                  <TableHead>Status de compliance</TableHead>
                  <TableHead>Confiabilidade</TableHead>
                  <TableHead className="text-right">A��o</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {complianceRows.map((row) => (
                  <TableRow key={`${row.id}-${row.versionNumber}`}>
                    <TableCell>
                      <div className="font-medium">{row.name}</div>
                      <div className="text-xs text-muted-foreground">v{row.versionNumber}</div>
                    </TableCell>
                    <TableCell>
                      <ComplianceStatusBadges items={row.complianceLabels} />
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <StatusBadge
                          status={
                            row.reliability >= CUT_OFF_SCORE
                              ? "published"
                              : row.reliability >= CUT_OFF_SCORE - 10
                                ? "draft"
                                : "archived"
                          }
                        />
                        <span className="text-sm font-semibold tabular-nums">
                          {row.reliability}/100
                        </span>
                        <span className="text-xs text-muted-foreground">(corte {CUT_OFF_SCORE})</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <Link
                        to="/compliance"
                        search={{ simulationId: row.id, versionNumber: row.versionNumber }}
                        className="inline-flex rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground hover:bg-primary/90"
                      >
                        Abrir vers�o
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </div>
      )}

      <ComplianceSheet
        open={hasContext}
        row={activeRow}
        version={versionQuery.data ?? null}
        auditEvents={auditQuery.data ?? []}
        loading={versionQuery.isLoading || auditQuery.isLoading}
        hasError={versionQuery.isError || auditQuery.isError}
        errorMessage={
          versionQuery.error instanceof Error
            ? versionQuery.error.message
            : auditQuery.error instanceof Error
              ? auditQuery.error.message
              : undefined
        }
        onClose={() => navigate({ to: "/compliance", search: {} })}
      />
    </AppShell>
  );
}

function buildComplianceLabels(
  simulation: SimulationSummaryResponse,
  privacy: { automatedDecisionWithoutReviewAllowed: boolean } | undefined,
) {
  const labels: string[] = [];

  if (!privacy?.automatedDecisionWithoutReviewAllowed) {
    labels.push("Decis�o cr�tica revisada por humano");
  }

  if (simulation.status === "published") {
    labels.push("Conversa real");
  }

  if (simulation.status !== "published") {
    labels.push("Sem resposta aberta autom�tica");
  }

  if (simulation.versionNumber > 1) {
    labels.push("Promessa proibida");
  }

  return labels;
}

function ComplianceStatusBadges({ items }: { items: string[] }) {
  if (items.length === 0) {
    return <span className="text-xs text-muted-foreground">Sem alerta</span>;
  }

  return (
    <div className="flex flex-wrap gap-1.5">
      {items.map((item) => (
        <span
          key={item}
          className="inline-flex items-center rounded-md border border-success/35 bg-success/10 px-2 py-0.5 text-[11px] text-success"
        >
          {item}
        </span>
      ))}
    </div>
  );
}

function ComplianceSheet({
  open,
  row,
  version,
  auditEvents,
  loading,
  hasError,
  errorMessage,
  onClose,
}: {
  open: boolean;
  row: ComplianceRow | null;
  version: SimulationVersionDetailResponse | null;
  auditEvents: AuditEventResponse[];
  loading: boolean;
  hasError: boolean;
  errorMessage?: string;
  onClose: () => void;
}) {
  if (!open) return null;

  const versionTitle = row ? `${row.name} � v${row.versionNumber}` : "Vers�o";

  return (
    <Sheet open={open} onOpenChange={(value) => !value && onClose()}>
      <SheetContent className="w-full max-w-2xl overflow-y-auto p-5">
        <SheetTitle className="sr-only">Detalhes da vers�o</SheetTitle>

        <div className="mb-4 flex items-start justify-between">
          <div>
            <div className="text-xs uppercase text-muted-foreground">Detalhes da vers�o</div>
            <h2 className="mt-1 text-xl font-semibold">{versionTitle}</h2>
            {row ? (
              <Link
                to="/governanca"
                search={{ simulationId: row.id, versionNumber: row.versionNumber }}
                className="mt-1 inline-flex items-center gap-1 text-sm text-primary underline"
              >
                <Link2 className="h-3.5 w-3.5" />
                Detalhes da vers�o
              </Link>
            ) : null}
          </div>
          <SheetClose asChild>
            <button className="rounded-md border border-border p-2 text-muted-foreground hover:bg-accent">
              <X className="h-4 w-4" />
            </button>
          </SheetClose>
        </div>

        <div className="space-y-4">
          <section className="space-y-1 rounded-md border border-border bg-background p-3">
            <h3 className="text-xs font-semibold uppercase text-muted-foreground">Aprova��o / publica��o</h3>
            {row ? (
              <>
                <p className="text-sm">Aprovador: ainda n�o carregado nesta tela.</p>
                <p className="text-xs text-muted-foreground">Atualizada em {formatDate(row.updatedAt)}</p>
              </>
            ) : (
              <p className="text-sm text-muted-foreground">Ainda n�o aprovada.</p>
            )}
          </section>

          <section className="space-y-2 rounded-md border border-border bg-background p-3">
            <h3 className="text-xs font-semibold uppercase text-muted-foreground">Confiabilidade do resultado</h3>
            {loading || !version ? (
              <div className="text-sm text-muted-foreground">Carregando an�lise da vers�o...</div>
            ) : hasError ? (
              <StateBanner tone="danger" title="Falha ao abrir dados de confiabilidade">
                {errorMessage}
              </StateBanner>
            ) : (
              <DossiePanel version={version} cutoff={CUT_OFF_SCORE} />
            )}
          </section>

          <section className="space-y-2 rounded-md border border-border bg-background p-3">
            <h3 className="text-xs font-semibold uppercase text-muted-foreground">Trilha de auditoria</h3>
            <AuditoriaPanel events={auditEvents} />
          </section>
        </div>
      </SheetContent>
    </Sheet>
  );
}

export function DossiePanel({
  version,
  cutoff,
}: {
  version: SimulationVersionDetailResponse;
  cutoff: number;
}) {
  const matrix = useMemo(() => buildMatrix(version), [version]);
  const paths = useMemo(() => buildPaths(version, matrix), [version, matrix]);
  const totalWeight = matrix.reduce((sum, item) => sum + item.peso, 0);

  return (
    <div className="space-y-5 text-sm">
      <div>
        <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          Como esta vers�o � avaliada
        </div>
        <div className="overflow-x-auto rounded-md border border-border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Crit�rio</TableHead>
                <TableHead>% do score</TableHead>
                <TableHead>Cobrado em</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {matrix.map((item) => {
                const percentual = totalWeight > 0 ? Math.round((item.peso / totalWeight) * 100) : 0;
                return (
                  <TableRow key={item.criterio}>
                    <TableCell className="font-medium">{item.criterio}</TableCell>
                    <TableCell title={`peso ${item.peso} de ${totalWeight}`}>{percentual}% do score</TableCell>
                    <TableCell
                      className="text-muted-foreground"
                      title="Quantos turnos da simula��o cobram este crit�rio"
                    >
                      cobrado em {item.cobertura} turnos
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      </div>

      <div>
        <div className="mb-2 flex items-center justify-between">
          <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Caminhos poss�veis
          </div>
          <div className="text-xs text-muted-foreground">Faixa de corte: = {cutoff}/100</div>
        </div>

        <div className="space-y-2">
          {paths.map((path) => {
            const bgClass = path.total >= cutoff ? "bg-success/15 text-success" : "bg-danger/15 text-danger";
            return (
              <div key={path.sequence} className="rounded-md border border-border bg-background p-3">
                <div className="flex items-start justify-between gap-2">
                  <p className="min-w-0 text-xs font-mono">{path.sequence}</p>
                  <span className={`rounded-md px-2 py-1 text-xs font-semibold ${bgClass}`}>{path.total}/100</span>
                </div>
                <div className="mt-2 flex flex-wrap items-center justify-between gap-2 border-t border-border pt-2">
                  <div className="grid w-full gap-2 text-xs md:grid-cols-3">
                    {matrix.map((item) => {
                      const gained = path.byCriteria[item.criterio] ?? 0;
                      return (
                        <div key={item.criterio}>
                          <div className="text-muted-foreground">{item.criterio}</div>
                          <div className="font-mono">{gained}/{item.peso * 10}</div>
                        </div>
                      );
                    })}
                  </div>
                  <button type="button" className="inline-flex items-center text-xs text-muted-foreground underline">
                    Ver tentativa #{paths.indexOf(path) + 1}
                  </button>
                </div>
              </div>
            );
          })}
          {paths.length === 0 ? (
            <div className="rounded-md border border-border bg-card p-3 text-sm text-muted-foreground">Sem caminhos mapeados.</div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export function AuditoriaPanel({ events }: { events: AuditEventResponse[] }) {
  const perPage = 4;
  const [page, setPage] = useState(0);
  const totalPages = Math.max(1, Math.ceil(events.length / perPage));
  const from = page * perPage;
  const to = Math.min(events.length, from + perPage);
  const visible = events.slice(from, to);

  return (
    <div className="space-y-2 rounded-md border border-border">
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Quando</TableHead>
              <TableHead>Quem</TableHead>
              <TableHead>Evento</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visible.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} className="text-sm text-muted-foreground">
                  Nenhum evento de auditoria.
                </TableCell>
              </TableRow>
            ) : (
              visible.map((event) => (
                <TableRow key={event.id}>
                  <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                    {formatDate(event.createdAt)}
                  </TableCell>
                  <TableCell className="whitespace-nowrap text-xs">{parseWho(event.metadata)}</TableCell>
                  <TableCell className="text-sm">{event.message}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
      <div className="flex items-center justify-between border-t border-border p-2 text-xs text-muted-foreground">
        <span>
          {events.length === 0 ? "0-0 de 0" : `${from + 1}�${to} de ${events.length}`}
        </span>
        <div className="flex gap-1">
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
            disabled={page === 0}
          >
            Anterior
          </button>
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
            disabled={page >= totalPages - 1}
          >
            Pr�xima
          </button>
        </div>
      </div>
    </div>
  );
}

function buildMatrix(version: SimulationVersionDetailResponse): MatrixItem[] {
  const coverageByCriterion = new Map<string, number>();

  for (const node of version.nodes) {
    const hit = new Set<string>();
    for (const option of node.options) {
      Object.keys(option.competencyLevels).forEach((criterio) => {
        hit.add(criterio);
      });
    }
    for (const criterio of hit) {
      coverageByCriterion.set(criterio, (coverageByCriterion.get(criterio) ?? 0) + 1);
    }
  }

  return version.blueprint.competencies.map((item) => ({
    criterio: item.name,
    peso: item.weight,
    cobertura: coverageByCriterion.get(item.name) ?? 0,
  }));
}

function buildPaths(version: SimulationVersionDetailResponse, matrix: MatrixItem[]) {
  const byId = new Map<string, SimulationVersionNodeResponse>(
    version.nodes.map((node) => [node.id, node]),
  );

  const paths: PathCandidate[] = [];
  if (version.nodes.length === 0) return paths;

  const start = [...version.nodes].sort((a, b) => a.turnIndex - b.turnIndex)[0];

  const walk = (
    node: SimulationVersionNodeResponse,
    sequence: string[],
    byCriteria: Record<string, number>,
    depth: number,
  ) => {
    if (depth > version.nodes.length + 4) return;

    if (node.options.length === 0) {
      paths.push({
        sequence: `${sequence.join(" ? ")} ? Encerramento conservador`,
        total: calculateTotal(byCriteria, matrix),
        byCriteria: { ...byCriteria },
      });
      return;
    }

    for (const option of node.options) {
      const nextSequence = [...sequence, `${node.id}�${option.id}`];
      const nextCriteria: Record<string, number> = { ...byCriteria };

      Object.entries(option.competencyLevels).forEach(([criterio, pontos]) => {
        nextCriteria[criterio] = (nextCriteria[criterio] ?? 0) + pontos;
      });

      if (option.nextNodeId) {
        const nextNode = byId.get(option.nextNodeId);
        if (nextNode) {
          walk(nextNode, nextSequence, nextCriteria, depth + 1);
          continue;
        }
      }

      paths.push({
        sequence: `${nextSequence.join(" ? ")} ? Encerramento conservador`,
        total: calculateTotal(nextCriteria, matrix),
        byCriteria: nextCriteria,
      });
    }
  };

  walk(start, [], {}, 0);
  return paths;
}

function calculateTotal(byCriteria: Record<string, number>, matrix: MatrixItem[]) {
  const total = matrix.reduce((sum, item) => sum + (byCriteria[item.criterio] ?? 0), 0);
  return Math.min(total, 100);
}

function parseWho(metadata?: string | null) {
  if (!metadata) return "Sistema";
  try {
    const value = JSON.parse(metadata) as { actor?: string; who?: string };
    if (typeof value === "object" && value !== null) {
      if (typeof value.actor === "string") return value.actor;
      if (typeof value.who === "string") return value.who;
    }
  } catch {
    return "Sistema";
  }
  return "Sistema";
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export default CompliancePage;

