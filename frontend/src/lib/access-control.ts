export const ADMIN_ROLE = "ADMIN";
export const EMPRESA_ROLE = "EMPRESA";
export const TEAM_MANAGER_ROLE = "TEAM_MANAGER";
export const PARTNER_MANAGER_ROLE = "PARTNER_MANAGER";
export const ASSESSMENT_EDITOR_ROLE = "ASSESSMENT_EDITOR";
export const RESULTS_ANALYST_ROLE = "RESULTS_ANALYST";
export const OPERATIONS_MANAGER_ROLE = "OPERATIONS_MANAGER";
export const PARTNER_SPECIALIST_ROLE = "PARTNER_SPECIALIST";

export type FrontendAccessArea =
  | "public"
  | "platform-admin"
  | "dashboard"
  | "assessments"
  | "assessment-authoring"
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

export type FrontendAction =
  | "assessment:create"
  | "assessment:edit"
  | "assessment:clone-published"
  | "assessment:publish"
  | "competency:manage"
  | "team:manage"
  | "partner:manage"
  | "integration:manage"
  | "billing:manage";

type RoutePolicy = {
  prefix: string;
  area: FrontendAccessArea;
};

const ROUTE_POLICIES: RoutePolicy[] = [
  { prefix: "/login", area: "public" },
  { prefix: "/recuperar-senha", area: "public" },
  { prefix: "/reset-password", area: "public" },
  { prefix: "/convite", area: "public" },
  { prefix: "/candidato", area: "public" },
  { prefix: "/admin", area: "platform-admin" },
  { prefix: "/dashboard", area: "dashboard" },
  { prefix: "/avaliacoes/especialista", area: "assessments" },
  { prefix: "/avaliacoes", area: "assessments" },
  { prefix: "/nova", area: "assessment-authoring" },
  { prefix: "/simulations/new", area: "assessment-authoring" },
  { prefix: "/jornadas", area: "journeys" },
  { prefix: "/participacoes", area: "participations" },
  { prefix: "/enviar-link", area: "participations" },
  { prefix: "/results", area: "results" },
  { prefix: "/resultados", area: "results" },
  { prefix: "/monitoramento", area: "operations" },
  { prefix: "/notifications", area: "operations" },
  { prefix: "/configuracoes/perfil", area: "company-profile" },
  { prefix: "/competencias", area: "competencies" },
  { prefix: "/team", area: "team" },
  { prefix: "/parceiros", area: "partners" },
  { prefix: "/integrations", area: "integrations" },
  { prefix: "/billing", area: "billing" },
  { prefix: "/configuracoes/conta", area: "account" },
  { prefix: "/comecar", area: "onboarding" },
  { prefix: "/manual", area: "manuals" },
];

const COMPANY_ADMIN_ROLES = new Set([EMPRESA_ROLE, TEAM_MANAGER_ROLE, PARTNER_MANAGER_ROLE]);

const ROLE_AREAS: Record<string, ReadonlySet<FrontendAccessArea>> = {
  [ASSESSMENT_EDITOR_ROLE]: new Set([
    "dashboard", "assessments", "assessment-authoring", "competencies", "team", "account", "manuals",
  ]),
  [RESULTS_ANALYST_ROLE]: new Set(["dashboard", "results", "team", "account", "manuals"]),
  [OPERATIONS_MANAGER_ROLE]: new Set([
    "dashboard", "journeys", "participations", "operations", "integrations", "team", "account", "manuals",
  ]),
  [PARTNER_SPECIALIST_ROLE]: new Set([
    "assessments", "assessment-authoring", "competencies", "account", "manuals",
  ]),
};

const ROLE_ACTIONS: Record<string, ReadonlySet<FrontendAction>> = {
  [ASSESSMENT_EDITOR_ROLE]: new Set(["assessment:create", "assessment:edit", "competency:manage"]),
  [PARTNER_SPECIALIST_ROLE]: new Set(["assessment:create", "assessment:edit"]),
  [PARTNER_MANAGER_ROLE]: new Set(["partner:manage"]),
  [TEAM_MANAGER_ROLE]: new Set(["team:manage"]),
  [OPERATIONS_MANAGER_ROLE]: new Set(["integration:manage"]),
};

export function hasRole(roles: string[], role: string): boolean {
  return roles.includes(role);
}

export function isRestrictedPartnerSpecialist(roles: string[]): boolean {
  return hasRole(roles, PARTNER_SPECIALIST_ROLE)
    && !hasRole(roles, EMPRESA_ROLE)
    && !hasRole(roles, ADMIN_ROLE);
}

export function isCompanyAdministrator(roles: string[]): boolean {
  return roles.some((role) => COMPANY_ADMIN_ROLES.has(role));
}

export function resolveDefaultAuthenticatedRoute(
  roles: string[],
): "/admin" | "/avaliacoes/especialista" | "/dashboard" | "/login" {
  if (hasRole(roles, ADMIN_ROLE)) return "/admin";
  if (isRestrictedPartnerSpecialist(roles)) return "/avaliacoes/especialista";
  if (roles.length > 0) return "/dashboard";
  return "/login";
}

export function resolveFrontendArea(pathname: string): FrontendAccessArea | null {
  const normalizedPath = normalizePath(pathname);
  if (normalizedPath === "/") return "public";
  return ROUTE_POLICIES.find(({ prefix }) => matchesPath(normalizedPath, prefix))?.area ?? null;
}

export function canAccessFrontendArea(area: FrontendAccessArea, roles: string[]): boolean {
  if (area === "public") return true;
  if (area === "platform-admin") return hasRole(roles, ADMIN_ROLE);
  if (hasRole(roles, ADMIN_ROLE) || isCompanyAdministrator(roles)) return true;
  return roles.some((role) => ROLE_AREAS[role]?.has(area));
}

export function canAccessFrontendPath(pathname: string, roles: string[]): boolean {
  const area = resolveFrontendArea(pathname);
  return area !== null && canAccessFrontendArea(area, roles);
}

export function canPerformFrontendAction(action: FrontendAction, roles: string[]): boolean {
  if (hasRole(roles, ADMIN_ROLE) || isCompanyAdministrator(roles)) return true;
  return roles.some((role) => ROLE_ACTIONS[role]?.has(action));
}

/**
 * Mantido temporariamente como contrato de compatibilidade para sessões antigas.
 * A autorização não manipula DOM, não observa mutações e não redireciona imperativamente.
 */
export function applyBrowserAccessPolicy(_roles: string[]): void {
  // A política é aplicada declarativamente pelo AppShell e pelos componentes de ação.
}

function normalizePath(pathname: string): string {
  const withoutQuery = pathname.split(/[?#]/, 1)[0] || "/";
  if (withoutQuery.length > 1 && withoutQuery.endsWith("/")) return withoutQuery.slice(0, -1);
  return withoutQuery;
}

function matchesPath(pathname: string, rootPath: string): boolean {
  return pathname === rootPath || pathname.startsWith(`${rootPath}/`);
}
