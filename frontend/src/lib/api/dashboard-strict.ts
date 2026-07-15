import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError, type DashboardResponse } from "./praxis";

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

  return response.json() as Promise<DashboardResponse>;
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
