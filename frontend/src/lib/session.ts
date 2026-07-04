import { useEffect, useState } from "react";

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

  const roles = parseRoles(localStorage.getItem("praxis.userRole"));
  return {
    token: localStorage.getItem("praxis.token"),
    empresaId: localStorage.getItem("praxis.empresaId"),
    workspaceName: localStorage.getItem("praxis.workspaceName") ?? "Workspace",
    userName: localStorage.getItem("praxis.userName") ?? "Usuário",
    userRole: localStorage.getItem("praxis.userRole") ?? "Operador",
    roles,
  };
}

export function useSession() {
  const [session, setSession] = useState<PraxisSession>(anonymousSession);

  useEffect(() => {
    setSession(getSession());
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

  localStorage.setItem("praxis.token", response.token);
  localStorage.setItem("praxis.empresaId", response.empresaId);
  localStorage.setItem("praxis.workspaceName", response.empresaId);
  localStorage.setItem("praxis.userName", response.name);
  localStorage.setItem("praxis.userRole", response.roles.join(", "));
}

export function clearAuthenticatedSession() {
  if (typeof window === "undefined") return;

  localStorage.removeItem("praxis.token");
  localStorage.removeItem("praxis.empresaId");
  localStorage.removeItem("praxis.workspaceName");
  localStorage.removeItem("praxis.userName");
  localStorage.removeItem("praxis.userRole");
}

export function defaultAuthenticatedRoute(): "/avaliacoes" {
  return "/avaliacoes";
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
