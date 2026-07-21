import { apiRequest } from "@/lib/api/http";

export type ParticipationType = "individual" | "journey";
export type ParticipationStatus =
  | "notStarted"
  | "inProgress"
  | "completed"
  | "abandoned"
  | "expired";
export type ParticipationLinkStatus = "active" | "expiringSoon" | "expired" | "canceled";

export interface ParticipationMonitoringItem {
  participationId: string;
  participationType: ParticipationType;
  candidateName: string;
  candidateEmail: string;
  simulationId: string | null;
  simulationName: string | null;
  versionNumber: number | null;
  journeyId: string | null;
  journeyName: string | null;
  sequenceKey: string | null;
  status: ParticipationStatus;
  currentTurn: number;
  estimatedTurns: number;
  progressPercent: number;
  elapsedSeconds: number;
  lastSignalAt: string;
  active: boolean;
  candidateUrl: string;
  expiresAt: string;
  linkStatus: ParticipationLinkStatus;
  remainingDays: number;
  canResend: boolean;
  canExtend: boolean;
  canCancel: boolean;
  resultAttemptId: string | null;
  createdAt: string;
}

export interface ParticipationMonitoringPage {
  items: ParticipationMonitoringItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ParticipationFilters {
  page: number;
  size?: number;
  simulationId?: string;
  candidate?: string;
}

export function searchParticipations(
  filters: ParticipationFilters,
): Promise<ParticipationMonitoringPage> {
  const params = new URLSearchParams({
    page: String(Math.max(0, filters.page)),
    size: String(filters.size ?? 25),
  });
  if (filters.simulationId) params.set("simulationId", filters.simulationId);
  if (filters.candidate?.trim()) params.set("candidate", filters.candidate.trim());

  return apiRequest<ParticipationMonitoringPage>(
    `/api/v1/candidate-links/participations?${params.toString()}`,
    {},
    { fallbackMessage: "Não foi possível carregar as participações." },
  );
}

export function resendParticipation(type: ParticipationType, participationId: string) {
  return participationAction(type, participationId, "resend");
}

export function extendParticipation(
  type: ParticipationType,
  participationId: string,
  additionalDays: number,
) {
  return participationAction(type, participationId, "extend", {
    additionalDays,
  });
}

export function cancelParticipation(type: ParticipationType, participationId: string) {
  return participationAction(type, participationId, "cancel");
}

function participationAction(
  type: ParticipationType,
  participationId: string,
  action: "resend" | "extend" | "cancel",
  body?: object,
) {
  return apiRequest<void>(
    `/api/v1/candidate-links/participations/${encodeURIComponent(type)}/${encodeURIComponent(participationId)}/${action}`,
    {
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    },
    { fallbackMessage: "Não foi possível atualizar a participação." },
  );
}
