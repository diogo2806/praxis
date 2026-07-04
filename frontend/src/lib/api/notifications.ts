import { PraxisApiError } from "@/lib/api/praxis";
import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type InAppNotification = {
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

export type UnreadNotificationCountResponse = {
  count: number;
};

export function listNotifications() {
  return notificationRequest<InAppNotification[]>("/api/v1/notifications");
}

export function getUnreadNotificationsCount() {
  return notificationRequest<UnreadNotificationCountResponse>("/api/v1/notifications/unread-count");
}

export function markNotificationAsRead(notificationId: number) {
  return notificationRequest<InAppNotification>(`/api/v1/notifications/${notificationId}/read`, "POST");
}

export function reprocessDelivery(deliveryId: number) {
  return notificationRequest(`/api/v1/gupy/result-deliveries/${deliveryId}/reprocess`, "POST");
}

async function notificationRequest<T>(path: string, method = "GET"): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (session.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    method,
    headers,
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
