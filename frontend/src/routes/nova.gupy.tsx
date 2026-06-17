import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, RefreshCw, Server, Webhook, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  NextStepContract,
  ScreenStateStrip,
  StateBanner,
  StatusBadge,
} from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  activateGupyIntegration,
  getGupyPreflight,
  listResultDeliveries,
  listSimulations,
  type GupyPreflightCheckResponse,
  type ResultDeliveryResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";

export const Route = createFileRoute("/nova/gupy")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
        ? Number(search.versionNumber)
        : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Gupy - Ativacao & Conferencia - Praxis" },
      {
        name: "description",
        content: "Diagnostico tecnico da integracao do Praxis com testes externos da Gupy.",
      },
    ],
  }),
  component: GupyActivation,
});

const endpoints = [
  { method: "GET", path: "/test", description: "lista simulacoes publicadas como Test[]" },
  {
    method: "POST",
    path: "/test/candidate",
    description: "registra candidato e devolve test_url + test_result_id",
  },
  {
    method: "GET",
    path: "/test/result/{resultId}",
    description: "devolve TestResult com score e competencias",
  },
];

function GupyActivation() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const hasParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasParams,
  });
  const preflightQuery = useQuery({
    queryKey: ["gupy-preflight", search.simulationId, search.versionNumber],
    queryFn: () => getGupyPreflight(search.simulationId!, search.versionNumber!),
    enabled: hasParams,
  });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries", search.simulationId, search.versionNumber],
    queryFn: () =>
      listResultDeliveries({
        simulationId: search.simulationId,
        versionNumber: search.versionNumber,
      }),
    enabled: hasParams,
  });
  const hasFailure = preflightQuery.data?.checks.some((item) => item.status === "blocker") ?? false;
  const activateMutation = useMutation({
    mutationFn: () => activateGupyIntegration(search.simulationId!, search.versionNumber!),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ["gupy-preflight", search.simulationId, search.versionNumber],
      });
      await queryClient.invalidateQueries({
        queryKey: ["simulation-audit", search.simulationId, search.versionNumber],
      });
    },
  });

  return (
    <AppShell>
      <WizardStepper current="publicacao" />
      <ScreenStateStrip blockedReason="checklist de ativacao incompleto bloqueia integracao ativa" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 4</div>
          <h1 className="mt-1 text-3xl font-semibold">Gupy - Ativacao & Conferencia</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
            A Gupy consome nossos endpoints externos; esta tela mostra o preflight real da versao e
            a fila real de entrega de resultados.
          </p>
        </div>
      </div>

      {!hasParams ? (
        <EmptyState
          title="Selecione uma versao para executar o preflight"
          description="A ativacao Gupy agora depende do diagnostico retornado pelo backend."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : (
        <>
          {preflightQuery.isLoading && (
            <StateBanner tone="info" title="Preflight conectado">
              Validando simulacao {search.simulationId} v{search.versionNumber}.
            </StateBanner>
          )}

          {preflightQuery.isError && (
            <StateBanner tone="danger" title="Nao foi possivel executar o preflight">
              {preflightQuery.error instanceof Error
                ? preflightQuery.error.message
                : "Verifique se o backend esta rodando."}
            </StateBanner>
          )}

          {preflightQuery.data && (
            <StateBanner
              tone={hasFailure ? "danger" : "ok"}
              title={hasFailure ? "Preflight bloqueado" : "Preflight aprovado"}
            >
              {hasFailure
                ? "Corrija os blockers retornados pelo backend antes de ativar a integracao."
                : preflightQuery.data.integrationActive
                  ? `Integracao ativa desde ${formatDateTime(preflightQuery.data.integrationActivatedAt)}.`
                  : "A versao passou nas verificacoes exigidas para integracao."}
            </StateBanner>
          )}

          {activateMutation.isError && (
            <div className="mt-3">
              <StateBanner tone="danger" title="Nao foi possivel ativar a integracao">
                {activateMutation.error instanceof Error
                  ? activateMutation.error.message
                  : "Verifique se a versao esta publicada e se o preflight esta aprovado."}
              </StateBanner>
            </div>
          )}

          <div className="mt-5">
            <NextStepContract
              primary={
                hasFailure
                  ? "Corrigir checklist de ativacao antes de marcar integracao ativa."
                  : "Registrar conferencia e aguardar ativacao/vinculo dentro da Gupy."
              }
              secondary="Cliente vincula a simulacao na vaga dentro da Gupy; gestor nao usa tela externa."
              versionRule="A Gupy lista apenas testes publicados e versoes imutaveis."
              lockedAfter="Integracao ativa nao publica rascunho nem altera tentativa ja iniciada."
            />
          </div>

          {preflightQuery.data && !preflightQuery.data.integrationActive && (
            <div className="mt-4">
              <button
                type="button"
                onClick={() => activateMutation.mutate()}
                disabled={!preflightQuery.data.ok || activateMutation.isPending}
                className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {activateMutation.isPending ? "Ativando..." : "Marcar integracao como ativa"}
              </button>
            </div>
          )}

          <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <main className="space-y-5">
              <section className="rounded-md border border-border bg-card p-5">
                <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
                  <Server className="h-4 w-4" />
                  Endpoints que expomos
                </div>
                <div className="grid gap-3">
                  {endpoints.map((endpoint) => (
                    <div
                      key={`${endpoint.method}-${endpoint.path}`}
                      className="grid gap-3 rounded-md border border-border bg-background p-3 md:grid-cols-[72px_220px_1fr]"
                    >
                      <span className="rounded-md border border-border bg-card px-2 py-1 text-xs font-semibold">
                        {endpoint.method}
                      </span>
                      <code className="text-sm text-foreground">{endpoint.path}</code>
                      <span className="text-sm text-muted-foreground">{endpoint.description}</span>
                    </div>
                  ))}
                </div>
              </section>

              <section className="rounded-md border border-border bg-card p-5">
                <h2 className="text-sm font-semibold">Checklist de ativacao</h2>
                <div className="mt-4 space-y-3">
                  {(preflightQuery.data?.checks ?? []).map((item) => (
                    <PreflightCheck key={item.code} item={item} />
                  ))}
                </div>
              </section>
            </main>

            <aside className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
                <Webhook className="h-4 w-4" />
                Fila de entrega
              </div>
              <DeliveryList
                deliveries={deliveriesQuery.data ?? []}
                loading={deliveriesQuery.isLoading}
                error={deliveriesQuery.isError}
              />
            </aside>
          </div>
        </>
      )}
    </AppShell>
  );
}

function PreflightCheck({ item }: { item: GupyPreflightCheckResponse }) {
  const ok = item.status === "ok";
  const Icon = ok ? CheckCircle2 : XCircle;
  return (
    <div
      className={`flex items-start gap-3 rounded-md border p-3 ${
        ok ? "border-success/20 bg-success/5" : "border-danger/30 bg-danger/5"
      }`}
    >
      <Icon className={`mt-0.5 h-4 w-4 ${ok ? "text-success" : "text-danger"}`} />
      <div>
        <div className="text-sm font-medium">{formatCheckCode(item.code)}</div>
        <div className="text-xs text-muted-foreground">{item.message}</div>
      </div>
    </div>
  );
}

function DeliveryList({
  deliveries,
  loading,
  error,
}: {
  deliveries: ResultDeliveryResponse[];
  loading: boolean;
  error: boolean;
}) {
  if (loading) {
    return (
      <div className="rounded-md border border-border bg-background p-3 text-sm">
        Carregando entregas...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-md border border-danger/30 bg-danger/10 p-3 text-sm text-danger">
        Nao foi possivel carregar entregas.
      </div>
    );
  }

  if (deliveries.length === 0) {
    return (
      <div className="rounded-md border border-border bg-background p-3 text-sm text-muted-foreground">
        Nenhuma entrega registrada.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {deliveries.slice(0, 5).map((delivery) => (
        <div key={delivery.id} className="rounded-md border border-border bg-background p-3">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <div className="truncate text-sm font-medium">{delivery.resultId}</div>
              <div className="truncate text-xs text-muted-foreground">{delivery.attemptId}</div>
            </div>
            <span className="rounded-md border border-border bg-card px-2 py-1 text-[11px]">
              {delivery.status}
            </span>
          </div>
          {delivery.status === "retrying" && (
            <div className="mt-2 inline-flex items-center gap-2 text-xs text-muted-foreground">
              <RefreshCw className="h-3.5 w-3.5" />
              proxima tentativa {formatDateTime(delivery.nextAttemptAt)}
            </div>
          )}
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
    return (
      <div className="rounded-md border border-border bg-card px-4 py-3 text-sm">
        Carregando simulacoes...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/avaliacao"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Criar simulacao
      </Link>
    );
  }

  return (
    <>
      {simulations.slice(0, 3).map((simulation) => (
        <Link
          key={simulation.id}
          to="/nova/gupy"
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

function formatCheckCode(value: string) {
  return value.replace(/([A-Z])/g, " $1").toLowerCase();
}

function formatDateTime(value: string | null) {
  if (!value) return "sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
