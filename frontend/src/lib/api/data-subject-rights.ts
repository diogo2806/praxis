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

export type CandidatePrivacyNotice = {
  controllerName: string;
  controllerTaxId?: string | null;
  serviceEmail?: string | null;
  serviceUrl?: string | null;
  dpoContact?: string | null;
  legalBasis: string;
  retentionDays: number;
  noticeVersion: string;
  noticeHash: string;
  configured: boolean;
};

export function getCandidatePrivacyNotice(attemptToken: string): Promise<CandidatePrivacyNotice> {
  return apiRequest<CandidatePrivacyNotice>(
    `/candidate/attempts/${encodeURIComponent(attemptToken)}/privacy-notice`,
    { method: "GET" },
    { authenticated: false, fallbackMessage: "Não foi possível carregar o aviso de privacidade." },
  );
}

export function acknowledgeCandidatePrivacyNotice(
  attemptToken: string,
  notice: CandidatePrivacyNotice,
  language: string,
): Promise<void> {
  return apiRequest<void>(
    `/candidate/attempts/${encodeURIComponent(attemptToken)}/privacy-notice/acknowledgement`,
    {
      method: "POST",
      body: JSON.stringify({
        noticeVersion: notice.noticeVersion,
        noticeHash: notice.noticeHash,
        language,
      }),
    },
    {
      authenticated: false,
      fallbackMessage: "Não foi possível registrar a ciência do aviso de privacidade.",
    },
  );
}

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
