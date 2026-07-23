"use client";

import { useQuery } from "@tanstack/react-query";
import { Link, useRouterState } from "@tanstack/react-router";
import { useEffect, type ReactNode } from "react";
import { Focus, HelpCircle, Menu } from "lucide-react";

import { AccessibilityPanel, useCognitivePreferences } from "@/components/app-shell-accessibility";
import { AppSidebar } from "@/components/app-shell-navigation";
import { DeliveryAlertBanner } from "@/components/delivery-alert-banner";
import { LanguageSelector } from "@/components/language-selector";
import { getUnreadNotificationsCount } from "@/lib/api/notifications";
import { isRestrictedPartnerSpecialist } from "@/lib/access-control";
import {
  resolveAppShellContext,
  resolveAppShellGoalKey,
  resolveAppShellRouteDataKey,
} from "@/lib/app-shell-context";
import { useLanguage } from "@/lib/language-context";
import { useSession } from "@/lib/session";
import { cn } from "@/lib/utils";

const shellCopy = {
  "pt-BR": {
    skip: "Ir para o conteúdo principal",
    openMenu: "Abrir menu",
    menu: "Menu principal",
    menuDescription: "Navegação organizada conforme as permissões do perfil.",
    help: "Ajuda",
    pageGoal: "Objetivo desta tela",
    exitFocus: "Sair do modo foco",
    specialistArea: "Área do especialista",
    home: "Início",
    workspace: "Workspace",
    dashboard: "Dashboard",
    assessments: "Avaliações",
    competencies: "Catálogo de competências",
    account: "Minha conta",
    settings: "Configurações",
    journeys: "Jornadas",
    participations: "Participações",
    results: "Resultados",
    operation: "Operação técnica",
    operations: "Central operacional",
  },
  en: {
    skip: "Skip to main content",
    openMenu: "Open menu",
    menu: "Main menu",
    menuDescription: "Navigation organized according to profile permissions.",
    help: "Help",
    pageGoal: "Goal of this page",
    exitFocus: "Exit focus mode",
    specialistArea: "Specialist area",
    home: "Home",
    workspace: "Workspace",
    dashboard: "Dashboard",
    assessments: "Assessments",
    competencies: "Competency catalog",
    account: "My account",
    settings: "Settings",
    journeys: "Journeys",
    participations: "Participations",
    results: "Results",
    operation: "Technical operations",
    operations: "Operations center",
  },
  "es-MX": {
    skip: "Ir al contenido principal",
    openMenu: "Abrir menú",
    menu: "Menú principal",
    menuDescription: "Navegación organizada según los permisos del perfil.",
    help: "Ayuda",
    pageGoal: "Objetivo de esta pantalla",
    exitFocus: "Salir del modo enfoque",
    specialistArea: "Área del especialista",
    home: "Inicio",
    workspace: "Workspace",
    dashboard: "Dashboard",
    assessments: "Evaluaciones",
    competencies: "Catálogo de competencias",
    account: "Mi cuenta",
    settings: "Configuración",
    journeys: "Jornadas",
    participations: "Participaciones",
    results: "Resultados",
    operation: "Operación técnica",
    operations: "Central operativa",
  },
} as const;

const goalCopy = {
  "pt-BR": {
    specialist: "Organize seu trabalho de criação e revisão sem acessar áreas administrativas da empresa.",
    assessments: "Crie e revise o conteúdo que será encaminhado à empresa para publicação.",
    default: "Conclua uma tarefa por vez e use o menu para acessar o próximo passo.",
  },
  en: {
    specialist: "Organize creation and review work without accessing company administration.",
    assessments: "Create and review content that the company will publish.",
    default: "Complete one task at a time and use the menu to access the next step.",
  },
  "es-MX": {
    specialist: "Organice el trabajo de creación y revisión sin acceder a la administración de la empresa.",
    assessments: "Cree y revise el contenido que la empresa publicará.",
    default: "Complete una tarea a la vez y use el menú para acceder al siguiente paso.",
  },
} as const;

function PageGoal({ pathname, language, specialist }: {
  pathname: string;
  language: keyof typeof shellCopy;
  specialist: boolean;
}) {
  const copy = shellCopy[language];
  const goals = goalCopy[language];
  const goal = goals[resolveAppShellGoalKey(pathname, specialist)];
  return (
    <section className="praxis-page-goal mb-6 flex max-w-4xl items-start gap-3 rounded-lg border border-primary/25 bg-primary/5 p-4">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">1</div>
      <div>
        <div className="text-sm font-semibold text-foreground">{copy.pageGoal}</div>
        <p className="mt-1 text-sm leading-6 text-muted-foreground">{goal}</p>
      </div>
    </section>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const session = useSession();
  const specialist = isRestrictedPartnerSpecialist(session.roles);
  const { language } = useLanguage();
  const copy = shellCopy[language];
  const contextKeys = resolveAppShellContext(pathname, specialist);
  const context = {
    section: copy[contextKeys.section],
    label: copy[contextKeys.label],
  };
  const { preferences, toggle, reset } = useCognitivePreferences();
  const unreadQuery = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: getUnreadNotificationsCount,
    enabled: session.token !== null && !specialist,
    retry: false,
    refetchInterval: specialist ? false : 60_000,
  });
  const unreadNotifications = specialist ? 0 : (unreadQuery.data?.count ?? 0);

  useEffect(() => {
    document.documentElement.dataset.praxisRoute = resolveAppShellRouteDataKey(pathname);
  }, [pathname]);

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <a href="#conteudo-principal" className="praxis-skip-link">{copy.skip}</a>

      {!preferences.focusMode && (
        <aside className="praxis-sidebar hidden w-64 shrink-0 flex-col border-r border-border bg-background text-foreground lg:flex">
          <AppSidebar
            pathname={pathname}
            unreadNotifications={unreadNotifications}
            simpleNavigation={preferences.simpleNavigation}
          />
        </aside>
      )}

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-10 flex min-h-16 items-center gap-3 border-b border-border bg-card px-4 py-2 sm:px-6">
          {!preferences.focusMode && (
            <Sheet>
              <SheetTrigger asChild>
                <button type="button" className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-md border border-border bg-card hover:bg-accent lg:hidden" aria-label={copy.openMenu}>
                  <Menu className="h-5 w-5" />
                </button>
              </SheetTrigger>
              <SheetContent side="left" className="flex w-[18rem] max-w-[88vw] flex-col overflow-hidden bg-background p-0 text-foreground sm:max-w-sm">
                <SheetHeader className="sr-only">
                  <SheetTitle>{copy.menu}</SheetTitle>
                  <SheetDescription>{copy.menuDescription}</SheetDescription>
                </SheetHeader>
                <AppSidebar
                  pathname={pathname}
                  unreadNotifications={unreadNotifications}
                  closeOnSelect
                  simpleNavigation={preferences.simpleNavigation}
                />
              </SheetContent>
            </Sheet>
          )}

          <div className="min-w-0 truncate text-sm text-muted-foreground">
            {context.section} <span className="font-medium text-foreground">/ {context.label}</span>
          </div>

          <div className="ml-auto flex shrink-0 items-center justify-end gap-2 text-xs">
            {preferences.focusMode && (
              <button type="button" onClick={() => toggle("focusMode")} className="inline-flex min-h-10 items-center gap-2 rounded-md border border-primary/40 bg-primary/10 px-3 py-2 text-sm font-medium text-primary hover:bg-primary/15">
                <Focus className="h-4 w-4" />
                <span className="hidden sm:inline">{copy.exitFocus}</span>
              </button>
            )}
            {!preferences.focusMode && <LanguageSelector />}
            <AccessibilityPanel preferences={preferences} toggle={toggle} reset={reset} />
            {!preferences.focusMode && (
              <Link to="/manual" className="inline-flex min-h-10 items-center gap-1.5 rounded-md border border-border bg-card px-3 py-2 text-muted-foreground hover:bg-accent">
                <HelpCircle className="h-4 w-4" />
                <span className="hidden sm:inline">{copy.help}</span>
              </Link>
            )}
          </div>
        </header>

        {!specialist && pathname === "/dashboard" && <DeliveryAlertBanner language={language} />}
        <div
          id="conteudo-principal"
          tabIndex={-1}
          className={cn(
            "praxis-page-shell min-h-[calc(100vh-4rem)] px-4 py-6 sm:px-6 lg:px-10 lg:py-8",
            preferences.focusMode && "mx-auto w-full max-w-6xl",
          )}
        >
          {preferences.simpleNavigation && <PageGoal pathname={pathname} language={language} specialist={specialist} />}
          {children}
        </div>
      </main>
    </div>
  );
}
