import { getRuntimeConfig } from "@/lib/runtime-config";

const EMPRESA_ROLE = "EMPRESA";
const PARTNER_MANAGER_ROLE = "PARTNER_MANAGER";

export function isPartnerModuleEnabled(): boolean {
  return getRuntimeConfig().partnerModuleEnabled;
}

export function canManagePartners(roles: string[]): boolean {
  return isPartnerModuleEnabled() && roles.includes(PARTNER_MANAGER_ROLE);
}

export function isLegacyCompanyManager(roles: string[]): boolean {
  return roles.length === 1 && roles.includes(EMPRESA_ROLE);
}
