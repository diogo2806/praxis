import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError, type ResultDeliveryResponse } from "@/lib/api/praxis";

export interface InAppNotificationResponse {
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
}

export interface ProcessReadyDeliveriesResponse {
  processedCount: number;
  deliveries: ResultDeliveryResponse[];
}

export interface ReprocessDeliveryResponse {
  delivery: ResultDeliveryResponse;
}

export function listNotifications() {
  return monitoringRequest<InAppNotificationResponse[]>("/api/v1/notifications");
}

export function processReadyResultDeliveries() {
  return monitoringRequest<ProcessReadyDeliveriesResponse>(
    "/api/v1/gupy/result-deliveries/process-ready",
    { method: "POST" },
  );
}

export function reprocessResultDelivery(deliveryId: number) {
  return monitoringRequest<ReprocessDeliveryResponse>(
    `/api/v1/gupy/result-deliveries/${deliveryId}/reprocess`,
    { method: "POST" },
  );
}

async function monitoringRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (session.token) headers.Authorization = `Bearer ${session.token}`;

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: { ...headers, ...init?.headers },
  });

  if (!response.ok) {
    throw new PraxisApiError(await readErrorMessage(response), response.status);
  }
  if (response.status === 204) return undefined as T;

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) return response.json() as Promise<T>;
  const text = await response.text();
  return (text.length > 0 ? text : undefined) as T;
}

async function readErrorMessage(response: Response) {
  const fallback = `Falha na API (${response.status})`;
  try {
    const body = (await response.json()) as { mensagem?: string; message?: string; error?: string };
    return body.mensagem ?? body.message ?? body.error ?? fallback;
  } catch {
    return fallback;
  }
}
