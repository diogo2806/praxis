import type { ReactNode } from "react";
import { useState } from "react";
import { Link, useRouterState } from "@tanstack/react-router";
import { Building2, LayoutDashboard, Menu, ShieldCheck, X } from "lucide-react";
import { LanguageSelector } from "@/components/language-selector";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

/**
 * Layout do painel administrativo da plataforma (perfil ADMIN).
 *
 * O ADMIN é o operador da plataforma: este shell é separado do app das empresas
 * (EMPRESA) e expõe apenas a navegação administrativa.
 */
export function AdminShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const { t } = useLanguage();
  const [menuOpen, setMenuOpen] = useState(false);

  const navItems = [
    { to: "/admin", label: t.admin.dashboard, icon: LayoutDashboard, exact: true },
    { to: "/admin/empresas", label: t.admin.clients, icon: Building2, exact: false },
  ];

  const navLinkClass = (active: boolean) =>
    cn(
      "flex min-h-11 items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
      active ? "bg-primary/10 text-primary" : "text-slate-600 hover:bg-slate-100",
    );

  return (
    <div className="admin-shell min-h-screen overflow-x-hidden bg-slate-50 text-slate-900">
      <style>{`
        @media (max-width: 639px) {
          .admin-shell .overflow-hidden { overflow-x: auto !important; }
          .admin-shell table { min-width: 42rem; }
          .admin-shell [role="dialog"] { width: calc(100vw - 2rem); max-width: calc(100vw - 2rem); }
        }
      `}</style>
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 py-3 sm:px-6 sm:py-4">
          <div className="flex min-w-0 items-center gap-2 font-semibold">
            <ShieldCheck className="size-5 shrink-0 text-primary" />
            <span>Praxis</span>
            <span className="truncate rounded bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
              {t.admin.header}
            </span>
          </div>

          <div className="flex shrink-0 items-center gap-2 sm:gap-3">
            <nav className="hidden items-center gap-1 sm:flex" aria-label="Navegação administrativa">
              {navItems.map((item) => {
                const active = item.exact ? pathname === item.to : pathname.startsWith(item.to);
                return (
                  <Link key={item.to} to={item.to} className={navLinkClass(active)}>
                    <item.icon className="size-4" />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
            <LanguageSelector />
            <button
              type="button"
              className="inline-flex size-11 items-center justify-center rounded-md text-slate-600 hover:bg-slate-100 sm:hidden"
              aria-label={menuOpen ? "Fechar menu" : "Abrir menu"}
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              {menuOpen ? <X className="size-5" /> : <Menu className="size-5" />}
            </button>
          </div>
        </div>

        {menuOpen && (
          <nav className="border-t border-slate-100 px-4 py-2 sm:hidden" aria-label="Navegação administrativa">
            <div className="mx-auto grid max-w-7xl gap-1">
              {navItems.map((item) => {
                const active = item.exact ? pathname === item.to : pathname.startsWith(item.to);
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    className={navLinkClass(active)}
                    onClick={() => setMenuOpen(false)}
                  >
                    <item.icon className="size-4" />
                    {item.label}
                  </Link>
                );
              })}
            </div>
          </nav>
        )}
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 sm:py-8">{children}</main>
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

const STATUS_KEYS: Record<string, keyof ReturnType<typeof useLanguage>["t"]["admin"]> = {
  ATIVO: "statusActive",
  EM_TESTE: "statusInTest",
  SUSPENSO: "statusSuspended",
  CANCELADO: "statusCancelled",
  CONVIDADO: "statusInvited",
  BLOQUEADO: "statusBlocked",
};

/** Selo visual de status reutilizado nas telas de cliente e de acessos. */
export function StatusBadge({ status }: { status: string }) {
  const { t } = useLanguage();
  const key = STATUS_KEYS[status];
  const label = key ? t.admin[key] : status;
  return (
    <span
      className={cn(
        "inline-flex max-w-full rounded-full px-2.5 py-0.5 text-xs font-medium",
        STATUS_STYLES[status] ?? "bg-slate-100 text-slate-600",
      )}
    >
      {label}
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
