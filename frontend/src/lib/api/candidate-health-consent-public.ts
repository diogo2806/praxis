import { recordHealthConsent as recordHealthConsentLegacy } from "@/lib/api/praxis-legacy";

/**
 * Registra o consentimento persistido e recarrega a tentativa para que o backend volte a entregar
 * a etapa protegida somente após validar a pré-condição de saúde.
 */
export async function recordHealthConsent(
  attemptId: string,
  onBehalfOfMinor = false,
): Promise<void> {
  await recordHealthConsentLegacy(attemptId, onBehalfOfMinor);
  if (typeof window !== "undefined") {
    window.location.reload();
  }
}
