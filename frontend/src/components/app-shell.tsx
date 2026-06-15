import { Link, useRouterState } from "@tanstack/react-router";
import type { ReactNode } from "react";
import {
  BarChart3,
  ClipboardCheck,
  FileText,
  Home,
  MessageSquare,
  Scale,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { cn } from "@/lib/utils";

const nav = [
  { to: "/", label: "Painel", icon: Home },
  { to: "/nova/blueprint", label: "Nova simulacao", icon: ClipboardCheck },
  { to: "/monitoramento", label: "Monitoramento", icon: BarChart3 },
  { to: "/relatorio/cand-thiago", label: "Relatorio do gestor", icon: FileText },
  { to: "/candidato", label: "Visao do candidato", icon: MessageSquare },
] as const;

const secondary = [
  { to: "/governanca", label: "Governanca & Auditoria", icon: ShieldCheck },
  { to: "/lgpd", label: "LGPD & Explicabilidade", icon: UserRound },
  { to: "/defensabilidade", label: "Defensabilidade", icon: Scale },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <aside className="hidden w-72 shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground lg:flex">
        <div className="border-b border-sidebar-border px-6 py-5">
          <div className="flex items-center gap-2 text-xs uppercase text-sidebar-foreground/60">
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-primary" />
            Motor SJT
          </div>
          <div className="mt-2 font-display text-2xl leading-tight">
            Avaliacao
            <br />
            <span className="text-sidebar-foreground/70">Situacional</span>
          </div>
          <div className="mt-3 text-[11px] uppercase text-sidebar-foreground/50">
            Integracao Gupy - v0.1
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4">
          <div className="px-3 pb-2 text-[10px] font-semibold uppercase text-sidebar-foreground/40">
            Operacao
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
                  "mb-1 flex items-center gap-3 rounded-md px-3 py-2 text-sm transition",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/75 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
                )}
              >
                <item.icon className="h-4 w-4 text-sidebar-foreground/60" />
                {item.label}
                {active && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" />}
              </Link>
            );
          })}

          <div className="mt-6 px-3 pb-2 text-[10px] font-semibold uppercase text-sidebar-foreground/40">
            Conformidade
          </div>
          {secondary.map((item) => {
            const active = pathname === item.to;
            return (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "mb-1 flex items-center gap-3 rounded-md px-3 py-2 text-sm transition",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/65 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
                )}
              >
                <item.icon className="h-4 w-4 text-sidebar-foreground/55" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-sidebar-border p-4">
          <div className="rounded-md border border-sidebar-border/60 bg-sidebar-accent/40 p-3 text-xs text-sidebar-foreground/80">
            <div className="font-medium text-sidebar-foreground">100% deterministico</div>
            <p className="mt-1 text-sidebar-foreground/60">
              Sem IA julgando candidato. Score sai de rubrica, peso e calculo.
            </p>
          </div>
          <div className="mt-3 flex items-center gap-3 px-1">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
              R
            </div>
            <div className="text-xs">
              <div className="font-medium text-sidebar-foreground">Renata Silveira</div>
              <div className="text-sidebar-foreground/55">RH - Aprovadora</div>
            </div>
          </div>
        </div>
      </aside>

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-4 border-b border-border bg-background/85 px-6 backdrop-blur">
          <div className="text-xs text-muted-foreground">
            Workspace <span className="text-foreground">/ Acme S.A.</span>
          </div>
          <div className="ml-auto flex items-center gap-2 text-xs">
            <span className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground">
              <span className="h-1.5 w-1.5 rounded-full bg-success" />
              Gupy conectada
            </span>
            <button className="rounded-md border border-border bg-card px-3 py-1.5 text-foreground hover:bg-accent">
              Buscar
            </button>
          </div>
        </header>
        <div className="min-h-[calc(100vh-3.5rem)] px-6 py-8 lg:px-10">{children}</div>
      </main>
    </div>
  );
}
