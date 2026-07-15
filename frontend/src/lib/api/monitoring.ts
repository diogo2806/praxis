import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type MonitoringAttemptStatus =
  | "notStarted"
  | "inProgress"
  | "completed"
  | "abandoned"
  | "expired";

export interface MonitoringAttempt {
  attemptId: string;
  candidateName: string;
  candidateEmail: string;
  simulationId: string;
  simulationName: string;
  versionNumber: number;
  status: MonitoringAttemptStatus;
  currentTurn: number;
  estimatedTurns: number;
  progressPercent: number;
  elapsedSeconds: number;
  lastSignalAt: string;
  active: boolean;
}

export interface MonitoringAttemptPage {
  items: MonitoringAttempt[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MonitoringAttemptFilters {
  page: number;
  size?: number;
  status?: MonitoringAttemptStatus | "all";
  simulationId?: string;
  candidate?: string;
}

export async function searchMonitoringAttempts(
  filters: MonitoringAttemptFilters,
): Promise<MonitoringAttemptPage> {
  const params = new URLSearchParams({
    page: String(Math.max(0, filters.page)),
    size: String(filters.size ?? 25),
  });
  if (filters.status && filters.status !== "all") params.set("status", filters.status);
  if (filters.simulationId) params.set("simulationId", filters.simulationId);
  if (filters.candidate?.trim()) params.set("candidate", filters.candidate.trim());

  const session = getSession();
  const response = await fetch(
    `${getApiBaseUrl()}/api/v1/candidate-links/attempts?${params.toString()}`,
    {
      headers: {
        Accept: "application/json",
        ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
      },
    },
  );

  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string; detail?: string } | null;
    throw new Error(body?.message ?? body?.detail ?? "Não foi possível carregar as tentativas.");
  }
  return response.json() as Promise<MonitoringAttemptPage>;
}
