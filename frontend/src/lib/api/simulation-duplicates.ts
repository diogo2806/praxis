import { apiRequest } from "@/lib/api/http";
import type { SimulationVersionDetailResponse } from "@/lib/api/praxis";

export function duplicateSimulation(
  simulationId: string,
  versionNumber: number,
  name: string,
): Promise<SimulationVersionDetailResponse> {
  return apiRequest<SimulationVersionDetailResponse>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/versions/${versionNumber}/duplicate`,
    {
      method: "POST",
      body: JSON.stringify({ name }),
    },
    {
      fallbackMessage: (status) =>
        `Não foi possível duplicar a avaliação (${status}).`,
    },
  );
}
