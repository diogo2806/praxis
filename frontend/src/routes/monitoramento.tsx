import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  Bell,
  CheckCircle2,
  Clock3,
  ExternalLink,
  Loader2,
  RefreshCw,
  RotateCw,
  XCircle,
} from "lucide-react";
import { useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  listIntegrations,
  listResultDeliveries,
  type IntegrationCenterItem,
  type IntegrationCenterStatus,
  type ResultDeliveryResponse,
} from "@/lib/api/praxis";
import {
  listNotifications,
  markNotificationAsRead,
  reprocessDelivery,
  type InAppNotification,
} from "@/lib/api/notifications";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/monitoramento")({
  head: () => ({
    meta: [
      { title: "Central operacional - Práxis" },
      {
        name: "description",
        content: "Trate somente alertas, falhas, retentativas e entregas que exigem intervenção.",
      },
    ],
  }),
  component: MonitoringPage,
});

type QueueFilter = "all" | "integrations" | "retrying" | "dlq" | "notifications";

function MonitoringPage() {
  const queryClient = useQueryClient();
  const [activeFilter, setActiveFilter] = useState<QueueFilter>("all");

  const integrationsQuery = useQuery({
    queryKey: ["integrations", "monitoring"],
    queryFn: listIntegrations,
    retry: false,
    refetchInterval: 30_000,
  });
  const deliveriesQuery = useQuery({
    queryKey: ["result-deliveries"],
    queryFn: () => listResultDeliveries(),
    retry: false,
    refetchInterval: 30_000,
  });
  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: listNotifications,
    retry: false,
    refetchInterval: 60_000,
  });

  const markReadMutation = useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["notifications"] }),
        queryClient.invalidateQueries({ queryKey: ["notifications", "unread-count"] }),
      ]);
    },
  });

  const reprocessMutation = useMutation({
    mutationFn: reprocessDelivery,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["result-deliveries"] }),
        queryClient.invalidateQueries({ queryKey: ["notifications"] }),
        queryClient.invalidateQueries({ queryKey: ["notifications", "unread-count"] }),
      ]);
    },
  });

  const integrations = integrationsQuery.data ?? [];
  const deliveries = deliveriesQuery.data ?? [];
  const notifications = notificationsQuery.data ?? [];
  const integrationAlerts = integrations.filter(
    (integration) => integration.status === "ERRO" || integration.status === "PENDENTE",
  );
  const retryingDeliveries = deliveries.filter(
    (delivery) => delivery.status === "retrying" || delivery.status === "pending",
  );
  const failedDeliveries = deliveries.filter((delivery) => delivery.status === "dlq");
  const unreadNotifications = notifications.filter((notification) => !notification.readAt);
  const actionableCount =
    integrationAlerts.length +
    retryingDeliveries.length +
    failedDeliveries.length +
    unreadNotifications.length;
  const loading =
    integrationsQuery.isLoading || deliveriesQuery.isLoading || notificationsQuery.isLoading;
  const error = integrationsQuery.error ?? deliveriesQuery.error ?? notificationsQuery.error;
  const refreshing =
    integrationsQuery.isFetching || deliveriesQuery.isFetching || notificationsQuery.isFetching;

  async function refreshAll() {
    await Promise.all([
      integrationsQuery.refetch(),
      deliveriesQuery.refetch(),
      notificationsQuery.refetch(),
    ]);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Operação técnica
            </div>
            <h1 className="mt-1 font-display text-3xl">Central operacional</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Somente exceções acionáveis aparecem aqui. Configuração, credenciais, entregas
              enviadas e integrações saudáveis permanecem fora desta fila.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline" className="gap-2 bg-card">
              <Link to="/integrations">
                <ExternalLink className="h-4 w-4" />
                Configurar integrações
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="gap-2 bg-card"
              onClick={() => void refreshAll()}
              disabled={refreshing}
            >
              <RefreshCw className={cn("h-4 w-4", refreshing && "animate-spin")} />
              Atualizar
            </Button>
          </div>
        </header>

        <section
          className="grid grid-cols-2 gap-3 md:grid-cols-4"
          aria-label="Filtros da fila operacional"
        >
          <QueueMetric
            label="Integrações com atenção"
            value={integrationAlerts.length}
            active={activeFilter === "integrations"}
            onClick={() =>
              setActiveFilter(activeFilter === "integrations" ? "all" : "integrations")
            }
          />
          <QueueMetric
            label="Em retentativa"
            value={retryingDeliveries.length}
            active={activeFilter === "retrying"}
            onClick={() => setActiveFilter(activeFilter === "retrying" ? "all" : "retrying")}
          />
          <QueueMetric
            label="Em DLQ"
            value={failedDeliveries.length}
            active={activeFilter === "dlq"}
            onClick={() => setActiveFilter(activeFilter === "dlq" ? "all" : "dlq")}
          />
          <QueueMetric
            label="Alertas não lidos"
            value={unreadNotifications.length}
            active={activeFilter === "notifications"}
            onClick={() =>
              setActiveFilter(activeFilter === "notifications" ? "all" : "notifications")
            }
          />
        </section>

        {activeFilter !== "all" && (
          <div className="flex items-center justify-between gap-3 rounded-lg border border-border bg-card px-4 py-3 text-sm">
            <span>Filtro ativo: {filterLabel(activeFilter)}</span>
            <button
              type="button"
              onClick={() => setActiveFilter("all")}
              className="font-medium text-primary hover:underline"
            >
              Mostrar toda a fila
            </button>
          </div>
        )}

        {loading ? (
          <section className="rounded-xl border border-border bg-card p-10 text-center text-sm text-muted-foreground">
            Carregando dados operacionais...
          </section>
        ) : error ? (
          <StateBanner
            tone="danger"
            title="Não foi possível carregar a operação"
            action={
              <button
                type="button"
                onClick={() => void refreshAll()}
                className="rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
              >
                Tentar novamente
              </button>
            }
          >
            {error instanceof Error ? error.message : "Tente novamente."}
          </StateBanner>
        ) : actionableCount === 0 ? (
          <StateBanner tone="ok" title="Nenhuma intervenção pendente">
            A fila operacional está vazia. Integrações saudáveis e entregas concluídas podem ser
            consultadas nas respectivas telas proprietárias.
          </StateBanner>
        ) : (
          <div className="space-y-6">
            {(activeFilter === "all" || activeFilter === "integrations") && (
              <IntegrationAttentionPanel integrations={integrationAlerts} />
            )}
            {(activeFilter === "all" || activeFilter === "retrying") && (
              <RetryingDeliveriesPanel deliveries={retryingDeliveries} />
            )}
            {(activeFilter === "all" || activeFilter === "dlq") && (
              <FailedDeliveriesPanel
                deliveries={failedDeliveries}
                reprocessingId={reprocessMutation.variables ?? null}
                reprocessing={reprocessMutation.isPending}
                error={reprocessMutation.error}
                onReprocess={(deliveryId) => reprocessMutation.mutate(deliveryId)}
              />
            )}
            {(activeFilter === "all" || activeFilter === "notifications") && (
              <NotificationsPanel
                notifications={unreadNotifications}
                markingReadId={markReadMutation.variables ?? null}
                markingRead={markReadMutation.isPending}
                error={markReadMutation.error}
                onMarkRead={(notificationId) => markReadMutation.mutate(notificationId)}
              />
            )}
          </div>
        )}
      </main>
    </AppShell>
  );
}

function QueueMetric({
  label,
  value,
  active,
  onClick,
}: {
  label: string;
  value: number;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={cn(
        "rounded-xl border bg-card p-4 text-left transition hover:bg-accent",
        value > 0 ? "border-warning/40" : "border-border",
        active && "border-primary bg-primary/10 ring-2 ring-primary/20",
      )}
    >
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="mt-2 text-3xl font-semibold tabular-nums">
        {value.toLocaleString("pt-BR")}
      </div>
    </button>
  );
}

function IntegrationAttentionPanel({ integrations }: { integrations: IntegrationCenterItem[] }) {
  return (
    <section
      className="rounded-xl border border-border bg-card"
      aria-labelledby="integration-attention-title"
    >
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border p-4">
        <div>
          <h2 id="integration-attention-title" className="text-lg font-semibold">
            Integrações que exigem atenção
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Somente conexões pendentes ou com erro aparecem nesta central.
          </p>
        </div>
        <Button asChild variant="outline" size="sm" className="bg-background">
          <Link to="/integrations">Abrir configurações</Link>
        </Button>
      </div>
      {integrations.length === 0 ? (
        <QueueEmpty message="Nenhuma integração exige intervenção." />
      ) : (
        <div className="grid gap-3 p-4 lg:grid-cols-3">
          {integrations.map((integration) => (
            <IntegrationAlertCard key={integration.provider} integration={integration} />
          ))}
        </div>
      )}
    </section>
  );
}

function IntegrationAlertCard({ integration }: { integration: IntegrationCenterItem }) {
  const slug = {
    GUPY: "gupy",
    RECRUTEI: "recrutei",
    CUSTOM_API: "custom-api",
  }[integration.provider];

  return (
    <article className="rounded-lg border border-warning/40 bg-warning/5 p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold">{integration.name}</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Última atividade: {formatDateTime(integration.lastSyncAt)}
          </p>
        </div>
        <IntegrationStatus status={integration.status} />
      </div>
      {integration.errorMessage && (
        <p className="mt-3 rounded-md bg-danger/10 p-2 text-xs text-danger" role="alert">
          {integration.errorMessage}
        </p>
      )}
      <Link
        to="/integrations/$provider"
        params={{ provider: slug }}
        className="mt-4 inline-flex min-h-10 items-center text-xs font-medium text-primary hover:underline"
      >
        Abrir diagnóstico
      </Link>
    </article>
  );
}

function IntegrationStatus({ status }: { status: IntegrationCenterStatus }) {
  const labels: Record<IntegrationCenterStatus, string> = {
    CONECTADA: "Conectada",
    PENDENTE: "Pendente",
    ERRO: "Erro",
    DESATIVADA: "Desativada",
    NAO_CONFIGURADA: "Não configurada",
  };
  return (
    <span
      className={cn(
        "rounded-full border px-2 py-1 text-[10px] font-semibold uppercase tracking-wide",
        status === "PENDENTE" && "border-warning/40 bg-warning/10 text-warning-foreground",
        status === "ERRO" && "border-danger/30 bg-danger/10 text-danger",
      )}
    >
      {labels[status]}
    </span>
  );
}

function RetryingDeliveriesPanel({ deliveries }: { deliveries: ResultDeliveryResponse[] }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="mb-4 flex items-center gap-2">
        <Clock3 className="h-4 w-4 text-warning" />
        <div>
          <h2 className="text-lg font-semibold">Entregas em retentativa</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            A fila automática ainda está tentando entregar estes resultados. Não reprocesse
            manualmente enquanto houver próxima tentativa agendada.
          </p>
        </div>
      </div>
      {deliveries.length === 0 ? (
        <QueueEmpty message="Nenhuma entrega aguarda retentativa automática." />
      ) : (
        <div className="space-y-3">
          {deliveries.map((delivery) => (
            <article
              key={delivery.id}
              className="rounded-md border border-warning/30 bg-warning/5 p-4"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="font-medium">Envio #{delivery.id}</div>
                  <Link
                    to="/results/$attemptId"
                    params={{ attemptId: delivery.attemptId }}
                    className="mt-1 inline-block text-sm font-medium text-primary hover:underline"
                    search={{
                      search: "",
                      simulationId: "",
                      period: "",
                      integrationProvider: "",
                      page: 0,
                    }}
                  >
                    Resultado relacionado
                  </Link>
                  {delivery.lastError && (
                    <p className="mt-3 rounded-md border border-border bg-card p-3 text-xs text-muted-foreground">
                      {delivery.lastError}
                    </p>
                  )}
                </div>
                <div className="rounded-md border border-warning/30 bg-background px-3 py-2 text-right text-xs">
                  <div className="font-medium">
                    {delivery.status === "pending" ? "Aguardando envio" : "Retentativa automática"}
                  </div>
                  <div className="mt-1 text-muted-foreground">
                    Próxima: {formatDateTime(delivery.nextAttemptAt)}
                  </div>
                  <div className="mt-1 text-muted-foreground">
                    {delivery.attemptCount} tentativa{delivery.attemptCount === 1 ? "" : "s"}
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function FailedDeliveriesPanel({
  deliveries,
  reprocessingId,
  reprocessing,
  error,
  onReprocess,
}: {
  deliveries: ResultDeliveryResponse[];
  reprocessingId: number | null;
  reprocessing: boolean;
  error: unknown;
  onReprocess: (deliveryId: number) => void;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="mb-4 flex items-center gap-2">
        <XCircle className="h-4 w-4 text-danger" />
        <div>
          <h2 className="text-lg font-semibold">Entregas em DLQ</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Falhas que esgotaram a política automática e exigem diagnóstico antes do
            reprocessamento.
          </p>
        </div>
      </div>
      {error != null && (
        <StateBanner tone="danger" title="Nova tentativa não concluída">
          {error instanceof Error ? error.message : "Tente novamente."}
        </StateBanner>
      )}
      {deliveries.length === 0 ? (
        <QueueEmpty message="Nenhuma entrega está em DLQ." />
      ) : (
        <div className="space-y-3">
          {deliveries.map((delivery) => {
            const current = reprocessing && reprocessingId === delivery.id;
            return (
              <article
                key={delivery.id}
                className="rounded-md border border-danger/30 bg-danger/5 p-4"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="font-medium">Envio #{delivery.id}</div>
                    <Link
                      to="/results/$attemptId"
                      params={{ attemptId: delivery.attemptId }}
                      className="mt-1 inline-block text-sm font-medium text-primary hover:underline"
                      search={{
                        search: "",
                        simulationId: "",
                        period: "",
                        integrationProvider: "",
                        page: 0,
                      }}
                    >
                      Resultado relacionado
                    </Link>
                    <p className="mt-3 rounded-md border border-border bg-card p-3 text-xs text-muted-foreground">
                      {delivery.lastError ||
                        "Sem detalhe registrado. Revise a configuração da integração."}
                    </p>
                    <div className="mt-2 text-xs text-muted-foreground">
                      {delivery.attemptCount} tentativa{delivery.attemptCount === 1 ? "" : "s"}
                      {delivery.lastAttemptAt &&
                        ` · Última em ${formatDateTime(delivery.lastAttemptAt)}`}
                    </div>
                  </div>
                  <button
                    type="button"
                    disabled={reprocessing}
                    onClick={() => onReprocess(delivery.id)}
                    className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent disabled:opacity-60"
                  >
                    {current ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <RotateCw className="h-3.5 w-3.5" />
                    )}
                    {current ? "Reprocessando..." : "Tentar novamente"}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

function NotificationsPanel({
  notifications,
  markingReadId,
  markingRead,
  error,
  onMarkRead,
}: {
  notifications: InAppNotification[];
  markingReadId: number | null;
  markingRead: boolean;
  error: unknown;
  onMarkRead: (notificationId: number) => void;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <div className="mb-4 flex items-center gap-2">
        <Bell className="h-4 w-4 text-primary" />
        <div>
          <h2 className="text-lg font-semibold">Alertas não lidos</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Alertas lidos deixam de ocupar a fila operacional.
          </p>
        </div>
      </div>
      {error != null && (
        <StateBanner tone="danger" title="Não foi possível atualizar a notificação">
          {error instanceof Error ? error.message : "Tente novamente."}
        </StateBanner>
      )}
      {notifications.length === 0 ? (
        <QueueEmpty message="Nenhum alerta pendente." />
      ) : (
        <div className="space-y-3">
          {notifications.map((notification) => {
            const current = markingRead && markingReadId === notification.id;
            return (
              <article
                key={notification.id}
                className="rounded-md border border-primary/30 bg-primary/5 p-4"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <h3 className="font-medium">{notification.title}</h3>
                    <p className="mt-1 text-sm text-muted-foreground">{notification.message}</p>
                    <div className="mt-2 flex flex-wrap gap-2 text-xs text-muted-foreground">
                      <span>{formatDateTime(notification.createdAt)}</span>
                      {notification.candidateName && <span>• {notification.candidateName}</span>}
                      {notification.candidateAttemptId && (
                        <Link
                          to="/results/$attemptId"
                          params={{ attemptId: notification.candidateAttemptId }}
                          className="font-medium text-primary hover:underline"
                          search={{
                            search: "",
                            simulationId: "",
                            period: "",
                            integrationProvider: "",
                            page: 0,
                          }}
                        >
                          Ver resultado
                        </Link>
                      )}
                    </div>
                  </div>
                  <button
                    type="button"
                    disabled={current}
                    onClick={() => onMarkRead(notification.id)}
                    className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent disabled:opacity-60"
                  >
                    {current ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <CheckCircle2 className="h-3.5 w-3.5" />
                    )}
                    Marcar como lida
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

function QueueEmpty({ message }: { message: string }) {
  return (
    <div className="p-6">
      <div className="flex items-start gap-3 rounded-lg border border-success/30 bg-success/10 p-4">
        <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-success" />
        <div className="font-medium">{message}</div>
      </div>
    </div>
  );
}

function filterLabel(filter: QueueFilter) {
  const labels: Record<QueueFilter, string> = {
    all: "Toda a fila",
    integrations: "Integrações com atenção",
    retrying: "Entregas em retentativa",
    dlq: "Entregas em DLQ",
    notifications: "Alertas não lidos",
  };
  return labels[filter];
}

function formatDateTime(value: string | null) {
  if (!value) return "Sem data";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
