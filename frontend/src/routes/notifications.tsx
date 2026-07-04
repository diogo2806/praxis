import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  Bell,
  CheckCircle2,
  ExternalLink,
  Inbox,
  Loader2,
  RefreshCw,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import {
  listResultDeliveries,
  PraxisApiError,
  type ResultDeliveryResponse,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/notifications")({
  head: () => ({
    meta: [{ title: "Notificações e DLQ - Práxis" }],
  }),
  component: NotificationsPage,
});

type InAppNotification = {
  id: number;
  type: string;
  title: string;
  message: string;
  candidateAttemptId: string | null;
  candidateName: string | null;
  candidateEmail: string | null;
  outboxEventId: number | null;
  createdAt: string;
  readAt: string | null;
};

function NotificationsPage() {
  const queryClient = useQueryClient();
  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: listNotifications,
    retry: false,
  });
  const dlqQuery = useQuery({
    queryKey: ["result-deliveries", "dlq"],
    queryFn: () => listResultDeliveries({ status: "dlq" }),
    retry: false,
  });
  const reprocessMutation = useMutation({
    mutationFn: reprocessDelivery,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["result-deliveries"] }),
        queryClient.invalidateQueries({ queryKey: ["notifications"] }),
      ]);
    },
  });

  const loading = notificationsQuery.isLoading || dlqQuery.isLoading;
  const error = notificationsQuery.error ?? dlqQuery.error;

  return (
    <AppShell>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold text-foreground">Notificações e DLQ</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Acompanhe alertas internos e reenvie entregas de resultado que falharam, sem depender de operação manual fora do produto.
          </p>
        </div>
        <Link
          to="/monitoramento"
          className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
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
          <NotificationsList notifications={notificationsQuery.data ?? []} />
          <DlqPanel
            deliveries={dlqQuery.data ?? []}
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

function NotificationsList({ notifications }: { notifications: InAppNotification[] }) {
  if (notifications.length === 0) {
    return (
      <EmptyState
        title="Nenhuma notificação"
        description="Quando houver falhas de entrega, alertas de cobrança ou avisos operacionais, eles aparecerão aqui."
      />
    );
  }

  return (
    <section className="rounded-md border border-border bg-card p-5">
      <div className="mb-4 flex items-center gap-2">
        <Bell className="h-4 w-4 text-primary" />
        <h2 className="text-lg font-semibold">Alertas internos</h2>
      </div>
      <div className="space-y-3">
        {notifications.map((notification) => (
          <article
            key={notification.id}
            className={cn(
              "rounded-md border border-border bg-background p-4",
              !notification.readAt && "border-primary/30 bg-primary/5",
            )}
          >
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <h3 className="font-medium text-foreground">{notification.title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{notification.message}</p>
              </div>
              <span className="rounded-full border border-border px-2 py-0.5 text-[10px] uppercase text-muted-foreground">
                {notification.type}
              </span>
            </div>
            <div className="mt-3 flex flex-wrap gap-2 text-xs text-muted-foreground">
              <span>{formatDateTime(notification.createdAt)}</span>
              {notification.candidateName && <span>• {notification.candidateName}</span>}
              {notification.outboxEventId && <span>• entrega #{notification.outboxEventId}</span>}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function DlqPanel({
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
          <h2 className="text-lg font-semibold">Entregas em DLQ</h2>
        </div>
        <span className="rounded-full border border-border px-2 py-0.5 text-xs text-muted-foreground">
          {deliveries.length} pendente{deliveries.length === 1 ? "" : "s"}
        </span>
      </div>

      {error && (
        <StateBanner tone="danger" title="Reprocessamento não concluído">
          {error instanceof Error ? error.message : "Tente novamente."}
        </StateBanner>
      )}

      {deliveries.length === 0 ? (
        <div className="rounded-md border border-border bg-background p-8 text-center">
          <CheckCircle2 className="mx-auto h-8 w-8 text-success" />
          <p className="mt-3 text-sm font-medium">Nenhuma entrega em DLQ.</p>
          <p className="mt-1 text-sm text-muted-foreground">A fila de falhas definitivas está limpa.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-border text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-3 py-2 text-left font-medium">Entrega</th>
                <th className="px-3 py-2 text-left font-medium">Tentativa</th>
                <th className="px-3 py-2 text-left font-medium">Último erro</th>
                <th className="px-3 py-2 text-right font-medium">Ação</th>
              </tr>
            </thead>
            <tbody>
              {deliveries.map((delivery) => (
                <tr key={delivery.id} className="border-b border-border last:border-0">
                  <td className="px-3 py-3 align-top">
                    <div className="font-medium">#{delivery.id}</div>
                    <div className="text-xs text-muted-foreground">{formatDateTime(delivery.createdAt)}</div>
                  </td>
                  <td className="px-3 py-3 align-top">
                    <div>{delivery.attemptId}</div>
                    <div className="text-xs text-muted-foreground">{delivery.attemptCount} tentativa{delivery.attemptCount === 1 ? "" : "s"}</div>
                  </td>
                  <td className="max-w-md px-3 py-3 align-top text-xs text-muted-foreground">
                    {delivery.lastError || "Sem detalhe registrado."}
                  </td>
                  <td className="px-3 py-3 text-right align-top">
                    <button
                      type="button"
                      disabled={isReprocessing && reprocessingId === delivery.id}
                      onClick={() => onReprocess(delivery.id)}
                      className="inline-flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {isReprocessing && reprocessingId === delivery.id ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <RefreshCw className="h-3.5 w-3.5" />
                      )}
                      Reprocessar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

async function listNotifications() {
  return authenticatedRequest<InAppNotification[]>("/api/v1/notifications");
}

async function reprocessDelivery(deliveryId: number) {
  return authenticatedRequest(`/api/v1/gupy/result-deliveries/${deliveryId}/reprocess`, {
    method: "POST",
  });
}

async function authenticatedRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (session.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      ...headers,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let message = `Falha na API (${response.status})`;
    try {
      const body = (await response.json()) as { mensagem?: string; message?: string; error?: string };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch {
      // Mantem mensagem padrao quando a API nao retorna JSON.
    }
    throw new PraxisApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
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
