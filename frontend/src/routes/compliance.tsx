import { Fragment, useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "@tanstack/react-router";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Eye, ShieldCheck, ShieldAlert, Gauge, AlertTriangle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useLanguage } from "@/lib/language-context";
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
  type ValidationIssueResponse,
} from "@/lib/api/praxis";

const CORTA = 60;
const PAGE_SIZE = 8;

type ComplianceRow = SimulationSummaryResponse & {
  completionRate: number;
};

type PathCandidate = {
  sequence: string;
  total: number;
  byCriteria: Record<string, number>;
  steps: PathStep[];
};

type PathStep = {
  nodeId: string;
  optionId: string;
  turnIndex: number;
  speaker: string;
  clientMessage: string;
  optionText: string;
  nextNodeId: string | null;
  competencyLevels: Record<string, number>;
};

type MatrixItem = {
  criterio: string;
  peso: number;
  cobertura: number;
};

const STATUS_OPTIONS = [
  "Todos os status",
  "Rascunho",
  "Em revisão",
  "Bloqueado",
  "Publicado",
] as const;

type StatusLabel = (typeof STATUS_OPTIONS)[number];

const STATUS_TEXT: Record<SimulationSummaryResponse["status"], string> = {
  draft: "Em revisão",
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
      : status === "Em revisão"
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
    meta: [{ title: "Conformidade das avaliações - Práxis" }],
  }),
  component: CompliancePage,
});

function CompliancePage() {
  const { t } = useLanguage();
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/compliance" });
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusLabel>("Todos os status");
  const [page, setPage] = useState(0);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
  });
  // Mantém o resumo de privacidade pré-carregado para a aba de conformidade.
  useQuery({
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

  // Reinicia a paginação quando os filtros mudam para não ficar em uma página vazia.
  useEffect(() => {
    setPage(0);
  }, [query, status]);

  const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const from = safePage * PAGE_SIZE;
  const to = Math.min(rows.length, from + PAGE_SIZE);
  const pageRows = rows.slice(from, to);

  const activeRow = useMemo(() => {
    if (!hasContext) return null;
    return (
      rows.find(
        (row) => row.id === search.simulationId && row.versionNumber === search.versionNumber,
      ) ?? null
    );
  }, [hasContext, rows, search.simulationId, search.versionNumber]);

  const closeDialog = () =>
    navigate({ to: "/compliance", search: { simulationId: undefined, versionNumber: undefined } });

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl px-2 py-8 sm:px-6">
        <div className="mb-5">
          <div className="text-[11px] font-semibold uppercase tracking-wider text-primary">
            {t.common.compliance}
          </div>
          <h1 className="mt-1 font-serif text-3xl leading-tight">{t.common.compliance}</h1>
          <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
            Acompanhe se cada versão de avaliação está pronta para ir ao ar com segurança. Use a
            busca e o filtro de status para encontrar uma versão e clique em{" "}
            <span className="font-medium text-foreground">Detalhes</span> para ver pendências,
            critérios, caminhos possíveis e o histórico de alterações.
          </p>
        </div>

        <section className="rounded-xl border border-border bg-card">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-4 py-3">
            <div>
              <div className="text-sm font-semibold">Versões</div>
              <div className="text-xs text-muted-foreground">
                {rows.length === 0
                  ? "Nenhuma versão para os filtros atuais"
                  : `${rows.length} ${rows.length === 1 ? "versão" : "versões"} listada${
                      rows.length === 1 ? "" : "s"
                    }`}
              </div>
            </div>
            <div className="flex items-center gap-2">
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Buscar avaliação, autor..."
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
                  <TableHead>Avaliação</TableHead>
                  <TableHead>Versão</TableHead>
                  <TableHead>{t.common.status}</TableHead>
                  <TableHead>Taxa de conclusão</TableHead>
                  <TableHead>Bloqueios</TableHead>
                  <TableHead>Tentativas</TableHead>
                  <TableHead>Atualizado</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {simulationsQuery.isLoading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="p-4 text-sm text-muted-foreground">
                      Carregando versões...
                    </TableCell>
                  </TableRow>
                ) : simulationsQuery.isError ? (
                  <TableRow>
                    <TableCell colSpan={8} className="p-4">
                      <StateBanner tone="danger" title="Não foi possível carregar as versões">
                        {simulationsQuery.error instanceof Error
                          ? simulationsQuery.error.message
                          : "Tente novamente em alguns instantes."}
                      </StateBanner>
                    </TableCell>
                  </TableRow>
                ) : rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="p-4 text-sm text-muted-foreground">
                      Nenhuma avaliação encontrada.
                    </TableCell>
                  </TableRow>
                ) : (
                  pageRows.map((row) => {
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
                          Sem dado
                        </TableCell>
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

          {rows.length > 0 ? (
            <TablePager
              from={from}
              to={to}
              total={rows.length}
              page={safePage}
              totalPages={totalPages}
              onPage={setPage}
            />
          ) : null}
        </section>
      </div>

      <ComplianceDialog
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
        onClose={closeDialog}
      />
    </AppShell>
  );
}

function ComplianceDialog({
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

  return (
    <Dialog open={open} onOpenChange={(value) => !value && onClose()}>
      <DialogContent className="max-h-[88vh] w-[calc(100vw-1.5rem)] max-w-3xl gap-0 overflow-y-auto p-0">
        <DialogHeader className="space-y-1 border-b border-border px-5 py-4 text-left">
          <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
            {versionTitle}
          </div>
          <DialogTitle className="font-serif text-xl">Detalhes da versão</DialogTitle>
          <DialogDescription>
            Um resumo claro do que esta versão avalia, o que falta para publicar e o histórico de
            alterações.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-8 p-5">
          {hasError ? (
            <StateBanner tone="danger" title="Falha ao carregar os detalhes da versão">
              {errorMessage ?? "Tente novamente em alguns instantes."}
            </StateBanner>
          ) : null}

          <ResumoConformidade validation={validation} loading={loading} />

          <DialogSection
            title="Pendências a resolver"
            description="Itens encontrados na verificação automática. Bloqueios impedem a publicação; avisos são recomendações que valem a pena revisar."
          >
            <IssuesPanel validation={validation} version={version} loading={loading} />
          </DialogSection>

          <DialogSection
            title="Como esta versão é avaliada"
            description="Cada critério tem um peso na nota final. Quanto maior o peso, mais ele influencia o resultado do candidato."
          >
            {loading || !version ? (
              <PanelSkeleton label="Carregando critérios de avaliação..." />
            ) : (
              <CriteriosPanel version={version} />
            )}
          </DialogSection>

          <DialogSection
            title="Caminhos possíveis"
            description={`Cada caminho é uma rota que o candidato pode seguir na avaliação. Caminhos verdes alcançam a nota de corte (${CORTA}/100); os vermelhos ficam abaixo.`}
          >
            {loading || !version ? (
              <PanelSkeleton label="Mapeando caminhos da avaliação..." />
            ) : (
              <CaminhosPanel version={version} cutoff={CORTA} />
            )}
          </DialogSection>

          <DialogSection
            title="Trilha de auditoria"
            description="Histórico de quem alterou o quê e quando, para rastreabilidade e conformidade."
          >
            <AuditoriaPanel events={auditEvents} loading={loading} />
          </DialogSection>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function DialogSection({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <section className="space-y-2">
      <div>
        <h3 className="text-sm font-semibold">{title}</h3>
        <p className="mt-0.5 text-xs text-muted-foreground">{description}</p>
      </div>
      {children}
    </section>
  );
}

function PanelSkeleton({ label }: { label: string }) {
  return (
    <div className="rounded-md border border-border bg-card p-4 text-sm text-muted-foreground">
      {label}
    </div>
  );
}

function ResumoConformidade({
  validation,
  loading,
}: {
  validation: SimulationValidationResponse | null;
  loading: boolean;
}) {
  if (loading || !validation) {
    return <PanelSkeleton label="Carregando resumo de conformidade..." />;
  }

  const cards = [
    {
      label: "Pode publicar?",
      value: validation.publishable ? "Sim, pronto" : "Ainda não",
      hint: validation.publishable
        ? "Nenhum bloqueio em aberto. A versão pode ir ao ar."
        : "Há bloqueios que precisam ser resolvidos antes de publicar.",
      Icon: validation.publishable ? ShieldCheck : ShieldAlert,
      tone: validation.publishable ? "ok" : "danger",
    },
    {
      label: "Bloqueios",
      value: String(validation.blockerCount),
      hint: "Problemas que impedem a publicação até serem corrigidos.",
      Icon: ShieldAlert,
      tone: validation.blockerCount > 0 ? "danger" : "ok",
    },
    {
      label: "Avisos",
      value: String(validation.warningCount),
      hint: "Recomendações de melhoria. Não impedem a publicação.",
      Icon: AlertTriangle,
      tone: validation.warningCount > 0 ? "warn" : "ok",
    },
    {
      label: "Qualidade",
      value: `${Math.round(validation.qualityScore)}/100`,
      hint: "Índice geral de qualidade calculado a partir das verificações.",
      Icon: Gauge,
      tone: validation.qualityScore >= CORTA ? "ok" : "warn",
    },
  ] as const;

  const toneClass: Record<string, string> = {
    ok: "border-success/30 bg-success/5",
    warn: "border-warning/30 bg-warning/5",
    danger: "border-danger/30 bg-danger/5",
  };
  const iconClass: Record<string, string> = {
    ok: "text-success",
    warn: "text-warning",
    danger: "text-danger",
  };

  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      {cards.map((card) => (
        <div
          key={card.label}
          title={card.hint}
          className={`rounded-lg border p-3 ${toneClass[card.tone]}`}
        >
          <div className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
            <card.Icon className={`h-3.5 w-3.5 ${iconClass[card.tone]}`} aria-hidden />
            {card.label}
          </div>
          <div className="mt-1.5 text-lg font-semibold">{card.value}</div>
          <p className="mt-0.5 text-[11px] leading-snug text-muted-foreground">{card.hint}</p>
        </div>
      ))}
    </div>
  );
}

const ISSUE_FILTERS = ["Todas", "Bloqueios", "Avisos"] as const;
type IssueFilter = (typeof ISSUE_FILTERS)[number];

function IssuesPanel({
  validation,
  version,
  loading,
}: {
  validation: SimulationValidationResponse | null;
  version: SimulationVersionDetailResponse | null;
  loading: boolean;
}) {
  const [filter, setFilter] = useState<IssueFilter>("Todas");
  const [page, setPage] = useState(0);

  const nodeLabelById = useMemo(() => {
    const map = new Map<string, string>();
    for (const node of version?.nodes ?? []) {
      map.set(node.id, `Etapa ${node.turnIndex}`);
    }
    return map;
  }, [version]);

  const issues = useMemo<ValidationIssueResponse[]>(() => {
    const all = validation?.issues ?? [];
    if (filter === "Bloqueios") return all.filter((issue) => issue.severity === "blocker");
    if (filter === "Avisos") return all.filter((issue) => issue.severity === "warning");
    return all;
  }, [validation, filter]);

  useEffect(() => {
    setPage(0);
  }, [filter, validation]);

  const perPage = 5;
  const totalPages = Math.max(1, Math.ceil(issues.length / perPage));
  const safePage = Math.min(page, totalPages - 1);
  const from = safePage * perPage;
  const to = Math.min(issues.length, from + perPage);
  const visible = issues.slice(from, to);

  if (loading || !validation) {
    return <PanelSkeleton label="Carregando pendências..." />;
  }

  if ((validation.issues ?? []).length === 0) {
    return (
      <StateBanner tone="ok" title="Nenhuma pendência encontrada">
        Esta versão passou em todas as verificações automáticas.
      </StateBanner>
    );
  }

  return (
    <div className="rounded-lg border border-border">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-3 py-2">
        <div className="text-xs text-muted-foreground">
          {validation.blockerCount} bloqueio(s) · {validation.warningCount} aviso(s)
        </div>
        <select
          value={filter}
          onChange={(event) => setFilter(event.target.value as IssueFilter)}
          className="rounded-md border border-border bg-background px-2 py-1 text-xs"
          aria-label="Filtrar pendências"
        >
          {ISSUE_FILTERS.map((option) => (
            <option key={option}>{option}</option>
          ))}
        </select>
      </div>

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-28">Tipo</TableHead>
              <TableHead>O que precisa de atenção</TableHead>
              <TableHead className="w-28">Onde</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visible.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} className="px-3 py-3 text-sm text-muted-foreground">
                  Nenhuma pendência para este filtro.
                </TableCell>
              </TableRow>
            ) : (
              visible.map((issue, index) => (
                <TableRow key={`${issue.severity}-${issue.nodeId ?? "geral"}-${from + index}`}>
                  <TableCell className="px-3 py-2">
                    <span
                      className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-medium ${
                        issue.severity === "blocker"
                          ? "border-danger/30 bg-danger/15 text-danger"
                          : "border-warning/30 bg-warning/15 text-warning-foreground"
                      }`}
                    >
                      {issue.severity === "blocker" ? "Bloqueio" : "Aviso"}
                    </span>
                  </TableCell>
                  <TableCell className="px-3 py-2 text-sm">{issue.message}</TableCell>
                  <TableCell className="px-3 py-2 text-xs text-muted-foreground">
                    {issue.nodeId ? (nodeLabelById.get(issue.nodeId) ?? "Etapa") : "Geral"}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <TablePager
        from={from}
        to={to}
        total={issues.length}
        page={safePage}
        totalPages={totalPages}
        onPage={setPage}
      />
    </div>
  );
}

function CriteriosPanel({ version }: { version: SimulationVersionDetailResponse }) {
  const matrix = useMemo(() => buildMatrix(version), [version]);
  const totalWeight = matrix.reduce((sum, item) => sum + item.peso, 0);

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Critério</TableHead>
            <TableHead>% da pontuação</TableHead>
            <TableHead>Avaliado em</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {matrix.length === 0 ? (
            <TableRow>
              <TableCell colSpan={3} className="px-3 py-3 text-sm text-muted-foreground">
                Nenhum critério configurado.
              </TableCell>
            </TableRow>
          ) : (
            matrix.map((item) => {
              const percentual = totalWeight > 0 ? Math.round((item.peso / totalWeight) * 100) : 0;
              return (
                <TableRow key={item.criterio}>
                  <TableCell className="font-medium">{item.criterio}</TableCell>
                  <TableCell title={`peso ${item.peso} de ${totalWeight}`}>
                    {percentual}% da pontuação
                  </TableCell>
                  <TableCell
                    className="text-muted-foreground"
                    title="Em quantas etapas da avaliação este critério é avaliado"
                  >
                    {item.cobertura === 1 ? "1 etapa" : `${item.cobertura} etapas`}
                  </TableCell>
                </TableRow>
              );
            })
          )}
        </TableBody>
      </Table>
    </div>
  );
}

const PATH_FILTERS = ["Todos", "Aprovados", "Reprovados"] as const;
type PathFilter = (typeof PATH_FILTERS)[number];

function CaminhosPanel({
  version,
  cutoff,
}: {
  version: SimulationVersionDetailResponse;
  cutoff: number;
}) {
  const [filter, setFilter] = useState<PathFilter>("Todos");
  const [page, setPage] = useState(0);
  const [expanded, setExpanded] = useState<string | null>(null);

  const matrix = useMemo(() => buildMatrix(version), [version]);
  const allPaths = useMemo(() => buildPaths(version, matrix), [version, matrix]);

  const paths = useMemo(() => {
    if (filter === "Aprovados") return allPaths.filter((path) => path.total >= cutoff);
    if (filter === "Reprovados") return allPaths.filter((path) => path.total < cutoff);
    return allPaths;
  }, [allPaths, filter, cutoff]);

  useEffect(() => {
    setPage(0);
    setExpanded(null);
  }, [filter]);

  const perPage = 5;
  const totalPages = Math.max(1, Math.ceil(paths.length / perPage));
  const safePage = Math.min(page, totalPages - 1);
  const from = safePage * perPage;
  const to = Math.min(paths.length, from + perPage);
  const visible = paths.slice(from, to);

  if (allPaths.length === 0) {
    return <PanelSkeleton label="Sem caminhos mapeados para esta versão." />;
  }

  return (
    <div className="rounded-lg border border-border">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-3 py-2">
        <div className="text-xs text-muted-foreground">
          {allPaths.length} caminho(s) · nota de corte{" "}
          <span className="font-semibold text-foreground">{cutoff}/100</span>
        </div>
        <select
          value={filter}
          onChange={(event) => setFilter(event.target.value as PathFilter)}
          className="rounded-md border border-border bg-background px-2 py-1 text-xs"
          aria-label="Filtrar caminhos"
        >
          {PATH_FILTERS.map((option) => (
            <option key={option}>{option}</option>
          ))}
        </select>
      </div>

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Caminho</TableHead>
              <TableHead className="w-32">Resultado</TableHead>
              <TableHead className="w-24">Nota</TableHead>
              <TableHead className="w-28 text-right">Detalhe</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visible.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} className="px-3 py-3 text-sm text-muted-foreground">
                  Nenhum caminho para este filtro.
                </TableCell>
              </TableRow>
            ) : (
              visible.map((path, index) => {
                const isPass = path.total >= cutoff;
                const globalIndex = from + index;
                const isExpanded = expanded === path.sequence;
                return (
                  <Fragment key={path.sequence}>
                    <TableRow>
                      <TableCell className="px-3 py-2 text-xs text-muted-foreground">
                        {path.steps.map((step) => `Etapa ${step.turnIndex}`).join(" → ")} → fim
                      </TableCell>
                      <TableCell className="px-3 py-2">
                        <span
                          className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-medium ${
                            isPass ? "bg-success/15 text-success" : "bg-danger/15 text-danger"
                          }`}
                        >
                          {isPass ? "Aprovado" : "Reprovado"}
                        </span>
                      </TableCell>
                      <TableCell className="px-3 py-2 font-mono text-xs font-semibold">
                        {path.total}/100
                      </TableCell>
                      <TableCell className="px-3 py-2 text-right">
                        <button
                          type="button"
                          aria-expanded={isExpanded}
                          onClick={() => setExpanded(isExpanded ? null : path.sequence)}
                          className="rounded-md border border-border px-2 py-0.5 text-xs text-muted-foreground hover:bg-accent hover:text-foreground"
                        >
                          {isExpanded ? "Ocultar" : `Caminho #${globalIndex + 1}`}
                        </button>
                      </TableCell>
                    </TableRow>
                    {isExpanded ? (
                      <TableRow>
                        <TableCell colSpan={4} className="bg-muted/30 px-3 py-3">
                          <PathBreakdown path={path} matrix={matrix} />
                        </TableCell>
                      </TableRow>
                    ) : null}
                  </Fragment>
                );
              })
            )}
          </TableBody>
        </Table>
      </div>

      <TablePager
        from={from}
        to={to}
        total={paths.length}
        page={safePage}
        totalPages={totalPages}
        onPage={setPage}
      />
    </div>
  );
}

function PathBreakdown({ path, matrix }: { path: PathCandidate; matrix: MatrixItem[] }) {
  return (
    <div className="space-y-3">
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        {matrix.map((item) => {
          const gained = Math.round(path.byCriteria[item.criterio] ?? 0);
          return (
            <div key={item.criterio} className="rounded-md border border-border bg-background p-2">
              <div className="truncate text-[11px] text-muted-foreground">{item.criterio}</div>
              <div className="font-mono text-sm">
                {gained}
                <span className="text-muted-foreground"> pts</span>
              </div>
            </div>
          );
        })}
      </div>
      <p className="text-[11px] text-muted-foreground">
        Os pontos por critério somam a nota do caminho, limitada a 100.
      </p>
      <PathAttemptDetails path={path} />
    </div>
  );
}

function PathAttemptDetails({ path }: { path: PathCandidate }) {
  return (
    <div className="space-y-2 border-t border-border pt-3">
      <div className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
        Passo a passo do caminho
      </div>
      {path.steps.map((step, index) => (
        <div
          key={`${step.nodeId}:${step.optionId}:${index}`}
          className="rounded-md bg-background p-2"
        >
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
            <span className="text-muted-foreground">Etapa {step.turnIndex}</span>
            <span className="text-muted-foreground">
              {step.nextNodeId ? "leva à próxima etapa" : "encerra a avaliação"}
            </span>
          </div>
          <div className="mt-2 text-xs">
            <div className="font-medium">{step.speaker || "Atendente"}</div>
            <div className="mt-0.5 text-muted-foreground">{step.clientMessage}</div>
          </div>
          <div className="mt-2 rounded-md border border-border bg-background p-2 text-xs">
            {step.optionText}
          </div>
          <div className="mt-2 flex flex-wrap gap-1">
            {Object.entries(step.competencyLevels).map(([criterion, score]) => (
              <span
                key={criterion}
                className="rounded-md border border-border bg-background px-2 py-0.5 text-[11px] text-muted-foreground"
              >
                {criterion}: <span className="font-mono text-foreground">{score}</span>
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

export function AuditoriaPanel({
  events,
  loading,
}: {
  events: AuditEventResponse[];
  loading?: boolean;
}) {
  const [page, setPage] = useState(0);
  const perPage = 5;
  const totalPages = Math.max(1, Math.ceil(events.length / perPage));
  const safePage = Math.min(page, totalPages - 1);
  const from = safePage * perPage;
  const to = Math.min(events.length, from + perPage);
  const visible = events.slice(from, to);

  if (loading) {
    return <PanelSkeleton label="Carregando trilha de auditoria..." />;
  }

  return (
    <div className="rounded-lg border border-border">
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
                <TableCell colSpan={3} className="px-3 py-3 text-sm text-muted-foreground">
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
                  <TableCell className="px-3 py-2">{formatAuditMessage(event.message)}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <TablePager
        from={from}
        to={to}
        total={events.length}
        page={safePage}
        totalPages={totalPages}
        onPage={setPage}
      />
    </div>
  );
}

function TablePager({
  from,
  to,
  total,
  page,
  totalPages,
  onPage,
}: {
  from: number;
  to: number;
  total: number;
  page: number;
  totalPages: number;
  onPage: (page: number) => void;
}) {
  return (
    <div className="flex items-center justify-between border-t border-border px-3 py-2 text-xs text-muted-foreground">
      <span>{total === 0 ? "0–0 de 0" : `${from + 1}–${to} de ${total}`}</span>
      <div className="flex items-center gap-2">
        <span className="hidden sm:inline">
          Página {page + 1} de {totalPages}
        </span>
        <div className="flex gap-1">
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => onPage(Math.max(0, page - 1))}
            disabled={page === 0}
          >
            Anterior
          </button>
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => onPage(Math.min(totalPages - 1, page + 1))}
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

const MAX_PATHS = 50;

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
    steps: PathStep[],
    depth: number,
  ) => {
    if (paths.length >= MAX_PATHS) return;
    if (depth > version.nodes.length + 5) return;

    if (node.options.length === 0) {
      paths.push({
        sequence: `${sequence.join(" → ")} → Encerramento`,
        total: calculateTotal(byCriteria, matrix),
        byCriteria: { ...byCriteria },
        steps,
      });
      return;
    }

    for (const option of node.options) {
      if (paths.length >= MAX_PATHS) return;

      const nextSequence = [...sequence, `${node.id}·${option.id}`];
      const nextCriteria: Record<string, number> = { ...byCriteria };
      const nextSteps = [
        ...steps,
        {
          nodeId: node.id,
          optionId: option.id,
          turnIndex: node.turnIndex,
          speaker: node.speaker,
          clientMessage: node.clientMessage,
          optionText: option.text,
          nextNodeId: option.nextNodeId,
          competencyLevels: option.competencyLevels,
        },
      ];

      Object.entries(option.competencyLevels).forEach(([criterio, pontos]) => {
        nextCriteria[criterio] = (nextCriteria[criterio] ?? 0) + pontos;
      });

      if (option.nextNodeId) {
        const nextNode = byId.get(option.nextNodeId);
        if (nextNode) {
          walk(nextNode, nextSequence, nextCriteria, nextSteps, depth + 1);
          continue;
        }
      }

      paths.push({
        sequence: `${nextSequence.join(" → ")} → Encerramento`,
        total: calculateTotal(nextCriteria, matrix),
        byCriteria: nextCriteria,
        steps: nextSteps,
      });
    }
  };

  walk(start, [], {}, [], 0);
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

function formatAuditMessage(message: string) {
  return message
    .replace("Nó de simulação adicionado.", "Etapa adicionada.")
    .replace("Nó de simulação atualizado.", "Etapa atualizada.")
    .replace("Nó de simulação removido.", "Etapa removida.")
    .replaceAll("simulação", "avaliação")
    .replaceAll("Simulação", "Avaliação")
    .replaceAll("nó", "etapa")
    .replaceAll("Nó", "Etapa");
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
