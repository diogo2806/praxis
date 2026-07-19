import { apiRequest } from "@/lib/api/http";

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

export function requestDataSubjectRight(
  attemptToken: string,
  payload: DataSubjectRequestPayload,
): Promise<void> {
  return apiRequest<void>(
    `/candidate/attempts/${encodeURIComponent(attemptToken)}/data-request`,
    {
      method: "POST",
      body: JSON.stringify({
        requestType: payload.requestType,
        contact: normalizeOptional(payload.contact),
        details: normalizeOptional(payload.details),
      }),
    },
    {
      authenticated: false,
      fallbackMessage: (status) =>
        `Não foi possível registrar a solicitação (${status}).`,
    },
  );
}

function normalizeOptional(value?: string | null) {
  const normalized = value?.trim();
  return normalized ? normalized : null;
}
