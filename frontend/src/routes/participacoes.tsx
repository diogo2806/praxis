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
  Trash2,
  UserRoundSearch,
  Workflow,
} from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  cancelParticipation,
  extendParticipation,
  type ParticipationMonitoringItem,
  type ParticipationStatus,
  type ParticipationType,
  resendParticipation,
  searchParticipations,
} from "@/lib/api/participations";
import { listSimulations } from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/participacoes")({
  head: () => ({
    meta: [
      { title: "Participações - Práxis" },
      {
        name: "description",
        content: "Centralize convites individuais e por jornada, validade, andamento e resultados.",
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
  const [copiedParticipationId, setCopiedParticipationId] = useState<string | null>(null);
  const [extensionDays, setExtensionDays] = useState<Record<string, number>>({});
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const participationsQuery = useQuery({
    queryKey: ["participations", page, simulationId, candidate],
    queryFn: () =>
      searchParticipations({
        page,
        size: 25,
        simulationId: simulationId || undefined,
        candidate: candidate.trim() || undefined,
      }),
    retry: false,
    refetchInterval: 30_000,
  });

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });

  const resendMutation = useMutation({
    mutationFn: ({ type, participationId }: { type: ParticipationType; participationId: string }) =>
      resendParticipation(type, participationId),
    onSuccess: async () => {
      setActionMessage("O convite foi reenviado sem criar uma nova participação.");
      await invalidateParticipationQueries(queryClient);
    },
  });

  const extendMutation = useMutation({
    mutationFn: ({
      type,
      participationId,
      days,
    }: {
      type: ParticipationType;
      participationId: string;
      days: number;
    }) => extendParticipation(type, participationId, days),
    onSuccess: async (_response, variables) => {
      setActionMessage(`A validade foi ampliada em ${variables.days} dia(s).`);
      await invalidateParticipationQueries(queryClient);
    },
  });

  const cancelMutation = useMutation({
    mutationFn: ({ type, participationId }: { type: ParticipationType; participationId: string }) =>
      cancelParticipation(type, participationId),
    onSuccess: async () => {
      setActionMessage("A participação por jornada foi cancelada.");
      await invalidateParticipationQueries(queryClient);
    },
  });

  const participationPage = participationsQuery.data;
  const participations = useMemo(
    () =>
      (participationPage?.items ?? []).filter((participation) =>
        matchesProcessFilter(participation, processFilter),
      ),
    [participationPage?.items, processFilter],
  );
  const simulations = (simulationsQuery.data ?? []).filter(
    (simulation) =>
      simulation.status === "published" || simulation.livePublishedVersionNumber != null,
  );
  const actionError = resendMutation.error ?? extendMutation.error ?? cancelMutation.error;

  async function copyLink(participation: ParticipationMonitoringItem) {
    if (participation.linkStatus === "expired" || participation.linkStatus === "canceled") return;
    await navigator.clipboard.writeText(toAbsoluteUrl(participation.candidateUrl));
    setCopiedParticipationId(participation.participationId);
    window.setTimeout(() => setCopiedParticipationId(null), 2000);
  }

  async function refreshAll() {
    await Promise.all([participationsQuery.refetch(), simulationsQuery.refetch()]);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Participações
            </div>
            <h1 className="mt-1 font-display text-3xl">Convites e acompanhamento</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Este é o ponto único para participações individuais e por jornada, incluindo validade,
              reenvio, cancelamento, progresso e acesso aos resultados concluídos.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild className="gap-2">
              <Link to="/enviar-link">
                <Link2 className="h-4 w-4" />
                Participação individual
              </Link>
            </Button>
            <Button asChild variant="outline" className="gap-2 bg-card">
              <Link to="/participacoes/jornada">
                <Send className="h-4 w-4" />
                Convite por jornada
              </Link>
            </Button>
            <Button
              variant="outline"
              className="gap-2 bg-card"
              onClick={() => void refreshAll()}
              disabled={participationsQuery.isFetching}
            >
              <RefreshCw
                className={cn("h-4 w-4", participationsQuery.isFetching && "animate-spin")}
              />
              Atualizar
            </Button>
          </div>
        </header>

        {actionMessage && (
          <StateBanner tone="ok" title="Participação atualizada">
            {actionMessage}
          </StateBanner>
        )}
        {actionError && (
          <StateBanner tone="danger" title="Não foi possível atualizar a participação">
            {actionError instanceof Error ? actionError.message : "Tente novamente."}
          </StateBanner>
        )}

        <section
          className="rounded-xl border border-border bg-card p-4"
          aria-label="Filtros das participações"
        >
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
              Avaliação incluída
              <select
                value={simulationId}
                onChange={(event) => {
                  setPage(0);
                  setSimulationId(event.target.value);
                }}
                className="input h-11 w-full"
              >
                <option value="">Todas as avaliações e jornadas</option>
                {simulations.map((simulation) => (
                  <option key={simulation.id} value={simulation.id}>
                    {simulation.name}
                  </option>
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

        {participationsQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível carregar as participações">
            {participationsQuery.error instanceof Error
              ? participationsQuery.error.message
              : "Verifique a conexão e tente novamente."}
          </StateBanner>
        ) : participationsQuery.isLoading ? (
          <section className="rounded-xl border border-border bg-card px-4 py-12 text-center text-sm text-muted-foreground">
            Carregando participações...
          </section>
        ) : participations.length === 0 ? (
          <EmptyState
            title="Nenhuma participação encontrada"
            description="Crie uma participação individual ou envie uma jornada para começar."
            actions={
              <div className="flex flex-wrap justify-center gap-2">
                <Button asChild>
                  <Link to="/enviar-link">Criar individual</Link>
                </Button>
                <Button asChild variant="outline">
                  <Link to="/participacoes/jornada">Enviar jornada</Link>
                </Button>
              </div>
            }
          />
        ) : (
          <ParticipationTable
            participations={participations}
            copiedParticipationId={copiedParticipationId}
            extensionDays={extensionDays}
            resendingParticipationId={
              resendMutation.isPending ? (resendMutation.variables?.participationId ?? null) : null
            }
            extendingParticipationId={
              extendMutation.isPending ? (extendMutation.variables?.participationId ?? null) : null
            }
            cancelingParticipationId={
              cancelMutation.isPending ? (cancelMutation.variables?.participationId ?? null) : null
            }
            onCopy={copyLink}
            onResend={(participation) => {
              setActionMessage(null);
              resendMutation.mutate({
                type: participation.participationType,
                participationId: participation.participationId,
              });
            }}
            onExtend={(participation) => {
              setActionMessage(null);
              extendMutation.mutate({
                type: participation.participationType,
                participationId: participation.participationId,
                days: extensionDays[participation.participationId] ?? 7,
              });
            }}
            onCancel={(participation) => {
              setActionMessage(null);
              cancelMutation.mutate({
                type: participation.participationType,
                participationId: participation.participationId,
              });
            }}
            onExtensionDaysChange={(participationId, days) =>
              setExtensionDays((current) => ({ ...current, [participationId]: days }))
            }
          />
        )}

        <div className="flex items-center justify-between gap-3" data-manual-pagination>
          <Button
            variant="outline"
            disabled={page <= 0 || participationsQuery.isFetching}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            Anterior
          </Button>
          <span className="text-xs text-muted-foreground">
            Página {(participationPage?.page ?? 0) + 1} de{" "}
            {Math.max(1, participationPage?.totalPages ?? 1)}
          </span>
          <Button
            variant="outline"
            disabled={
              !participationPage ||
              page + 1 >= participationPage.totalPages ||
              participationsQuery.isFetching
            }
            onClick={() => setPage((current) => current + 1)}
          >
            Próxima
          </Button>
        </div>
      </main>
    </AppShell>
  );
}

function ParticipationTable({
  participations,
  copiedParticipationId,
  extensionDays,
  resendingParticipationId,
  extendingParticipationId,
  cancelingParticipationId,
  onCopy,
  onResend,
  onExtend,
  onCancel,
  onExtensionDaysChange,
}: {
  participations: ParticipationMonitoringItem[];
  copiedParticipationId: string | null;
  extensionDays: Record<string, number>;
  resendingParticipationId: string | null;
  extendingParticipationId: string | null;
  cancelingParticipationId: string | null;
  onCopy: (participation: ParticipationMonitoringItem) => Promise<void>;
  onResend: (participation: ParticipationMonitoringItem) => void;
  onExtend: (participation: ParticipationMonitoringItem) => void;
  onCancel: (participation: ParticipationMonitoringItem) => void;
  onExtensionDaysChange: (participationId: string, days: number) => void;
}) {
  return (
    <section className="overflow-x-auto rounded-xl border border-border bg-card">
      <table data-server-pagination className="w-full min-w-[1240px] text-left text-sm">
        <thead className="border-b border-border bg-muted/40 text-[11px] uppercase text-muted-foreground">
          <tr>
            <th className="px-4 py-3 font-medium">Participante</th>
            <th className="px-4 py-3 font-medium">Tipo e processo</th>
            <th className="px-4 py-3 font-medium">Situação</th>
            <th className="px-4 py-3 font-medium">Progresso</th>
            <th className="px-4 py-3 font-medium">Validade</th>
            <th className="px-4 py-3 text-right font-medium">Ações</th>
          </tr>
        </thead>
        <tbody>
          {participations.map((participation) => (
            <tr
              key={`${participation.participationType}-${participation.participationId}`}
              className="border-b border-border last:border-0 hover:bg-accent/30"
            >
              <td className="px-4 py-3">
                <div className="font-medium">{participation.candidateName}</div>
                <div className="text-xs text-muted-foreground">{participation.candidateEmail}</div>
              </td>
              <td className="px-4 py-3">
                <ProcessSummary participation={participation} />
              </td>
              <td className="px-4 py-3">
                <ParticipationStatusBadge participation={participation} />
              </td>
              <td className="px-4 py-3">
                {participation.currentTurn}/{participation.estimatedTurns} ·{" "}
                {participation.progressPercent}%
              </td>
              <td className="px-4 py-3">
                <LinkStatus participation={participation} />
              </td>
              <td className="px-4 py-3 text-right">
                <ParticipationActions
                  participation={participation}
                  copied={copiedParticipationId === participation.participationId}
                  extensionDays={extensionDays[participation.participationId] ?? 7}
                  resending={resendingParticipationId === participation.participationId}
                  extending={extendingParticipationId === participation.participationId}
                  canceling={cancelingParticipationId === participation.participationId}
                  onCopy={onCopy}
                  onResend={onResend}
                  onExtend={onExtend}
                  onCancel={onCancel}
                  onExtensionDaysChange={onExtensionDaysChange}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function ProcessSummary({ participation }: { participation: ParticipationMonitoringItem }) {
  const isJourney = participation.participationType === "journey";
  return (
    <div>
      <span
        className={cn(
          "inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase",
          isJourney
            ? "border-primary/30 bg-primary/10 text-primary"
            : "border-border bg-muted text-muted-foreground",
        )}
      >
        {isJourney ? <Workflow className="h-3 w-3" /> : <Link2 className="h-3 w-3" />}
        {isJourney ? "Jornada" : "Individual"}
      </span>
      <div className="mt-1 font-medium">
        {isJourney ? participation.journeyName : participation.simulationName}
      </div>
      <div className="text-xs text-muted-foreground">
        {isJourney
          ? `Sequência ${participation.sequenceKey ?? "principal"}`
          : `Versão ${participation.versionNumber ?? "-"}`}
      </div>
    </div>
  );
}

function ParticipationActions({
  participation,
  copied,
  extensionDays,
  resending,
  extending,
  canceling,
  onCopy,
  onResend,
  onExtend,
  onCancel,
  onExtensionDaysChange,
}: {
  participation: ParticipationMonitoringItem;
  copied: boolean;
  extensionDays: number;
  resending: boolean;
  extending: boolean;
  canceling: boolean;
  onCopy: (participation: ParticipationMonitoringItem) => Promise<void>;
  onResend: (participation: ParticipationMonitoringItem) => void;
  onExtend: (participation: ParticipationMonitoringItem) => void;
  onCancel: (participation: ParticipationMonitoringItem) => void;
  onExtensionDaysChange: (participationId: string, days: number) => void;
}) {
  if (participation.resultAttemptId) {
    return (
      <Button asChild variant="outline" size="sm">
        <Link
          to="/results/$attemptId"
          params={{ attemptId: participation.resultAttemptId }}
          search={{ search: "", simulationId: "", period: "", integrationProvider: "", page: 0 }}
        >
          Analisar resultado
        </Link>
      </Button>
    );
  }

  const linkUnavailable =
    participation.linkStatus === "expired" || participation.linkStatus === "canceled";

  return (
    <div className="flex flex-wrap justify-end gap-2">
      <Button
        variant="outline"
        size="sm"
        disabled={linkUnavailable}
        onClick={() => void onCopy(participation)}
      >
        {copied ? (
          <CheckCircle2 className="mr-1 h-3.5 w-3.5" />
        ) : (
          <Copy className="mr-1 h-3.5 w-3.5" />
        )}
        {copied ? "Copiado" : "Copiar"}
      </Button>

      {participation.canResend && (
        <Button
          variant="outline"
          size="sm"
          disabled={resending}
          onClick={() => onResend(participation)}
        >
          {resending ? "Reenviando..." : "Reenviar"}
        </Button>
      )}

      {participation.canExtend && (
        <div className="flex items-center rounded-md border border-border bg-background">
          <select
            value={extensionDays}
            onChange={(event) =>
              onExtensionDaysChange(participation.participationId, Number(event.target.value))
            }
            aria-label="Dias adicionais"
            className="h-8 border-0 bg-transparent px-2 text-xs outline-none"
          >
            {extensionOptions.map((days) => (
              <option key={days} value={days}>
                +{days} dia(s)
              </option>
            ))}
          </select>
          <button
            type="button"
            disabled={extending}
            onClick={() => onExtend(participation)}
            className="inline-flex h-8 items-center gap-1 border-l border-border px-2 text-xs font-medium text-primary disabled:opacity-50"
          >
            <CalendarPlus className="h-3.5 w-3.5" />
            {extending
              ? "Atualizando..."
              : participation.linkStatus === "expired"
                ? "Reativar"
                : "Ampliar"}
          </button>
        </div>
      )}

      {participation.canCancel && (
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button variant="outline" size="sm" className="text-danger" disabled={canceling}>
              <Trash2 className="mr-1 h-3.5 w-3.5" />
              {canceling ? "Cancelando..." : "Cancelar"}
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Cancelar participação por jornada?</AlertDialogTitle>
              <AlertDialogDescription>
                O link deixará de funcionar e a pessoa não poderá iniciar novas etapas desta
                jornada. O histórico já registrado será preservado.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Voltar</AlertDialogCancel>
              <AlertDialogAction onClick={() => onCancel(participation)}>
                Confirmar cancelamento
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </div>
  );
}

function ParticipationStatusBadge({
  participation,
}: {
  participation: ParticipationMonitoringItem;
}) {
  const meta = statusMeta(participation.status, participation.active);
  const Icon = meta.icon;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2 py-1 text-[11px] font-medium",
        meta.className,
      )}
    >
      <Icon className="h-3.5 w-3.5" />
      {meta.label}
    </span>
  );
}

function LinkStatus({ participation }: { participation: ParticipationMonitoringItem }) {
  const meta = {
    active: {
      label: `Ativo · ${participation.remainingDays} dia(s)`,
      className: "border-success/30 bg-success/10 text-success",
    },
    expiringSoon: {
      label: `Expira em ${participation.remainingDays} dia(s)`,
      className: "border-warning/40 bg-warning/10 text-warning-foreground",
    },
    expired: {
      label: "Expirado",
      className: "border-danger/30 bg-danger/10 text-danger",
    },
    canceled: {
      label: "Cancelado",
      className: "border-border bg-muted text-muted-foreground",
    },
  }[participation.linkStatus];

  return (
    <span
      className={cn(
        "inline-flex rounded-full border px-2 py-1 text-[11px] font-medium",
        meta.className,
      )}
    >
      {meta.label}
    </span>
  );
}

function statusMeta(status: ParticipationStatus, active: boolean) {
  if (status === "inProgress" && !active) {
    return {
      label: "Sem atividade recente",
      icon: AlertTriangle,
      className: "border-warning/40 bg-warning/10 text-warning-foreground",
    };
  }
  return (
    {
      notStarted: {
        label: "Aguardando início",
        icon: Clock3,
        className: "border-border bg-muted text-foreground",
      },
      inProgress: {
        label: "Em andamento",
        icon: UserRoundSearch,
        className: "border-primary/30 bg-primary/10 text-primary",
      },
      completed: {
        label: "Concluída",
        icon: CheckCircle2,
        className: "border-success/30 bg-success/10 text-success",
      },
      abandoned: {
        label: "Cancelada",
        icon: AlertTriangle,
        className: "border-danger/30 bg-danger/10 text-danger",
      },
      expired: {
        label: "Expirada",
        icon: AlertTriangle,
        className: "border-warning/40 bg-warning/10 text-warning-foreground",
      },
    } satisfies Record<
      ParticipationStatus,
      { label: string; icon: typeof Clock3; className: string }
    >
  )[status];
}

function matchesProcessFilter(participation: ParticipationMonitoringItem, filter: ProcessFilter) {
  if (filter === "all") return true;
  if (filter === "waiting") return participation.status === "notStarted";
  if (filter === "active") return participation.status === "inProgress" && participation.active;
  if (filter === "completed") return participation.status === "completed";
  return (
    participation.status === "abandoned" ||
    participation.status === "expired" ||
    (participation.status === "inProgress" && !participation.active)
  );
}

function toAbsoluteUrl(path: string) {
  if (/^https?:\/\//i.test(path)) return path;
  if (typeof window === "undefined") return path;
  return `${window.location.origin}${path.startsWith("/") ? path : `/${path}`}`;
}

async function invalidateParticipationQueries(queryClient: ReturnType<typeof useQueryClient>) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ["participations"] }),
    queryClient.invalidateQueries({ queryKey: ["assessment-journey-attempts"] }),
    queryClient.invalidateQueries({ queryKey: ["candidate-links"] }),
  ]);
}
