"use client";

import { Link, useRouterState } from "@tanstack/react-router";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import {
  BarChart3,
  Building2,
  ChevronDown,
  ClipboardCheck,
  ClipboardList,
  HelpCircle,
  Home,
  KeyRound,
  Link2,
  Menu,
  Settings,
  ShieldCheck,
  Sparkles,
  Target,
  UserRound,
  Workflow,
} from "lucide-react";
import { GlobalErrorFlow, GlobalProductStateBar, StateBanner } from "@/components/praxis-ui";
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
import { useGupyConnectionState, useViewMode } from "@/lib/view-mode";
import { cn } from "@/lib/utils";

type TranslationMap = ReturnType<typeof useLanguage>["t"];

const getNav = (t: TranslationMap) =>
  [
    { to: "/dashboard", label: t.common.dashboard, icon: Home, desc: t.descriptions.dashboard },
    {
      to: "/nova/blueprint",
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
    {
      to: "/results",
      label: "Resultados",
      icon: ClipboardList,
      desc: "Resultados e decisões humanas",
    },
    {
      to: "/talent-match",
      label: t.common.talentMatch,
      icon: Target,
      desc: t.descriptions.talentMatch,
    },
    {
      to: "/jornadas",
      label: "Jornadas",
      icon: Workflow,
      desc: "Sequencias de testes publicados",
    },
    { to: "/enviar-link", label: t.common.sendLink, icon: Link2, desc: t.descriptions.sendLink },
  ] as const;

const getSecondary = (t: TranslationMap) =>
  [
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
      label: "Perfil da empresa",
      icon: Building2,
      desc: t.descriptions.settings,
    },
    {
      to: "/configuracoes/conta",
      label: "Minha conta",
      icon: UserRound,
      desc: "Dados de acesso",
    },
    {
      to: "/configuracoes/integracoes",
      label: "Integrações",
      icon: KeyRound,
      desc: "Conexões externas",
    },
    {
      to: "/competencias",
      label: t.common.competencies,
      icon: Settings,
      desc: t.descriptions.competencies,
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
    pathname === "/configuracoes" ||
    pathname.startsWith("/configuracoes/");
  const [settingsOpen, setSettingsOpen] = useState(settingsActive);

  useEffect(() => {
    if (settingsActive) setSettingsOpen(true);
  }, [settingsActive]);

  return (
    <>
      <div className="border-b border-border px-5 py-5">
        <div className="flex items-center gap-2 text-xs uppercase text-muted-foreground">
          <span className="inline-block h-1.5 w-1.5 rounded-full bg-primary" />
          {t.common.testCreator}
        </div>
        <div className="mt-2 font-display text-2xl leading-tight">
          {t.common.situationalAssessment.split("\n")[0]}
          <br />
          <span className="text-foreground/85">
            {t.common.situationalAssessment.split("\n")[1] || "Situacional"}
          </span>
        </div>
        <div className="mt-3 text-[11px] uppercase text-muted-foreground">
          Avaliações situacionais com critérios rastreáveis
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
              const active = pathname === item.to;
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
          title="A pontuação da participação é calculada por regras declaradas (critérios de pontuação, peso e cálculo). Nenhum modelo de IA decide ou julga o resultado."
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
  if (pathname === "/monitoramento") return t.common.monitoring;
  if (pathname === "/results" || pathname.startsWith("/results/")) return "Resultados";
  if (pathname === "/dashboard" || pathname === "/app") return t.common.dashboard;
  if (pathname.startsWith("/nova")) return t.common.createTest;
  if (pathname === "/talent-match") return t.common.talentMatch;
  if (pathname === "/jornadas") return "Jornadas";
  if (pathname === "/enviar-link") return t.common.sendLink;
  if (pathname === "/compliance") return t.common.compliance;
  if (pathname === "/configuracoes" || pathname === "/configuracoes/perfil")
    return "Perfil da empresa";
  if (pathname === "/configuracoes/conta") return "Minha conta";
  if (pathname === "/configuracoes/integracoes") return "Integrações";
  if (pathname === "/competencias") return t.common.competencies;
  if (pathname === "/comecar") return t.common.startHere;
  return t.common.workspace;
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const mode = useViewMode();
  const session = useSession();
  const isIntegrationPage = pathname === "/nova/gupy";
  const gupyState = useGupyConnectionState(pathname);
  const { t } = useLanguage();
  const hasIntegrationError = isIntegrationPage && gupyState === "error";
  const nav = getNav(t);
  const secondary = getSecondary(t);
  const productState = isIntegrationPage
    ? {
        gupy: gupyState,
        draft: "published" as const,
        publication: hasIntegrationError ? ("blocked" as const) : ("running" as const),
      }
    : pathname === "/nova/validador"
      ? { draft: "dirty" as const, publication: "idle" as const }
      : pathname === "/nova/piloto" || pathname.startsWith("/nova/mapa")
        ? { draft: "saved" as const, publication: "idle" as const }
        : { draft: "saved" as const, publication: "idle" as const };

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
                aria-label="Abrir menu"
              >
                <Menu className="h-4 w-4" />
              </button>
            </SheetTrigger>
            <SheetContent
              side="left"
              className="flex w-[18rem] max-w-[85vw] flex-col overflow-hidden bg-background p-0 text-foreground sm:max-w-sm"
            >
              <SheetHeader className="sr-only">
                <SheetTitle>Menu</SheetTitle>
                <SheetDescription>Navegação principal do Práxis.</SheetDescription>
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
              <span className="hidden sm:inline">Ajuda</span>
            </Link>
            {mode === "technical" && (
              <a
                href={pathname}
                className="rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground hover:bg-accent"
              >
                {t.common.viewCommercialMode}
              </a>
            )}
          </div>
        </header>
        <div className="min-h-[calc(100vh-3.5rem)] px-6 py-8 lg:px-10">
          <GlobalProductStateBar state={productState} />
          {hasIntegrationError && mode === "commercial" && (
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
          {(mode === "technical" || hasIntegrationError) && <GlobalErrorFlow />}
          {children}
        </div>
      </main>
    </div>
  );
}
