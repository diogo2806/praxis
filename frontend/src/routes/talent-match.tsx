import { useMutation, useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { BarChart3, Check, CircleAlert, EyeOff, FileText, Target, UsersRound } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
} from "recharts";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getEvidenceReport,
  getSimulationVersion,
  getTalentMatch,
  listCandidateLinks,
  registerCandidateDisposition,
  type CandidateLinkResponse,
  type CandidateRadarDto,
  type CompetencyBenchmarkDto,
  type HumanDecision,
  type TalentMatchResponse,
} from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/talent-match")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number" && Number.isFinite(search.versionNumber)
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Comparar resultados - Práxis" },
      {
        name: "description",
        content:
          "Compare participações concluídas dentro do contexto de uma avaliação e versão específicas.",
      },
    ],
  }),
  component: TalentMatchPage,
});

const MAX_SELECTED = 5;
const PAGE_SIZE = 10;
const BLIND_MODE_STORAGE_KEY = "praxis.talent-match.blindMode";
const palette = ["#0f766e", "#b45309", "#2563eb", "#be123c", "#6d28d9"];

const contextualCopy = {
  "pt-BR": {
    contextMissingTitle: "Abra uma avaliação para comparar resultados",
    contextMissingBody:
      "Talent Match não mantém uma segunda lista global. Abra a comparação pela avaliação desejada ou pelos resultados filtrados.",
    openAssessments: "Abrir avaliações",
    openResults: "Abrir resultados",
    backToAssessment: "Voltar para avaliações",
    assessmentContext: "Contexto da comparação",
    completedOnly: "Somente participações concluídas desta avaliação e versão são carregadas.",
    loadCandidatesError: "Não foi possível carregar as participações desta versão",
    searchPlaceholder: "Buscar participante neste contexto",
    noSearchResults: "Nenhuma participação corresponde à busca.",
    previous: "Anterior",
    next: "Próxima",
    page: "Página",
    of: "de",
    viewResult: "Abrir resultado",
  },
  en: {
    contextMissingTitle: "Open an assessment to compare results",
    contextMissingBody:
      "Talent Match does not keep a second global list. Open the comparison from the selected assessment or filtered results.",
    openAssessments: "Open assessments",
    openResults: "Open results",
    backToAssessment: "Back to assessments",
    assessmentContext: "Comparison context",
    completedOnly: "Only completed participations from this assessment and version are loaded.",
    loadCandidatesError: "Could not load participations for this version",
    searchPlaceholder: "Search participants in this context",
    noSearchResults: "No participation matches the search.",
    previous: "Previous",
    next: "Next",
    page: "Page",
    of: "of",
    viewResult: "Open result",
  },
  "es-MX": {
    contextMissingTitle: "Abra una evaluación para comparar resultados",
    contextMissingBody:
      "Talent Match no mantiene una segunda lista global. Abra la comparación desde la evaluación elegida o desde resultados filtrados.",
    openAssessments: "Abrir evaluaciones",
    openResults: "Abrir resultados",
    backToAssessment: "Volver a evaluaciones",
    assessmentContext: "Contexto de comparación",
    completedOnly: "Solo se cargan participaciones concluidas de esta evaluación y versión.",
    loadCandidatesError: "No se pudieron cargar las participaciones de esta versión",
    searchPlaceholder: "Buscar participante en este contexto",
    noSearchResults: "Ninguna participación coincide con la búsqueda.",
    previous: "Anterior",
    next: "Siguiente",
    page: "Página",
    of: "de",
    viewResult: "Abrir resultado",
  },
} as const;

function TalentMatchPage() {
  const { language, t } = useLanguage();
  const copy = contextualCopy[language];
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const [selectedAttemptIds, setSelectedAttemptIds] = useState<string[]>([]);
  const [candidateSearch, setCandidateSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [blindMode, setBlindMode] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    return window.localStorage.getItem(BLIND_MODE_STORAGE_KEY) === "true";
  });

  useEffect(() => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(BLIND_MODE_STORAGE_KEY, String(blindMode));
    }
  }, [blindMode]);

  useEffect(() => {
    setSelectedAttemptIds([]);
    setCandidateSearch("");
    setCurrentPage(1);
  }, [search.simulationId, search.versionNumber, blindMode]);

  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
    retry: false,
  });

  const candidateLinksQuery = useQuery({
    queryKey: [
      "candidate-links",
      "talent-match-context",
      search.simulationId,
      search.versionNumber,
      blindMode,
    ],
    queryFn: () =>
      listCandidateLinks(blindMode, {
        simulationId: search.simulationId,
        versionNumber: search.versionNumber,
        status: "completed",
      }),
    enabled: hasContext,
    retry: false,
  });

  const talentMatchQuery = useQuery({
    queryKey: [
      "talent-match",
      search.simulationId,
      search.versionNumber,
      selectedAttemptIds,
      blindMode,
    ],
    queryFn: () =>
      getTalentMatch(search.simulationId!, search.versionNumber!, selectedAttemptIds, blindMode),
    enabled: hasContext && selectedAttemptIds.length > 0,
    retry: false,
  });

  const completedCandidates = candidateLinksQuery.data ?? [];
  const filteredCandidates = useMemo(() => {
    const normalized = candidateSearch.trim().toLocaleLowerCase(language);
    if (!normalized) return completedCandidates;
    return completedCandidates.filter((candidate) =>
      `${candidate.candidateName} ${candidate.candidateEmail ?? ""}`
        .toLocaleLowerCase(language)
        .includes(normalized),
    );
  }, [candidateSearch, completedCandidates, language]);

  const totalPages = Math.max(1, Math.ceil(filteredCandidates.length / PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages);
  const visibleCandidates = filteredCandidates.slice(
    (safePage - 1) * PAGE_SIZE,
    safePage * PAGE_SIZE,
  );
  const selectedVersion = versionQuery.data;
  const isVersionPublished = selectedVersion?.status === "published";
  const benchmark =
    talentMatchQuery.data?.benchmark ??
    selectedVersion?.blueprint.competencies.map((competency) => ({
      competencyName: competency.name,
      targetScore: competency.targetScore ?? 70,
    })) ??
    [];
  const selectedCandidateRows = talentMatchQuery.data?.candidates ?? [];
  const selectedLimitReached = selectedAttemptIds.length >= MAX_SELECTED;

  useEffect(() => {
    if (currentPage !== safePage) setCurrentPage(safePage);
  }, [currentPage, safePage]);

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-5">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="text-xs uppercase text-primary">{t.common.talentMatch}</div>
            <h1 className="mt-1 text-3xl font-semibold">{t.talentMatchPage.pageTitle}</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
              {t.talentMatchPage.pageIntro}
            </p>
          </div>
          <Button asChild variant="outline" className="bg-card">
            <Link to="/avaliacoes">{copy.backToAssessment}</Link>
          </Button>
        </header>

        {!hasContext ? (
          <EmptyState
            title={copy.contextMissingTitle}
            description={copy.contextMissingBody}
            actions={
              <div className="flex flex-wrap justify-center gap-3">
                <Button asChild>
                  <Link to="/avaliacoes">{copy.openAssessments}</Link>
                </Button>
                <Button asChild variant="outline">
                  <Link
                    to="/results"
                    search={{
                      search: "",
                      simulationId: "",
                      period: "",
                      integrationProvider: "",
                      page: 0,
                    }}
                  >
                    {copy.openResults}
                  </Link>
                </Button>
              </div>
            }
          />
        ) : (
          <div className="space-y-5">
            <section className="rounded-xl border border-border bg-card p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-primary">
                    {copy.assessmentContext}
                  </div>
                  <h2 className="mt-1 text-xl font-semibold">
                    {selectedVersion?.name ?? search.simulationId}
                  </h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Versão {search.versionNumber} · {copy.completedOnly}
                  </p>
                </div>
                {selectedVersion && (
                  <StatusBadge status={selectedVersion.status} variant="status" />
                )}
              </div>
            </section>

            <StateBanner tone="info" title={t.talentMatchPage.evidenceBannerTitle}>
              {t.talentMatchPage.evidenceBannerBody}
            </StateBanner>

            {versionQuery.isError && (
              <StateBanner tone="danger" title={t.talentMatchPage.versionUnavailableTitle}>
                {versionQuery.error instanceof Error
                  ? versionQuery.error.message
                  : t.talentMatchPage.versionUnavailableBody}
              </StateBanner>
            )}

            {!versionQuery.isLoading && selectedVersion && !isVersionPublished && (
              <StateBanner tone="warn" title={t.talentMatchPage.versionUnavailableTitle}>
                {t.talentMatchPage.versionUnavailableBody}
              </StateBanner>
            )}

            {candidateLinksQuery.isError && (
              <StateBanner tone="danger" title={copy.loadCandidatesError}>
                {candidateLinksQuery.error instanceof Error
                  ? candidateLinksQuery.error.message
                  : t.talentMatchPage.loadComparisonErrorFallback}
              </StateBanner>
            )}

            {selectedLimitReached && (
              <StateBanner tone="warn" title={t.talentMatchPage.visualLimitTitle}>
                {t.talentMatchPage.visualLimitBody}
              </StateBanner>
            )}

            {talentMatchQuery.isError && (
              <StateBanner tone="danger" title={t.talentMatchPage.loadComparisonErrorTitle}>
                {talentMatchQuery.error instanceof Error
                  ? talentMatchQuery.error.message
                  : t.talentMatchPage.loadComparisonErrorFallback}
              </StateBanner>
            )}

            <div className="grid gap-5 xl:grid-cols-[minmax(320px,400px)_minmax(0,1fr)]">
              <section className="rounded-xl border border-border bg-card p-5">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2 text-sm font-semibold">
                      <UsersRound className="h-4 w-4" />
                      {t.talentMatchPage.participantsHeading}
                    </div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      {t.talentMatchPage.selectedCount
                        .replace("{count}", String(selectedAttemptIds.length))
                        .replace("{max}", String(MAX_SELECTED))}
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setSelectedAttemptIds([])}
                    disabled={selectedAttemptIds.length === 0}
                    className="rounded-md border border-border bg-background px-3 py-1.5 text-xs hover:bg-accent disabled:opacity-50"
                  >
                    {t.talentMatchPage.clear}
                  </button>
                </div>

                <label className="mb-3 flex cursor-pointer items-start gap-2.5 rounded-md border border-border bg-background p-3">
                  <input
                    type="checkbox"
                    checked={blindMode}
                    onChange={(event) => setBlindMode(event.target.checked)}
                    className="mt-0.5 h-4 w-4 shrink-0"
                  />
                  <span className="min-w-0">
                    <span className="flex items-center gap-1.5 text-xs font-medium">
                      <EyeOff className="h-3.5 w-3.5" />
                      {t.talentMatchPage.blindMode}
                    </span>
                    <span className="mt-0.5 block text-[11px] text-muted-foreground">
                      {t.talentMatchPage.blindModeDescription}
                    </span>
                  </span>
                </label>

                <input
                  type="search"
                  value={candidateSearch}
                  onChange={(event) => {
                    setCandidateSearch(event.target.value);
                    setCurrentPage(1);
                  }}
                  placeholder={copy.searchPlaceholder}
                  className="input mb-3 w-full"
                />

                <CandidateSelector
                  candidates={visibleCandidates}
                  loading={candidateLinksQuery.isLoading}
                  noResults={filteredCandidates.length === 0}
                  noResultsLabel={
                    candidateSearch.trim()
                      ? copy.noSearchResults
                      : t.talentMatchPage.noCompletedForAssessment
                  }
                  selectedAttemptIds={selectedAttemptIds}
                  onToggle={(attemptId) => {
                    setSelectedAttemptIds((current) => {
                      if (current.includes(attemptId)) {
                        return current.filter((id) => id !== attemptId);
                      }
                      if (current.length >= MAX_SELECTED) return current;
                      return [...current, attemptId];
                    });
                  }}
                />

                {filteredCandidates.length > PAGE_SIZE && (
                  <div className="mt-3 flex items-center justify-between gap-2 border-t border-border pt-3 text-xs">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={safePage === 1}
                      onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                    >
                      {copy.previous}
                    </Button>
                    <span className="text-muted-foreground">
                      {copy.page} {safePage} {copy.of} {totalPages}
                    </span>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={safePage === totalPages}
                      onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                    >
                      {copy.next}
                    </Button>
                  </div>
                )}
              </section>

              <section className="rounded-xl border border-border bg-card p-5">
                <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2 text-sm font-semibold">
                      <Target className="h-4 w-4" />
                      {t.talentMatchPage.radarVsBenchmark}
                    </div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      {t.talentMatchPage.simulationVersionLabel
                        .replace("{id}", String(search.simulationId))
                        .replace("{version}", String(search.versionNumber))}
                    </div>
                  </div>
                  <BenchmarkSummary benchmark={benchmark} />
                </div>
                <RadarComparisonChart
                  benchmark={benchmark}
                  data={talentMatchQuery.data}
                  loading={versionQuery.isLoading || talentMatchQuery.isFetching}
                />
              </section>
            </div>

            {selectedCandidateRows.length > 0 ? (
              <CandidateLegend
                candidates={selectedCandidateRows}
                viewResultLabel={copy.viewResult}
              />
            ) : (
              <StateBanner tone="info" title={t.talentMatchPage.pendingComparisonTitle}>
                {t.talentMatchPage.pendingComparisonBody}
              </StateBanner>
            )}
          </div>
        )}
      </main>
    </AppShell>
  );
}

function CandidateSelector({
  candidates,
  loading,
  noResults,
  noResultsLabel,
  selectedAttemptIds,
  onToggle,
}: {
  candidates: CandidateLinkResponse[];
  loading: boolean;
  noResults: boolean;
  noResultsLabel: string;
  selectedAttemptIds: string[];
  onToggle: (attemptId: string) => void;
}) {
  const { t } = useLanguage();
  if (loading) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm">
        {t.talentMatchPage.loading}
      </div>
    );
  }
  if (noResults) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        <CircleAlert className="mb-2 h-5 w-5" />
        {noResultsLabel}
      </div>
    );
  }

  const limitReached = selectedAttemptIds.length >= MAX_SELECTED;
  return (
    <div className="space-y-2">
      {candidates.map((candidate) => {
        const selected = selectedAttemptIds.includes(candidate.attemptId);
        const disabled = limitReached && !selected;
        return (
          <button
            key={candidate.attemptId}
            type="button"
            disabled={disabled}
            onClick={() => onToggle(candidate.attemptId)}
            className={cn(
              "flex w-full items-center gap-3 rounded-md border border-border bg-background p-3 text-left text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50",
              selected && "border-primary/50 bg-primary/10",
            )}
          >
            <span
              className={cn(
                "flex h-5 w-5 shrink-0 items-center justify-center rounded border border-border",
                selected && "border-primary bg-primary text-primary-foreground",
              )}
            >
              {selected && <Check className="h-3.5 w-3.5" />}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate font-medium">{candidate.candidateName}</span>
              {candidate.candidateEmail && (
                <span className="mt-0.5 block truncate text-xs text-muted-foreground">
                  {candidate.candidateEmail}
                </span>
              )}
            </span>
          </button>
        );
      })}
    </div>
  );
}

function RadarComparisonChart({
  benchmark,
  data,
  loading,
}: {
  benchmark: CompetencyBenchmarkDto[];
  data?: TalentMatchResponse;
  loading: boolean;
}) {
  const { t } = useLanguage();
  const chartData = benchmark.map((item) => {
    const row: Record<string, string | number> = {
      competency: item.competencyName,
      benchmark: item.targetScore,
    };
    data?.candidates.forEach((candidate) => {
      row[candidate.attemptId] =
        candidate.competencies.find((score) => score.competencyName === item.competencyName)
          ?.score ?? 0;
    });
    return row;
  });

  if (benchmark.length === 0) {
    return (
      <div className="flex min-h-[420px] items-center justify-center rounded-md border border-border bg-background text-sm text-muted-foreground">
        {t.talentMatchPage.benchmarkUnavailable}
      </div>
    );
  }

  return (
    <div className="min-h-[440px]">
      {loading && (
        <div className="mb-2 text-xs text-muted-foreground">{t.talentMatchPage.updatingRadar}</div>
      )}
      <ResponsiveContainer width="100%" height={420}>
        <RadarChart data={chartData} outerRadius="72%">
          <PolarGrid stroke="hsl(var(--border))" />
          <PolarAngleAxis
            dataKey="competency"
            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
          />
          <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fontSize: 10 }} />
          <RechartsTooltip content={<RadarTooltip candidates={data?.candidates ?? []} />} />
          <Radar
            name={t.talentMatchPage.configuredReference}
            dataKey="benchmark"
            stroke="#52525b"
            strokeDasharray="5 4"
            fill="#a1a1aa"
            fillOpacity={0.18}
            isAnimationActive={false}
          />
          {(data?.candidates ?? []).map((candidate, index) => (
            <Radar
              key={candidate.attemptId}
              name={candidate.candidateName}
              dataKey={candidate.attemptId}
              stroke={palette[index % palette.length]}
              fill={palette[index % palette.length]}
              fillOpacity={0.32}
            />
          ))}
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}

function RadarTooltip({
  active,
  label,
  payload,
  candidates,
}: {
  active?: boolean;
  label?: string;
  payload?: Array<{ name: string; value: number; dataKey: string }>;
  candidates: CandidateRadarDto[];
}) {
  const { t } = useLanguage();
  if (!active || !payload?.length) return null;

  const candidateNames = new Map(
    candidates.map((candidate) => [candidate.attemptId, candidate.candidateName]),
  );
  const rows = payload
    .map((item) => ({
      name:
        item.dataKey === "benchmark"
          ? t.talentMatchPage.configuredReference
          : (candidateNames.get(String(item.dataKey)) ?? item.name),
      value: Number(item.value ?? 0),
      benchmark: item.dataKey === "benchmark",
    }))
    .sort((a, b) => b.value - a.value);

  return (
    <div className="rounded-md border border-border bg-popover p-3 text-xs shadow-md">
      <div className="mb-2 font-semibold">{label}</div>
      <div className="space-y-1">
        {rows.map((row) => (
          <div key={row.name} className="flex min-w-44 items-center justify-between gap-4">
            <span className={row.benchmark ? "text-muted-foreground" : "text-foreground"}>
              {row.name}
            </span>
            <span className="font-medium tabular-nums">{row.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function BenchmarkSummary({ benchmark }: { benchmark: CompetencyBenchmarkDto[] }) {
  const { t } = useLanguage();
  const average =
    benchmark.length === 0
      ? 0
      : Math.round(benchmark.reduce((sum, item) => sum + item.targetScore, 0) / benchmark.length);

  return (
    <div className="inline-flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-xs">
      <BarChart3 className="h-3.5 w-3.5 text-muted-foreground" />
      <span className="text-muted-foreground">{t.talentMatchPage.averageReference}</span>
      <span className="font-semibold tabular-nums">{average}</span>
    </div>
  );
}

function CandidateLegend({
  candidates,
  viewResultLabel,
}: {
  candidates: CandidateRadarDto[];
  viewResultLabel: string;
}) {
  const { t } = useLanguage();
  return (
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
      {candidates.map((candidate, index) => (
        <div key={candidate.attemptId} className="rounded-md border border-border bg-card p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span
                  className="h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: palette[index % palette.length] }}
                />
                <div className="truncate text-sm font-medium">{candidate.candidateName}</div>
              </div>
              <div className="mt-1 truncate text-[11px] text-muted-foreground">
                {candidate.attemptId}
              </div>
            </div>
            <div className="text-right">
              <div className="text-xl font-semibold tabular-nums">{candidate.generalScore}</div>
              <div className="text-[10px] uppercase text-muted-foreground">
                {t.talentMatchPage.scoreSupportLabel}
              </div>
            </div>
          </div>
          <Button asChild variant="outline" size="sm" className="mt-3 w-full">
            <Link
              to="/results/$attemptId"
              params={{ attemptId: candidate.attemptId }}
              search={{
                search: "",
                simulationId: "",
                period: "",
                integrationProvider: "",
                page: 0,
              }}
            >
              {viewResultLabel}
            </Link>
          </Button>
          <EvidenceReportButton attemptId={candidate.attemptId} />
          <CandidateDecisionControl attemptId={candidate.attemptId} />
        </div>
      ))}
    </div>
  );
}

function EvidenceReportButton({ attemptId }: { attemptId: string }) {
  const { t } = useLanguage();
  const mutation = useMutation({
    mutationFn: async () => {
      const report = await getEvidenceReport(attemptId);
      const blob = new Blob([JSON.stringify(report, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `evidencia-${attemptId}.json`;
      anchor.click();
      URL.revokeObjectURL(url);
    },
  });

  return (
    <div className="mt-3">
      <button
        type="button"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
        className="inline-flex w-full items-center justify-center gap-1.5 rounded-md border border-border bg-background px-3 py-1.5 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
        title={t.talentMatchPage.evidenceReportTooltip}
      >
        <FileText className="h-3.5 w-3.5" />
        {mutation.isPending ? t.talentMatchPage.generating : t.talentMatchPage.evidenceReport}
      </button>
      {mutation.isError && (
        <p className="mt-1.5 text-[11px] text-destructive">
          {mutation.error instanceof Error
            ? mutation.error.message
            : t.talentMatchPage.evidenceReportErrorFallback}
        </p>
      )}
    </div>
  );
}

function CandidateDecisionControl({ attemptId }: { attemptId: string }) {
  const { t } = useLanguage();
  const [decision, setDecision] = useState<HumanDecision | "">("");
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: () =>
      registerCandidateDisposition(attemptId, {
        decision: decision as HumanDecision,
        reason: reason.trim() || undefined,
      }),
  });

  const decisionOptions: { value: HumanDecision; label: string }[] = [
    { value: "advanced", label: t.talentMatchPage.decisionAdvanced },
    { value: "rejected", label: t.talentMatchPage.decisionRejected },
    { value: "hired", label: t.talentMatchPage.decisionHired },
    { value: "onHold", label: t.talentMatchPage.decisionOnHold },
  ];

  if (mutation.isSuccess) {
    return (
      <div className="mt-3 rounded-md border border-primary/40 bg-primary/5 p-2.5 text-[11px] text-muted-foreground">
        {t.talentMatchPage.decisionRecorded}
      </div>
    );
  }

  return (
    <div className="mt-3 border-t border-border pt-3">
      <label className="block text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
        {t.talentMatchPage.registerDecision}
      </label>
      <select
        value={decision}
        onChange={(event) => setDecision(event.target.value as HumanDecision | "")}
        className="mt-1.5 w-full rounded-md border border-border bg-background px-2 py-1.5 text-xs"
      >
        <option value="">{t.talentMatchPage.selectDecisionPlaceholder}</option>
        {decisionOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <textarea
        value={reason}
        onChange={(event) => setReason(event.target.value)}
        rows={2}
        maxLength={1000}
        placeholder={t.talentMatchPage.reasonPlaceholder}
        className="mt-2 w-full resize-none rounded-md border border-border bg-background px-2 py-1.5 text-xs"
      />
      <button
        type="button"
        disabled={!decision || mutation.isPending}
        onClick={() => mutation.mutate()}
        className="mt-2 w-full rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {mutation.isPending ? t.talentMatchPage.registering : t.talentMatchPage.registerDecision}
      </button>
      {mutation.isError && (
        <p className="mt-1.5 text-[11px] text-destructive">
          {mutation.error instanceof Error
            ? mutation.error.message
            : t.talentMatchPage.registerDecisionErrorFallback}
        </p>
      )}
    </div>
  );
}
