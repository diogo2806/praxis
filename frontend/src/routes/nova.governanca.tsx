import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  approveSimulationVersion,
  listSimulations,
  listSimulationVersionAuditEvents,
  publishSimulationVersion,
  rejectSimulationVersion,
  submitSimulationVersionForReview,
  type AuditEventResponse,
  type SimulationSummaryResponse,
  type SimulationVersionStatus,
  type SimulationVersionStatusResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/governanca")({
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
    meta: [
      { title: "Governança & Aprovações - Praxis" },
      { name: "description", content: "Estados, papéis e versionamento imutável." },
    ],
  }),
  component: Page,
});

const workflowStates: Array<{ status: SimulationVersionStatus; label: string }> = [
  { status: "draft", label: "Rascunho" },
  { status: "inReview", label: "Em revisão" },
  { status: "approved", label: "Aprovada" },
  { status: "published", label: "Publicada" },
];

type TransitionAction = "submit-review" | "approve" | "reject" | "publish";

const transitionCopy: Record<
  TransitionAction,
  { title: string; description: string; cta: string }
> = {
  "submit-review": {
    title: "Enviar para revisão?",
    description:
      "O sistema só aceita esta transição quando a versão está em rascunho ou reprovada e sem bloqueios críticos.",
    cta: "Enviar para revisão",
  },
  approve: {
    title: "Aprovar versão?",
    description: "A versão ficará aprovada e liberada para publicação.",
    cta: "Aprovar",
  },
  reject: {
    title: "Reprovar versão?",
    description:
      "Informe uma justificativa. Ela será enviada ao sistema e preservada no histórico de governança.",
    cta: "Reprovar",
  },
  publish: {
    title: "Publicar versão?",
    description: "A publicação protege a versão contra alterações. Bloqueios críticos continuam sem ajuste manual.",
    cta: "Publicar",
  },
};

function Page() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const hasGovernanceParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasGovernanceParams,
  });
  const [currentStatus, setCurrentStatus] = useState<SimulationVersionStatus | null>(null);
  const [pendingAction, setPendingAction] = useState<TransitionAction | null>(null);
  const [rejectReason, setRejectReason] = useState("Ajustar pontos indicados pela revisão.");
  const auditQuery = useQuery({
    queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
    queryFn: () => listSimulationVersionAuditEvents(search.simulationId!, search.versionNumber!),
    enabled: hasGovernanceParams,
  });

  const transitionMutation = useMutation({
    mutationFn: async (action: TransitionAction) => {
      if (action === "submit-review") {
        return submitSimulationVersionForReview(search.simulationId!, search.versionNumber!);
      }
      if (action === "approve") {
        return approveSimulationVersion(search.simulationId!, search.versionNumber!);
      }
      if (action === "reject") {
        return rejectSimulationVersion(search.simulationId!, search.versionNumber!, rejectReason);
      }
      return publishSimulationVersion(search.simulationId!, search.versionNumber!);
    },
    onSuccess: async (response) => {
      setCurrentStatus(response.status);
      setPendingAction(null);
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
      });
    },
  });

  const visibleStatus = currentStatus ?? inferStatusFromEvents(auditQuery.data);

  return (
    <AppShell>
      <WizardStepper current="publicacao" />
      <ScreenStateStrip blockedReason="aguardando aprovação de gestor ou compliance" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 4</div>
        <h1 className="mt-1 font-display text-3xl">Governança de publicação</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          RH não publica direto em vaga crítica. Estados, papéis, versionamento imutável.
        </p>
      </div>

      {hasGovernanceParams && auditQuery.isLoading && (
        <StateBanner tone="info" title="Governança conectada">
          Buscando registro de auditoria da simulação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      )}

      {hasGovernanceParams && auditQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar a governança">
          {auditQuery.error instanceof Error
            ? auditQuery.error.message
            : "Verifique se o servidor está rodando e se a versão existe."}
        </StateBanner>
      )}

      {transitionMutation.isSuccess && (
        <StateBanner tone="ok" title="Transição aplicada">
          Estado atual retornado pelo sistema: {statusLabel(transitionMutation.data.status)}.
        </StateBanner>
      )}

      {transitionMutation.isError && (
        <StateBanner tone="danger" title="Transição recusada">
          {transitionMutation.error instanceof Error
            ? transitionMutation.error.message
            : "O sistema recusou a transição de estado."}
        </StateBanner>
      )}

      {!hasGovernanceParams ? (
        <EmptyState
          title="Selecione uma versão para governança"
          description="As transições de estado e o registro de auditoria agora dependem do servidor."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : (
        <div className="rounded-xl border border-border bg-card p-5">
          <h3 className="text-sm font-semibold">Estado atual</h3>
          <ol className="mt-4 flex flex-wrap gap-2 text-xs">
            {visibleStatus ? (
              workflowStates.map((state) => {
                const tone = stateTone(state.status, visibleStatus);
                return (
                  <li
                    key={state.status}
                    className={cn(
                      "rounded-full border px-3 py-1.5",
                      tone === "done" && "border-success/30 bg-success/10 text-success",
                      tone === "current" && "border-primary bg-primary/10 text-primary",
                      tone === "future" && "border-border bg-card text-muted-foreground",
                    )}
                  >
                    {tone === "done" ? "ok " : tone === "current" ? "atual " : ""}
                    {state.label}
                  </li>
                );
              })
            ) : (
              <li className="rounded-full border border-border bg-card px-3 py-1.5 text-muted-foreground">
                Sem eventos suficientes para inferir estado
              </li>
            )}
            {visibleStatus === "rejected" && (
              <li className="rounded-full border border-danger/30 bg-danger/10 px-3 py-1.5 text-danger">
                atual Reprovada
              </li>
            )}
          </ol>

          <div className="mt-5 grid gap-2 md:grid-cols-4">
            <TransitionButton
              label="Enviar para revisão"
              disabled={!hasGovernanceParams}
              onClick={() => setPendingAction("submit-review")}
            />
            <TransitionButton
              label="Aprovar"
              disabled={!hasGovernanceParams}
              onClick={() => setPendingAction("approve")}
            />
            <TransitionButton
              label="Reprovar"
              disabled={!hasGovernanceParams}
              onClick={() => setPendingAction("reject")}
            />
            <TransitionButton
              label="Publicar"
              disabled={!hasGovernanceParams}
              primary
              onClick={() => setPendingAction("publish")}
            />
          </div>
        </div>
      )}

      <div className="mt-6 rounded-xl border border-border bg-card p-5">
        <h3 className="text-sm font-semibold">Registro de auditoria imutável</h3>
        {hasGovernanceParams ? (
          <AuditLog events={auditQuery.data ?? []} loading={auditQuery.isLoading} />
        ) : (
          <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
            Selecione uma versão para carregar eventos reais.
          </div>
        )}
      </div>

      <div className="mt-8 flex justify-between">
        <Link
          to="/nova/mapa"
          search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar: Mapa
        </Link>
        <Link
          to="/nova/gupy"
          search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
          className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Gupy: verificação
        </Link>
      </div>

      {pendingAction && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/30 p-4">
          <div className="w-full max-w-md rounded-md border border-border bg-card p-5 shadow-xl">
            <div className="text-sm font-semibold">{transitionCopy[pendingAction].title}</div>
            <p className="mt-2 text-sm text-muted-foreground">
              {transitionCopy[pendingAction].description}
            </p>
            {pendingAction === "reject" && (
              <label className="mt-4 block text-sm">
                <span className="text-xs font-medium text-muted-foreground">Justificativa</span>
                <textarea
                  value={rejectReason}
                  onChange={(event) => setRejectReason(event.target.value)}
                  className="mt-1 min-h-24 w-full rounded-md border border-border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
                />
              </label>
            )}
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setPendingAction(null)}
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => transitionMutation.mutate(pendingAction)}
                disabled={
                  transitionMutation.isPending ||
                  (pendingAction === "reject" && !rejectReason.trim())
                }
                className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-60"
              >
                {transitionMutation.isPending ? "Enviando..." : "Confirmar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function TransitionButton({
  label,
  disabled,
  primary,
  onClick,
}: {
  label: string;
  disabled: boolean;
  primary?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={cn(
        "rounded-md px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60",
        primary
          ? "bg-primary text-primary-foreground hover:bg-primary/90"
          : "border border-border bg-card hover:bg-accent",
      )}
    >
      {label}
    </button>
  );
}

function AuditLog({ events, loading }: { events: AuditEventResponse[]; loading: boolean }) {
  if (loading) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm">
        Carregando eventos...
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhum evento de auditoria registrado para esta versão.
      </div>
    );
  }

  return (
    <ul className="mt-4 divide-y divide-border">
      {events.map((event) => (
        <li key={event.id} className="flex items-start gap-3 py-3 text-sm">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-muted text-[11px] font-medium">
            {event.eventType[0]?.toUpperCase() ?? "A"}
          </div>
          <div className="min-w-0 flex-1">
            <div className="truncate font-medium">{event.message}</div>
            <div className="truncate text-xs text-muted-foreground">
              {formatEventType(event.eventType)} - {event.aggregateId}
            </div>
          </div>
          <div className="text-xs tabular-nums text-muted-foreground">
            {formatDateTime(event.createdAt)}
          </div>
        </li>
      ))}
    </ul>
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
    return (
      <div className="rounded-md border border-border bg-card px-4 py-3 text-sm">
        Carregando simulações...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/avaliacao"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Criar simulação
      </Link>
    );
  }

  return (
    <>
      {simulations.slice(0, 3).map((simulation) => (
        <Link
          key={simulation.id}
          to="/nova/publicacao"
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
      ))}
    </>
  );
}

function inferStatusFromEvents(events?: AuditEventResponse[]): SimulationVersionStatus | null {
  if (!events?.length) return null;
  const eventTypes = events.map((event) => event.eventType);
  if (eventTypes.includes("simulationVersionPublished")) return "published";
  if (eventTypes.includes("simulationVersionApproved")) return "approved";
  if (eventTypes.includes("simulationVersionRejected")) return "rejected";
  if (eventTypes.includes("simulationVersionSubmittedForReview")) return "inReview";
  return "draft";
}

function stateTone(status: SimulationVersionStatus, current: SimulationVersionStatus) {
  const order: SimulationVersionStatus[] = ["draft", "inReview", "approved", "published"];
  const statusIndex = order.indexOf(status);
  const currentIndex = order.indexOf(current);
  if (status === current) return "current";
  if (statusIndex !== -1 && currentIndex !== -1 && statusIndex < currentIndex) return "done";
  return "future";
}

function statusLabel(status: SimulationVersionStatus) {
  const labels: Record<SimulationVersionStatus, string> = {
    draft: "rascunho",
    inReview: "em revisão",
    approved: "aprovada",
    rejected: "reprovada",
    published: "publicada",
    archived: "arquivada",
  };
  return labels[status];
}

function formatEventType(value: string) {
  return value.replace(/([A-Z])/g, " $1").toLowerCase();
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
