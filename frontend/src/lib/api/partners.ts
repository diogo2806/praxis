import { PraxisApiError } from "@/lib/api/praxis-legacy";
import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";

export type PartnerProvider = "GUPY" | "RECRUTEI";
export type PartnerUserStatus = "ATIVO" | "CONVIDADO" | "BLOQUEADO";

export interface PartnerSpecialist {
  id: number;
  name: string;
  email: string;
  status: PartnerUserStatus;
  createdAt: string;
}

export interface PartnerClient {
  id: string;
  name: string;
  externalCompanyId: string;
  provider: PartnerProvider;
  active: boolean;
  tokenConfigured: boolean;
  assignedTests: number;
  createdAt: string;
}

export interface PartnerCatalogItem {
  simulationId: string;
  name: string;
  description: string;
  assigned: boolean;
}

export interface PartnerClientToken {
  clientId: string;
  provider: PartnerProvider;
  token: string;
  createdAt: string;
}

export interface PartnerInviteResponse {
  user: {
    id: number;
    name: string;
    email: string;
    roles: string[];
    status: PartnerUserStatus;
    createdAt: string;
  };
  inviteUrl: string;
}

export function listPartnerSpecialists() {
  return partnerRequest<PartnerSpecialist[]>("/api/v1/partners/specialists");
}

export function invitePartnerSpecialist(payload: { name: string; email: string }) {
  return partnerRequest<PartnerInviteResponse>("/api/v1/partners/specialists/invite", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function removePartnerSpecialist(userId: number) {
  return partnerRequest<void>(`/api/v1/partners/specialists/${userId}/remove`, {
    method: "POST",
  });
}

export function listPartnerClients() {
  return partnerRequest<PartnerClient[]>("/api/v1/partners/clients");
}

export function createPartnerClient(payload: {
  name: string;
  externalCompanyId: string;
  provider: PartnerProvider;
}) {
  return partnerRequest<PartnerClient>("/api/v1/partners/clients", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function setPartnerClientActive(clientId: string, active: boolean) {
  return partnerRequest<PartnerClient>(
    `/api/v1/partners/clients/${clientId}/${active ? "activate" : "deactivate"}`,
    { method: "POST" },
  );
}

export function rotatePartnerClientToken(clientId: string) {
  return partnerRequest<PartnerClientToken>(`/api/v1/partners/clients/${clientId}/token`, {
    method: "POST",
  });
}

export function listPartnerClientCatalog(clientId: string) {
  return partnerRequest<PartnerCatalogItem[]>(`/api/v1/partners/clients/${clientId}/catalog`);
}

export function updatePartnerClientCatalog(clientId: string, simulationIds: string[]) {
  return partnerRequest<PartnerCatalogItem[]>(`/api/v1/partners/clients/${clientId}/catalog`, {
    method: "PUT",
    body: JSON.stringify({ simulationIds }),
  });
}

async function partnerRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (session.token) headers.Authorization = `Bearer ${session.token}`;

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: { ...headers, ...init?.headers },
  });
  if (!response.ok) {
    throw new PraxisApiError(await readErrorMessage(response), response.status);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

async function readErrorMessage(response: Response) {
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
