import { PraxisApiError } from "@/lib/api/praxis-legacy";
import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export async function createSimulationBranchNode(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  optionId: string,
) {
  const session = getSession();
  const response = await fetch(
    `${getApiBaseUrl()}/api/v1/simulations/${encodeURIComponent(simulationId)}` +
      `/versions/${versionNumber}/nodes/${encodeURIComponent(nodeId)}` +
      `/options/${encodeURIComponent(optionId)}/branch`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}),
      },
    },
  );

  if (!response.ok) {
    let message = `Falha ao criar a etapa ramificada (${response.status})`;
    try {
      const body = (await response.json()) as { mensagem?: string; message?: string; error?: string };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch {
      // Mantém a mensagem HTTP quando a resposta não contém JSON.
    }
    throw new PraxisApiError(message, response.status);
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json() as Promise<string>;
  }
  return response.text();
}
