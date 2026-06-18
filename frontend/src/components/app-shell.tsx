"use client";

import { Link, useRouterState } from "@tanstack/react-router";
import type { ReactNode } from "react";
import {
  BarChart3,
  ClipboardCheck,
  Home,
  Link2,
  MessageSquare,
  HelpCircle,
  Scale,
  ShieldCheck,
  Sparkles,
  UserRound,
} from "lucide-react";
import { GlobalErrorFlow, GlobalProductStateBar, StateBanner } from "@/components/praxis-ui";
import { LanguageSelector } from "@/components/language-selector";
import { useSession } from "@/lib/session";
import { useLanguage } from "@/lib/language-context";
import { useGupyConnectionState, useViewMode } from "@/lib/view-mode";
import { cn } from "@/lib/utils";

const getNav = (t: any) => [
  { to: "/", label: t.common.dashboard, icon: Home, desc: t.descriptions.dashboard },
  {
    to: "/nova/avaliacao",
    label: t.common.createTest,
    icon: ClipboardCheck,
    desc: t.descriptions.createTest,
  },
  {
    to: "/monitoramento",
    label: t.common.monitoring,
    icon: BarChart3,
    desc: t.descriptions.monitoring,
  },
  { to: "/enviar-link", label: t.common.sendLink, icon: Link2, desc: t.descriptions.sendLink },
  {
    to: "/candidato",
    label: t.common.candidateView,
    icon: MessageSquare,
    desc: t.descriptions.candidateView,
  },
] as const;

const getSecondary = (t: any) => [
  {
    to: "/governanca",
    label: t.common.governance,
    icon: ShieldCheck,
    desc: t.descriptions.governance,
  },
  {
    to: "/lgpd",
    label: t.common.lgpd,
    icon: UserRound,
    desc: t.descriptions.lgpd,
  },
  {
    to: "/defensabilidade",
    label: t.common.defensibility,
    icon: Scale,
    desc: t.descriptions.defensibility,
  },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const mode = useViewMode();
  const session = useSession();
  const gupyState = useGupyConnectionState(pathname);
  const { t } = useLanguage();
  const hasGlobalError = gupyState === "error";
  const modeHref = mode === "technical" ? pathname : `${pathname}?mode=technical`;
  const modeLabel =
    mode === "technical" ? t.common.viewCommercialMode : t.common.viewAdvancedSettings;
  const nav = getNav(t);
  const secondary = getSecondary(t);
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
            {t.common.testCreator}
          </div>
          <div className="mt-2 font-display text-2xl leading-tight">
            {t.common.situationalAssessment.split("\n")[0]}
            <br />
            <span className="text-sidebar-foreground/85">{t.common.situationalAssessment.split("\n")[1] || "Situacional"}</span>
          </div>
          <div className="mt-3 text-[11px] uppercase text-sidebar-foreground/80">
            {t.common.integrationGupy}
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4">
          <Link
            to="/comecar"
            className={cn(
              "mb-3 flex items-start gap-3 rounded-md border px-3 py-2.5 text-sm transition",
              pathname === "/comecar"
                ? "border-primary/40 bg-primary/15 text-sidebar-foreground"
                : "border-sidebar-border/60 bg-sidebar-accent/30 text-sidebar-foreground/85 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
            )}
          >
            <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
            <span className="min-w-0 flex-1">
              <span className="block font-medium">{t.common.startHere}</span>
              <span className="mt-0.5 block text-[11px] leading-snug text-sidebar-foreground/65">
                {t.common.whatIsPraxis}
              </span>
            </span>
          </Link>

          <div className="px-3 pb-2 text-[10px] font-semibold uppercase text-sidebar-foreground/75">
            {t.common.operation}
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
                  "mb-1.5 flex items-start gap-3 rounded-md px-3 py-2.5 text-sm transition",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/85 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
                )}
              >
                <item.icon className="mt-0.5 h-4 w-4 shrink-0 text-sidebar-foreground/80" />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2">
                    {item.label}
                    {active && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
                  </span>
                  <span className="mt-0.5 block text-[11px] leading-snug text-sidebar-foreground/65">
                    {item.desc}
                  </span>
                </span>
              </Link>
            );
          })}

          <div className="mt-6 px-3 pb-2 text-[10px] font-semibold uppercase text-sidebar-foreground/75">
            {t.common.compliance}
          </div>
          {secondary.map((item) => {
            const active = pathname === item.to;
            return (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "mb-1.5 flex items-start gap-3 rounded-md px-3 py-2.5 text-sm transition",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/85 hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
                )}
              >
                <item.icon className="mt-0.5 h-4 w-4 shrink-0 text-sidebar-foreground/80" />
                <span className="min-w-0 flex-1">
                  <span className="block">{item.label}</span>
                  <span className="mt-0.5 block text-[11px] leading-snug text-sidebar-foreground/65">
                    {item.desc}
                  </span>
                </span>
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-sidebar-border p-4">
          <div
            className="rounded-md border border-sidebar-border/60 bg-sidebar-accent/40 p-3 text-xs text-sidebar-foreground/80"
            title="A nota do candidato é calculada por regras declaradas (critérios de pontuação, peso e cálculo). Nenhum modelo de IA decide ou julga o resultado."
          >
            <div className="font-medium text-sidebar-foreground">{t.common.rulesBasedScoring}</div>
            <p className="mt-1 text-sidebar-foreground/80">
              {t.common.noSubjectiveAnswers}
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
            {t.common.workspace} <span className="text-foreground">/ {session.workspaceName}</span>
          </div>
          <div className="ml-auto flex items-center gap-2 text-xs">
            <LanguageSelector />
            <Link
              to="/comecar"
              className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground hover:bg-accent"
            >
              <HelpCircle className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">{t.common.help}</span>
            </Link>
            <a
              href={modeHref}
              className="rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground hover:bg-accent"
            >
              {modeLabel}
            </a>
            <button className="rounded-md border border-border bg-card px-3 py-1.5 text-foreground hover:bg-accent">
              {t.common.search}
            </button>
          </div>
        </header>
        <div className="min-h-[calc(100vh-3.5rem)] px-6 py-8 lg:px-10">
          <GlobalProductStateBar state={productState} />
          {hasGlobalError && mode === "commercial" && (
            <StateBanner
              tone="danger"
              title={t.common.gupyConnectionError}
              action={
                <a
                  href={`${pathname}?mode=technical&gupy=error`}
                  className="shrink-0 rounded-md border border-current/20 bg-background/60 px-3 py-1.5 text-xs font-medium"
                >
                  {t.common.openDiagnostics}
                </a>
              }
            >
              {t.common.couldNotConfirmSubmission}
            </StateBanner>
          )}
          {(mode === "technical" || hasGlobalError) && <GlobalErrorFlow />}
          {children}
        </div>
      </main>
    </div>
  );
}
