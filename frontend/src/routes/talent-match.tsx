import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { BarChart3, Check, CircleAlert, EyeOff, FileText, Target, UsersRound } from "lucide-react";
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
import { StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  getEvidenceReport,
  getSimulationVersion,
  getTalentMatch,
  listCandidateLinks,
  listSimulations,
  registerCandidateDisposition,
  type CandidateLinkResponse,
  type CandidateRadarDto,
  type CompetencyBenchmarkDto,
  type HumanDecision,
  type SimulationSummaryResponse,
  type TalentMatchResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/talent-match")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
        ? Number(search.versionNumber)
        : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Comparar resultados - Práxis" },
      {
        name: "description",
        content:
          "Comparação visual de participações concluídas com a referência configurada para a avaliação.",
      },
    ],
  }),
  component: TalentMatchPage,
});

const MAX_SELECTED = 5;
const palette = ["#0f766e", "#b45309", "#2563eb", "#be123c", "#6d28d9"];

const DECISION_OPTIONS: { value: HumanDecision; label: string }[] = [
  { value: "advanced", label: "Avançar" },
  { value: "rejected", label: "Não avançar" },
  { value: "hired", label: "Contratar" },
  { value: "onHold", label: "Em espera" },
];

const BLIND_MODE_STORAGE_KEY = "praxis.talent-match.blindMode";

function TalentMatchPage() {
  const { t } = useLanguage();
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const [selectedAttemptIds, setSelectedAttemptIds] = useState<string[]>([]);
  const [blindMode, setBlindMode] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    return window.localStorage.getItem(BLIND_MODE_STORAGE_KEY) === "true";
  });

  useEffect(() => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(BLIND_MODE_STORAGE_KEY, String(blindMode));
    }
  }, [blindMode]);

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
  const candidateLinksQuery = useQuery({
    queryKey: ["candidate-links", { blind: blindMode }],
    queryFn: () => listCandidateLinks(blindMode),
    enabled: hasContext,
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
  });

  const candidates = useMemo(() => {
    return (candidateLinksQuery.data ?? [])
      .filter((candidate) => candidate.simulationId === search.simulationId)
      .sort((a, b) => Number(new Date(b.createdAt)) - Number(new Date(a.createdAt)));
  }, [candidateLinksQuery.data, search.simulationId]);

  const completedCandidates = candidates.filter((candidate) => candidate.status === "completed");
  const selectedVersion = versionQuery.data;
  const isVersionPublished = selectedVersion?.status === "published";

  useEffect(() => {
    setSelectedAttemptIds([]);
  }, [search.simulationId, search.versionNumber]);

  const benchmark =
    talentMatchQuery.data?.benchmark ??
    versionQuery.data?.blueprint.competencies.map((competency) => ({
      competencyName: competency.name,
      targetScore: competency.targetScore ?? 70,
    })) ??
    [];

  const selectedCandidateRows = talentMatchQuery.data?.candidates ?? [];
  const selectedLimitReached = selectedAttemptIds.length >= MAX_SELECTED;

  return (
    <AppShell>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">{t.common.talentMatch}</div>
          <h1 className="mt-1 text-3xl font-semibold">Comparação de participações</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Evidência para análise: sobreponha até 5 participações no radar e compare cada
            competência com a referência configurada para a avaliação. A pontuação é indicador de
            apoio; a interpretação permanece com a equipe responsável.
          </p>
        </div>
        <Link
          to="/dashboard"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>

      {!hasContext ? (
        <SimulationSelectionTable
          loading={simulationsQuery.isLoading}
          simulations={simulationsQuery.data ?? []}
        />
      ) : (
        <div className="space-y-5">
          <StateBanner tone="info" title="Evidência para análise — a decisão é sua">
            Os números são indicadores de apoio, não um veredito automático. A Práxis não aprova nem
            reprova pessoas: a decisão final é registrada por uma pessoa, e um erro crítico aciona
            revisão humana.
          </StateBanner>
          {!isVersionPublished && (
            <StateBanner tone="warn" title="Versão não disponível para comparação">
              Esta versão ainda não está publicada. Selecione uma versão publicada para comparar
              resultados.
            </StateBanner>
          )}
          {isVersionPublished && completedCandidates.length === 0 && (
            <StateBanner tone="info" title="Ainda não há participações concluídas">
              Selecione ou aguarde tentativas concluídas para essa versão antes de comparar no
              radar.
            </StateBanner>
          )}
          {selectedLimitReached && (
            <StateBanner tone="warn" title="Limite visual atingido">
              O radar aceita no máximo 5 participações por comparação para manter leitura clara.
            </StateBanner>
          )}
          {talentMatchQuery.isError && (
            <StateBanner tone="danger" title="Não foi possível carregar o comparativo">
              {talentMatchQuery.error instanceof Error
                ? talentMatchQuery.error.message
                : "Revise as participações selecionadas e tente novamente."}
            </StateBanner>
          )}

          <div className="grid gap-5 xl:grid-cols-[minmax(300px,380px)_minmax(0,1fr)]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 text-sm font-semibold">
                    <UsersRound className="h-4 w-4" />
                    Participantes
                  </div>
                  <div className="mt-1 text-xs text-muted-foreground">
                    {selectedAttemptIds.length}/{MAX_SELECTED} selecionados
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setSelectedAttemptIds([])}
                  className="rounded-md border border-border bg-background px-3 py-1.5 text-xs hover:bg-accent"
                >
                  Limpar
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
                    Modo cego
                  </span>
                  <span className="mt-0.5 block text-[11px] text-muted-foreground">
                    Oculta nome e e-mail para reduzir viés na análise. A identidade não é enviada
                    pelo servidor enquanto ativo.
                  </span>
                </span>
              </label>
              <CandidateSelector
                candidates={completedCandidates}
                loading={candidateLinksQuery.isLoading}
                selectedAttemptIds={selectedAttemptIds}
                onToggle={(attemptId) => {
                  setSelectedAttemptIds((current) => {
                    if (current.includes(attemptId)) {
                      return current.filter((id) => id !== attemptId);
                    }
                    if (current.length >= MAX_SELECTED) {
                      return current;
                    }
                    return [...current, attemptId];
                  });
                }}
              />
            </section>

            <section className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 text-sm font-semibold">
                    <Target className="h-4 w-4" />
                    Radar contra benchmark
                  </div>
                  <div className="mt-1 text-xs text-muted-foreground">
                    {search.simulationId} v{search.versionNumber}
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

          {isVersionPublished && completedCandidates.length > 0 ? (
            <CandidateLegend candidates={selectedCandidateRows} />
          ) : (
            <StateBanner tone="info" title="Comparativo pendente">
              A comparação aparece quando houver participações concluídas selecionadas.
            </StateBanner>
          )}
        </div>
      )}
    </AppShell>
  );
}

function CandidateSelector({
  candidates,
  loading,
  selectedAttemptIds,
  onToggle,
}: {
  candidates: CandidateLinkResponse[];
  loading: boolean;
  selectedAttemptIds: string[];
  onToggle: (attemptId: string) => void;
}) {
  if (loading) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm">Carregando...</div>
    );
  }

  if (candidates.length === 0) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhuma participação concluída encontrada para esta avaliação.
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
        Benchmark indisponível para esta versão.
      </div>
    );
  }

  return (
    <div className="min-h-[440px]">
      {loading && <div className="mb-2 text-xs text-muted-foreground">Atualizando radar...</div>}
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
            name="Referência configurada"
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
  if (!active || !payload?.length) return null;

  const candidateNames = new Map(
    candidates.map((candidate) => [candidate.attemptId, candidate.candidateName]),
  );
  const rows = payload
    .map((item) => ({
      name:
        item.dataKey === "benchmark"
          ? "Referência configurada"
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
  const average =
    benchmark.length === 0
      ? 0
      : Math.round(benchmark.reduce((sum, item) => sum + item.targetScore, 0) / benchmark.length);

  return (
    <div className="inline-flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-xs">
      <BarChart3 className="h-3.5 w-3.5 text-muted-foreground" />
      <span className="text-muted-foreground">Referência média</span>
      <span className="font-semibold tabular-nums">{average}</span>
    </div>
  );
}

function EvidenceReportButton({ attemptId }: { attemptId: string }) {
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
        title="Declaração de scoring determinístico (sem IA), fórmula, caminho, competências e trilha."
      >
        <FileText className="h-3.5 w-3.5" />
        {mutation.isPending ? "Gerando…" : "Relatório de evidência"}
      </button>
      {mutation.isError && (
        <p className="mt-1.5 text-[11px] text-destructive">
          {mutation.error instanceof Error
            ? mutation.error.message
            : "Não foi possível gerar o relatório."}
        </p>
      )}
    </div>
  );
}

function CandidateDecisionControl({ attemptId }: { attemptId: string }) {
  const [decision, setDecision] = useState<HumanDecision | "">("");
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: () =>
      registerCandidateDisposition(attemptId, {
        decision: decision as HumanDecision,
        reason: reason.trim() || undefined,
      }),
  });

  if (mutation.isSuccess) {
    return (
      <div className="mt-3 rounded-md border border-primary/40 bg-primary/5 p-2.5 text-[11px] text-muted-foreground">
        Decisão registrada na trilha de auditoria. A pontuação é apenas apoio — a decisão é sua.
      </div>
    );
  }

  return (
    <div className="mt-3 border-t border-border pt-3">
      <label className="block text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
        Registrar decisão
      </label>
      <select
        value={decision}
        onChange={(event) => setDecision(event.target.value as HumanDecision | "")}
        className="mt-1.5 w-full rounded-md border border-border bg-background px-2 py-1.5 text-xs"
      >
        <option value="">Selecione a decisão…</option>
        {DECISION_OPTIONS.map((option) => (
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
        placeholder="Justificativa (opcional, recomendada)"
        className="mt-2 w-full resize-none rounded-md border border-border bg-background px-2 py-1.5 text-xs"
      />
      <button
        type="button"
        disabled={!decision || mutation.isPending}
        onClick={() => mutation.mutate()}
        className="mt-2 w-full rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {mutation.isPending ? "Registrando…" : "Registrar decisão"}
      </button>
      {mutation.isError && (
        <p className="mt-1.5 text-[11px] text-destructive">
          {mutation.error instanceof Error
            ? mutation.error.message
            : "Não foi possível registrar a decisão."}
        </p>
      )}
    </div>
  );
}

function CandidateLegend({ candidates }: { candidates: CandidateRadarDto[] }) {
  if (candidates.length === 0) {
    return (
      <StateBanner tone="info" title="Benchmark visível">
        Selecione participações concluídas para sobrepor os perfis individuais ao radar.
      </StateBanner>
    );
  }

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
              <div className="text-[10px] uppercase text-muted-foreground">pontuação · apoio</div>
            </div>
          </div>
          <EvidenceReportButton attemptId={candidate.attemptId} />
          <CandidateDecisionControl attemptId={candidate.attemptId} />
        </div>
      ))}
    </div>
  );
}

function SimulationSelectionTable({
  simulations,
  loading,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
}) {
  if (loading) {
    return (
      <section className="rounded-md border border-border bg-card p-5">
        <div className="text-sm text-muted-foreground">Carregando avaliações...</div>
      </section>
    );
  }

  if (simulations.length === 0) {
    return (
      <section className="rounded-md border border-border bg-card p-8 text-center">
        <CircleAlert className="mx-auto h-6 w-6 text-muted-foreground" />
        <h2 className="mt-3 text-lg font-semibold">Nenhuma avaliação cadastrada</h2>
        <p className="mx-auto mt-1 max-w-md text-sm text-muted-foreground">
          Crie uma avaliação e conclua participações para comparar resultados.
        </p>
        <Link
          to="/nova/avaliacao"
          className="mt-5 inline-flex rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Criar avaliação
        </Link>
      </section>
    );
  }

  const sorted = [...simulations].sort((a, b) => {
    const rank = (simulation: SimulationSummaryResponse) => {
      const hasReference = simulation.competencies.length > 0;
      const comparable =
        simulation.status === "published" && simulation.attemptsCompleted > 0 && hasReference;

      if (comparable) return 0;
      if (simulation.status === "published") return 1;
      return 2;
    };

    const rankDelta = rank(a) - rank(b);
    if (rankDelta !== 0) return rankDelta;
    return Number(new Date(b.updatedAt)) - Number(new Date(a.updatedAt));
  });

  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border p-5">
        <h2 className="text-xl font-semibold">Selecione uma avaliação para comparar resultados</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          O painel usa participações concluídas e a referência configurada no plano da avaliação.
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[860px] text-left text-sm">
          <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 font-medium">Avaliação</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Versão</th>
              <th className="px-4 py-3 text-right font-medium">Participações concluídas</th>
              <th className="px-4 py-3 font-medium">Referência</th>
              <th className="px-4 py-3 text-right font-medium">Ação</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((simulation) => {
              const hasReference = simulation.competencies.length > 0;
              const canCompare =
                simulation.status === "published" &&
                simulation.attemptsCompleted > 0 &&
                hasReference;

              return (
                <tr
                  key={simulation.id}
                  className="border-b border-border last:border-0 hover:bg-accent/35"
                >
                  <td className="px-4 py-3">
                    <div className="font-medium text-foreground">{simulation.name}</div>
                    {simulation.description && (
                      <div className="mt-1 max-w-md truncate text-xs text-muted-foreground">
                        {simulation.description}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge
                      status={simulation.status}
                      maturity={maturityForStatus(simulation.status)}
                      variant="status"
                    />
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-muted-foreground">
                    v{simulation.versionNumber}
                  </td>
                  <td className="px-4 py-3 text-right font-medium tabular-nums">
                    {simulation.attemptsCompleted.toLocaleString("pt-BR")}
                  </td>
                  <td className="px-4 py-3">
                    {hasReference ? (
                      <span className="text-xs text-muted-foreground">
                        {simulation.competencies.length} competência
                        {simulation.competencies.length === 1 ? "" : "s"}
                      </span>
                    ) : (
                      <span className="text-xs text-warning">Configure a referência</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {canCompare ? (
                      <Link
                        to="/talent-match"
                        search={{
                          simulationId: simulation.id,
                          versionNumber: simulation.versionNumber,
                        }}
                        className="inline-flex rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                      >
                        Comparar participações
                      </Link>
                    ) : (
                      <span className="text-xs text-muted-foreground">
                        {simulation.status !== "published"
                          ? "Publique a avaliação"
                          : !hasReference
                            ? "Configure a referência"
                            : "Sem dados suficientes"}
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
