import { useEffect, useState } from "react";

export type PraxisSession = {
  token: string | null;
  tenantId: string | null;
  workspaceName: string;
  userName: string;
  userRole: string;
  demo: boolean;
};

const anonymousSession: PraxisSession = {
  token: null,
  tenantId: null,
  workspaceName: "Workspace",
  userName: "Usuario",
  userRole: "Operador",
  demo: false,
};

export function getSession(): PraxisSession {
  if (typeof window === "undefined") {
    return anonymousSession;
  }

  return {
    token: localStorage.getItem("praxis.token"),
    tenantId: localStorage.getItem("praxis.tenantId"),
    workspaceName: localStorage.getItem("praxis.workspaceName") ?? "Workspace",
    userName: localStorage.getItem("praxis.userName") ?? "Usuario",
    userRole: localStorage.getItem("praxis.userRole") ?? "Operador",
    demo: false,
  };
}

export function useSession() {
  const [session, setSession] = useState<PraxisSession>(anonymousSession);

  useEffect(() => {
    setSession(getSession());
  }, []);

  return session;
}
