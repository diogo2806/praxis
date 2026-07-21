import { useEffect, useState } from "react";
import {
  ADMIN_ROLE,
  applyBrowserAccessPolicy,
  hasRole,
  isRestrictedPartnerSpecialist,
} from "@/lib/access-control";
import { getRuntimeConfig, refreshRuntimeConfigFromBackend } from "@/lib/runtime-config";

export type PraxisSession = {
  token: string | null;
  empresaId: string | null;
  workspaceName: string;
  userName: string;
  userRole: string;
  roles: string[];
};

type JwtPayload = {
  empresa_id?: unknown;
  roles?: unknown;
};

const EMPRESA_ROLE = "EMPRESA";
const SECURITY_DISABLED_TOKEN = "praxis-security-disabled";
const KNOWN_ROLES = new Set([
  "ADMIN",
  EMPRESA_ROLE,
  "TEAM_MANAGER",
  "PARTNER_MANAGER",
  "ASSESSMENT_EDITOR",
  "RESULTS_ANALYST",
  "OPERATIONS_MANAGER",
  "PARTNER_SPECIALIST",
]);

const anonymousSession: PraxisSession = {
  token: null,
  empresaId: null,
  workspaceName: "Workspace",
  userName: "Usuário",
  userRole: "Operador",
  roles: [],
};

export function getSession(): PraxisSession {
  if (typeof window === "undefined") {
    return anonymousSession;
  }

  const runtimeConfig = getRuntimeConfig();
  if (!runtimeConfig.securityEnabled) {
    return securityDisabledSession(runtimeConfig.defaultEmpresaId);
  }

  const token = sessionStorage.getItem("praxis.token");
  const payload = decodeJwtPayload(token);
  const storedRoles = parseRoles(sessionStorage.getItem("praxis.userRole"));
  const tokenRoles = parseJwtRoles(payload?.roles);
  const roles = resolveAuthenticatedRoles(token, tokenRoles, storedRoles);
  const empresaId =
    sessionStorage.getItem("praxis.empresaId") ?? readStringClaim(payload?.empresa_id);

  return {
    token,
    empresaId,
    workspaceName: sessionStorage.getItem("praxis.workspaceName") ?? empresaId ?? "Workspace",
    userName: sessionStorage.getItem("praxis.userName") ?? "Usuário",
    userRole: roles.length > 0 ? roles.join(", ") : "Operador",
    roles,
  };
}

export function useSession() {
  const [session, setSession] = useState<PraxisSession>(anonymousSession);

  useEffect(() => {
    let active = true;

    const synchronizeSession = () => {
      if (!active) return;
      const currentSession = getSession();
      setSession(currentSession);
      applyBrowserAccessPolicy(currentSession.roles);
    };

    migrateLegacySession();
    synchronizeSession();
    void refreshRuntimeConfigFromBackend().then(synchronizeSession);

    return () => {
      active = false;
    };
  }, []);

  return session;
}

export type AuthenticatedSessionResponse = {
  token: string;
  empresaId: string;
  name: string;
  roles: string[];
};

export function saveAuthenticatedSession(response: AuthenticatedSessionResponse) {
  if (typeof window === "undefined") return;

  clearLegacyLocalStorage();
  sessionStorage.setItem("praxis.token", response.token);
  sessionStorage.setItem("praxis.empresaId", response.empresaId);
  sessionStorage.setItem("praxis.workspaceName", response.empresaId);
  sessionStorage.setItem("praxis.userName", response.name);
  sessionStorage.setItem("praxis.userRole", response.roles.join(", "));
  applyBrowserAccessPolicy(response.roles);
}

export function clearAuthenticatedSession() {
  if (typeof window === "undefined") return;

  sessionStorage.removeItem("praxis.token");
  sessionStorage.removeItem("praxis.empresaId");
  sessionStorage.removeItem("praxis.workspaceName");
  sessionStorage.removeItem("praxis.userName");
  sessionStorage.removeItem("praxis.userRole");
  clearLegacyLocalStorage();
  applyBrowserAccessPolicy(getSession().roles);
}

export function defaultAuthenticatedRoute():
  | "/admin"
  | "/avaliacoes/especialista"
  | "/dashboard" {
  const roles = getSession().roles;
  if (hasRole(roles, ADMIN_ROLE)) {
    return "/admin";
  }
  if (isRestrictedPartnerSpecialist(roles)) {
    return "/avaliacoes/especialista";
  }
  return "/dashboard";
}

function securityDisabledSession(defaultEmpresaId: string): PraxisSession {
  const empresaId =
    sessionStorage.getItem("praxis.empresaId")?.trim() || defaultEmpresaId || "empresa-1";
  return {
    token: SECURITY_DISABLED_TOKEN,
    empresaId,
    workspaceName: sessionStorage.getItem("praxis.workspaceName") ?? empresaId,
    userName: sessionStorage.getItem("praxis.userName") ?? "Acesso local",
    userRole: "Acesso livre",
    roles: [EMPRESA_ROLE],
  };
}

function migrateLegacySession() {
  if (typeof window === "undefined") return;
  if (!sessionStorage.getItem("praxis.token")) {
    const legacyToken = localStorage.getItem("praxis.token");
    if (legacyToken) {
      sessionStorage.setItem("praxis.token", legacyToken);
      copyLegacy("praxis.empresaId");
      copyLegacy("praxis.workspaceName");
      copyLegacy("praxis.userName");
      copyLegacy("praxis.userRole");
    }
  }
  clearLegacyLocalStorage();
}

function copyLegacy(key: string) {
  const value = localStorage.getItem(key);
  if (value) sessionStorage.setItem(key, value);
}

function clearLegacyLocalStorage() {
  localStorage.removeItem("praxis.token");
  localStorage.removeItem("praxis.empresaId");
  localStorage.removeItem("praxis.workspaceName");
  localStorage.removeItem("praxis.userName");
  localStorage.removeItem("praxis.userRole");
}

function resolveAuthenticatedRoles(
  token: string | null,
  tokenRoles: string[],
  storedRoles: string[],
): string[] {
  if (tokenRoles.length > 0) {
    return tokenRoles;
  }

  const recognizedStoredRoles = storedRoles.filter((role) => KNOWN_ROLES.has(role));
  if (recognizedStoredRoles.length > 0) {
    return recognizedStoredRoles;
  }

  // Sessões criadas antes dos subperfis podem conter somente o token. O backend
  // continua sendo a barreira de autorização; este fallback apenas evita ocultar
  // toda a navegação de uma sessão EMPRESA que já está autenticada.
  return token ? [EMPRESA_ROLE] : [];
}

function parseRoles(value: string | null): string[] {
  if (!value) {
    return [];
  }
  return normalizeRoles(value.split(","));
}

function parseJwtRoles(value: unknown): string[] {
  if (Array.isArray(value)) {
    return normalizeRoles(value.filter((role): role is string => typeof role === "string"));
  }
  if (typeof value === "string") {
    return normalizeRoles(value.split(","));
  }
  return [];
}

function normalizeRoles(roles: string[]): string[] {
  return [...new Set(roles.map(normalizeRole).filter(Boolean))];
}

function normalizeRole(role: string): string {
  return role
    .trim()
    .toUpperCase()
    .replace(/^ROLE_/, "")
    .replace(/[\s-]+/g, "_");
}

function decodeJwtPayload(token: string | null): JwtPayload | null {
  if (!token) {
    return null;
  }

  const payloadSegment = token.split(".")[1];
  if (!payloadSegment) {
    return null;
  }

  try {
    const base64 = payloadSegment.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
    const decoded = window.atob(padded);
    const bytes = Uint8Array.from(decoded, (character) => character.charCodeAt(0));
    const parsed: unknown = JSON.parse(new TextDecoder().decode(bytes));
    return isRecord(parsed) ? (parsed as JwtPayload) : null;
  } catch {
    return null;
  }
}

function readStringClaim(value: unknown): string | null {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

if (typeof window !== "undefined") {
  migrateLegacySession();
  applyBrowserAccessPolicy(getSession().roles);
}
