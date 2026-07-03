"use client";

import { Link, useRouterState } from "@tanstack/react-router";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import {
  BarChart3,
  Building2,
  ChevronDown,
  ClipboardList,
  CreditCard,
  HelpCircle,
  Home,
  KeyRound,
  ListChecks,
  Link2,
  Menu,
  Settings,
  ShieldCheck,
  Sparkles,
  Target,
  UserRound,
  Users,
  Workflow,
} from "lucide-react";
import { LanguageSelector } from "@/components/language-selector";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useSession } from "@/lib/session";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

type TranslationMap = ReturnType<typeof useLanguage>["t"];

const getNav = (t: TranslationMap) =>
  [
    {
      to: "/dashboard",
      label: t.common.dashboard,
      icon: Home,
      desc: t.common.operationCenter,
    },
    {
      to: "/avaliacoes",
      label: t.common.situationalAssessment,
      icon: ListChecks,
      desc: t.common.seeAndEdit,
    },
    {
      to: "/results",
      label: t.common.results,
      icon: ClipboardList,
      desc: t.common.resultsAndDecisions,
    },
    {
      to: "/enviar-link",
      label: t.common.sendLink,
      icon: Link2,
      desc: t.common.sendLinkDesc,
    },
    {
      to: "/monitoramento",
      label: t.common.monitoring,
      icon: BarChart3,
      desc: t.descriptions.monitoring,
    },
    {
      to: "/jornadas",
      label: t.common.journeys,
      icon: Workflow,
      desc: t.common.journeysDesc,
    },
    {
      to: "/talent-match",
      label: t.common.talentMatch,
      icon: Target,
      desc: t.descriptions.talentMatch,
    },
  ] as const;

const getSecondary = (t: TranslationMap) =>
  [
    {
      to: "/billing",
      label: t.common.plan,
      icon: CreditCard,
      desc: t.common.planDesc,
    },
    {
      to: "/compliance",
      label: t.common.compliance,
      icon: ShieldCheck,
      desc: t.descriptions.complianceNav,
    },
  ] as const;

const getSettingsNav = (t: TranslationMap) =>
  [
    {
      to: "/configuracoes/perfil",
      label: t.common.profile,
      icon: Building2,
      desc: t.descriptions.settings,
    },
    {
      to: "/configuracoes/conta",
      label: t.common.myAccount,
      icon: UserRound,
      desc: t.common.accountAccess,
    },
    {
      to: "/team",
      label: t.common.myTeam,
      icon: Users,
      desc: t.common.teamDesc,
    },
    {
      to: "/competencias",
      label: t.common.competencies,
      icon: Settings,
      desc: t.descriptions.competencies,
    },
    {
      to: "/integrations",
      label: t.common.integrations,
      icon: KeyRound,
      desc: t.common.integrationsDesc,
    },
  ] as const;

function ShellLink({ children, closeOnSelect }: { children: ReactNode; closeOnSelect?: boolean }) {
  if (!closeOnSelect) {
    return <>{children}</>;
  }

  return <SheetClose asChild>{children}</SheetClose>;
}

function SidebarContent({
  pathname,
  t,
  session,
  nav,
  secondary,
  closeOnSelect = false,
}: {
  pathname: string;
  t: TranslationMap;
  session: ReturnType<typeof useSession>;
  nav: ReturnType<typeof getNav>;
  secondary: ReturnType<typeof getSecondary>;
  closeOnSelect?: boolean;
}) {
  const settingsNav = getSettingsNav(t);
  const settingsActive =
    pathname === "/competencias" ||
    pathname === "/team" ||
    pathname === "/integrations" ||
    pathname.startsWith("/integrations/") ||
    pathname === "/configuracoes" ||
    pathname.startsWith("/configuracoes/");
  const [settingsOpen, setSettingsOpen] = useState(settingsActive);

  useEffect(() => {
    if (settingsActive) setSettingsOpen(true);
  }, [settingsActive]);

  return (
    <>
      <div className="border-b border-border px-5 py-5">
        <ShellLink closeOnSelect={closeOnSelect}>
          <Link to="/dashboard" className="inline-flex items-baseline gap-1.5">
            <span className="font-display text-3xl leading-none text-foreground">Práxis</span>
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-primary" />
          </Link>
        </ShellLink>
        <div className="mt-3 text-[11px] uppercase leading-relaxed text-muted-foreground">
          {t.common.tagline}
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <ShellLink closeOnSelect={closeOnSelect}>
          <Link
            to="/comecar"
            className={cn(
              "mb-3 flex items-start gap-3 rounded-lg border px-3 py-2.5 text-sm transition",
              pathname === "/comecar"
                ? "border-primary/40 bg-primary/10 text-foreground"
                : "border-border bg-card text-foreground/85 hover:bg-accent hover:text-foreground",
            )}
          >
            <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
            <span className="min-w-0 flex-1">
              <span className="block font-medium">{t.common.startHere}</span>
              <span className="mt-0.5 block text-[11px] leading-snug text-muted-foreground">
                {t.common.whatIsPraxis}
              </span>
            </span>
          </Link>
        </ShellLink>

        <div className="px-3 pb-2 text-[10px] font-semibold uppercase text-muted-foreground">
          {t.common.operation}
        </div>
        {nav.map((item) => {
          const active =
            item.to === "/dashboard"
              ? pathname === "/dashboard"
              : pathname === item.to || pathname.startsWith(item.to + "/");
          return (
            <ShellLink key={item.to} closeOnSelect={closeOnSelect}>
              <Link
                to={item.to}
                className={cn(
                  "mb-1.5 flex items-center gap-3 rounded-md px-3 py-2 text-sm transition",
                  active
                    ? "bg-accent text-accent-foreground"
                    : "text-foreground/85 hover:bg-accent hover:text-foreground",
                )}
              >
                <item.icon className="h-4 w-4 shrink-0 text-muted-foreground" />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2">
                    {item.label}
                    {active && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
                  </span>
                </span>
              </Link>
            </ShellLink>
          );
        })}

        <div className="mt-6 px-3 pb-2 text-[10px] font-semibold uppercase text-muted-foreground">
          {t.common.compliance}
        </div>
        <button
          type="button"
          onClick={() => setSettingsOpen((current) => !current)}
          className={cn(
            "mb-1.5 flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm transition",
            settingsActive
              ? "bg-accent text-accent-foreground"
              : "text-foreground/85 hover:bg-accent hover:text-foreground",
          )}
          aria-expanded={settingsOpen}
        >
          <Settings className="h-4 w-4 shrink-0 text-muted-foreground" />
          <span className="min-w-0 flex-1">{t.common.settings}</span>
          <ChevronDown
            className={cn(
              "h-4 w-4 shrink-0 text-muted-foreground transition-transform",
              settingsOpen && "rotate-180",
            )}
          />
        </button>
        {settingsOpen && (
          <div className="mb-2 ml-3 border-l border-border pl-2">
            {settingsNav.map((item) => {
              const active = pathname === item.to || pathname.startsWith(item.to + "/");
              return (
                <ShellLink key={item.to} closeOnSelect={closeOnSelect}>
                  <Link
                    to={item.to}
                    className={cn(
                      "mb-1 flex items-center gap-2 rounded-md px-3 py-2 text-sm transition",
                      active
                        ? "bg-accent text-accent-foreground"
                        : "text-foreground/80 hover:bg-accent hover:text-foreground",
                    )}
                  >
                    <item.icon className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                    <span className="min-w-0 flex-1">{item.label}</span>
                  </Link>
                </ShellLink>
              );
            })}
          </div>
        )}
        {secondary.map((item) => {
          const active = pathname === item.to;
          return (
            <ShellLink key={item.to} closeOnSelect={closeOnSelect}>
              <Link
                to={item.to}
                className={cn(
                  "mb-1.5 flex items-center gap-3 rounded-md px-3 py-2 text-sm transition",
                  active
                    ? "bg-accent text-accent-foreground"
                    : "text-foreground/85 hover:bg-accent hover:text-foreground",
                )}
              >
                <item.icon className="h-4 w-4 shrink-0 text-muted-foreground" />
                <span className="min-w-0 flex-1">
                  <span className="block">{item.label}</span>
                </span>
              </Link>
            </ShellLink>
          );
        })}
      </nav>

      <div className="border-t border-border p-4">
        <div
          className="rounded-md border border-border bg-accent/40 p-3 text-xs text-muted-foreground"
          title={t.common.scoringTooltip}
        >
          <div className="font-medium text-foreground">{t.common.rulesBasedScoring}</div>
          <p className="mt-1 text-muted-foreground">{t.common.noSubjectiveAnswers}</p>
        </div>
        <div className="mt-3 flex items-center gap-3 px-1">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
            {session.userName.trim().charAt(0).toUpperCase() || "?"}
          </div>
          <div className="text-xs">
            <div className="font-medium text-foreground">{session.userName}</div>
            <div className="text-muted-foreground">{session.userRole}</div>
          </div>
        </div>
      </div>
    </>
  );
}

function pageLabel(pathname: string, t: TranslationMap) {
  if (pathname === "/dashboard") return t.common.dashboard;
  if (pathname === "/avaliacoes") return t.common.situationalAssessment;
  if (pathname.startsWith("/nova")) return t.common.createAssessment;
  if (pathname === "/results" || pathname.startsWith("/results/")) return t.common.results;
  if (pathname === "/enviar-link") return t.common.sendLink;
  if (pathname === "/integrations" || pathname.startsWith("/integrations/"))
    return t.common.integrations;
  if (pathname === "/monitoramento") return t.common.monitoring;
  if (pathname === "/jornadas") return t.common.journeys;
  if (pathname === "/talent-match") return t.common.talentMatch;
  if (pathname === "/billing") return t.common.plan;
  if (pathname === "/compliance") return t.common.compliance;
  if (pathname === "/configuracoes" || pathname === "/configuracoes/perfil")
    return t.common.profile;
  if (pathname === "/configuracoes/conta") return t.common.myAccount;
  if (pathname === "/competencias") return t.common.competencies;
  if (pathname === "/comecar") return t.common.startHere;
  if (pathname === "/team") return t.common.myTeam;
  if (pathname === "/candidato" || pathname.startsWith("/candidato/"))
    return t.common.candidateView;
  return "Práxis";
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const session = useSession();
  const { t } = useLanguage();
  const nav = getNav(t);
  const secondary = getSecondary(t);

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <aside className="hidden w-64 shrink-0 flex-col border-r border-border bg-background text-foreground lg:flex">
        <SidebarContent
          pathname={pathname}
          t={t}
          session={session}
          nav={nav}
          secondary={secondary}
        />
      </aside>

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex min-h-14 items-center gap-3 border-b border-border bg-card px-4 py-2 sm:px-6">
          <Sheet>
            <SheetTrigger asChild>
              <button
                type="button"
                className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md border border-border bg-card text-foreground hover:bg-accent lg:hidden"
                aria-label={t.common.openMenu}
              >
                <Menu className="h-4 w-4" />
              </button>
            </SheetTrigger>
            <SheetContent
              side="left"
              className="flex w-[18rem] max-w-[85vw] flex-col overflow-hidden bg-background p-0 text-foreground sm:max-w-sm"
            >
              <SheetHeader className="sr-only">
                <SheetTitle>{t.common.menu}</SheetTitle>
                <SheetDescription>{t.common.menuDescription}</SheetDescription>
              </SheetHeader>
              <SidebarContent
                pathname={pathname}
                t={t}
                session={session}
                nav={nav}
                secondary={secondary}
                closeOnSelect
              />
            </SheetContent>
          </Sheet>
          <div className="min-w-0 truncate text-sm text-muted-foreground">
            {t.common.workspace} <span className="text-foreground">/ {pageLabel(pathname, t)}</span>
          </div>
          <div className="ml-auto flex shrink-0 items-center justify-end gap-2 text-xs">
            <LanguageSelector />
            <Link
              to="/comecar"
              className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground hover:bg-accent"
            >
              <HelpCircle className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">{t.common.help}</span>
            </Link>
          </div>
        </header>
        <div className="min-h-[calc(100vh-3.5rem)] px-6 py-8 lg:px-10">{children}</div>
      </main>
    </div>
  );
}
