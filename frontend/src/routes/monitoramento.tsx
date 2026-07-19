import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  AlertTriangle,
  Bell,
  CheckCircle2,
  ExternalLink,
  Loader2,
  PlugZap,
  RefreshCw,
  RotateCw,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
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
        content: "Trate integrações, notificações, retentativas e falhas técnicas do Práxis.",
      },
    ],
  }),
  component: MonitoringPage,
});

function MonitoringPage() {
  const queryClient = useQueryClient();
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
  const failedDeliveries = deliveries.filter((delivery) => delivery.status === "dlq");
  const retryingDeliveries = deliveries.filter(
    (delivery) => delivery.status === "retrying" || delivery.status === "pending",
  );
  const unreadNotifications = notifications.filter((notification) => !notification.readAt);
  const connectedIntegrations = integrations.filter((integration) => integration.status === "CONECTADA");
  const integrationAlerts = integrations.filter(
    (integration) => integration.status === "ERRO" || integration.status === "PENDENTE",
  );
  const loading = integrationsQuery.isLoading || deliveriesQuery.isLoading || notificationsQuery.isLoading;
  const error = integrationsQuery.error ?? deliveriesQuery.error ?? notificationsQuery.error;
  const refreshing = integrationsQuery.isFetching || deliveriesQuery.isFetching || notificationsQuery.isFetching;

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
              Integrações, alertas e falhas de entrega ficam concentrados aqui. O acompanhamento de
              candidatos foi movido para Participações.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild variant="outline" className="gap-2 bg-card">
              <Link to="/participacoes">
                <ExternalLink className="h-4 w-4" />
                Ver participações
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

        <section className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5" aria-label="Resumo operacional">
          <Metric label="Integrações conectadas" value={connectedIntegrations.length} icon={CheckCircle2} />
          <Metric label="Integrações com atenção" value={integrationAlerts.length} icon={AlertTriangle} warning={integrationAlerts.length > 0} />
          <Metric label="Entregas em retentativa" value={retryingDeliveries.length} icon={RotateCw} warning={retryingDeliveries.length > 0} />
          <Metric label="Entregas em DLQ" value={failedDeliveries.length} icon={XCircle} warning={failedDeliveries.length > 0} />
          <Metric label="Alertas não lidos" value={unreadNotifications.length} icon={Bell} warning={unreadNotifications.length > 0} />
        </section>

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
        ) : (
          <>
            <IntegrationHealthPanel integrations={integrations} />
            <div className="grid gap-6 xl:grid-cols-[1fr_1.2fr]">
              <NotificationsPanel
                notifications={notifications}
                markingReadId={markReadMutation.variables ?? null}
                markingRead={markReadMutation.isPending}
                error={markReadMutation.error}
                onMarkRead={(notificationId) => markReadMutation.mutate(notificationId)}
              />
              <FailedDeliveriesPanel
                deliveries={failedDeliveries}
                reprocessingId={reprocessMutation.variables ?? null}
                reprocessing={reprocessMutation.isPending}
                error={reprocessMutation.error}
                onReprocess={(deliveryId) => reprocessMutation.mutate(deliveryId)}
              />
            </div>
          </>
        )}
      </main>
    </AppShell>
  );
}

function Metric({
  label,
  value,
  icon: Icon,
  warning = false,
}: {
  label: string;
  value: number;
  icon: typeof Bell;
  warning?: boolean;
}) {
  return (
    <article className={cn("rounded-xl border bg-card p-4", warning ? "border-warning/40" : "border-border")}>
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs text-muted-foreground">{label}</span>
        <Icon className={cn("h-4 w-4", warning ? "text-warning" : "text-muted-foreground")} />
      </div>
      <div className="mt-2 text-3xl font-semibold tabular-nums">{value.toLocaleString("pt-BR")}</div>
    </article>
  );
}

function IntegrationHealthPanel({ integrations }: { integrations: IntegrationCenterItem[] }) {
  return (
    <section className="rounded-xl border border-border bg-card" aria-labelledby="integration-health-title">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border p-4">
        <div>
          <h2 id="integration-health-title" className="text-lg font-semibold">Saúde das integrações</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Consulte estado, última atividade e erro atual de cada provedor.
          </p>
        </div>
        <Button asChild variant="outline" size="sm" className="bg-background">
          <Link to="/integrations">Configurar integrações</Link>
        </Button>
      </div>
      {integrations.length === 0 ? (
        <EmptyState
          title="Nenhuma integração configurada"
          description="Configure Gupy, Recrutei ou API própria para acompanhar a saúde da conexão."
          actions={
            <Button asChild>
              <Link to="/integrations">Abrir integrações</Link>
            </Button>
          }
        />
      ) : (
        <div className="grid gap-3 p-4 lg:grid-cols-3">
          {integrations.map((integration) => (
            <IntegrationCard key={integration.provider} integration={integration} />
          ))}
        </div>
      )}
    </section>
  );
}

function IntegrationCard({ integration }: { integration: IntegrationCenterItem }) {
  const attention = integration.status === "ERRO" || integration.status === "PENDENTE";
  const slug = { GUPY: "gupy", RECRUTEI: "recrutei", CUSTOM_API: "custom-api" }[integration.provider];
  const Icon = integration.status === "CONECTADA" ? CheckCircle2 : attention ? AlertTriangle : PlugZap;

  return (
    <article className={cn("rounded-lg border p-4", attention ? "border-warning/40 bg-warning/5" : "border-border bg-background")}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <Icon className={cn("h-4 w-4 shrink-0", integration.status === "CONECTADA" ? "text-success" : attention ? "text-warning" : "text-muted-foreground")} />
          <h3 className="truncate text-sm font-semibold">{integration.name}</h3>
        </div>
        <IntegrationStatus status={integration.status} />
      </div>
      <dl className="mt-4 grid gap-2 text-xs">
        <div>
          <dt className="text-muted-foreground">Última atividade</dt>
          <dd className="mt-0.5 font-medium">{formatDateTime(integration.lastSyncAt)}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Configurada em</dt>
          <dd className="mt-0.5 font-medium">{formatDateTime(integration.configuredAt)}</dd>
        </div>
      </dl>
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
        status === "CONECTADA" && "border-success/30 bg-success/10 text-success",
        status === "PENDENTE" && "border-warning/40 bg-warning/10 text-warning-foreground",
        status === "ERRO" && "border-danger/30 bg-danger/10 text-danger",
        (status === "DESATIVADA" || status === "NAO_CONFIGURADA") && "border-border bg-muted/40 text-muted-foreground",
      )}
    >
      {labels[status]}
    </span>
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
          <h2 className="text-lg font-semibold">Alertas operacionais</h2>
          <p className="mt-1 text-xs text-muted-foreground">Notificações foram incorporadas à central operacional.</p>
        </div>
      </div>
      {error && (
        <StateBanner tone="danger" title="Não foi possível atualizar a notificação">
          {error instanceof Error ? error.message : "Tente novamente."}
        </StateBanner>
      )}
      {notifications.length === 0 ? (
        <EmptyState title="Nenhum alerta" description="Falhas e ocorrências operacionais aparecerão aqui." />
      ) : (
        <div className="space-y-3">
          {notifications.map((notification) => {
            const unread = !notification.readAt;
            const current = markingRead && markingReadId === notification.id;
            return (
              <article
                key={notification.id}
                className={cn("rounded-md border border-border bg-background p-4", unread && "border-primary/30 bg-primary/5")}
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
                        >
                          Ver resultado
                        </Link>
                      )}
                    </div>
                  </div>
                  {unread ? (
                    <button
                      type="button"
                      disabled={current}
                      onClick={() => onMarkRead(notification.id)}
                      className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent disabled:opacity-60"
                    >
                      {current ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <CheckCircle2 className="h-3.5 w-3.5" />}
                      Marcar como lida
                    </button>
                  ) : (
                    <span className="rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">Lida</span>
                  )}
                </div>
              </article>
            );
          })}
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
        <AlertTriangle className="h-4 w-4 text-warning" />
        <div>
          <h2 className="text-lg font-semibold">Entregas que exigem intervenção</h2>
          <p className="mt-1 text-xs text-muted-foreground">Reprocesse somente depois de corrigir a causa indicada.</p>
        </div>
      </div>
      {error && (
        <StateBanner tone="danger" title="Nova tentativa não concluída">
          {error instanceof Error ? error.message : "Tente novamente."}
        </StateBanner>
      )}
      {deliveries.length === 0 ? (
        <div className="rounded-md border border-border bg-background p-8 text-center">
          <CheckCircle2 className="mx-auto h-8 w-8 text-success" />
          <p className="mt-3 text-sm font-medium">Nenhuma entrega exige intervenção.</p>
          <p className="mt-1 text-sm text-muted-foreground">Retentativas pendentes continuam automáticas.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {deliveries.map((delivery) => {
            const current = reprocessing && reprocessingId === delivery.id;
            return (
              <article key={delivery.id} className="rounded-md border border-border bg-background p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="font-medium">Envio #{delivery.id}</div>
                    <Link
                      to="/results/$attemptId"
                      params={{ attemptId: delivery.attemptId }}
                      className="mt-1 inline-block text-sm font-medium text-primary hover:underline"
                    >
                      Resultado relacionado
                    </Link>
                    <p className="mt-3 rounded-md border border-border bg-card p-3 text-xs text-muted-foreground">
                      {delivery.lastError || "Sem detalhe registrado. Revise a configuração da integração."}
                    </p>
                    <div className="mt-2 text-xs text-muted-foreground">
                      {delivery.attemptCount} tentativa{delivery.attemptCount === 1 ? "" : "s"}
                      {delivery.lastAttemptAt && ` · Última em ${formatDateTime(delivery.lastAttemptAt)}`}
                    </div>
                  </div>
                  <button
                    type="button"
                    disabled={reprocessing}
                    onClick={() => onReprocess(delivery.id)}
                    className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent disabled:opacity-60"
                  >
                    {current ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <RefreshCw className="h-3.5 w-3.5" />}
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

function formatDateTime(value: string | null) {
  if (!value) return "Sem atividade";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sem atividade";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
