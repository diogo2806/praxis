import { getApiBaseUrl } from "@/lib/runtime-config";

export const DATA_SUBJECT_REQUEST_TYPES = [
  "confirmationAccess",
  "rectification",
  "anonymizationDeletion",
  "portability",
  "deletionConsent",
  "informationSharing",
  "consentRevocation",
] as const;

export type DataSubjectRequestType = (typeof DATA_SUBJECT_REQUEST_TYPES)[number];

export type DataSubjectRequestPayload = {
  requestType: DataSubjectRequestType;
  contact?: string | null;
  details?: string | null;
};

type ApiErrorBody = {
  mensagem?: string;
  message?: string;
  error?: string;
};

export async function requestDataSubjectRight(
  attemptToken: string,
  payload: DataSubjectRequestPayload,
): Promise<void> {
  const response = await fetch(
    `${getApiBaseUrl()}/candidate/attempts/${encodeURIComponent(attemptToken)}/data-request`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        requestType: payload.requestType,
        contact: normalizeOptional(payload.contact),
        details: normalizeOptional(payload.details),
      }),
    },
  );

  if (response.ok) return;

  let message = `Não foi possível registrar a solicitação (${response.status}).`;
  try {
    const body = (await response.json()) as ApiErrorBody;
    message = body.mensagem ?? body.message ?? body.error ?? message;
  } catch {
    // Mantém a mensagem HTTP quando a API não retorna JSON.
  }

  throw new Error(message);
}

function normalizeOptional(value?: string | null) {
  const normalized = value?.trim();
  return normalized ? normalized : null;
}
