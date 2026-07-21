export const TEAM_MANAGER_ROLE = "TEAM_MANAGER";
export const PARTNER_MANAGER_ROLE = "PARTNER_MANAGER";
export const ASSESSMENT_EDITOR_ROLE = "ASSESSMENT_EDITOR";
export const RESULTS_ANALYST_ROLE = "RESULTS_ANALYST";
export const OPERATIONS_MANAGER_ROLE = "OPERATIONS_MANAGER";
export const EMPRESA_ROLE = "EMPRESA";

export type CompanyAccessArea =
  | "dashboard"
  | "assessments"
  | "journeys"
  | "participations"
  | "results"
  | "operations"
  | "company-profile"
  | "competencies"
  | "team"
  | "partners"
  | "integrations"
  | "billing"
  | "account"
  | "onboarding"
  | "manuals";

export function isLegacyCompanyAdministrator(roles: string[]): boolean {
  return roles.length === 1 && roles.includes(EMPRESA_ROLE);
}

export function isCompanyAdministrator(roles: string[]): boolean {
  return (
    roles.includes(TEAM_MANAGER_ROLE) ||
    roles.includes(PARTNER_MANAGER_ROLE) ||
    isLegacyCompanyAdministrator(roles)
  );
}

export function canAccessCompanyArea(roles: string[], area: CompanyAccessArea): boolean {
  if (isCompanyAdministrator(roles)) return true;

  if (area === "dashboard" || area === "team" || area === "account" || area === "manuals") {
    return roles.some((role) =>
      [ASSESSMENT_EDITOR_ROLE, RESULTS_ANALYST_ROLE, OPERATIONS_MANAGER_ROLE].includes(role),
    );
  }

  if (roles.includes(ASSESSMENT_EDITOR_ROLE)) {
    return area === "assessments" || area === "competencies";
  }

  if (roles.includes(RESULTS_ANALYST_ROLE)) {
    return area === "results";
  }

  if (roles.includes(OPERATIONS_MANAGER_ROLE)) {
    return (
      area === "journeys" ||
      area === "participations" ||
      area === "operations" ||
      area === "integrations"
    );
  }

  return false;
}

export function companyProfileLabel(roles: string[]): string {
  if (isCompanyAdministrator(roles)) return "Administrador";
  if (roles.includes(ASSESSMENT_EDITOR_ROLE)) return "Autor de avaliações";
  if (roles.includes(RESULTS_ANALYST_ROLE)) return "Analista de resultados";
  if (roles.includes(OPERATIONS_MANAGER_ROLE)) return "Operador";
  return "Acesso restrito";
}
