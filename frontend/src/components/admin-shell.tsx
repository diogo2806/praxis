import type { ReactNode } from "react";
import { Link, useRouterState } from "@tanstack/react-router";
import { LayoutDashboard, Building2, ClipboardCheck, RotateCcw, ShieldCheck, Store } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Layout do painel administrativo da plataforma (perfil ADMIN).
 *
 * O ADMIN é o operador da plataforma: este shell é separado do app das empresas
 * (EMPRESA) e expõe apenas a navegação administrativa.
 */
export function AdminShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });

  const navItems = [
    { to: "/admin", label: "Dashboard", icon: LayoutDashboard, exact: true },
    { to: "/admin/empresas", label: "Clientes", icon: Building2, exact: false },
    { to: "/admin/marketplace/professionals", label: "Profissionais", icon: Store, exact: false },
    { to: "/admin/marketplace/listings", label: "Moderação de testes", icon: ClipboardCheck, exact: false },
    { to: "/admin/marketplace/disputes", label: "Disputas", icon: RotateCcw, exact: false },
  ];

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2 font-semibold">
            <ShieldCheck className="size-5 text-primary" />
            <span>Praxis</span>
            <span className="rounded bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
              Admin da plataforma
            </span>
          </div>
          <nav className="flex items-center gap-1">
            {navItems.map((item) => {
              const active = item.exact
                ? pathname === item.to
                : pathname.startsWith(item.to);
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={cn(
                    "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                    active
                      ? "bg-primary/10 text-primary"
                      : "text-slate-600 hover:bg-slate-100",
                  )}
                >
                  <item.icon className="size-4" />
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-6 py-8">{children}</main>
    </div>
  );
}

const STATUS_STYLES: Record<string, string> = {
  ATIVO: "bg-emerald-100 text-emerald-700",
  EM_TESTE: "bg-sky-100 text-sky-700",
  SUSPENSO: "bg-amber-100 text-amber-700",
  CANCELADO: "bg-rose-100 text-rose-700",
  CONVIDADO: "bg-sky-100 text-sky-700",
  BLOQUEADO: "bg-rose-100 text-rose-700",
};

const STATUS_LABELS: Record<string, string> = {
  ATIVO: "Ativo",
  EM_TESTE: "Em teste",
  SUSPENSO: "Suspenso",
  CANCELADO: "Cancelado",
  CONVIDADO: "Convidado",
  BLOQUEADO: "Bloqueado",
};

/** Selo visual de status reutilizado nas telas de cliente e de acessos. */
export function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium",
        STATUS_STYLES[status] ?? "bg-slate-100 text-slate-600",
      )}
    >
      {STATUS_LABELS[status] ?? status}
    </span>
  );
}

const PLAN_LABELS: Record<string, string> = {
  AVULSO: "Avulso (crédito pré-pago)",
  PROFISSIONAL: "Profissional (assinatura)",
  ENTERPRISE: "Enterprise (contrato)",
};

export function planLabel(plan: string): string {
  return PLAN_LABELS[plan] ?? plan;
}
