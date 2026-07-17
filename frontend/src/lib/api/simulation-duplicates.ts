import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError, type SimulationVersionDetailResponse } from "@/lib/api/praxis";

export async function duplicateSimulation(
  simulationId: string,
  versionNumber: number,
  name: string,
): Promise<SimulationVersionDetailResponse> {
  const session = getSession();
  const response = await fetch(
    `${getApiBaseUrl()}/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/duplicate`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
      },
      body: JSON.stringify({ name }),
    },
  );

  if (!response.ok) {
    let message = `Não foi possível duplicar a avaliação (${response.status}).`;
    try {
      const body = (await response.json()) as {
        mensagem?: string;
        message?: string;
        error?: string;
      };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch {
      // Mantém a mensagem contextual quando a resposta não vem em JSON.
    }
    throw new PraxisApiError(message, response.status);
  }

  return response.json() as Promise<SimulationVersionDetailResponse>;
}
