import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Link2, RefreshCw, Send, XCircle } from "lucide-react";
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
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Gupy - Preflight & Conferência - Praxis" },
      {
        name: "description",
        content:
          "Diagnóstico técnico em tempo real da integração do Praxis com testes externos da Gupy.",
      },
    ],
  }),
  component: GupyActivation,
});

const integrationChecks = [
  "A Gupy consegue encontrar as testes que estão no ar.",
  "O convite do candidato abre o teste correta.",
  "A pontuação e as competências são enviadas de volta após a conclusão.",
];

function GupyActivation() {
  const search = Route.useSearch();
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

  return (
    <AppShell>
      <WizardStepper current="publicacao" />
      <ScreenStateStrip blockedReason="lista de verificação incompleta bloqueia a integração Gupy" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 4</div>
          <h1 className="mt-1 text-3xl font-semibold">Gupy - Preflight e conferência</h1>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
            Confira se este teste está pronta para aparecer na Gupy e se os resultados serão
            enviados corretamente depois que o candidato terminar.
          </p>
        </div>
      </div>

      {!hasParams ? (
        <EmptyState
          title="Selecione uma versão para executar a verificação prévia"
          description="A disponibilidade para a Gupy depende do diagnóstico em tempo real de versões publicadas."
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
            <StateBanner tone="info" title="Verificação conectada">
              Validando o teste {search.simulationId} v{search.versionNumber}.
            </StateBanner>
          )}

          {preflightQuery.isError && (
            <StateBanner tone="danger" title="Não foi possível executar a verificação prévia">
              {preflightQuery.error instanceof Error
                ? preflightQuery.error.message
                : "Verifique se o sistema está disponível e tente novamente."}
            </StateBanner>
          )}

          {preflightQuery.data && (
            <StateBanner
              tone={hasFailure ? "danger" : "ok"}
              title={hasFailure ? "Verificação bloqueada" : "Verificação aprovada"}
            >
              {hasFailure
                ? "Corrija os bloqueios calculados pelo sistema antes de vincular a versão na Gupy."
                : "A versão publicada passou nas verificações exigidas para integração."}
            </StateBanner>
          )}

          <div className="mt-5">
            <NextStepContract
              primary={
                hasFailure
                  ? "Corrigir a lista de verificação antes de vincular o teste na Gupy."
                  : "Vincular a versão publicada dentro da Gupy quando o diagnóstico estiver aprovado."
              }
              secondary="Cliente vincula o teste na vaga dentro da Gupy; o gestor não usa tela externa."
              versionRule="A Gupy lista apenos testes no ar e versões protegidas."
              lockedAfter="O diagnóstico não coloca rascunho no ar nem altera tentativa já iniciada."
            />
          </div>

          <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <main className="space-y-5">
              <section className="rounded-md border border-border bg-card p-5">
                <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
                  <Link2 className="h-4 w-4" />O que será conferido
                </div>
                <div className="grid gap-3">
                  {integrationChecks.map((check) => (
                    <div
                      key={check}
                      className="flex items-start gap-3 rounded-md border border-border bg-background p-3"
                    >
                      <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                      <span className="text-sm text-muted-foreground">{check}</span>
                    </div>
                  ))}
                </div>
              </section>

              <section className="rounded-md border border-border bg-card p-5">
                <h2 className="text-sm font-semibold">Lista de verificação da integração</h2>
                <div className="mt-4 space-y-3">
                  {(preflightQuery.data?.checks ?? []).map((item) => (
                    <PreflightCheck key={item.code} item={item} />
                  ))}
                </div>
              </section>
            </main>

            <aside className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex items-center gap-2 text-sm font-semibold">
                <Send className="h-4 w-4" />
                Envios de resultado
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
        Carregando envios...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-md border border-danger/30 bg-danger/10 p-3 text-sm text-danger">
        Não foi possível carregar os envios.
      </div>
    );
  }

  if (deliveries.length === 0) {
    return (
      <div className="rounded-md border border-border bg-background p-3 text-sm text-muted-foreground">
        Nenhum envio registrado.
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
              próxima tentativa {formatDateTime(delivery.nextAttemptAt)}
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
