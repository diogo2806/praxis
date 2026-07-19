import { apiRequest } from "@/lib/api/http";

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

export function searchMonitoringAttempts(
  filters: MonitoringAttemptFilters,
): Promise<MonitoringAttemptPage> {
  const params = new URLSearchParams({
    page: String(Math.max(0, filters.page)),
    size: String(filters.size ?? 25),
  });
  if (filters.status && filters.status !== "all") params.set("status", filters.status);
  if (filters.simulationId) params.set("simulationId", filters.simulationId);
  if (filters.candidate?.trim()) params.set("candidate", filters.candidate.trim());

  return apiRequest<MonitoringAttemptPage>(
    `/api/v1/candidate-links/attempts?${params.toString()}`,
    {},
    { fallbackMessage: "Não foi possível carregar as tentativas." },
  );
}
