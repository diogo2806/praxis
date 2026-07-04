import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Bell, CheckCircle2, ExternalLink, Loader2, RefreshCw } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import {
  listNotifications,
  markNotificationAsRead,
  reprocessDelivery,
  type InAppNotification,
} from "@/lib/api/notifications";
import { listResultDeliveries, type ResultDeliveryResponse } from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/notifications")({
  head: () => ({
    meta: [{ title: "Notificações e falhas de entrega - Práxis" }],
  }),
  component: NotificationsPage,
});

function NotificationsPage() {
  const queryClient = useQueryClient();
  const notificationsQuery = useQuery({ queryKey: ["notifications"], queryFn: listNotifications, retry: false });
  const failedDeliveriesQuery = useQuery({
    queryKey: ["result-deliveries", "dlq"],
    queryFn: () => listResultDeliveries({ status: "dlq" }),
    retry: false,
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

  const loading = notificationsQuery.isLoading || failedDeliveriesQuery.isLoading;
  const error = notificationsQuery.error ?? failedDeliveriesQuery.error;

  return (
    <AppShell>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold text-foreground">Notificações e falhas de entrega</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Acompanhe alertas internos, marque pendências como lidas e reenvie resultados que não chegaram à integração externa.
          </p>
        </div>
        <Link to="/monitoramento" className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent">
          <ExternalLink className="h-4 w-4" />
          Ver monitoramento
        </Link>
      </div>

      {loading ? (
        <div className="flex items-center justify-center gap-2 rounded-md border border-border bg-card p-10 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          Carregando alertas operacionais...
        </div>
      ) : error ? (
        <StateBanner tone="danger" title="Não foi possível carregar notificações">
          {error instanceof Error ? error.message : "Tente novamente."}
        </StateBanner>
      ) : (
        <div className="grid gap-6 xl:grid-cols-[1fr_1.2fr]">
          <NotificationsList
            notifications={notificationsQuery.data ?? []}
            markingReadId={markReadMutation.variables ?? null}
            isMarkingRead={markReadMutation.isPending}
            onMarkRead={(notificationId) => markReadMutation.mutate(notificationId)}
            error={markReadMutation.error}
          />
          <FailedDeliveriesPanel
            deliveries={failedDeliveriesQuery.data ?? []}
            reprocessingId={reprocessMutation.variables ?? null}
            isReprocessing={reprocessMutation.isPending}
            onReprocess={(deliveryId) => reprocessMutation.mutate(deliveryId)}
            error={reprocessMutation.error}
          />
        </div>
      )}
    </AppShell>
  );
}

function NotificationsList({
  notifications,
  markingReadId,
  isMarkingRead,
  onMarkRead,
  error,
}: {
  notifications: InAppNotification[];
  markingReadId: number | null;
  isMarkingRead: boolean;
  onMarkRead: (notificationId: number) => void;
  error: unknown;
}) {
  if (notifications.length === 0) {
    return <EmptyState title="Nenhuma notificação" description="Alertas operacionais aparecerão aqui." />;
  }

  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="mb-4 flex items-center gap-2">
        <Bell className="h-4 w-4 text-primary" />
        <h2 className="text-lg font-semibold">Alertas internos</h2>
      </div>
      {error && (
        <div className="mb-4">
          <StateBanner tone="danger" title="Não foi possível marcar como lida">
            {error instanceof Error ? error.message : "Tente novamente."}
          </StateBanner>
        </div>
      )}
      <div className="space-y-3">
        {notifications.map((notification) => {
          const unread = !notification.readAt;
          const current = isMarkingRead && markingReadId === notification.id;
          return (
            <article key={notification.id} className={cn("rounded-md border border-border bg-background p-4", unread && "border-primary/30 bg-primary/5")}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 className="font-medium text-foreground">{notification.title}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{notification.message}</p>
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
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-muted-foreground">
                <span>{formatDateTime(notification.createdAt)}</span>
                {notification.candidateName && <span>• {notification.candidateName}</span>}
                {notification.outboxEventId && <span>• envio #{notification.outboxEventId}</span>}
                {notification.candidateAttemptId && (
                  <a href={resultHref(notification.candidateAttemptId)} className="font-medium text-primary hover:underline">
                    Ver resultado
                  </a>
                )}
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

function FailedDeliveriesPanel({
  deliveries,
  reprocessingId,
  isReprocessing,
  onReprocess,
  error,
}: {
  deliveries: ResultDeliveryResponse[];
  reprocessingId: number | null;
  isReprocessing: boolean;
  onReprocess: (deliveryId: number) => void;
  error: unknown;
}) {
  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-warning" />
          <h2 className="text-lg font-semibold">Entregas com falha</h2>
        </div>
        <span className="rounded-full border border-border px-2 py-0.5 text-xs text-muted-foreground">
          {deliveries.length} pendente{deliveries.length === 1 ? "" : "s"}
        </span>
      </div>
      {error && (
        <StateBanner tone="danger" title="Reenvio não concluído">
          {error instanceof Error ? error.message : "Tente novamente."}
        </StateBanner>
      )}
      {deliveries.length === 0 ? (
        <div className="rounded-md border border-border bg-background p-8 text-center">
          <CheckCircle2 className="mx-auto h-8 w-8 text-success" />
          <p className="mt-3 text-sm font-medium">Nenhuma entrega com falha.</p>
          <p className="mt-1 text-sm text-muted-foreground">Todos os resultados foram enviados ou ainda estão dentro das tentativas automáticas.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {deliveries.map((delivery) => (
            <article key={delivery.id} className="rounded-md border border-border bg-background p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="font-medium">Envio #{delivery.id}</div>
                  <a href={resultHref(delivery.attemptId)} className="text-sm font-medium text-primary hover:underline">
                    Resultado da tentativa {delivery.attemptId}
                  </a>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {delivery.lastError || "Sem detalhe registrado. Tente reenviar ou acione o suporte se a falha continuar."}
                  </p>
                </div>
                <button
                  type="button"
                  disabled={isReprocessing && reprocessingId === delivery.id}
                  onClick={() => onReprocess(delivery.id)}
                  className="inline-flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-xs hover:bg-accent disabled:opacity-60"
                >
                  {isReprocessing && reprocessingId === delivery.id ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <RefreshCw className="h-3.5 w-3.5" />}
                  Reenviar resultado
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function resultHref(attemptId: string) {
  return `/results/${encodeURIComponent(attemptId)}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
