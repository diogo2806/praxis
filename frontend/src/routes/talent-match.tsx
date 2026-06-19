import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { BarChart3, Check, CircleAlert, Target, UsersRound } from "lucide-react";
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
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  getSimulationVersion,
  getTalentMatch,
  listCandidateLinks,
  listSimulations,
  type CandidateLinkResponse,
  type CandidateRadarDto,
  type CompetencyBenchmarkDto,
  type SimulationSummaryResponse,
  type TalentMatchResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
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
      { title: "Talent Match - Praxis" },
      {
        name: "description",
        content: "Comparativo visual de candidatos contra a régua alvo da vaga.",
      },
    ],
  }),
  component: TalentMatchPage,
});

const MAX_SELECTED = 5;
const palette = ["#0f766e", "#b45309", "#2563eb", "#be123c", "#6d28d9"];

function TalentMatchPage() {
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const [selectedAttemptIds, setSelectedAttemptIds] = useState<string[]>([]);

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
    queryKey: ["candidate-links"],
    queryFn: listCandidateLinks,
    enabled: hasContext,
  });
  const talentMatchQuery = useQuery({
    queryKey: ["talent-match", search.simulationId, search.versionNumber, selectedAttemptIds],
    queryFn: () => getTalentMatch(search.simulationId!, search.versionNumber!, selectedAttemptIds),
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
      <ScreenStateStrip blockedReason="sem candidatos concluídos para comparar" />
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Talent Match</div>
          <h1 className="mt-1 text-3xl font-semibold">Comparativo de candidatos</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Sobreponha até 5 perfis no radar e compare cada competência contra a régua alvo da
            vaga.
          </p>
        </div>
        <Link
          to="/app"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma simulação para comparar talentos"
          description="O painel usa candidatos concluídos da simulação e a régua alvo configurada no blueprint da vaga."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : (
        <div className="space-y-5">
          {!isVersionPublished && (
            <StateBanner tone="warn" title="Versão não disponível para comparação">
              Esta versão ainda não está publicada. Selecione uma versão publicada para comparar
              talentos.
            </StateBanner>
          )}
          {isVersionPublished && completedCandidates.length === 0 && (
            <StateBanner tone="info" title="Ainda não há candidatos concluídos">
              Selecione ou aguarde tentativas concluídas para essa versão antes de comparar no radar.
            </StateBanner>
          )}
          {selectedLimitReached && (
          <StateBanner tone="warn" title="Limite visual atingido">
              O radar aceita no máximo 5 candidatos por comparação para manter leitura clara.
            </StateBanner>
          )}
          {talentMatchQuery.isError && (
            <StateBanner tone="danger" title="Não foi possível carregar o comparativo">
              {talentMatchQuery.error instanceof Error
                ? talentMatchQuery.error.message
                : "Revise os candidatos selecionados e tente novamente."}
            </StateBanner>
          )}

          <div className="grid gap-5 xl:grid-cols-[minmax(300px,380px)_minmax(0,1fr)]">
          <section className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 text-sm font-semibold">
                    <UsersRound className="h-4 w-4" />
                    Candidatos
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
          A comparação aparece quando houver candidatos concluídos selecionados.
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
    return <div className="rounded-md border border-border bg-background p-4 text-sm">Carregando...</div>;
  }

      if (candidates.length === 0) {
    return (
      <div className="rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
          Nenhum candidato concluído encontrado para esta simulação.
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
              <span className="mt-0.5 block truncate text-xs text-muted-foreground">
                {candidate.candidateEmail}
              </span>
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
          <PolarAngleAxis dataKey="competency" tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }} />
          <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fontSize: 10 }} />
            <RechartsTooltip content={<RadarTooltip candidates={data?.candidates ?? []} />} />
          <Radar
              name="Régua alvo"
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

  const candidateNames = new Map(candidates.map((candidate) => [candidate.attemptId, candidate.candidateName]));
  const rows = payload
    .map((item) => ({
      name: item.dataKey === "benchmark" ? "Régua alvo" : candidateNames.get(String(item.dataKey)) ?? item.name,
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
            <span className={row.benchmark ? "text-muted-foreground" : "text-foreground"}>{row.name}</span>
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
      <span className="text-muted-foreground">Media alvo</span>
      <span className="font-semibold tabular-nums">{average}</span>
    </div>
  );
}

function CandidateLegend({ candidates }: { candidates: CandidateRadarDto[] }) {
  if (candidates.length === 0) {
    return (
        <StateBanner tone="info" title="Benchmark visível">
        Selecione candidatos concluídos para sobrepor os perfis individuais ao radar da vaga.
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
              <div className="text-[10px] uppercase text-muted-foreground">score</div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function SimulationLinks({
  simulations,
  loading,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
}) {
  if (loading) {
    return <div className="rounded-md border border-border bg-card px-4 py-3 text-sm">Carregando...</div>;
  }

      if (simulations.length === 0) {
    return (
      <div className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-4 py-3 text-sm text-muted-foreground">
        <CircleAlert className="h-4 w-4" />
        Nenhuma simulação cadastrada.
      </div>
    );
  }

  return (
    <>
      {simulations.slice(0, 4).map((simulation) => {
        const canSelect = simulation.status === "published" && simulation.attemptsCreated > 0;
        return canSelect ? (
          <Link
            key={simulation.id}
            to="/talent-match"
            search={{
              simulationId: simulation.id,
              versionNumber: simulation.versionNumber,
            }}
            className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
          >
            <span className="block font-medium">{simulation.name}</span>
            <span className="mt-1 block">
              <StatusBadge
                status={simulation.status}
                maturity={maturityForStatus(simulation.status)}
              />
            </span>
          </Link>
        ) : (
          <button
            type="button"
            key={simulation.id}
            disabled
            title={
              simulation.status !== "published"
                ? "A versão precisa estar publicada para comparar talentos."
                : "Esta versão ainda não possui candidatos concluídos."
            }
            className="rounded-md border border-border bg-card px-4 py-3 text-sm opacity-70"
          >
            <span className="block font-medium text-left">{simulation.name}</span>
            <span className="mt-1 block text-xs text-muted-foreground">Sem dados suficientes</span>
          </button>
        );
      })}
    </>
  );
}
