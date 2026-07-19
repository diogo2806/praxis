import { apiRequest } from "@/lib/api/http";

export function archiveSimulation(simulationId: string) {
  return apiRequest<void>(
    `/api/v1/simulations/${encodeURIComponent(simulationId)}/archive`,
    { method: "POST" },
    {
      fallbackMessage: (status) => `Falha ao arquivar avaliação (${status})`,
    },
  );
}
