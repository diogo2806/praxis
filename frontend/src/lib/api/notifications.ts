import { apiRequest } from "@/lib/api/http";

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
  return apiRequest<InAppNotification[]>("/api/v1/notifications");
}

export function getUnreadNotificationsCount() {
  return apiRequest<UnreadNotificationCountResponse>(
    "/api/v1/notifications/unread-count",
  );
}

export function markNotificationAsRead(notificationId: number) {
  return apiRequest<InAppNotification>(
    `/api/v1/notifications/${notificationId}/read`,
    { method: "POST" },
  );
}

export function reprocessDelivery(deliveryId: number) {
  return apiRequest<void>(
    `/api/v1/gupy/result-deliveries/${deliveryId}/reprocess`,
    { method: "POST" },
  );
}
