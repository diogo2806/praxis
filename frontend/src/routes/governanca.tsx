import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ArchiveRestore, Lock, RefreshCw } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Termo } from "@/components/glossario";
import { ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  cloneSimulationVersionToDraft,
  listSimulations,
  listSimulationVersionAuditEvents,
  type AuditEventResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";

export const Route = createFileRoute("/governanca")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
        ? Number(search.versionNumber)
        : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Governança & Auditoria - Praxis" },
      { name: "description", content: "Controles operacionais, auditoria e versionamento." },
    ],
  }),
  component: GovernanceHub,
});

function GovernanceHub() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const [versionDialogOpen, setVersionDialogOpen] = useState(false);
  const hasGovernanceParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasGovernanceParams,
  });
  const auditQuery = useQuery({
    queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
    queryFn: () => listSimulationVersionAuditEvents(search.simulationId!, search.versionNumber!),
    enabled: hasGovernanceParams,
  });
  const cloneMutation = useMutation({
    mutationFn: () => cloneSimulationVersionToDraft(search.simulationId!, search.versionNumber!),
    onSuccess: async () => {
      setVersionDialogOpen(false);
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
      });
    },
  });

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="papel atual não tem permissão para esta transição" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">Conformidade</div>
        <h1 className="mt-1 text-3xl font-semibold">Governança e auditoria</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Trilha imutável de decisões, <Termo id="versionamento">versionamento</Termo> e
          reprocessamento restrito a admin.
        </p>
      </div>

      {hasGovernanceParams && auditQuery.isLoading && (
        <StateBanner tone="info" title="Registro de auditoria conectado">
          Buscando eventos da simulação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      )}

      {hasGovernanceParams && auditQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar a auditoria">
          {auditQuery.error instanceof Error
            ? auditQuery.error.message
            : "Verifique sua conexão e tente novamente. Confira também se a versão existe."}
        </StateBanner>
      )}

      {cloneMutation.isSuccess && (
        <StateBanner tone="ok" title="Nova versão criada">
          Rascunho v{cloneMutation.data.newVersionNumber} criado a partir da v
          {cloneMutation.data.sourceVersionNumber}.
        </StateBanner>
      )}

      {cloneMutation.isError && (
        <StateBanner tone="danger" title="Não foi possível criar a versão">
          {cloneMutation.error instanceof Error
            ? cloneMutation.error.message
            : "A mudança não foi permitida pelo sistema."}
        </StateBanner>
      )}

      <div className="mt-5 space-y-5">
        <section className="rounded-md border border-border bg-card p-5">
          <h2 className="text-sm font-semibold">
            <Termo id="auditlog">AuditLog</Termo> (registro de auditoria) imutável
          </h2>
          {hasGovernanceParams ? (
            <AuditEventList events={auditQuery.data ?? []} loading={auditQuery.isLoading} />
          ) : (
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          )}
        </section>
        <aside className="space-y-3">
          <StateBanner tone="warn" title="Edição de publicada cria nova versão">
            Candidatos em andamento continuam na versão atual.
          </StateBanner>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <button
              type="button"
              onClick={() => setVersionDialogOpen(true)}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60"
            >
              <RefreshCw className="h-4 w-4 shrink-0" />
              <span className="min-w-0 truncate">
                {hasGovernanceParams
                  ? `Criar nova versão a partir da v${search.versionNumber}`
                  : "Selecione uma versão"}
              </span>
            </button>
            <button
              disabled
              className="inline-flex min-h-10 cursor-not-allowed items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm opacity-60"
            >
              <ArchiveRestore className="h-4 w-4 shrink-0" />
              Restaurar arquivada
            </button>
            <Link
              to="/nova/publicacao"
              className="inline-flex min-h-10 items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
            >
              Ver etapa do guia (wizard)
            </Link>
            <Link
              to="/"
              className="inline-flex min-h-10 items-center justify-center rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar ao painel
            </Link>
          </div>
          <div className="flex items-start gap-2 rounded-md border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
            <Lock className="mt-0.5 h-3.5 w-3.5" />
            <span>
              Publicar com <Termo id="blocker">bloqueio</Termo> é impedido na mudança de estado, não
              apenas no registro.
            </span>
          </div>
        </aside>
      </div>
      {versionDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/30 p-4">
          <div className="w-full max-w-md rounded-md border border-border bg-card p-5 shadow-xl">
            <div className="text-sm font-semibold">Criar nova versão?</div>
            <p className="mt-2 text-sm text-muted-foreground">
              {hasGovernanceParams
                ? `Isto cria um rascunho a partir da versão ${search.versionNumber}. Candidatos em andamento continuam na versão atual.`
                : "Escolha uma simulação real antes de criar nova versão."}
            </p>
            <div className="mt-4 rounded-md border border-border bg-muted/45 p-3 text-xs text-muted-foreground">
              Um erro de digitação conta como mudança menor. Mudanças em pontuação, peso ou no fluxo
              da conversa contam como mudança maior.
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setVersionDialogOpen(false)}
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  if (hasGovernanceParams) {
                    cloneMutation.mutate();
                  } else {
                    setVersionDialogOpen(false);
                  }
                }}
                disabled={cloneMutation.isPending}
                className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-60"
              >
                {cloneMutation.isPending ? "Criando..." : "Confirmar nova versão"}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function AuditEventList({ events, loading }: { events: AuditEventResponse[]; loading: boolean }) {
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
      {events.map((event, index) => (
        <li key={event.id} className="flex items-center gap-3 py-3 text-sm">
          <span className="flex h-7 w-7 items-center justify-center rounded-md bg-muted text-xs">
            {index + 1}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block truncate">{event.message}</span>
            <span className="mt-1 block text-xs text-muted-foreground">
              {formatEventType(event.eventType)} · {event.aggregateId}
            </span>
          </span>
          <span className="text-xs text-muted-foreground">{formatDateTime(event.createdAt)}</span>
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
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm">
        Carregando simulações...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhuma simulação ativa encontrada.
      </div>
    );
  }

  return (
    <div className="mt-4 grid gap-3">
      {simulations.map((simulation) => (
        <Link
          key={simulation.id}
          to="/governanca"
          search={{
            simulationId: simulation.id,
            versionNumber: simulation.versionNumber,
          }}
          className="grid gap-3 rounded-md border border-border bg-background p-3 text-sm hover:bg-accent md:grid-cols-[1fr_220px]"
        >
          <span>
            <span className="block font-medium">{simulation.name}</span>
            <span className="text-xs text-muted-foreground">v{simulation.versionNumber}</span>
          </span>
          <StatusBadge status={simulation.status} maturity={maturityForStatus(simulation.status)} />
        </Link>
      ))}
    </div>
  );
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
