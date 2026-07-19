import { apiRequest } from "@/lib/api/http";
import {
  listNotifications,
  type InAppNotification as InAppNotificationResponse,
} from "@/lib/api/notifications";
import type { ResultDeliveryResponse, ResultDeliveryStatus } from "@/lib/api/praxis";

export type { InAppNotificationResponse };
export { listNotifications };

export interface ProcessReadyDeliveriesResponse {
  processedCount: number;
  deliveries: ResultDeliveryResponse[];
}

export interface ReprocessDeliveryResponse {
  delivery: ResultDeliveryResponse;
}

/**
 * Fachada mantida para compatibilidade dos consumidores operacionais.
 * A implementação HTTP pertence a apiRequest e notificações têm uma única fonte.
 */
export function processReadyResultDeliveries() {
  return apiRequest<ProcessReadyDeliveriesResponse>(
    "/api/v1/gupy/result-deliveries/process-ready",
    { method: "POST" },
  );
}

export function reprocessResultDelivery(deliveryId: number) {
  return apiRequest<ReprocessDeliveryResponse>(
    `/api/v1/gupy/result-deliveries/${deliveryId}/reprocess`,
    { method: "POST" },
  );
}

export function listDeliveriesByStatus(status: ResultDeliveryStatus) {
  return apiRequest<ResultDeliveryResponse[]>(
    `/api/v1/gupy/result-deliveries?status=${encodeURIComponent(status)}`,
  );
}
