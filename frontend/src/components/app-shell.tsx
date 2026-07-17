"use client";

import { Link, useRouterState } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState, type ReactNode } from "react";
import {
  Accessibility,
  BarChart3,
  Bell,
  BookOpenCheck,
  Building2,
  ChevronDown,
  ClipboardList,
  CreditCard,
  Eye,
  Focus,
  HelpCircle,
  Home,
  KeyRound,
  Link2,
  ListChecks,
  Menu,
  RotateCcw,
  ShieldCheck,
  Sparkles,
  Target,
  Type,
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
type Language = ReturnType<typeof useLanguage>["language"];

type CognitivePreferences = {
  simpleNavigation: boolean;
  largeText: boolean;
  reducedMotion: boolean;
  focusMode: boolean;
};

const COGNITIVE_PREFERENCES_KEY = "praxis-cognitive-preferences";
const DEFAULT_COGNITIVE_PREFERENCES: CognitivePreferences = {
  simpleNavigation: true,
  largeText: false,
  reducedMotion: true,
  focusMode: false,
};

const cognitiveCopy = {
  "pt-BR": {
    accessibility: "Acessibilidade",
    accessibilityDescription: "Ajuste a interface para reduzir distrações e facilitar a leitura.",
    simpleNavigation: "Menu simples",
    simpleNavigationDescription: "Mostra primeiro apenas as tarefas mais usadas.",
    largeText: "Texto maior",
    largeTextDescription: "Aumenta textos, campos e áreas de clique.",
    reducedMotion: "Menos movimento",
    reducedMotionDescription: "Reduz animações e transições da interface.",
    focusMode: "Modo foco",
    focusModeDescription: "Oculta o menu lateral e limita a largura do conteúdo.",
    reset: "Restaurar ajustes",
    enabled: "Ativado",
    disabled: "Desativado",
    mainTasks: "Tarefas principais",
    moreOptions: "Mais opções",
    participants: "Participantes",
    assessmentProcesses: "Processos de avaliação",
    administration: "Administração",
    skipToContent: "Ir para o conteúdo principal",
    pageGoal: "Objetivo desta tela",
    exitFocus: "Sair do modo foco",
  },
  en: {
    accessibility: "Accessibility",
    accessibilityDescription: "Adjust the interface to reduce distractions and improve readability.",
    simpleNavigation: "Simple menu",
    simpleNavigationDescription: "Shows the most common tasks first.",
    largeText: "Larger text",
    largeTextDescription: "Increases text, fields and click targets.",
    reducedMotion: "Less motion",
    reducedMotionDescription: "Reduces interface animations and transitions.",
    focusMode: "Focus mode",
    focusModeDescription: "Hides the sidebar and limits the content width.",
    reset: "Reset adjustments",
    enabled: "Enabled",
    disabled: "Disabled",
    mainTasks: "Main tasks",
    moreOptions: "More options",
    participants: "Participants",
    assessmentProcesses: "Assessment processes",
    administration: "Administration",
    skipToContent: "Skip to main content",
    pageGoal: "Goal of this page",
    exitFocus: "Exit focus mode",
  },
  "es-MX": {
    accessibility: "Accesibilidad",
    accessibilityDescription: "Ajusta la interfaz para reducir distracciones y facilitar la lectura.",
    simpleNavigation: "Menú simple",
    simpleNavigationDescription: "Muestra primero las tareas más utilizadas.",
    largeText: "Texto más grande",
    largeTextDescription: "Aumenta textos, campos y áreas de interacción.",
    reducedMotion: "Menos movimiento",
    reducedMotionDescription: "Reduce animaciones y transiciones de la interfaz.",
    focusMode: "Modo enfoque",
    focusModeDescription: "Oculta el menú lateral y limita el ancho del contenido.",
    reset: "Restaurar ajustes",
    enabled: "Activado",
    disabled: "Desactivado",
    mainTasks: "Tareas principales",
    moreOptions: "Más opciones",
    participants: "Participantes",
    assessmentProcesses: "Procesos de evaluación",
    administration: "Administración",
    skipToContent: "Ir al contenido principal",
    pageGoal: "Objetivo de esta pantalla",
    exitFocus: "Salir del modo enfoque",
  },
} as const;

const pageGoalCopy = {
  "pt-BR": {
    dashboard: "Veja primeiro o que precisa da sua atenção hoje.",
    assessments: "Crie, continue, publique ou arquive avaliações.",
    participants: "Convide uma pessoa e acompanhe os links já enviados.",
    journeys: "Organize várias avaliações em uma sequência única.",
    results: "Consulte resultados e evidências antes da decisão humana.",
    settings: "Ajuste empresa, equipe, integrações e plano.",
    default: "Conclua uma tarefa por vez. As opções adicionais continuam disponíveis no menu.",
  },
  en: {
    dashboard: "See what needs your attention today first.",
    assessments: "Create, continue, publish or archive assessments.",
    participants: "Invite a person and track links already sent.",
    journeys: "Organize several assessments into one sequence.",
    results: "Review results and evidence before the human decision.",
    settings: "Adjust company, team, integrations and plan.",
    default: "Complete one task at a time. Additional options remain available in the menu.",
  },
  "es-MX": {
    dashboard: "Vea primero lo que necesita su atención hoy.",
    assessments: "Cree, continúe, publique o archive evaluaciones.",
    participants: "Invite a una persona y acompañe los enlaces enviados.",
    journeys: "Organice varias evaluaciones en una sola secuencia.",
    results: "Revise resultados y evidencias antes de la decisión humana.",
    settings: "Ajuste empresa, equipo, integraciones y plan.",
    default: "Complete una tarea a la vez. Las opciones adicionales siguen disponibles en el menú.",
  },
} as const;

function ShellLink({ children, closeOnSelect }: { children: ReactNode; closeOnSelect?: boolean }) {
  return closeOnSelect ? <SheetClose asChild>{children}</SheetClose> : <>{children}</>;
}

function useCognitivePreferences() {
  const [preferences, setPreferences] = useState<CognitivePreferences>(DEFAULT_COGNITIVE_PREFERENCES);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(COGNITIVE_PREFERENCES_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as Partial<CognitivePreferences>;
        setPreferences({ ...DEFAULT_COGNITIVE_PREFERENCES, ...parsed });
      }
    } catch {
      setPreferences(DEFAULT_COGNITIVE_PREFERENCES);
    } finally {
      setHydrated(true);
    }
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    root.dataset.simpleNavigation = String(preferences.simpleNavigation);
    root.dataset.largeText = String(preferences.largeText);
    root.dataset.reducedMotion = String(preferences.reducedMotion);
    root.dataset.focusMode = String(preferences.focusMode);

    if (hydrated) {
      window.localStorage.setItem(COGNITIVE_PREFERENCES_KEY, JSON.stringify(preferences));
    }
  }, [hydrated, preferences]);

  function toggle(preference: keyof CognitivePreferences) {
    setPreferences((current) => ({ ...current, [preference]: !current[preference] }));
  }

  function reset() {
    setPreferences(DEFAULT_COGNITIVE_PREFERENCES);
  }

  return { preferences, toggle, reset };
}

function SidebarContent({
  closeOnSelect = false,
  pathname,
  unreadNotifications,
  simpleNavigation,
}: {
  closeOnSelect?: boolean;
  pathname: string;
  unreadNotifications: number;
  simpleNavigation: boolean;
}) {
  const session = useSession();
  const { language, t } = useLanguage();
  const copy = appShellCopy[language];
  const cognitive = cognitiveCopy[language];

  const primaryItems = [
    { to: "/dashboard", label: t.common.dashboard, icon: Home },
    { to: "/avaliacoes", label: t.common.situationalAssessment, icon: ListChecks },
    { to: "/enviar-link", label: cognitive.participants, icon: Link2 },
    { to: "/results", label: t.common.results, icon: ClipboardList },
  ] as const;

  const secondaryGroups = [
    {
      label: t.common.situationalAssessment,
      items: [
        { to: "/competencias", label: t.common.competencies, icon: BookOpenCheck },
        { to: "/jornadas", label: cognitive.assessmentProcesses, icon: Workflow },
        { to: "/talent-match", label: t.common.talentMatch, icon: Target },
      ],
    },
    {
      label: t.common.operation,
      items: [
        { to: "/monitoramento", label: t.common.operationCenter, icon: BarChart3 },
        { to: "/compliance", label: t.common.compliance, icon: ShieldCheck },
        { to: "/notifications", label: copy.notificationsLabel, icon: Bell, badge: unreadNotifications },
      ],
    },
    {
      label: cognitive.administration,
      items: [
        { to: "/configuracoes/perfil", label: t.common.profile, icon: Building2 },
        { to: "/team", label: t.common.myTeam, icon: Users },
        { to: "/integrations", label: t.common.integrations, icon: KeyRound },
        { to: "/billing", label: t.common.plan, icon: CreditCard },
        { to: "/configuracoes/conta", label: t.common.myAccount, icon: UserRound },
      ],
    },
  ] as const;

  const secondaryActive = secondaryGroups.some((group) =>
    group.items.some((item) => isActivePath(pathname, item.to)),
  );

  return (
    <>
      <div className="border-b border-border px-5 py-5">
        <ShellLink closeOnSelect={closeOnSelect}>
          <Link to="/dashboard" className="inline-flex items-baseline gap-1.5">
            <span className="font-display text-3xl leading-none text-foreground">Práxis</span>
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-primary" />
          </Link>
        </ShellLink>
        {!simpleNavigation && (
          <div className="mt-3 text-[11px] uppercase leading-relaxed text-muted-foreground">
            {t.common.tagline}
          </div>
        )}
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label={t.common.menu}>
        <ShellLink closeOnSelect={closeOnSelect}>
          <Link
            to="/comecar"
            aria-current={pathname === "/comecar" ? "page" : undefined}
            className={cn(
              "mb-4 flex items-start gap-3 rounded-lg border px-3 py-3 text-sm transition",
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

        <div className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
          {cognitive.mainTasks}
        </div>
        {primaryItems.map((item) => (
          <NavigationItem
            key={item.to}
            item={item}
            active={isActivePath(pathname, item.to)}
            closeOnSelect={closeOnSelect}
          />
        ))}

        <details
          className="group mt-5 border-t border-border/70 pt-4"
          open={!simpleNavigation || secondaryActive}
        >
          <summary className="flex cursor-pointer list-none items-center justify-between rounded-md px-3 py-2 text-sm font-medium hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <span>{cognitive.moreOptions}</span>
            <ChevronDown className="h-4 w-4 transition-transform group-open:rotate-180" />
          </summary>
          <div className="mt-2 space-y-5">
            {secondaryGroups.map((group) => (
              <div key={group.label}>
                <div className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                  {group.label}
                </div>
                {group.items.map((item) => (
                  <NavigationItem
                    key={item.to}
                    item={item}
                    active={isActivePath(pathname, item.to)}
                    closeOnSelect={closeOnSelect}
                  />
                ))}
              </div>
            ))}
          </div>
        </details>
      </nav>

      <div className="border-t border-border p-4">
        <div className="flex items-center gap-3 px-1 text-xs">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary font-semibold text-primary-foreground">
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

type NavigationItemData = {
  to: string;
  label: string;
  icon: typeof Home;
  badge?: number;
};

function NavigationItem({
  item,
  active,
  closeOnSelect,
}: {
  item: NavigationItemData;
  active: boolean;
  closeOnSelect: boolean;
}) {
  return (
    <ShellLink closeOnSelect={closeOnSelect}>
      <Link
        to={item.to}
        aria-current={active ? "page" : undefined}
        className={cn(
          "mb-1 flex min-h-11 items-center gap-3 rounded-md px-3 py-2 text-sm transition",
          active ? "bg-accent font-medium text-accent-foreground" : "text-foreground/85 hover:bg-accent",
        )}
      >
        <item.icon
          className={cn(
            "h-4 w-4 shrink-0",
            active ? "text-accent-foreground" : "text-muted-foreground",
          )}
        />
        <span className="min-w-0 flex-1">{item.label}</span>
        {(item.badge ?? 0) > 0 && (
          <span className="rounded-full bg-danger px-1.5 py-0.5 text-[10px] font-semibold text-danger-foreground">
            {(item.badge ?? 0) > 99 ? "99+" : item.badge}
          </span>
        )}
      </Link>
    </ShellLink>
  );
}

function AccessibilityPanel({
  language,
  preferences,
  toggle,
  reset,
}: {
  language: Language;
  preferences: CognitivePreferences;
  toggle: (preference: keyof CognitivePreferences) => void;
  reset: () => void;
}) {
  const copy = cognitiveCopy[language];
  const options = [
    {
      key: "simpleNavigation" as const,
      title: copy.simpleNavigation,
      description: copy.simpleNavigationDescription,
      icon: Eye,
    },
    {
      key: "largeText" as const,
      title: copy.largeText,
      description: copy.largeTextDescription,
      icon: Type,
    },
    {
      key: "reducedMotion" as const,
      title: copy.reducedMotion,
      description: copy.reducedMotionDescription,
      icon: Accessibility,
    },
    {
      key: "focusMode" as const,
      title: copy.focusMode,
      description: copy.focusModeDescription,
      icon: Focus,
    },
  ];

  return (
    <Sheet>
      <SheetTrigger asChild>
        <button
          type="button"
          className="inline-flex min-h-10 items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-accent"
          aria-label={copy.accessibility}
        >
          <Accessibility className="h-4 w-4" />
          <span className="hidden sm:inline">{copy.accessibility}</span>
        </button>
      </SheetTrigger>
      <SheetContent side="right" className="w-[24rem] max-w-[92vw] overflow-y-auto bg-background p-0 text-foreground">
        <SheetHeader className="border-b border-border p-5 text-left">
          <SheetTitle className="flex items-center gap-2 text-xl">
            <Accessibility className="h-5 w-5" />
            {copy.accessibility}
          </SheetTitle>
          <SheetDescription className="text-sm leading-6">
            {copy.accessibilityDescription}
          </SheetDescription>
        </SheetHeader>
        <div className="space-y-3 p-5">
          {options.map((option) => {
            const enabled = preferences[option.key];
            return (
              <button
                key={option.key}
                type="button"
                aria-pressed={enabled}
                onClick={() => toggle(option.key)}
                className={cn(
                  "flex w-full items-start gap-3 rounded-lg border p-4 text-left transition",
                  enabled ? "border-primary/50 bg-primary/10" : "border-border bg-card hover:bg-accent",
                )}
              >
                <option.icon className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
                <span className="min-w-0 flex-1">
                  <span className="block font-semibold">{option.title}</span>
                  <span className="mt-1 block text-sm leading-5 text-muted-foreground">
                    {option.description}
                  </span>
                  <span className="mt-2 block text-xs font-medium text-primary">
                    {enabled ? copy.enabled : copy.disabled}
                  </span>
                </span>
              </button>
            );
          })}
          <button
            type="button"
            onClick={reset}
            className="mt-2 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent"
          >
            <RotateCcw className="h-4 w-4" />
            {copy.reset}
          </button>
        </div>
      </SheetContent>
    </Sheet>
  );
}

function PageGoal({ pathname, language }: { pathname: string; language: Language }) {
  const copy = cognitiveCopy[language];
  const goals = pageGoalCopy[language];
  const key = pageGoalKey(pathname);

  return (
    <section className="praxis-page-goal mb-6 flex max-w-4xl items-start gap-3 rounded-lg border border-primary/25 bg-primary/5 p-4">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
        1
      </div>
      <div>
        <div className="text-sm font-semibold text-foreground">{copy.pageGoal}</div>
        <p className="mt-1 text-sm leading-6 text-muted-foreground">{goals[key]}</p>
      </div>
    </section>
  );
}

function pageGoalKey(pathname: string): keyof (typeof pageGoalCopy)["pt-BR"] {
  if (pathname === "/dashboard") return "dashboard";
  if (pathname.startsWith("/avaliacoes") || pathname.startsWith("/nova")) return "assessments";
  if (pathname === "/enviar-link") return "participants";
  if (pathname === "/jornadas" || pathname.startsWith("/jornada/")) return "journeys";
  if (pathname.startsWith("/results") || pathname === "/talent-match") return "results";
  if (
    pathname.startsWith("/configuracoes") ||
    pathname.startsWith("/integrations") ||
    pathname === "/team" ||
    pathname === "/billing"
  ) {
    return "settings";
  }
  return "default";
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

function routeDataKey(pathname: string) {
  if (pathname.startsWith("/avaliacoes")) return "avaliacoes";
  if (pathname === "/enviar-link") return "enviar-link";
  if (pathname === "/jornadas" || pathname.startsWith("/jornada/")) return "jornadas";
  if (pathname.startsWith("/results")) return "results";
  if (pathname === "/dashboard") return "dashboard";
  return "other";
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const { language, t } = useLanguage();
  const copy = appShellCopy[language];
  const cognitive = cognitiveCopy[language];
  const context = pageContext(pathname, t, copy);
  const { preferences, toggle, reset } = useCognitivePreferences();
  const unreadNotificationsQuery = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: getUnreadNotificationsCount,
    retry: false,
    refetchInterval: 60_000,
  });
  const unreadNotifications = unreadNotificationsQuery.data?.count ?? 0;

  useEffect(() => {
    document.documentElement.dataset.praxisRoute = routeDataKey(pathname);
  }, [pathname]);

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <a href="#conteudo-principal" className="praxis-skip-link">
        {cognitive.skipToContent}
      </a>

      {!preferences.focusMode && (
        <aside className="praxis-sidebar hidden w-64 shrink-0 flex-col border-r border-border bg-background text-foreground lg:flex">
          <SidebarContent
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
                <button
                  type="button"
                  className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-md border border-border bg-card hover:bg-accent lg:hidden"
                  aria-label={t.common.openMenu}
                >
                  <Menu className="h-5 w-5" />
                </button>
              </SheetTrigger>
              <SheetContent side="left" className="flex w-[18rem] max-w-[88vw] flex-col overflow-hidden bg-background p-0 text-foreground sm:max-w-sm">
                <SheetHeader className="sr-only">
                  <SheetTitle>{t.common.menu}</SheetTitle>
                  <SheetDescription>{t.common.menuDescription}</SheetDescription>
                </SheetHeader>
                <SidebarContent
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
              <button
                type="button"
                onClick={() => toggle("focusMode")}
                className="inline-flex min-h-10 items-center gap-2 rounded-md border border-primary/40 bg-primary/10 px-3 py-2 text-sm font-medium text-primary hover:bg-primary/15"
              >
                <Focus className="h-4 w-4" />
                <span className="hidden sm:inline">{cognitive.exitFocus}</span>
              </button>
            )}
            {!preferences.focusMode && <LanguageSelector />}
            <AccessibilityPanel
              language={language}
              preferences={preferences}
              toggle={toggle}
              reset={reset}
            />
            {!preferences.focusMode && (
              <Link
                to="/comecar"
                className="inline-flex min-h-10 items-center gap-1.5 rounded-md border border-border bg-card px-3 py-2 text-muted-foreground hover:bg-accent"
              >
                <HelpCircle className="h-4 w-4" />
                <span className="hidden sm:inline">{t.common.help}</span>
              </Link>
            )}
          </div>
        </header>

        {pathname === "/dashboard" && <DeliveryAlertBanner language={language} />}

        <div
          id="conteudo-principal"
          tabIndex={-1}
          className={cn(
            "praxis-page-shell min-h-[calc(100vh-4rem)] px-4 py-6 sm:px-6 lg:px-10 lg:py-8",
            preferences.focusMode && "mx-auto w-full max-w-6xl",
          )}
        >
          {preferences.simpleNavigation && <PageGoal pathname={pathname} language={language} />}
          {children}
        </div>
      </main>
    </div>
  );
}
