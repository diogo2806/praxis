import { apiRequest } from "@/lib/api/http";
import { PraxisApiError } from "@/lib/api/praxis-legacy";

export interface DashboardParticipationSummary {
  total: number;
  started: number;
  notStarted: number;
  inProgress: number;
  completed: number;
  abandoned: number;
  expired: number;
  completionRatePercent: number;
  dropOffRatePercent: number;
  averageScoreLast30Days: number | null;
}

export interface DashboardActivityPoint {
  date: string;
  created: number;
  completed: number;
  abandoned: number;
}

export interface DashboardAnalyticsResponse {
  generatedAt: string;
  periodDays: number;
  participations: DashboardParticipationSummary;
  activity: DashboardActivityPoint[];
}

export async function getDashboardAnalytics(): Promise<DashboardAnalyticsResponse | null> {
  try {
    return await apiRequest<DashboardAnalyticsResponse>("/api/v1/dashboard/analytics");
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}
