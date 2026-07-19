import { apiRequest } from "@/lib/api/http";
import type {
  CommercialPlanType,
  EmpresaAdminSummary,
  EmpresaStatus,
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
  return apiRequest<EmpresaAdminPageResponse>(
    `/api/admin/empresas/page?${params.toString()}`,
  );
}

/** Mantém o contrato de lista usado pelas telas atuais sobre o endpoint paginado. */
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
