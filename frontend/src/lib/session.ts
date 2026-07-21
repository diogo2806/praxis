import { useEffect, useState } from "react";
import {
  ADMIN_ROLE,
  applyBrowserAccessPolicy,
  hasRole,
  isRestrictedPartnerSpecialist,
} from "@/lib/access-control";

export type PraxisSession = {
  token: string | null;
  empresaId: string | null;
  workspaceName: string;
  userName: string;
  userRole: string;
  roles: string[];
};

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

  const roles = parseRoles(sessionStorage.getItem("praxis.userRole"));
  return {
    token: sessionStorage.getItem("praxis.token"),
    empresaId: sessionStorage.getItem("praxis.empresaId"),
    workspaceName: sessionStorage.getItem("praxis.workspaceName") ?? "Workspace",
    userName: sessionStorage.getItem("praxis.userName") ?? "Usuário",
    userRole: sessionStorage.getItem("praxis.userRole") ?? "Operador",
    roles,
  };
}

export function useSession() {
  const [session, setSession] = useState<PraxisSession>(anonymousSession);

  useEffect(() => {
    migrateLegacySession();
    const authenticatedSession = getSession();
    setSession(authenticatedSession);
    applyBrowserAccessPolicy(authenticatedSession.roles);
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
  applyBrowserAccessPolicy([]);
}

export function defaultAuthenticatedRoute(): "/admin" | "/avaliacoes" | "/dashboard" {
  const roles = getSession().roles;
  if (hasRole(roles, ADMIN_ROLE)) {
    return "/admin";
  }
  if (isRestrictedPartnerSpecialist(roles)) {
    return "/avaliacoes";
  }
  return "/dashboard";
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

function parseRoles(value: string | null) {
  if (!value) {
    return [];
  }
  return value
    .split(",")
    .map((role) => role.trim())
    .filter(Boolean);
}

if (typeof window !== "undefined") {
  migrateLegacySession();
  applyBrowserAccessPolicy(getSession().roles);
}
