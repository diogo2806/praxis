import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import {
  PraxisApiError,
  type CommercialPlanType,
  type EmpresaAdminSummary,
  type EmpresaStatus,
} from "@/lib/api/praxis-legacy";

export interface AdminEmpresaFilters {
  search?: string;
  status?: EmpresaStatus;
  plan?: CommercialPlanType;
  periodStart?: string;
  periodEnd?: string;
}

export interface EmpresaAdminPageResponse {
  items: EmpresaAdminSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const PAGE_SIZE = 100;

export function searchAdminEmpresas(page: number, filters: AdminEmpresaFilters = {}) {
  const params = new URLSearchParams();
  params.set("page", String(Math.max(0, page)));
  params.set("size", String(PAGE_SIZE));
  if (filters.search?.trim()) params.set("search", filters.search.trim());
  if (filters.status) params.set("status", filters.status);
  if (filters.plan) params.set("plan", filters.plan);
  if (filters.periodStart) params.set("periodStart", filters.periodStart);
  if (filters.periodEnd) params.set("periodEnd", filters.periodEnd);
  return adminRequest<EmpresaAdminPageResponse>(`/api/admin/empresas/page?${params.toString()}`);
}

/**
 * Mantém o contrato de lista usado pelas telas atuais, mas percorre o endpoint paginado.
 * A API executa as métricas de uso e saldo em lote por página.
 */
export async function listAdminEmpresas(
  filters: AdminEmpresaFilters = {},
): Promise<EmpresaAdminSummary[]> {
  const firstPage = await searchAdminEmpresas(0, filters);
  const empresasById = new Map(
    firstPage.items.map((empresa) => [empresa.empresaId, empresa] as const),
  );

  for (let page = 1; page < firstPage.totalPages; page += 1) {
    const response = await searchAdminEmpresas(page, filters);
    for (const empresa of response.items) {
      empresasById.set(empresa.empresaId, empresa);
    }
  }

  return Array.from(empresasById.values()).sort(
    (left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt),
  );
}

async function adminRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (session.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      ...headers,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    throw new PraxisApiError(await readErrorMessage(response), response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
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
