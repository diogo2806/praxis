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
import { buildStepLabels, localizeStepIds, stepLabelOf } from "@/lib/step-labels";

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

const STATUS_BADGE_CLASS: Record<SimulationSummaryResponse["status"], string> = {
  draft: "bg-warning/15 text-warning-foreground border-warning/30",
  published: "bg-success/15 text-success border-success/30",
  archived: "bg-danger/15 text-danger border-danger/30",
};

const mapSearchStatusToQuery = (status: string): SimulationSummaryResponse["status"] | null =>
  status === "publicado"
    ? "published"
    : status === "bloqueado"
      ? "archived"
      : status === "em-revisao"
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
  const [status, setStatus] = useState<string>("all");
  const [page, setPage] = useState(0);

  const STATUS_OPTIONS = [
    { value: "all", label: t.compliance.statusAll },
    { value: "rascunho", label: t.compliance.statusDraft },
    { value: "em-revisao", label: t.compliance.statusInReview },
    { value: "bloqueado", label: t.compliance.statusBlocked },
    { value: "publicado", label: t.compliance.statusPublished },
  ];
  const STATUS_TEXT: Record<SimulationSummaryResponse["status"], string> = {
    draft: t.compliance.statusInReview,
    published: t.compliance.statusPublished,
    archived: t.compliance.statusBlocked,
  };

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
            {t.compliance.introBeforeDetails}
            <span className="font-medium text-foreground">{t.compliance.detailsAction}</span>
            {t.compliance.introAfterDetails}
          </p>
        </div>

        <section className="rounded-xl border border-border bg-card">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-4 py-3">
            <div>
              <div className="text-sm font-semibold">{t.compliance.versionsHeading}</div>
              <div className="text-xs text-muted-foreground">
                {rows.length === 0
                  ? t.compliance.noVersionsForFilters
                  : (rows.length === 1
                      ? t.compliance.versionsListedSingular
                      : t.compliance.versionsListedPlural
                    ).replace("{count}", String(rows.length))}
              </div>
            </div>
            <div className="flex items-center gap-2">
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={t.compliance.searchPlaceholder}
                className="rounded-md border border-border bg-background px-2.5 py-1.5 text-xs outline-none focus:ring-1 focus:ring-ring"
              />
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                className="rounded-md border border-border bg-background px-2 py-1.5 text-xs"
              >
                {STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t.compliance.evaluationHeader}</TableHead>
                  <TableHead>{t.compliance.versionHeader}</TableHead>
                  <TableHead>{t.common.status}</TableHead>
                  <TableHead>{t.compliance.completionRateHeader}</TableHead>
                  <TableHead>{t.compliance.blockers}</TableHead>
                  <TableHead>{t.compliance.attemptsHeader}</TableHead>
                  <TableHead>{t.compliance.updatedHeader}</TableHead>
                  <TableHead className="text-right">{t.compliance.actionsHeader}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {simulationsQuery.isLoading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="p-4 text-sm text-muted-foreground">
                      {t.compliance.loadingVersions}
                    </TableCell>
                  </TableRow>
                ) : simulationsQuery.isError ? (
                  <TableRow>
                    <TableCell colSpan={8} className="p-4">
                      <StateBanner tone="danger" title={t.compliance.versionsLoadError}>
                        {simulationsQuery.error instanceof Error
                          ? simulationsQuery.error.message
                          : t.compliance.tryAgainLater}
                      </StateBanner>
                    </TableCell>
                  </TableRow>
                ) : rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="p-4 text-sm text-muted-foreground">
                      {t.compliance.noEvaluationsFound}
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
                            {t.compliance.scoreOutOf100.replace(
                              "{value}",
                              String(row.completionRate),
                            )}
                          </span>
                        </TableCell>
                        <TableCell className="px-4 py-2.5 text-sm text-muted-foreground">
                          {t.compliance.noData}
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
                            {t.compliance.detailsAction}
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
  const { t } = useLanguage();
  const versionTitle = row ? `${row.name} · v${row.versionNumber}` : t.compliance.versionFallback;

  return (
    <Dialog open={open} onOpenChange={(value) => !value && onClose()}>
      <DialogContent className="max-h-[88vh] w-[calc(100vw-1.5rem)] max-w-3xl gap-0 overflow-y-auto p-0">
        <DialogHeader className="space-y-1 border-b border-border px-5 py-4 text-left">
          <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
            {versionTitle}
          </div>
          <DialogTitle className="font-serif text-xl">
            {t.compliance.versionDetailsTitle}
          </DialogTitle>
          <DialogDescription>{t.compliance.versionDetailsDescription}</DialogDescription>
        </DialogHeader>

        <div className="space-y-8 p-5">
          {hasError ? (
            <StateBanner tone="danger" title={t.compliance.versionDetailsLoadError}>
              {errorMessage ?? t.compliance.tryAgainLater}
            </StateBanner>
          ) : null}

          <ResumoConformidade validation={validation} loading={loading} />

          <DialogSection
            title={t.compliance.pendingTitle}
            description={t.compliance.pendingDescription}
          >
            <IssuesPanel validation={validation} version={version} loading={loading} />
          </DialogSection>

          <DialogSection
            title={t.compliance.howEvaluatedTitle}
            description={t.compliance.howEvaluatedDescription}
          >
            {loading || !version ? (
              <PanelSkeleton label={t.compliance.loadingCriteria} />
            ) : (
              <CriteriosPanel version={version} />
            )}
          </DialogSection>

          <DialogSection
            title={t.compliance.pathsTitle}
            description={t.compliance.pathsDescription.replace("{cutoff}", String(CORTA))}
          >
            {loading || !version ? (
              <PanelSkeleton label={t.compliance.loadingPaths} />
            ) : (
              <CaminhosPanel version={version} cutoff={CORTA} />
            )}
          </DialogSection>

          <DialogSection
            title={t.compliance.auditTrailTitle}
            description={t.compliance.auditTrailDescription}
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
  const { t } = useLanguage();
  if (loading || !validation) {
    return <PanelSkeleton label={t.compliance.loadingSummary} />;
  }

  const cards = [
    {
      label: t.compliance.canPublishLabel,
      value: validation.publishable ? t.compliance.canPublishYes : t.compliance.canPublishNo,
      hint: validation.publishable ? t.compliance.canPublishHintYes : t.compliance.canPublishHintNo,
      Icon: validation.publishable ? ShieldCheck : ShieldAlert,
      tone: validation.publishable ? "ok" : "danger",
    },
    {
      label: t.compliance.blockers,
      value: String(validation.blockerCount),
      hint: t.compliance.blockersHint,
      Icon: ShieldAlert,
      tone: validation.blockerCount > 0 ? "danger" : "ok",
    },
    {
      label: t.compliance.warnings,
      value: String(validation.warningCount),
      hint: t.compliance.warningsHint,
      Icon: AlertTriangle,
      tone: validation.warningCount > 0 ? "warn" : "ok",
    },
    {
      label: t.compliance.qualityLabel,
      value: t.compliance.scoreOutOf100.replace(
        "{value}",
        String(Math.round(validation.qualityScore)),
      ),
      hint: t.compliance.qualityHint,
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

function IssuesPanel({
  validation,
  version,
  loading,
}: {
  validation: SimulationValidationResponse | null;
  version: SimulationVersionDetailResponse | null;
  loading: boolean;
}) {
  const { t } = useLanguage();
  const [filter, setFilter] = useState<string>("all");
  const [page, setPage] = useState(0);

  const ISSUE_FILTERS = [
    { value: "all", label: t.compliance.filterAll },
    { value: "blockers", label: t.compliance.blockers },
    { value: "warnings", label: t.compliance.warnings },
  ];

  // Mesmos rótulos do mapa do validador (1.0, 5.0…); o id interno (turno-N) não aparece.
  const stepLabels = useMemo(() => buildStepLabels(version?.nodes ?? []), [version]);
  const issueLocation = (nodeId: string | null) => {
    if (!nodeId) return t.compliance.general;
    const label = stepLabelOf(stepLabels, nodeId);
    return label === nodeId ? label : t.compliance.stepLabel.replace("{value}", label);
  };

  const issues = useMemo<ValidationIssueResponse[]>(() => {
    const all = validation?.issues ?? [];
    if (filter === "blockers") return all.filter((issue) => issue.severity === "blocker");
    if (filter === "warnings") return all.filter((issue) => issue.severity === "warning");
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
    return <PanelSkeleton label={t.compliance.loadingPending} />;
  }

  if ((validation.issues ?? []).length === 0) {
    return (
      <StateBanner tone="ok" title={t.compliance.noPendingTitle}>
        {t.compliance.noPendingDescription}
      </StateBanner>
    );
  }

  return (
    <div className="rounded-lg border border-border">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-3 py-2">
        <div className="text-xs text-muted-foreground">
          {t.compliance.blockerWarningCount
            .replace("{blockers}", String(validation.blockerCount))
            .replace("{warnings}", String(validation.warningCount))}
        </div>
        <select
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
          className="rounded-md border border-border bg-background px-2 py-1 text-xs"
          aria-label={t.compliance.filterPendingAria}
        >
          {ISSUE_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-28">{t.compliance.typeHeader}</TableHead>
              <TableHead>{t.compliance.needsAttentionHeader}</TableHead>
              <TableHead className="w-28">{t.compliance.whereHeader}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visible.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} className="px-3 py-3 text-sm text-muted-foreground">
                  {t.compliance.noPendingForFilter}
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
                      {issue.severity === "blocker"
                        ? t.compliance.blockerBadge
                        : t.compliance.warningBadge}
                    </span>
                  </TableCell>
                  <TableCell className="px-3 py-2 text-sm">
                    {localizeStepIds(issue.message, stepLabels)}
                  </TableCell>
                  <TableCell className="px-3 py-2 text-xs text-muted-foreground">
                    {issueLocation(issue.nodeId)}
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
  const { t } = useLanguage();
  const matrix = useMemo(() => buildMatrix(version), [version]);
  const totalWeight = matrix.reduce((sum, item) => sum + item.peso, 0);

  return (
    <div className="overflow-x-auto rounded-lg border border-border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t.compliance.criterionHeader}</TableHead>
            <TableHead>{t.compliance.scorePercentHeader}</TableHead>
            <TableHead>{t.compliance.evaluatedInHeader}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {matrix.length === 0 ? (
            <TableRow>
              <TableCell colSpan={3} className="px-3 py-3 text-sm text-muted-foreground">
                {t.compliance.noCriteriaConfigured}
              </TableCell>
            </TableRow>
          ) : (
            matrix.map((item) => {
              const percentual = totalWeight > 0 ? Math.round((item.peso / totalWeight) * 100) : 0;
              return (
                <TableRow key={item.criterio}>
                  <TableCell className="font-medium">{item.criterio}</TableCell>
                  <TableCell
                    title={t.compliance.weightTooltip
                      .replace("{weight}", String(item.peso))
                      .replace("{total}", String(totalWeight))}
                  >
                    {t.compliance.percentOfScore.replace("{percent}", String(percentual))}
                  </TableCell>
                  <TableCell
                    className="text-muted-foreground"
                    title={t.compliance.evaluatedInTooltip}
                  >
                    {item.cobertura === 1
                      ? t.compliance.stepCountSingular
                      : t.compliance.stepCountPlural.replace("{count}", String(item.cobertura))}
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

function CaminhosPanel({
  version,
  cutoff,
}: {
  version: SimulationVersionDetailResponse;
  cutoff: number;
}) {
  const { t } = useLanguage();
  const [filter, setFilter] = useState<string>("all");
  const [page, setPage] = useState(0);
  const [expanded, setExpanded] = useState<string | null>(null);

  const PATH_FILTERS = [
    { value: "all", label: t.compliance.pathFilterAll },
    { value: "approved", label: t.compliance.pathFilterApproved },
    { value: "failed", label: t.compliance.pathFilterFailed },
  ];

  const matrix = useMemo(() => buildMatrix(version), [version]);
  const allPaths = useMemo(() => buildPaths(version, matrix), [version, matrix]);

  const paths = useMemo(() => {
    if (filter === "approved") return allPaths.filter((path) => path.total >= cutoff);
    if (filter === "failed") return allPaths.filter((path) => path.total < cutoff);
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
    return <PanelSkeleton label={t.compliance.noPathsMapped} />;
  }

  return (
    <div className="rounded-lg border border-border">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-3 py-2">
        <div className="text-xs text-muted-foreground">
          {t.compliance.pathsCountCutoff.replace("{count}", String(allPaths.length))}{" "}
          <span className="font-semibold text-foreground">
            {t.compliance.scoreOutOf100.replace("{value}", String(cutoff))}
          </span>
        </div>
        <select
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
          className="rounded-md border border-border bg-background px-2 py-1 text-xs"
          aria-label={t.compliance.filterPathsAria}
        >
          {PATH_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t.compliance.pathHeader}</TableHead>
              <TableHead className="w-32">{t.compliance.resultHeader}</TableHead>
              <TableHead className="w-24">{t.compliance.scoreHeader}</TableHead>
              <TableHead className="w-28 text-right">{t.compliance.detailHeader}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visible.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} className="px-3 py-3 text-sm text-muted-foreground">
                  {t.compliance.noPathsForFilter}
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
                        {path.steps
                          .map((step) =>
                            t.compliance.stepLabel.replace("{value}", String(step.turnIndex)),
                          )
                          .join(" → ")}
                        {" → "}
                        {t.compliance.pathEnd}
                      </TableCell>
                      <TableCell className="px-3 py-2">
                        <span
                          className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-medium ${
                            isPass ? "bg-success/15 text-success" : "bg-danger/15 text-danger"
                          }`}
                        >
                          {isPass ? t.compliance.approved : t.compliance.failed}
                        </span>
                      </TableCell>
                      <TableCell className="px-3 py-2 font-mono text-xs font-semibold">
                        {t.compliance.scoreOutOf100.replace("{value}", String(path.total))}
                      </TableCell>
                      <TableCell className="px-3 py-2 text-right">
                        <button
                          type="button"
                          aria-expanded={isExpanded}
                          onClick={() => setExpanded(isExpanded ? null : path.sequence)}
                          className="rounded-md border border-border px-2 py-0.5 text-xs text-muted-foreground hover:bg-accent hover:text-foreground"
                        >
                          {isExpanded
                            ? t.compliance.hide
                            : t.compliance.pathNumber.replace("{n}", String(globalIndex + 1))}
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
  const { t } = useLanguage();
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
                <span className="text-muted-foreground">{t.compliance.pointsSuffix}</span>
              </div>
            </div>
          );
        })}
      </div>
      <p className="text-[11px] text-muted-foreground">{t.compliance.pointsSumNote}</p>
      <PathAttemptDetails path={path} />
    </div>
  );
}

function PathAttemptDetails({ path }: { path: PathCandidate }) {
  const { t } = useLanguage();
  return (
    <div className="space-y-2 border-t border-border pt-3">
      <div className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
        {t.compliance.stepByStepTitle}
      </div>
      {path.steps.map((step, index) => (
        <div
          key={`${step.nodeId}:${step.optionId}:${index}`}
          className="rounded-md bg-background p-2"
        >
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
            <span className="text-muted-foreground">
              {t.compliance.stepLabel.replace("{value}", String(step.turnIndex))}
            </span>
            <span className="text-muted-foreground">
              {step.nextNodeId ? t.compliance.leadsToNextStep : t.compliance.endsEvaluation}
            </span>
          </div>
          <div className="mt-2 text-xs">
            <div className="font-medium">{step.speaker || t.compliance.attendant}</div>
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
  const { t } = useLanguage();
  const [page, setPage] = useState(0);
  const perPage = 5;
  const totalPages = Math.max(1, Math.ceil(events.length / perPage));
  const safePage = Math.min(page, totalPages - 1);
  const from = safePage * perPage;
  const to = Math.min(events.length, from + perPage);
  const visible = events.slice(from, to);

  if (loading) {
    return <PanelSkeleton label={t.compliance.loadingAuditTrail} />;
  }

  return (
    <div className="rounded-lg border border-border">
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t.compliance.whenHeader}</TableHead>
              <TableHead>{t.compliance.whoHeader}</TableHead>
              <TableHead>{t.compliance.eventHeader}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visible.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} className="px-3 py-3 text-sm text-muted-foreground">
                  {t.compliance.noAuditEvents}
                </TableCell>
              </TableRow>
            ) : (
              visible.map((event) => (
                <TableRow key={event.id}>
                  <TableCell className="whitespace-nowrap px-3 py-2 text-xs text-muted-foreground">
                    {formatDate(event.createdAt)}
                  </TableCell>
                  <TableCell className="whitespace-nowrap px-3 py-2 text-xs">
                    {parseWho(event.metadata, t.compliance.systemActor)}
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
  const { t } = useLanguage();
  return (
    <div className="flex items-center justify-between border-t border-border px-3 py-2 text-xs text-muted-foreground">
      <span>
        {total === 0
          ? t.compliance.pagerEmpty
          : t.compliance.pagerRange
              .replace("{from}", String(from + 1))
              .replace("{to}", String(to))
              .replace("{total}", String(total))}
      </span>
      <div className="flex items-center gap-2">
        <span className="hidden sm:inline">
          {t.compliance.pagerPage
            .replace("{page}", String(page + 1))
            .replace("{total}", String(totalPages))}
        </span>
        <div className="flex gap-1">
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => onPage(Math.max(0, page - 1))}
            disabled={page === 0}
          >
            {t.compliance.previous}
          </button>
          <button
            type="button"
            className="rounded-md border border-border px-2 py-1 hover:bg-accent disabled:cursor-not-allowed disabled:opacity-40"
            onClick={() => onPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
          >
            {t.compliance.next}
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

function parseWho(metadata: string | null | undefined, fallback: string) {
  if (!metadata) return fallback;
  try {
    const value = JSON.parse(metadata) as { actor?: string; who?: string };
    if (typeof value === "object" && value !== null) {
      if (typeof value.actor === "string") return value.actor;
      if (typeof value.who === "string") return value.who;
    }
  } catch {
    return fallback;
  }
  return fallback;
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
