import { Link } from "@tanstack/react-router";
import type { ReactNode } from "react";
import {
  BarChart3,
  BookOpenCheck,
  Building2,
  ChevronDown,
  ClipboardList,
  CreditCard,
  FilePlus2,
  HelpCircle,
  Home,
  KeyRound,
  ListChecks,
  Sparkles,
  UserRound,
  Users,
  Workflow,
} from "lucide-react";

import { SheetClose } from "@/components/ui/sheet";
import { isRestrictedPartnerSpecialist } from "@/lib/access-control";
import { useLanguage } from "@/lib/language-context";
import { useSession } from "@/lib/session";
import { cn } from "@/lib/utils";

type NavigationItem = {
  to: string;
  label: string;
  icon: typeof Home;
  badge?: number;
};

type NavigationGroup = {
  label: string;
  items: NavigationItem[];
};

const navigationCopy = {
  "pt-BR": {
    main: "Fluxo principal",
    more: "Mais opções",
    operation: "Operação técnica",
    settings: "Configurações",
    help: "Ajuda",
    references: "Referências",
    accountHelp: "Conta e ajuda",
    specialistArea: "Área do especialista",
    specialistDescription: "Criação e revisão de avaliações em nome da empresa parceira.",
    specialistHome: "Início",
    create: "Criar avaliação",
    assessments: "Avaliações",
    journeys: "Jornadas",
    participations: "Participações",
    results: "Resultados",
    operations: "Central operacional",
    company: "Empresa",
    competencies: "Catálogo de competências",
    team: "Equipe",
    integrations: "Integrações",
    plan: "Plano",
    account: "Minha conta",
    gettingStarted: "Primeiros passos",
    manuals: "Central de manuais",
    menu: "Menu principal",
    specialistRole: "Especialista parceiro",
    workspace: "Workspace",
  },
  en: {
    main: "Main flow",
    more: "More options",
    operation: "Technical operations",
    settings: "Settings",
    help: "Help",
    references: "References",
    accountHelp: "Account and help",
    specialistArea: "Specialist area",
    specialistDescription: "Create and review assessments for the partner company.",
    specialistHome: "Home",
    create: "Create assessment",
    assessments: "Assessments",
    journeys: "Journeys",
    participations: "Participations",
    results: "Results",
    operations: "Operations center",
    company: "Company",
    competencies: "Competency catalog",
    team: "Team",
    integrations: "Integrations",
    plan: "Plan",
    account: "My account",
    gettingStarted: "Getting started",
    manuals: "Manual center",
    menu: "Main menu",
    specialistRole: "Partner specialist",
    workspace: "Workspace",
  },
  "es-MX": {
    main: "Flujo principal",
    more: "Más opciones",
    operation: "Operación técnica",
    settings: "Configuración",
    help: "Ayuda",
    references: "Referencias",
    accountHelp: "Cuenta y ayuda",
    specialistArea: "Área del especialista",
    specialistDescription: "Creación y revisión de evaluaciones para la empresa asociada.",
    specialistHome: "Inicio",
    create: "Crear evaluación",
    assessments: "Evaluaciones",
    journeys: "Jornadas",
    participations: "Participaciones",
    results: "Resultados",
    operations: "Central operativa",
    company: "Empresa",
    competencies: "Catálogo de competencias",
    team: "Equipo",
    integrations: "Integraciones",
    plan: "Plan",
    account: "Mi cuenta",
    gettingStarted: "Primeros pasos",
    manuals: "Central de manuales",
    menu: "Menú principal",
    specialistRole: "Especialista asociado",
    workspace: "Workspace",
  },
} as const;

function ShellLink({ children, closeOnSelect }: { children: ReactNode; closeOnSelect: boolean }) {
  return closeOnSelect ? <SheetClose asChild>{children}</SheetClose> : <>{children}</>;
}

function isActive(pathname: string, itemPath: string): boolean {
  if (itemPath === "/dashboard" || itemPath === "/avaliacoes/especialista") {
    return pathname === itemPath;
  }
  if (itemPath === "/avaliacoes") {
    return (
      pathname === itemPath ||
      (pathname.startsWith("/avaliacoes/") && pathname !== "/avaliacoes/especialista")
    );
  }
  if (itemPath === "/nova/avaliacao") {
    return pathname.startsWith("/nova") || pathname.startsWith("/simulations/new");
  }
  if (itemPath === "/participacoes") {
    return pathname === itemPath || pathname.startsWith("/participacoes/") || pathname === "/enviar-link";
  }
  if (itemPath === "/monitoramento") {
    return pathname === itemPath || pathname === "/notifications";
  }
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`);
}

function Item({ item, pathname, closeOnSelect }: {
  item: NavigationItem;
  pathname: string;
  closeOnSelect: boolean;
}) {
  const active = isActive(pathname, item.to);
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
        <item.icon className={cn("h-4 w-4 shrink-0", active ? "text-accent-foreground" : "text-muted-foreground")} />
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

function Group({ group, pathname, closeOnSelect }: {
  group: NavigationGroup;
  pathname: string;
  closeOnSelect: boolean;
}) {
  return (
    <div>
      <div className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
        {group.label}
      </div>
      {group.items.map((item) => (
        <Item key={item.to} item={item} pathname={pathname} closeOnSelect={closeOnSelect} />
      ))}
    </div>
  );
}

export function AppSidebar({
  pathname,
  unreadNotifications,
  simpleNavigation,
  closeOnSelect = false,
}: {
  pathname: string;
  unreadNotifications: number;
  simpleNavigation: boolean;
  closeOnSelect?: boolean;
}) {
  const session = useSession();
  const { language } = useLanguage();
  const copy = navigationCopy[language];
  const specialist = isRestrictedPartnerSpecialist(session.roles);

  const companyPrimary: NavigationItem[] = [
    { to: "/dashboard", label: "Dashboard", icon: Home },
    { to: "/avaliacoes", label: copy.assessments, icon: ListChecks },
    { to: "/jornadas", label: copy.journeys, icon: Workflow },
    { to: "/participacoes", label: copy.participations, icon: Users },
    { to: "/results", label: copy.results, icon: ClipboardList },
  ];
  const companyGroups: NavigationGroup[] = [
    {
      label: copy.operation,
      items: [{ to: "/monitoramento", label: copy.operations, icon: BarChart3, badge: unreadNotifications }],
    },
    {
      label: copy.settings,
      items: [
        { to: "/configuracoes/perfil", label: copy.company, icon: Building2 },
        { to: "/competencias", label: copy.competencies, icon: BookOpenCheck },
        { to: "/team", label: copy.team, icon: Users },
        { to: "/integrations", label: copy.integrations, icon: KeyRound },
        { to: "/billing", label: copy.plan, icon: CreditCard },
        { to: "/configuracoes/conta", label: copy.account, icon: UserRound },
      ],
    },
    {
      label: copy.help,
      items: [
        { to: "/comecar", label: copy.gettingStarted, icon: Sparkles },
        { to: "/manual", label: copy.manuals, icon: HelpCircle },
      ],
    },
  ];

  const specialistPrimary: NavigationItem[] = [
    { to: "/avaliacoes/especialista", label: copy.specialistHome, icon: Home },
    { to: "/avaliacoes", label: copy.assessments, icon: ListChecks },
    { to: "/nova/avaliacao", label: copy.create, icon: FilePlus2 },
  ];
  const specialistGroups: NavigationGroup[] = [
    {
      label: copy.references,
      items: [{ to: "/competencias", label: copy.competencies, icon: BookOpenCheck }],
    },
    {
      label: copy.accountHelp,
      items: [
        { to: "/configuracoes/conta", label: copy.account, icon: UserRound },
        { to: "/manual", label: copy.manuals, icon: HelpCircle },
      ],
    },
  ];

  const primary = specialist ? specialistPrimary : companyPrimary;
  const groups = specialist ? specialistGroups : companyGroups;
  const secondaryActive = groups.some((group) => group.items.some((item) => isActive(pathname, item.to)));
  const home = specialist ? "/avaliacoes/especialista" : "/dashboard";

  return (
    <>
      <div className="border-b border-border px-5 py-5">
        <ShellLink closeOnSelect={closeOnSelect}>
          <Link to={home} className="inline-flex items-baseline gap-1.5">
            <span className="font-display text-3xl leading-none text-foreground">Práxis</span>
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-primary" />
          </Link>
        </ShellLink>
        <p className="mt-3 text-xs font-semibold uppercase tracking-wide text-primary">
          {specialist ? copy.specialistArea : copy.workspace}
        </p>
        {(!simpleNavigation || specialist) && (
          <p className="mt-2 text-xs leading-relaxed text-muted-foreground">
            {specialist ? copy.specialistDescription : "Avaliações situacionais organizadas pelo processo do cliente."}
          </p>
        )}
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label={copy.menu}>
        <div className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
          {copy.main}
        </div>
        {primary.map((item) => (
          <Item key={item.to} item={item} pathname={pathname} closeOnSelect={closeOnSelect} />
        ))}

        {specialist ? (
          <div className="mt-5 space-y-5 border-t border-border/70 pt-4">
            {groups.map((group) => (
              <Group key={group.label} group={group} pathname={pathname} closeOnSelect={closeOnSelect} />
            ))}
          </div>
        ) : (
          <details className="group mt-5 border-t border-border/70 pt-4" open={!simpleNavigation || secondaryActive}>
            <summary className="flex cursor-pointer list-none items-center justify-between rounded-md px-3 py-2 text-sm font-medium hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
              <span>{copy.more}</span>
              <ChevronDown className="h-4 w-4 transition-transform group-open:rotate-180" />
            </summary>
            <div className="mt-2 space-y-5">
              {groups.map((group) => (
                <Group key={group.label} group={group} pathname={pathname} closeOnSelect={closeOnSelect} />
              ))}
            </div>
          </details>
        )}
      </nav>

      <div className="border-t border-border p-4">
        <div className="flex items-center gap-3 px-1 text-xs">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary font-semibold text-primary-foreground">
            {session.userName.trim().charAt(0).toUpperCase() || "?"}
          </div>
          <div className="min-w-0">
            <div className="truncate font-medium text-foreground">{session.userName}</div>
            <div className="truncate text-muted-foreground">
              {specialist ? copy.specialistRole : session.userRole}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
