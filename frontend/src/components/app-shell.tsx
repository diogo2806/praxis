"use client";

import { Link, useRouterState } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import {
  BarChart3,
  Bell,
  BookOpenCheck,
  Building2,
  ClipboardList,
  CreditCard,
  HelpCircle,
  Home,
  KeyRound,
  ListChecks,
  Link2,
  Menu,
  ShieldCheck,
  Sparkles,
  Target,
  UserRound,
  Users,
  Workflow,
} from "lucide-react";
import { DeliveryAlertBanner } from "@/components/delivery-alert-banner";
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
import { getUnreadNotificationsCount } from "@/lib/api/notifications";
import { appShellCopy } from "@/lib/app-shell-copy";
import { useLanguage } from "@/lib/language-context";
import { useSession } from "@/lib/session";
import { cn } from "@/lib/utils";

type TranslationMap = ReturnType<typeof useLanguage>["t"];
type CopyMap = (typeof appShellCopy)[keyof typeof appShellCopy];

function ShellLink({ children, closeOnSelect }: { children: ReactNode; closeOnSelect?: boolean }) {
  return closeOnSelect ? <SheetClose asChild>{children}</SheetClose> : <>{children}</>;
}

function SidebarContent({
  closeOnSelect = false,
  pathname,
  unreadNotifications,
}: {
  closeOnSelect?: boolean;
  pathname: string;
  unreadNotifications: number;
}) {
  const session = useSession();
  const { language, t } = useLanguage();
  const copy = appShellCopy[language];
  const navGroups = [
    {
      label: t.common.workspace,
      items: [
        { to: "/dashboard", label: t.common.dashboard, icon: Home },
        { to: "/notifications", label: copy.notificationsLabel, icon: Bell, badge: unreadNotifications },
      ],
    },
    {
      label: t.common.situationalAssessment,
      items: [
        { to: "/avaliacoes", label: t.common.situationalAssessment, icon: ListChecks },
        { to: "/competencias", label: t.common.competencies, icon: BookOpenCheck },
        { to: "/enviar-link", label: t.common.sendLink, icon: Link2 },
        { to: "/jornadas", label: t.common.journeys, icon: Workflow },
      ],
    },
    {
      label: t.common.results,
      items: [
        { to: "/results", label: t.common.results, icon: ClipboardList },
        { to: "/talent-match", label: t.common.talentMatch, icon: Target },
      ],
    },
    {
      label: t.common.operation,
      items: [
        { to: "/monitoramento", label: t.common.operationCenter, icon: BarChart3 },
        { to: "/compliance", label: t.common.compliance, icon: ShieldCheck },
      ],
    },
    {
      label: t.common.settings,
      items: [
        { to: "/configuracoes/perfil", label: t.common.profile, icon: Building2 },
        { to: "/team", label: t.common.myTeam, icon: Users },
        { to: "/integrations", label: t.common.integrations, icon: KeyRound },
        { to: "/billing", label: t.common.plan, icon: CreditCard },
        { to: "/configuracoes/conta", label: t.common.myAccount, icon: UserRound },
      ],
    },
  ] as const;

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
      <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label={t.common.menu}>
        <ShellLink closeOnSelect={closeOnSelect}>
          <Link
            to="/comecar"
            aria-current={pathname === "/comecar" ? "page" : undefined}
            className={cn(
              "mb-3 flex items-start gap-3 rounded-lg border px-3 py-2.5 text-sm transition",
              pathname === "/comecar" ? "border-primary/40 bg-primary/10" : "border-border bg-card hover:bg-accent",
            )}
          >
            <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
            <span>
              <span className="block font-medium">{t.common.startHere}</span>
              <span className="mt-0.5 block text-[11px] leading-snug text-muted-foreground">
                {t.common.whatIsPraxis}
              </span>
            </span>
          </Link>
        </ShellLink>
        {navGroups.map((group, groupIndex) => (
          <div key={group.label} className={cn(groupIndex > 0 && "mt-5 border-t border-border/70 pt-5")}>
            <div className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
              {group.label}
            </div>
            {group.items.map((item) => {
              const active = isActivePath(pathname, item.to);
              const badge = "badge" in item ? item.badge : 0;
              return (
                <ShellLink key={item.to} closeOnSelect={closeOnSelect}>
                  <Link
                    to={item.to}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "mb-1 flex items-center gap-3 rounded-md px-3 py-2 text-sm transition",
                      active ? "bg-accent text-accent-foreground" : "text-foreground/85 hover:bg-accent",
                    )}
                  >
                    <item.icon
                      className={cn(
                        "h-4 w-4 shrink-0",
                        active ? "text-accent-foreground" : "text-muted-foreground",
                      )}
                    />
                    <span className="min-w-0 flex-1">{item.label}</span>
                    {badge > 0 && (
                      <span className="rounded-full bg-danger px-1.5 py-0.5 text-[10px] font-semibold text-danger-foreground">
                        {badge > 99 ? "99+" : badge}
                      </span>
                    )}
                  </Link>
                </ShellLink>
              );
            })}
          </div>
        ))}
      </nav>
      <div className="border-t border-border p-4">
        <div className="rounded-md border border-border bg-accent/40 p-3 text-xs text-muted-foreground" title={t.common.scoringTooltip}>
          <div className="font-medium text-foreground">{t.common.rulesBasedScoring}</div>
          <p className="mt-1">{t.common.noSubjectiveAnswers}</p>
        </div>
        <div className="mt-3 flex items-center gap-3 px-1 text-xs">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary font-semibold text-primary-foreground">
            {session.userName.trim().charAt(0).toUpperCase() || "?"}
          </div>
          <div className="min-w-0">
            <div className="truncate font-medium text-foreground">{session.userName}</div>
            <div className="truncate text-muted-foreground">{session.userRole}</div>
          </div>
        </div>
      </div>
    </>
  );
}

function isActivePath(pathname: string, itemPath: string) {
  if (itemPath === "/dashboard") return pathname === itemPath;
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`);
}

function pageContext(pathname: string, t: TranslationMap, copy: CopyMap) {
  if (pathname === "/dashboard") return { section: t.common.workspace, label: t.common.dashboard };
  if (pathname === "/notifications") return { section: t.common.workspace, label: copy.notificationsLabel };
  if (pathname === "/comecar") return { section: t.common.workspace, label: t.common.startHere };

  if (pathname === "/avaliacoes") return { section: t.common.situationalAssessment, label: t.common.situationalAssessment };
  if (pathname.startsWith("/nova")) return { section: t.common.situationalAssessment, label: t.common.createAssessment };
  if (pathname === "/competencias") return { section: t.common.situationalAssessment, label: t.common.competencies };
  if (pathname === "/enviar-link") return { section: t.common.situationalAssessment, label: t.common.sendLink };
  if (pathname === "/jornadas" || pathname.startsWith("/jornada/")) {
    return { section: t.common.situationalAssessment, label: t.common.journeys };
  }

  if (pathname === "/results" || pathname.startsWith("/results/")) {
    return { section: t.common.results, label: t.common.results };
  }
  if (pathname === "/talent-match") return { section: t.common.results, label: t.common.talentMatch };

  if (pathname === "/monitoramento") return { section: t.common.operation, label: t.common.operationCenter };
  if (pathname === "/compliance") return { section: t.common.operation, label: t.common.compliance };

  if (pathname === "/configuracoes/perfil") return { section: t.common.settings, label: t.common.profile };
  if (pathname === "/configuracoes/conta") return { section: t.common.settings, label: t.common.myAccount };
  if (pathname === "/configuracoes/api" || pathname.startsWith("/docs/integracao-api-propria")) {
    return { section: t.common.settings, label: "API" };
  }
  if (pathname === "/team") return { section: t.common.settings, label: t.common.myTeam };
  if (pathname.startsWith("/integrations")) return { section: t.common.settings, label: t.common.integrations };
  if (pathname === "/billing") return { section: t.common.settings, label: t.common.plan };
  if (pathname.startsWith("/configuracoes")) return { section: t.common.settings, label: t.common.settings };

  if (pathname.startsWith("/candidato")) return { section: t.common.workspace, label: t.common.candidateView };
  return { section: t.common.workspace, label: "Práxis" };
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const { language, t } = useLanguage();
  const copy = appShellCopy[language];
  const context = pageContext(pathname, t, copy);
  const unreadNotificationsQuery = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: getUnreadNotificationsCount,
    retry: false,
    refetchInterval: 60_000,
  });
  const unreadNotifications = unreadNotificationsQuery.data?.count ?? 0;

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <aside className="hidden w-64 shrink-0 flex-col border-r border-border bg-background text-foreground lg:flex">
        <SidebarContent pathname={pathname} unreadNotifications={unreadNotifications} />
      </aside>
      <main className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex min-h-14 items-center gap-3 border-b border-border bg-card px-4 py-2 sm:px-6">
          <Sheet>
            <SheetTrigger asChild>
              <button type="button" className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md border border-border bg-card hover:bg-accent lg:hidden" aria-label={t.common.openMenu}>
                <Menu className="h-4 w-4" />
              </button>
            </SheetTrigger>
            <SheetContent side="left" className="flex w-[18rem] max-w-[85vw] flex-col overflow-hidden bg-background p-0 text-foreground sm:max-w-sm">
              <SheetHeader className="sr-only">
                <SheetTitle>{t.common.menu}</SheetTitle>
                <SheetDescription>{t.common.menuDescription}</SheetDescription>
              </SheetHeader>
              <SidebarContent pathname={pathname} unreadNotifications={unreadNotifications} closeOnSelect />
            </SheetContent>
          </Sheet>
          <div className="min-w-0 truncate text-sm text-muted-foreground">
            {context.section} <span className="text-foreground">/ {context.label}</span>
          </div>
          <div className="ml-auto flex shrink-0 items-center justify-end gap-2 text-xs">
            <LanguageSelector />
            <Link to="/comecar" className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1 text-muted-foreground hover:bg-accent">
              <HelpCircle className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">{t.common.help}</span>
            </Link>
          </div>
        </header>
        {pathname === "/dashboard" && <DeliveryAlertBanner language={language} />}
        <div className="min-h-[calc(100vh-3.5rem)] px-6 py-8 lg:px-10">{children}</div>
      </main>
    </div>
  );
}
