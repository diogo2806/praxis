import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  CalendarPlus,
  CheckCircle2,
  Clock3,
  Copy,
  Link2,
  RefreshCw,
  Search,
  Send,
  UserRoundSearch,
} from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  extendCandidateLink,
  listCandidateLinks,
  resendCandidateLink,
  type CandidateLinkResponse,
} from "@/lib/api/candidate-links";
import {
  searchMonitoringAttempts,
  type MonitoringAttempt,
  type MonitoringAttemptStatus,
} from "@/lib/api/monitoring";
import { listSimulations } from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/participacoes")({
  head: () => ({
    meta: [
      { title: "Participações - Práxis" },
      {
        name: "description",
        content: "Centralize convites, validade, andamento e resultados das avaliações.",
      },
    ],
  }),
  component: ParticipacoesPage,
});

type ProcessFilter = "all" | "waiting" | "active" | "completed" | "attention";

const processFilters: Array<{ value: ProcessFilter; label: string }> = [
  { value: "all", label: "Todas" },
  { value: "waiting", label: "Aguardando início" },
  { value: "active", label: "Em andamento" },
  { value: "completed", label: "Concluídas" },
  { value: "attention", label: "Com problema" },
];

const extensionOptions = [1, 3, 7, 15, 30];

function ParticipacoesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [processFilter, setProcessFilter] = useState<ProcessFilter>("all");
  const [simulationId, setSimulationId] = useState("");
  const [candidate, setCandidate] = useState("");
  const [copiedAttemptId, setCopiedAttemptId] = useState<string | null>(null);
  const [extensionDays, setExtensionDays] = useState<Record<string, number>>({});
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const attemptsQuery = useQuery({
    queryKey: ["participations", page, simulationId, candidate],
    queryFn: () =>
      searchMonitoringAttempts({
        page,
        size: 25,
        status: "all",
        simulationId: simulationId || undefined,
        candidate: candidate.trim() || undefined,
      }),
    retry: false,
    refetchInterval: 30_000,
  });

  const linksQuery = useQuery({
    queryKey: ["candidate-links", "participations", simulationId, candidate],
    queryFn: () =>
      listCandidateLinks(false, {
        simulationId: simulationId || undefined,
        candidate: candidate.trim() || undefined,
      }),
    retry: false,
  });

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });

  const resendMutation = useMutation({
    mutationFn: resendCandidateLink,
    onSuccess: async () => {
      setActionMessage("O link foi reenviado sem criar uma nova participação.");
      await invalidateParticipationQueries(queryClient);
    },
  });

  const extendMutation = useMutation({
    mutationFn: ({ attemptId, days }: { attemptId: string; days: number }) =>
      extendCandidateLink(attemptId, days),
    onSuccess: async (_response, variables) => {
      setActionMessage(`A validade foi ampliada em ${variables.days} dia(s).`);
      await invalidateParticipationQueries(queryClient);
    },
  });

  const attemptsPage = attemptsQuery.data;
  const attempts = useMemo(
    () => (attemptsPage?.items ?? []).filter((attempt) => matchesProcessFilter(attempt, processFilter)),
    [attemptsPage?.items, processFilter],
  );
  const linksByAttempt = useMemo(
    () => new Map((linksQuery.data ?? []).map((link) => [link.attemptId, link] as const)),
    [linksQuery.data],
  );
  const simulations = (simulationsQuery.data ?? []).filter(
    (simulation) => simulation.status === "published" || simulation.livePublishedVersionNumber != null,
  );
  const actionError = resendMutation.error ?? extendMutation.error;

  async function copyLink(link: CandidateLinkResponse) {
    if (link.linkStatus === "expired") return;
    await navigator.clipboard.writeText(toAbsoluteUrl(link.candidateUrl));
    setCopiedAttemptId(link.attemptId);
    window.setTimeout(() => setCopiedAttemptId(null), 2000);
  }

  async function refreshAll() {
    await Promise.all([attemptsQuery.refetch(), linksQuery.refetch(), simulationsQuery.refetch()]);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Participações</div>
            <h1 className="mt-1 font-display text-3xl">Convites e acompanhamento</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Este é o ponto único para criar convites, acompanhar o progresso, atualizar a validade e abrir resultados.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild className="gap-2">
              <Link to="/enviar-link"><Link2 className="h-4 w-4" />Participação individual</Link>
            </Button>
            <Button asChild variant="outline" className="gap-2 bg-card">
              <Link to="/participacoes/jornada"><Send className="h-4 w-4" />Convite por jornada</Link>
            </Button>
            <Button variant="outline" className="gap-2 bg-card" onClick={() => void refreshAll()} disabled={attemptsQuery.isFetching || linksQuery.isFetching}>
              <RefreshCw className={cn("h-4 w-4", (attemptsQuery.isFetching || linksQuery.isFetching) && "animate-spin")} />
              Atualizar
            </Button>
          </div>
        </header>

        {actionMessage && <StateBanner tone="ok" title="Participação atualizada">{actionMessage}</StateBanner>}
        {actionError && (
          <StateBanner tone="danger" title="Não foi possível atualizar a participação">
            {actionError instanceof Error ? actionError.message : "Tente novamente."}
          </StateBanner>
        )}

        <section className="rounded-xl border border-border bg-card p-4" aria-label="Filtros das participações">
          <div className="flex flex-wrap gap-2" role="tablist" aria-label="Situação do processo">
            {processFilters.map((filter) => (
              <button
                key={filter.value}
                type="button"
                role="tab"
                aria-selected={processFilter === filter.value}
                onClick={() => setProcessFilter(filter.value)}
                className={cn(
                  "min-h-10 rounded-md border px-3 py-2 text-sm font-medium",
                  processFilter === filter.value
                    ? "border-primary/40 bg-primary/10 text-primary"
                    : "border-border bg-background hover:bg-accent",
                )}
              >
                {filter.label}
              </button>
            ))}
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Avaliação
              <select
                value={simulationId}
                onChange={(event) => {
                  setPage(0);
                  setSimulationId(event.target.value);
                }}
                className="input h-11 w-full"
              >
                <option value="">Todas as avaliações</option>
                {simulations.map((simulation) => (
                  <option key={simulation.id} value={simulation.id}>{simulation.name}</option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-xs font-medium text-muted-foreground">
              Nome ou e-mail
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-muted-foreground" />
                <input
                  value={candidate}
                  onChange={(event) => {
                    setPage(0);
                    setCandidate(event.target.value);
                  }}
                  className="input h-11 w-full pl-9"
                  placeholder="Buscar participante"
                />
              </div>
            </label>
          </div>
        </section>

        {attemptsQuery.isError || linksQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível carregar as participações">
            Verifique a conexão e tente novamente.
          </StateBanner>
        ) : attemptsQuery.isLoading || linksQuery.isLoading ? (
          <section className="rounded-xl border border-border bg-card px-4 py-12 text-center text-sm text-muted-foreground">
            Carregando participações...
          </section>
        ) : attempts.length === 0 ? (
          <EmptyState
            title="Nenhuma participação encontrada"
            description="Crie uma participação individual ou envie uma jornada para começar."
            actions={
              <div className="flex flex-wrap justify-center gap-2">
                <Button asChild><Link to="/enviar-link">Criar individual</Link></Button>
                <Button asChild variant="outline"><Link to="/participacoes/jornada">Enviar jornada</Link></Button>
              </div>
            }
          />
        ) : (
          <ParticipationTable
            attempts={attempts}
            linksByAttempt={linksByAttempt}
            copiedAttemptId={copiedAttemptId}
            extensionDays={extensionDays}
            resendingAttemptId={resendMutation.isPending ? resendMutation.variables ?? null : null}
            extendingAttemptId={extendMutation.isPending ? extendMutation.variables?.attemptId ?? null : null}
            onCopy={copyLink}
            onResend={(attemptId) => {
              setActionMessage(null);
              resendMutation.mutate(attemptId);
            }}
            onExtend={(attemptId) => {
              setActionMessage(null);
              extendMutation.mutate({ attemptId, days: extensionDays[attemptId] ?? 7 });
            }}
            onExtensionDaysChange={(attemptId, days) =>
              setExtensionDays((current) => ({ ...current, [attemptId]: days }))
            }
          />
        )}

        <div className="flex items-center justify-between gap-3" data-manual-pagination>
          <Button variant="outline" disabled={page <= 0 || attemptsQuery.isFetching} onClick={() => setPage((current) => Math.max(0, current - 1))}>Anterior</Button>
          <span className="text-xs text-muted-foreground">Página {(attemptsPage?.page ?? 0) + 1} de {Math.max(1, attemptsPage?.totalPages ?? 1)}</span>
          <Button variant="outline" disabled={!attemptsPage || page + 1 >= attemptsPage.totalPages || attemptsQuery.isFetching} onClick={() => setPage((current) => current + 1)}>Próxima</Button>
        </div>
      </main>
    </AppShell>
  );
}

function ParticipationTable({
  attempts,
  linksByAttempt,
  copiedAttemptId,
  extensionDays,
  resendingAttemptId,
  extendingAttemptId,
  onCopy,
  onResend,
  onExtend,
  onExtensionDaysChange,
}: {
  attempts: MonitoringAttempt[];
  linksByAttempt: Map<string, CandidateLinkResponse>;
  copiedAttemptId: string | null;
  extensionDays: Record<string, number>;
  resendingAttemptId: string | null;
  extendingAttemptId: string | null;
  onCopy: (link: CandidateLinkResponse) => Promise<void>;
  onResend: (attemptId: string) => void;
  onExtend: (attemptId: string) => void;
  onExtensionDaysChange: (attemptId: string, days: number) => void;
}) {
  return (
    <section className="overflow-x-auto rounded-xl border border-border bg-card">
      <table data-server-pagination className="w-full min-w-[1120px] text-left text-sm">
        <thead className="border-b border-border bg-muted/40 text-[11px] uppercase text-muted-foreground">
          <tr>
            <th className="px-4 py-3 font-medium">Participante</th>
            <th className="px-4 py-3 font-medium">Avaliação</th>
            <th className="px-4 py-3 font-medium">Situação</th>
            <th className="px-4 py-3 font-medium">Progresso</th>
            <th className="px-4 py-3 font-medium">Link</th>
            <th className="px-4 py-3 text-right font-medium">Ações</th>
          </tr>
        </thead>
        <tbody>
          {attempts.map((attempt) => {
            const link = linksByAttempt.get(attempt.attemptId);
            return (
              <tr key={attempt.attemptId} className="border-b border-border last:border-0 hover:bg-accent/30">
                <td className="px-4 py-3"><div className="font-medium">{attempt.candidateName}</div><div className="text-xs text-muted-foreground">{attempt.candidateEmail}</div></td>
                <td className="px-4 py-3 text-muted-foreground">{attempt.simulationName} · v{attempt.versionNumber}</td>
                <td className="px-4 py-3"><ParticipationStatus attempt={attempt} /></td>
                <td className="px-4 py-3">{attempt.currentTurn}/{attempt.estimatedTurns} · {attempt.progressPercent}%</td>
                <td className="px-4 py-3"><LinkStatus link={link} /></td>
                <td className="px-4 py-3 text-right">
                  <ParticipationActions
                    attempt={attempt}
                    link={link}
                    copied={copiedAttemptId === attempt.attemptId}
                    extensionDays={extensionDays[attempt.attemptId] ?? 7}
                    resending={resendingAttemptId === attempt.attemptId}
                    extending={extendingAttemptId === attempt.attemptId}
                    onCopy={onCopy}
                    onResend={onResend}
                    onExtend={onExtend}
                    onExtensionDaysChange={onExtensionDaysChange}
                  />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

function ParticipationActions({ attempt, link, copied, extensionDays, resending, extending, onCopy, onResend, onExtend, onExtensionDaysChange }: {
  attempt: MonitoringAttempt;
  link?: CandidateLinkResponse;
  copied: boolean;
  extensionDays: number;
  resending: boolean;
  extending: boolean;
  onCopy: (link: CandidateLinkResponse) => Promise<void>;
  onResend: (attemptId: string) => void;
  onExtend: (attemptId: string) => void;
  onExtensionDaysChange: (attemptId: string, days: number) => void;
}) {
  if (attempt.status === "completed") {
    return <Button asChild variant="outline" size="sm"><Link to="/results/$attemptId" params={{ attemptId: attempt.attemptId }}>Analisar resultado</Link></Button>;
  }
  if (!link) return <span className="text-xs text-muted-foreground">Ações indisponíveis</span>;
  const expired = link.linkStatus === "expired";
  return (
    <div className="flex flex-wrap justify-end gap-2">
      <Button variant="outline" size="sm" disabled={expired} onClick={() => void onCopy(link)}>
        {copied ? <CheckCircle2 className="mr-1 h-3.5 w-3.5" /> : <Copy className="mr-1 h-3.5 w-3.5" />}{copied ? "Copiado" : "Copiar"}
      </Button>
      <Button variant="outline" size="sm" disabled={expired || resending} onClick={() => onResend(attempt.attemptId)}>{resending ? "Reenviando..." : "Reenviar"}</Button>
      <div className="flex items-center rounded-md border border-border bg-background">
        <select value={extensionDays} onChange={(event) => onExtensionDaysChange(attempt.attemptId, Number(event.target.value))} aria-label="Dias adicionais" className="h-8 border-0 bg-transparent px-2 text-xs outline-none">
          {extensionOptions.map((days) => <option key={days} value={days}>+{days} dia(s)</option>)}
        </select>
        <button type="button" disabled={extending} onClick={() => onExtend(attempt.attemptId)} className="inline-flex h-8 items-center gap-1 border-l border-border px-2 text-xs font-medium text-primary disabled:opacity-50">
          <CalendarPlus className="h-3.5 w-3.5" />{extending ? "Atualizando..." : expired ? "Reativar" : "Ampliar"}
        </button>
      </div>
    </div>
  );
}

function ParticipationStatus({ attempt }: { attempt: MonitoringAttempt }) {
  const meta = statusMeta(attempt.status, attempt.active);
  const Icon = meta.icon;
  return <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2 py-1 text-[11px] font-medium", meta.className)}><Icon className="h-3.5 w-3.5" />{meta.label}</span>;
}

function LinkStatus({ link }: { link?: CandidateLinkResponse }) {
  if (!link) return <span className="text-xs text-muted-foreground">Sem informação</span>;
  const meta = {
    active: { label: "Ativo", className: "border-success/30 bg-success/10 text-success" },
    expiringSoon: { label: `Expira em ${link.remainingDays} dia(s)`, className: "border-warning/40 bg-warning/10 text-warning-foreground" },
    expired: { label: "Expirado", className: "border-danger/30 bg-danger/10 text-danger" },
  }[link.linkStatus];
  return <span className={cn("inline-flex rounded-full border px-2 py-1 text-[11px] font-medium", meta.className)}>{meta.label}</span>;
}

function statusMeta(status: MonitoringAttemptStatus, active: boolean) {
  if (status === "inProgress" && !active) {
    return { label: "Sem atividade recente", icon: AlertTriangle, className: "border-warning/40 bg-warning/10 text-warning-foreground" };
  }
  return ({
    notStarted: { label: "Aguardando início", icon: Clock3, className: "border-border bg-muted text-foreground" },
    inProgress: { label: "Em andamento", icon: UserRoundSearch, className: "border-primary/30 bg-primary/10 text-primary" },
    completed: { label: "Concluída", icon: CheckCircle2, className: "border-success/30 bg-success/10 text-success" },
    abandoned: { label: "Abandonada", icon: AlertTriangle, className: "border-danger/30 bg-danger/10 text-danger" },
    expired: { label: "Expirada", icon: AlertTriangle, className: "border-warning/40 bg-warning/10 text-warning-foreground" },
  } satisfies Record<MonitoringAttemptStatus, { label: string; icon: typeof Clock3; className: string }>)[status];
}

function matchesProcessFilter(attempt: MonitoringAttempt, filter: ProcessFilter) {
  if (filter === "all") return true;
  if (filter === "waiting") return attempt.status === "notStarted";
  if (filter === "active") return attempt.status === "inProgress" && attempt.active;
  if (filter === "completed") return attempt.status === "completed";
  return attempt.status === "abandoned" || attempt.status === "expired" || (attempt.status === "inProgress" && !attempt.active);
}

function toAbsoluteUrl(path: string) {
  if (/^https?:\/\//i.test(path)) return path;
  if (typeof window === "undefined") return path;
  return `${window.location.origin}${path.startsWith("/") ? path : `/${path}`}`;
}

async function invalidateParticipationQueries(queryClient: ReturnType<typeof useQueryClient>) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ["participations"] }),
    queryClient.invalidateQueries({ queryKey: ["candidate-links"] }),
  ]);
}
