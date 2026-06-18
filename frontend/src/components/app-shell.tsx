import { Link, useRouterState } from "@tanstack/react-router";
import type { ReactNode } from "react";
import {
  BarChart3,
  ClipboardCheck,
  Home,
  Link2,
  MessageSquare,
  Scale,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { GlobalErrorFlow, GlobalProductStateBar, StateBanner } from "@/components/praxis-ui";
import { useSession } from "@/lib/session";
import { gupyConnectionLabels, useGupyConnectionState, useViewMode } from "@/lib/view-mode";
import { cn } from "@/lib/utils";

const nav = [
  { to: "/", label: "Painel", icon: Home },
  { to: "/nova/avaliacao", label: "Nova simulação", icon: ClipboardCheck },
  { to: "/monitoramento", label: "Monitoramento", icon: BarChart3 },
  { to: "/enviar-link", label: "Enviar link", icon: Link2 },
  { to: "/candidato", label: "Visão do candidato", icon: MessageSquare },
] as const;

const secondary = [
  { to: "/governanca", label: "Governança & Auditoria", icon: ShieldCheck },
  { to: "/lgpd", label: "LGPD & Explicabilidade", icon: UserRound },
  { to: "/defensabilidade", label: "Defensabilidade", icon: Scale },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const mode = useViewMode();
  const session = useSession();
  const gupyState = useGupyConnectionState(pathname);
  const hasGlobalError = gupyState === "error";
  const modeHref = mode === "technical" ? pathname : `${pathname}?mode=technical`;
  const modeLabel = mode === "technical" ? "Ver modo comercial" : "Ver modo técnico";
  const productState =
    pathname === "/nova/gupy" || pathname === "/nova/publicacao"
      ? {
          gupy: gupyState,
          draft: "published" as const,
          publication: hasGlobalError ? ("blocked" as const) : ("running" as const),
        }
      : pathname === "/nova/validador" || pathname === "/nova/revisao"
        ? { gupy: gupyState, draft: "dirty" as const, publication: "blocked" as const }
        : pathname === "/nova/piloto" ||
            pathname === "/nova/publicacao" ||
            pathname.startsWith("/nova/mapa")
          ? {
              gupy: gupyState,
              draft: "published" as const,
              publication: "idle" as const,
            }
          : { gupy: gupyState, draft: "saved" as const, publication: "idle" as const };

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <aside className="hidden w-72 shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground lg:flex">
        <div className="border-b border-sidebar-border px-6 py-5">
          <div className="flex items-center gap-2 text-xs uppercase text-sidebar-foreground/80">
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-primary" />
            Motor SJT
          </div>
          <div className="mt-2 font-display text-2xl leading-tight">
            Avaliação
            <br />
            <span className="text-sidebar-foreground/85">Situacional</span>
          </div>
          <div className="mt-3 text-[11px] uppercase text-sidebar-foreground/80">
            Integração Gupy - v0.1
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4">
          <div className="px-3 pb-2 text-[10px] font-semibold uppercase text-sidebar-foreground/75">
            Operação
          </div>
          {nav.map((item) => {
            const active =
              item.to === "/"
                ? pathname === "/"
                : pathname === item.to || pathname.startsWith(item.to + "/");
            return (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "mb-1.5 flex items-center gap-3 rounded-md px-3 py-2.5 text-sm transition",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/85 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
                )}
              >
                <item.icon className="h-4 w-4 text-sidebar-foreground/80" />
                {item.label}
                {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" />}
              </Link>
            );
          })}

          <div className="mt-6 px-3 pb-2 text-[10px] font-semibold uppercase text-sidebar-foreground/75">
            Conformidade
          </div>
          {secondary.map((item) => {
            const active = pathname === item.to;
            return (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "mb-1.5 flex items-center gap-3 rounded-md px-3 py-2.5 text-sm transition",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/85 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
                )}
              >
                <item.icon className="h-4 w-4 text-sidebar-foreground/80" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-sidebar-border p-4">
          <div
            className="rounded-md border border-sidebar-border/60 bg-sidebar-accent/40 p-3 text-xs text-sidebar-foreground/80"
            title="A nota do candidato é calculada por regras declaradas (rubrica, peso e cálculo). Nenhum modelo de IA decide ou julga o resultado."
          >
            <div className="font-medium text-sidebar-foreground">100% determinístico</div>
            <p className="mt-1 text-sidebar-foreground/80">
              Sem IA julgando candidato. A nota sai de rubrica, peso e cálculo.
            </p>
          </div>
          <div className="mt-3 flex items-center gap-3 px-1">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
              {session.userName.trim().charAt(0).toUpperCase() || "?"}
            </div>
            <div className="text-xs">
              <div className="font-medium text-sidebar-foreground">{session.userName}</div>
              <div className="text-sidebar-foreground/80">{session.userRole}</div>
            </div>
          </div>
        </div>
      </aside>

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-4 border-b border-border bg-background/85 px-6 backdrop-blur">
          <div className="text-xs text-muted-foreground">
            Espaço de trabalho <span className="text-foreground">/ {session.workspaceName}</span>
          </div>
          <div className="ml-auto flex items-center gap-2 text-xs">
            <span className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground">
              <span className="h-1.5 w-1.5 rounded-full bg-success" />
              {gupyConnectionLabels[gupyState]}
            </span>
            <a
              href={modeHref}
              className="rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground hover:bg-accent"
            >
              {modeLabel}
            </a>
            <button className="rounded-md border border-border bg-card px-3 py-1.5 text-foreground hover:bg-accent">
              Buscar
            </button>
          </div>
        </header>
        <div className="min-h-[calc(100vh-3.5rem)] px-6 py-8 lg:px-10">
          <GlobalProductStateBar state={productState} />
          {hasGlobalError && mode === "commercial" && (
            <StateBanner
              tone="danger"
              title="Erro na integração Gupy"
              action={
                <a
                  href={`${pathname}?mode=technical&gupy=error`}
                  className="shrink-0 rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
                >
                  Abrir diagnóstico
                </a>
              }
            >
              Não foi possível confirmar o envio do resultado.
            </StateBanner>
          )}
          {(mode === "technical" || hasGlobalError) && <GlobalErrorFlow />}
          {children}
        </div>
      </main>
    </div>
  );
}
