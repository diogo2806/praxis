import { useEffect, useState } from "react";

export type PraxisSession = {
  token: string | null;
  tenantId: string | null;
  workspaceName: string;
  userName: string;
  userRole: string;
};

const anonymousSession: PraxisSession = {
  token: null,
  tenantId: null,
  workspaceName: "Workspace",
  userName: "Usuário",
  userRole: "Operador",
};

export function getSession(): PraxisSession {
  if (typeof window === "undefined") {
    return anonymousSession;
  }

  return {
    token: localStorage.getItem("praxis.token"),
    tenantId: localStorage.getItem("praxis.tenantId"),
    workspaceName: localStorage.getItem("praxis.workspaceName") ?? "Workspace",
    userName: localStorage.getItem("praxis.userName") ?? "Usuário",
    userRole: localStorage.getItem("praxis.userRole") ?? "Operador",
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
  tenantId: string;
  name: string;
  roles: string[];
};

export function saveAuthenticatedSession(response: AuthenticatedSessionResponse) {
  if (typeof window === "undefined") return;

  localStorage.setItem("praxis.token", response.token);
  localStorage.setItem("praxis.tenantId", response.tenantId);
  localStorage.setItem("praxis.workspaceName", response.tenantId);
  localStorage.setItem("praxis.userName", response.name);
  localStorage.setItem("praxis.userRole", response.roles.join(", "));
}