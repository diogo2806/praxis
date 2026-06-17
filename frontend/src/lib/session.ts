import { isDemoMode } from "@/lib/runtime-config";

export type PraxisSession = {
  token: string | null;
  tenantId: string | null;
  workspaceName: string;
  userName: string;
  userRole: string;
  demo: boolean;
};

const demoSession: PraxisSession = {
  token: null,
  tenantId: "tenant-1",
  workspaceName: "Acme S.A.",
  userName: "Renata Silveira",
  userRole: "RH - Aprovadora",
  demo: true,
};

export function getSession(): PraxisSession {
  const demoEnabled = isDemoMode();

  if (demoEnabled || typeof window === "undefined") {
    return demoSession;
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
