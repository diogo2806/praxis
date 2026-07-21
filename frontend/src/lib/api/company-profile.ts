import { apiRequest } from "@/lib/api/request";

export interface CompanyProfileResponse {
  tradeName: string | null;
  legalName: string | null;
  taxId: string | null;
  corporateEmail: string | null;
  phone: string | null;
  website: string | null;
}

export interface UpdateCompanyProfileRequest {
  tradeName: string;
  legalName: string | null;
  taxId: string | null;
  corporateEmail: string | null;
  phone: string | null;
  website: string | null;
}

export function getCompanyProfile() {
  return apiRequest<CompanyProfileResponse>("/api/v1/company-profile", undefined, {
    fallbackMessage: "Não foi possível carregar o perfil da empresa.",
  });
}

export function updateCompanyProfile(body: UpdateCompanyProfileRequest) {
  return apiRequest<CompanyProfileResponse>(
    "/api/v1/company-profile",
    {
      method: "PUT",
      body: JSON.stringify(body),
    },
    {
      fallbackMessage: "Não foi possível salvar o perfil da empresa.",
    },
  );
}
