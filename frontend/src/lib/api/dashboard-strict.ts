import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import type { DashboardResponse } from "./praxis-contract";
import { PraxisApiError } from "./praxis-legacy";

const CANONICAL_ROUTES: Record<string, string> = {
  "/simulations/new": "/nova/avaliacao",
  "/candidate-links/new": "/enviar-link",
  "/assessment-journeys": "/jornadas",
  "/assessment-journeys/new": "/jornadas",
};

export class DashboardCompatibilityError extends PraxisApiError {
  constructor() {
    super(
      "O backend conectado não disponibiliza o endpoint de dashboard desta versão do frontend.",
      404,
    );
    this.name = "DashboardCompatibilityError";
  }
}

export async function getDashboard(): Promise<DashboardResponse> {
  const session = getSession();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (session.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(`${getApiBaseUrl()}/api/v1/dashboard`, {
    headers,
  });

  if (!response.ok) {
    if (response.status === 404) {
      throw new DashboardCompatibilityError();
    }

    throw new PraxisApiError(await readApiErrorMessage(response), response.status);
  }

  return normalizeDashboardRoutes(await response.json() as DashboardResponse);
}

function normalizeDashboardRoutes(dashboard: DashboardResponse): DashboardResponse {
  return {
    ...dashboard,
    latestResults: dashboard.latestResults.map((item) => ({
      ...item,
      actionRoute: canonicalRoute(item.actionRoute),
    })),
    journeys: dashboard.journeys.map((item) => ({
      ...item,
      actionRoute: canonicalRoute(item.actionRoute),
    })),
    recommendedActions: dashboard.recommendedActions.map((item) => ({
      ...item,
      route: canonicalRoute(item.route),
    })),
  };
}

function canonicalRoute(route: string) {
  return CANONICAL_ROUTES[route] ?? route;
}

async function readApiErrorMessage(response: Response): Promise<string> {
  const fallback = `Falha na API (${response.status})`;

  try {
    const body = (await response.json()) as {
      mensagem?: string;
      message?: string;
      error?: string;
    };
    return body.mensagem ?? body.message ?? body.error ?? fallback;
  } catch {
    return fallback;
  }
}
