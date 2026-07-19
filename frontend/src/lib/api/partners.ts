import { apiRequest } from "@/lib/api/http";

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
  return apiRequest<PartnerSpecialist[]>("/api/v1/partners/specialists");
}

export function invitePartnerSpecialist(payload: { name: string; email: string }) {
  return apiRequest<PartnerInviteResponse>("/api/v1/partners/specialists/invite", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function removePartnerSpecialist(userId: number) {
  return apiRequest<void>(`/api/v1/partners/specialists/${userId}/remove`, {
    method: "POST",
  });
}

export function listPartnerClients() {
  return apiRequest<PartnerClient[]>("/api/v1/partners/clients");
}

export function createPartnerClient(payload: {
  name: string;
  externalCompanyId: string;
  provider: PartnerProvider;
}) {
  return apiRequest<PartnerClient>("/api/v1/partners/clients", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function setPartnerClientActive(clientId: string, active: boolean) {
  return apiRequest<PartnerClient>(
    `/api/v1/partners/clients/${clientId}/${active ? "activate" : "deactivate"}`,
    { method: "POST" },
  );
}

export function rotatePartnerClientToken(clientId: string) {
  return apiRequest<PartnerClientToken>(
    `/api/v1/partners/clients/${clientId}/token`,
    { method: "POST" },
  );
}

export function listPartnerClientCatalog(clientId: string) {
  return apiRequest<PartnerCatalogItem[]>(
    `/api/v1/partners/clients/${clientId}/catalog`,
  );
}

export function updatePartnerClientCatalog(clientId: string, simulationIds: string[]) {
  return apiRequest<PartnerCatalogItem[]>(
    `/api/v1/partners/clients/${clientId}/catalog`,
    {
      method: "PUT",
      body: JSON.stringify({ simulationIds }),
    },
  );
}
