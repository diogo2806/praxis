export type AppShellSectionKey = "specialistArea" | "workspace" | "operation" | "settings";

export type AppShellLabelKey =
  | "home"
  | "dashboard"
  | "assessments"
  | "competencies"
  | "account"
  | "help"
  | "journeys"
  | "participations"
  | "results"
  | "operations"
  | "settings";

export type AppShellGoalKey = "specialist" | "assessments" | "default";

export interface AppShellContext {
  section: AppShellSectionKey;
  label: AppShellLabelKey;
}

export function resolveAppShellContext(pathname: string, specialist: boolean): AppShellContext {
  if (specialist) {
    if (pathname === "/avaliacoes/especialista") {
      return { section: "specialistArea", label: "home" };
    }
    if (pathname.startsWith("/avaliacoes") || pathname.startsWith("/nova")) {
      return { section: "specialistArea", label: "assessments" };
    }
    if (pathname === "/competencias") {
      return { section: "specialistArea", label: "competencies" };
    }
    if (pathname === "/configuracoes/conta") {
      return { section: "specialistArea", label: "account" };
    }
    if (pathname === "/manual") {
      return { section: "specialistArea", label: "help" };
    }
  }

  if (pathname === "/dashboard") {
    return { section: "workspace", label: "dashboard" };
  }
  if (pathname.startsWith("/avaliacoes") || pathname.startsWith("/nova")) {
    return { section: "workspace", label: "assessments" };
  }
  if (pathname === "/jornadas" || pathname.startsWith("/jornada/")) {
    return { section: "workspace", label: "journeys" };
  }
  if (pathname.startsWith("/participacoes") || pathname === "/enviar-link") {
    return { section: "workspace", label: "participations" };
  }
  if (pathname.startsWith("/results") || pathname === "/talent-match") {
    return { section: "workspace", label: "results" };
  }
  if (pathname === "/monitoramento" || pathname === "/notifications") {
    return { section: "operation", label: "operations" };
  }
  return { section: "settings", label: "settings" };
}

export function resolveAppShellRouteDataKey(pathname: string): string {
  if (pathname === "/avaliacoes/especialista") return "especialista";
  if (pathname.startsWith("/avaliacoes")) return "avaliacoes";
  if (pathname === "/jornadas" || pathname.startsWith("/jornada/")) return "jornadas";
  if (pathname.startsWith("/participacoes") || pathname === "/enviar-link") return "participacoes";
  if (pathname.startsWith("/results")) return "results";
  if (pathname === "/dashboard") return "dashboard";
  return "other";
}

export function resolveAppShellGoalKey(pathname: string, specialist: boolean): AppShellGoalKey {
  if (specialist && pathname === "/avaliacoes/especialista") {
    return "specialist";
  }
  if (pathname.startsWith("/avaliacoes") || pathname.startsWith("/nova")) {
    return "assessments";
  }
  return "default";
}
