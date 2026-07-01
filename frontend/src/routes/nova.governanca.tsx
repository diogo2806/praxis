import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  acceptHealthUseTerm,
  acceptResponsibilityTerm,
  getHealthUseAcceptance,
  getHealthUseTerm,
  getResponsibilityAcceptance,
  getResponsibilityTerm,
  listSimulations,
  listSimulationVersionAuditEvents,
  publishSimulationVersion,
  type AuditEventResponse,
  type SimulationSummaryResponse,
  type SimulationVersionStatus,
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
      { name: "description", content: "Publicação validada do teste." },
    ],
  }),
  component: Page,
});

const workflowStates: Array<{ status: string; label: string }> = [
  { status: "draft", label: "Rascunho" },
  { status: "published", label: "No ar" },
];

type TransitionAction = "publish";

const transitionCopy: Record<
  TransitionAction,
  { title: string; description: string; cta: string }
> = {
  publish: {
    title: "Colocar versão no ar?",
    description:
      "Ao entrar no ar, a versão fica protegida contra alterações. Bloqueios críticos continuam sem ajuste manual.",
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
  const auditQuery = useQuery({
    queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
    queryFn: () => listSimulationVersionAuditEvents(search.simulationId!, search.versionNumber!),
    enabled: hasGovernanceParams,
  });

  const transitionMutation = useMutation({
    mutationFn: async () => publishSimulationVersion(search.simulationId!, search.versionNumber!),
    onSuccess: async (response) => {
      setCurrentStatus(response.status);
      setPendingAction(null);
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
      });
    },
    onError: () => {
      setPendingAction(null);
    },
  });

  const termQuery = useQuery({
    queryKey: ["responsibility-term"],
    queryFn: getResponsibilityTerm,
  });
  const acceptanceQuery = useQuery({
    queryKey: ["responsibility-acceptance"],
    queryFn: getResponsibilityAcceptance,
  });
  const acceptMutation = useMutation({
    mutationFn: () => acceptResponsibilityTerm(termQuery.data!.version),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["responsibility-acceptance"] });
    },
  });
  const termAccepted = acceptanceQuery.data?.accepted ?? false;

  // Termo de uso na vertical de saúde (Minuta C). A publicação só o exige quando o empresa opera
  // nessa vertical: o backend bloqueia com 409, e então mostramos o aceite para liberar a republicação.
  const healthTermQuery = useQuery({
    queryKey: ["health-use-term"],
    queryFn: getHealthUseTerm,
  });
  const healthAcceptanceQuery = useQuery({
    queryKey: ["health-use-acceptance"],
    queryFn: getHealthUseAcceptance,
  });
  const healthAcceptMutation = useMutation({
    mutationFn: () => acceptHealthUseTerm(healthTermQuery.data!.version),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["health-use-acceptance"] });
    },
  });
  const healthTermAccepted = healthAcceptanceQuery.data?.accepted ?? false;
  const publishBlockedByHealthTerm =
    !healthTermAccepted &&
    transitionMutation.isError &&
    transitionMutation.error instanceof Error &&
    transitionMutation.error.message.toLowerCase().includes("vertical de saúde");

  const visibleStatus = currentStatus ?? inferStatusFromEvents(auditQuery.data);

  return (
    <AppShell>
      <WizardStepper current="publicacao" />
      <ScreenStateStrip blockedReason="validação automática pendente" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 4</div>
        <h1 className="mt-1 font-display text-3xl">Publicação para colocar no ar</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Antes de entrar no ar, o teste passa pela validação automática e pelo preflight.
        </p>
      </div>

      {hasGovernanceParams && auditQuery.isLoading && (
        <StateBanner tone="info" title="Governança conectada">
          Buscando registro de auditoria do teste {search.simulationId} v{search.versionNumber}.
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
            ? transitionMutation.error.message.replace("publicacao", "publicação")
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
          </ol>

          <ResponsibilityTermGate
            text={termQuery.data?.text ?? ""}
            accepted={termAccepted}
            acceptedAt={acceptanceQuery.data?.acceptedAt ?? null}
            accepting={acceptMutation.isPending}
            failed={acceptMutation.isError}
            canAccept={Boolean(termQuery.data)}
            onAccept={() => acceptMutation.mutate()}
          />

          {(publishBlockedByHealthTerm || healthAcceptMutation.isSuccess) && (
            <HealthUseTermGate
              text={healthTermQuery.data?.text ?? ""}
              accepted={healthTermAccepted}
              acceptedAt={healthAcceptanceQuery.data?.acceptedAt ?? null}
              accepting={healthAcceptMutation.isPending}
              failed={healthAcceptMutation.isError}
              canAccept={Boolean(healthTermQuery.data)}
              onAccept={() => healthAcceptMutation.mutate()}
            />
          )}

          <div className="mt-5 grid gap-2 md:grid-cols-1">
            <TransitionButton
              label="Publicar"
              disabled={!hasGovernanceParams || !termAccepted}
              primary
              onClick={() => setPendingAction("publish")}
            />
            {!termAccepted && (
              <p className="text-xs text-muted-foreground">
                Aceite o termo de responsabilidade acima para liberar a publicação.
              </p>
            )}
          </div>
        </div>
      )}

      <div className="mt-6 rounded-xl border border-border bg-card p-5">
        <h3 className="text-sm font-semibold">Registro de auditoria conectado</h3>
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
                disabled={transitionMutation.isPending}
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

function ResponsibilityTermGate({
  text,
  accepted,
  acceptedAt,
  accepting,
  failed,
  canAccept,
  onAccept,
}: {
  text: string;
  accepted: boolean;
  acceptedAt: string | null;
  accepting: boolean;
  failed: boolean;
  canAccept: boolean;
  onAccept: () => void;
}) {
  return (
    <div className="mt-5 rounded-md border border-border bg-background p-4">
      <h4 className="text-sm font-semibold">Termo de responsabilidade</h4>
      <p className="mt-2 text-sm text-muted-foreground">{text}</p>
      {accepted ? (
        <p className="mt-3 text-xs font-medium text-success">
          Aceito{acceptedAt ? ` em ${formatDateTime(acceptedAt)}` : ""}.
        </p>
      ) : (
        <div className="mt-3">
          <button
            type="button"
            disabled={accepting || !canAccept}
            onClick={onAccept}
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {accepting ? "Registrando..." : "Li e aceito a responsabilidade"}
          </button>
          {failed && (
            <p className="mt-2 text-xs text-danger">
              Não foi possível registrar o aceite. Tente novamente.
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function HealthUseTermGate({
  text,
  accepted,
  acceptedAt,
  accepting,
  failed,
  canAccept,
  onAccept,
}: {
  text: string;
  accepted: boolean;
  acceptedAt: string | null;
  accepting: boolean;
  failed: boolean;
  canAccept: boolean;
  onAccept: () => void;
}) {
  return (
    <div className="mt-4 rounded-md border border-amber-300 bg-amber-50 p-4">
      <h4 className="text-sm font-semibold text-amber-900">Termo de uso na vertical de saúde</h4>
      <p className="mt-1 text-xs text-amber-900/80">
        Esta empresa opera na vertical de saúde. Para publicar, aceite as condições de uso educativo
        e tratamento de dado sensível (LGPD).
      </p>
      <p className="mt-2 text-sm text-muted-foreground">{text}</p>
      {accepted ? (
        <p className="mt-3 text-xs font-medium text-success">
          Aceito{acceptedAt ? ` em ${formatDateTime(acceptedAt)}` : ""}. Você já pode publicar.
        </p>
      ) : (
        <div className="mt-3">
          <button
            type="button"
            disabled={accepting || !canAccept}
            onClick={onAccept}
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {accepting ? "Registrando..." : "Li e aceito o termo de uso em saúde"}
          </button>
          {failed && (
            <p className="mt-2 text-xs text-danger">
              Não foi possível registrar o aceite. Tente novamente.
            </p>
          )}
        </div>
      )}
    </div>
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
        Carregando testes...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/blueprint"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Criar teste
      </Link>
    );
  }

  return (
    <>
      {simulations.slice(0, 3).map((simulation) => {
        const versionNumber = simulation.livePublishedVersionNumber ?? simulation.versionNumber;
        return (
          <Link
            key={`${simulation.id}-${versionNumber}`}
            to="/nova/piloto"
            search={{
              simulationId: simulation.id,
              versionNumber,
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
        );
      })}
    </>
  );
}

function inferStatusFromEvents(events?: AuditEventResponse[]): SimulationVersionStatus | null {
  if (!events?.length) return null;
  const eventTypes = events.map((event) => event.eventType);
  if (eventTypes.includes("simulationVersionPublished")) return "published";
  return "draft";
}

function stateTone(status: string, current: SimulationVersionStatus) {
  const order = ["draft", "published"];
  const statusIndex = order.indexOf(status);
  const currentIndex = order.indexOf(current);
  if (status === current) return "current";
  if (statusIndex !== -1 && currentIndex !== -1 && statusIndex < currentIndex) return "done";
  return "future";
}

function statusLabel(status: SimulationVersionStatus) {
  const labels: Record<string, string> = {
    draft: "rascunho",
    published: "no ar",
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
