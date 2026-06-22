import { useMemo, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Eye, Globe, CircleHelp, Link2, X } from "lucide-react";
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
  getSimulationValidation,
  getSimulationVersion,
  listSimulationVersionAuditEvents,
  listSimulations,
  type AuditEventResponse,
  type SimulationSummaryResponse,
  type SimulationValidationResponse,
  type SimulationVersionDetailResponse,
} from "@/lib/api/praxis";

const CORTA = 60;

type ComplianceRow = SimulationSummaryResponse & {
  completionRate: number;
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

const STATUS_OPTIONS = [
  "Todos os status",
  "Rascunho",
  "Em validação",
  "Bloqueado",
  "Publicado",
] as const;

type StatusLabel = (typeof STATUS_OPTIONS)[number];

const STATUS_TEXT: Record<SimulationSummaryResponse["status"], string> = {
  draft: "Em validação",
  published: "Publicado",
  archived: "Bloqueado",
};

const STATUS_BADGE_CLASS: Record<SimulationSummaryResponse["status"], string> = {
  draft: "bg-warning/15 text-warning-foreground border-warning/30",
  published: "bg-success/15 text-success border-success/30",
  archived: "bg-danger/15 text-danger border-danger/30",
};

const mapSearchStatusToQuery = (status: string): SimulationSummaryResponse["status"] | null =>
  status === "Publicado"
    ? "published"
    : status === "Bloqueado"
      ? "archived"
      : status === "Em validação"
        ? "draft"
        : null;

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
    meta: [{ title: "Compliance dos testes - Praxis" }],
  }),
  component: CompliancePage,
});

function CompliancePage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/compliance" });
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusLabel>("Todos os status");

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
  const validationQuery = useQuery({
    queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationValidation(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const auditQuery = useQuery({
    queryKey: ["simulation-audit", search.simulationId, search.versionNumber],
    queryFn: () => listSimulationVersionAuditEvents(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });

  const rows = useMemo<ComplianceRow[]>(() => {
    return (simulationsQuery.data ?? [])
      .map((simulation) => ({
        ...simulation,
        completionRate: Math.round(simulation.completionRatePercent),
      }))
      .filter((item) => {
        const normalized = `${item.name} ${item.versionNumber}`.toLowerCase();
        if (query.trim() && !normalized.includes(query.trim().toLowerCase())) {
          return false;
        }

        const filterStatus = mapSearchStatusToQuery(status);
        if (!filterStatus) {
          return true;
        }

        return item.status === filterStatus;
      })
      .sort((a, b) => {
        if (a.updatedAt === b.updatedAt) {
          return b.versionNumber - a.versionNumber;
        }
        return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime();
      });
  }, [simulationsQuery.data, query, status]);

  const activeRow = useMemo(() => {
    if (!hasContext) return null;
    return (
      rows.find(
        (row) => row.id === search.simulationId && row.versionNumber === search.versionNumber,
      ) ?? null
    );
  }, [hasContext, rows, search.simulationId, search.versionNumber]);

  const closeDrawer = () => navigate({ to: "/compliance", search: {} });

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl px-2 py-8 sm:px-6">
        <div className="mb-6">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-primary">
            Compliance
          </div>
          <h1 className="mt-1 font-serif text-3xl leading-tight">Compliance dos testes</h1>
          <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
            Cada linha é uma versão de teste: status de governança, completude da configuração e
            bloqueios em aberto, em um único lugar.
          </p>
        </div>

        <section className="rounded-xl border border-border bg-card">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-4 py-3">
            <div className="text-sm font-semibold">Versões</div>
            <div className="flex items-center gap-2">
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Buscar teste, autor..."
                className="rounded-md border border-border bg-background px-2.5 py-1.5 text-xs outline-none focus:ring-1 focus:ring-ring"
              />
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value as StatusLabel)}
                className="rounded-md border border-border bg-background px-2 py-1.5 text-xs"
              >
                {STATUS_OPTIONS.map((option) => (
                  <option key={option}>{option}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Teste</TableHead>
                  <TableHead>Versão</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Completude</TableHead>
                  <TableHead>Bloqueios</TableHead>
                  <TableHead>Autor</TableHead>
                  <TableHead>Tentativas</TableHead>
                  <TableHead>Atualizado</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {simulationsQuery.isLoading ? (
                  <TableRow>
                    <TableCell colSpan={9} className="p-4 text-sm text-muted-foreground">
                      Carregando versões...
                    </TableCell>
                  </TableRow>
                ) : simulationsQuery.isError ? (
                  <TableRow>
                    <TableCell colSpan={9} className="p-4">
                      <StateBanner tone="danger" title="Não foi possível carregar as versões">
                        {simulationsQuery.error instanceof Error
                          ? simulationsQuery.error.message
                          : "Tente novamente em alguns instantes."}
                      </StateBanner>
                    </TableCell>
                  </TableRow>
                ) : rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} className="p-4 text-sm text-muted-foreground">
                      Nenhum teste encontrado.
                    </TableCell>
                  </TableRow>
                ) : (
                  rows.map((row) => {
                    const completionTone: "published" | "draft" | "archived" =
                      row.completionRate >= CORTA
                        ? "published"
                        : row.completionRate >= 55
                          ? "draft"
                          : "archived";

                    return (
                      <TableRow
                        key={`${row.id}-${row.versionNumber}`}
                        className="border-t border-border hover:bg-muted/30"
                      >
                        <TableCell className="px-4 py-2.5">
                          <div className="font-medium">{row.name}</div>
                        </TableCell>
                        <TableCell className="px-4 py-2.5 font-mono text-xs">
                          v{row.versionNumber}
                        </TableCell>
                        <TableCell className="px-4 py-2.5">
                          <span
                            className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs border ${STATUS_BADGE_CLASS[row.status]}`}
                          >
                            {STATUS_TEXT[row.status]}
                          </span>
                        </TableCell>
                        <TableCell className="px-4 py-2.5">
                          <span
                            className={`font-semibold ${
                              completionTone === "published"
                                ? "text-success"
                                : completionTone === "draft"
                                  ? "text-warning"
                                  : "text-danger"
                            }`}
                          >
                            {row.completionRate}/100
                          </span>
                        </TableCell>
                        <TableCell className="px-4 py-2.5 text-sm text-muted-foreground">
                          —
                        </TableCell>
                        <TableCell className="px-4 py-2.5">{row.resultUse ?? "—"}</TableCell>
                        <TableCell className="px-4 py-2.5">{row.attemptsCreated}</TableCell>
                        <TableCell className="px-4 py-2.5 text-muted-foreground">
                          {formatDate(row.updatedAt)}
                        </TableCell>
                        <TableCell className="px-4 py-2.5">
                          <Link
                            to="/compliance"
                            search={{ simulationId: row.id, versionNumber: row.versionNumber }}
                            className="inline-flex w-full items-center justify-center rounded-md border border-border bg-background px-2 py-1 text-xs hover:bg-accent"
                          >
                            <Eye className="mr-1.5 h-3 w-3" />
                            Detalhes
                          </Link>
                        </TableCell>
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          </div>
        </section>
      </div>

      <ComplianceSheet
        open={hasContext}
        row={activeRow}
        version={versionQuery.data ?? null}
        validation={validationQuery.data ?? null}
        auditEvents={auditQuery.data ?? []}
        loading={versionQuery.isLoading || validationQuery.isLoading || auditQuery.isLoading}
        hasError={versionQuery.isError || validationQuery.isError || auditQuery.isError}
        errorMessage={
          versionQuery.error instanceof Error
            ? versionQuery.error.message
            : validationQuery.error instanceof Error
              ? validationQuery.error.message
              : auditQuery.error instanceof Error
                ? auditQuery.error.message
                : undefined
        }
        onClose={closeDrawer}
      />

      <div className="fixed bottom-3 right-3 flex items-center gap-2">
        <button className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1.5 text-xs hover:bg-accent">
          <Globe className="h-3.5 w-3.5" /> PT-BR
        </button>
        <button className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1.5 text-xs hover:bg-accent">
          <CircleHelp className="h-3.5 w-3.5" /> Ajuda
        </button>
      </div>
    </AppShell>
  );
}

function ComplianceSheet({
  open,
  row,
  version,
  validation,
  auditEvents,
  loading,
  hasError,
  errorMessage,
  onClose,
}: {
  open: boolean;
  row: ComplianceRow | null;
  version: SimulationVersionDetailResponse | null;
  validation: SimulationValidationResponse | null;
  auditEvents: AuditEventResponse[];
  loading: boolean;
  hasError: boolean;
  errorMessage?: string;
  onClose: () => void;
}) {
  const versionTitle = row ? `${row.name} · v${row.versionNumber}` : "Versão";

  if (!open) return null;

  return (
    <Sheet open={open} onOpenChange={(value) => !value && onClose()}>
      <SheetContent className="w-full max-w-xl overflow-y-auto p-0">
        <SheetTitle className="sr-only">Detalhes da versão</SheetTitle>

        <div className="flex items-start justify-between border-b border-border px-5 py-4">
          <div>
            <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
              {versionTitle}
            </div>
            <div className="font-serif text-xl">Detalhes da versão</div>
          </div>
          <SheetClose asChild>
            <button className="rounded-md p-1 hover:bg-accent" aria-label="Fechar">
              <X className="h-4 w-4" />
            </button>
          </SheetClose>
        </div>

        <div className="space-y-8 p-5">
          <section className="rounded-lg border border-border bg-background p-3 text-sm">
            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Aprovação / publicação
            </div>
            <div className="mt-1.5">
              <div className="font-medium">
                {validation?.publishable ? "Aprovador registrado" : "Sem aprovação final"}
              </div>
              <div className="text-xs text-muted-foreground">
                {validation
                  ? `Status interno: ${validation.issues.length} validações processadas`
                  : "Carregando validação..."}
              </div>
            </div>
          </section>

          <section className="space-y-2">
            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Estrutura e rastreabilidade do cálculo
            </div>
            {loading || !version ? (
              <div className="rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
                Carregando análise da versão...
              </div>
            ) : hasError ? (
              <StateBanner tone="danger" title="Falha ao abrir dados da configuração">
                {errorMessage}
              </StateBanner>
            ) : (
              <DossiePanel version={version} cutoff={CORTA} />
            )}
          </section>

          <section>
            <div className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Trilha de auditoria
            </div>
            <AuditoriaPanel events={auditEvents} />
          </section>

          {validation ? (
            <section className="rounded-lg border border-border bg-background p-3 text-sm">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Resumo técnico
              </div>
              <div className="mt-1.5 text-muted-foreground">
                Bloqueios em aberto: {validation.blockerCount}
              </div>
              <Link
                to="/governanca"
                className="mt-2 inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
              >
                <Link2 className="h-3.5 w-3.5" /> Abrir trilha técnica
              </Link>
            </section>
          ) : null}
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
      <div className="rounded-lg border border-border bg-background">
        <div className="px-3 py-2 text-xs font-semibold uppercase text-muted-foreground">
          Como esta versão é avaliada
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Critério</TableHead>
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
                  <TableCell title={`peso ${item.peso} de ${totalWeight}`}>
                    {percentual}% do score
                  </TableCell>
                  <TableCell
                    className="text-muted-foreground"
                    title="Quantas etapas do teste cobram este critério"
                  >
                    cobrado em {item.cobertura} etapas
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      <div>
        <div className="mb-2 flex items-center justify-between">
          <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Caminhos possíveis
          </div>
          <div className="text-xs text-muted-foreground">
            Faixa de corte: <span className="font-semibold text-foreground">≥ {cutoff}/100</span>
          </div>
        </div>

        <div className="space-y-2">
          {paths.length === 0 ? (
            <div className="rounded-md border border-border bg-card p-3 text-sm text-muted-foreground">
              Sem caminhos mapeados.
            </div>
          ) : (
            paths.map((path, index) => {
              const isPass = path.total >= cutoff;
              return (
                <div
                  key={path.sequence}
                  className="rounded-md border border-border bg-background px-3 py-2"
                >
                  <div className="flex items-center justify-between gap-3">
                    <span className="font-mono text-xs">{path.sequence}</span>
                    <span
                      className={`rounded-md px-2 py-0.5 text-xs font-semibold ${
                        isPass ? "bg-success/15 text-success" : "bg-danger/15 text-danger"
                      }`}
                    >
                      {path.total}/100
                    </span>
                  </div>
                  <div className="mt-2 grid grid-cols-3 gap-2 border-t border-border pt-2 text-xs">
                    {matrix.map((item) => {
                      const gained = path.byCriteria[item.criterio] ?? 0;
                      return (
                        <div key={item.criterio} className="flex flex-col">
                          <div className="truncate text-muted-foreground">{item.criterio}</div>
                          <div className="font-mono">
                            {gained}
                            <span className="text-muted-foreground">/{item.peso * 10}</span>
                          </div>
                        </div>
                      );
                    })}
                    <div className="col-span-3 text-right">
                      <button
                        type="button"
                        className="rounded-md border border-border px-2 py-0.5 text-xs text-muted-foreground hover:bg-accent hover:text-foreground"
                      >
                        Ver tentativa #{index + 1}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}

export function AuditoriaPanel({ events }: { events: AuditEventResponse[] }) {
  const [page, setPage] = useState(0);
  const perPage = 4;
  const totalPages = Math.max(1, Math.ceil(events.length / perPage));
  const from = page * perPage;
  const to = Math.min(events.length, from + perPage);
  const visible = events.slice(from, to);

  return (
    <div className="space-y-2 rounded-md border border-border">
      <div className="overflow-x-auto rounded-lg border border-border bg-background">
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
                  <TableCell className="whitespace-nowrap px-3 py-2 text-xs text-muted-foreground">
                    {formatDate(event.createdAt)}
                  </TableCell>
                  <TableCell className="whitespace-nowrap px-3 py-2 text-xs">
                    {parseWho(event.metadata)}
                  </TableCell>
                  <TableCell className="px-3 py-2">{event.message}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex items-center justify-between border-t border-border px-3 py-2 text-xs text-muted-foreground">
        <span>{events.length === 0 ? "0-0 de 0" : `${from + 1}-${to} de ${events.length}`}</span>
        <div className="flex gap-1">
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => setPage((value) => Math.max(0, value - 1))}
            disabled={page === 0}
          >
            Anterior
          </button>
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}
            disabled={page >= totalPages - 1}
          >
            Próxima
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
      Object.keys(option.competencyLevels).forEach((criterion) => {
        hit.add(criterion);
      });
    }
    for (const criterion of hit) {
      coverageByCriterion.set(criterion, (coverageByCriterion.get(criterion) ?? 0) + 1);
    }
  }

  return version.blueprint.competencies.map((item) => ({
    criterio: item.name,
    peso: item.weight,
    cobertura: coverageByCriterion.get(item.name) ?? 0,
  }));
}

function buildPaths(version: SimulationVersionDetailResponse, matrix: MatrixItem[]) {
  const byId = new Map<string, (typeof version.nodes)[number]>(
    version.nodes.map((node) => [node.id, node]),
  );
  const paths: PathCandidate[] = [];
  if (version.nodes.length === 0) return paths;

  const sorted = [...version.nodes].sort((a, b) => a.turnIndex - b.turnIndex);
  const start = sorted[0];

  const walk = (
    node: (typeof version.nodes)[number],
    sequence: string[],
    byCriteria: Record<string, number>,
    depth: number,
  ) => {
    if (depth > version.nodes.length + 5) return;

    if (node.options.length === 0) {
      paths.push({
        sequence: `${sequence.join(" ? ")} ? Encerramento conservador`,
        total: calculateTotal(byCriteria, matrix),
        byCriteria: { ...byCriteria },
      });
      return;
    }

    for (const option of node.options) {
      const nextSequence = [...sequence, `${node.id}·${option.id}`];
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
