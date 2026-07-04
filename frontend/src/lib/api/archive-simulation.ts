import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError } from "@/lib/api/praxis";

export async function archiveSimulation(simulationId: string) {
  const session = getSession();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (session.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(
    `${getApiBaseUrl()}/api/v1/simulations/${encodeURIComponent(simulationId)}/archive`,
    {
      method: "POST",
      headers,
    },
  );

  if (!response.ok) {
    let message = `Falha ao arquivar avaliação (${response.status})`;
    try {
      const body = (await response.json()) as { mensagem?: string; message?: string; error?: string };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch {
      // Mantem mensagem padrao quando a API nao retorna JSON.
    }
    throw new PraxisApiError(message, response.status);
  }
}
