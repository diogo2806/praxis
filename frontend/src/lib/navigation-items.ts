import {
  Activity,
  BarChart3,
  BriefcaseBusiness,
  Building2,
  ClipboardList,
  CreditCard,
  FileStack,
  FileText,
  FlaskConical,
  GitCompare,
  Gauge,
  HelpCircle,
  Home,
  KeyRound,
  LayoutDashboard,
  Link2,
  ListChecks,
  Network,
  Plug,
  ReceiptText,
  Send,
  Settings,
  ShieldCheck,
  Sparkles,
  Target,
  TestTube2,
  UserRoundCheck,
  Users,
  UsersRound,
  Workflow,
} from "lucide-react";

import type { SessionProfile } from "@/lib/session";

export type NavigationIcon = typeof Home;
export type NavigationItem = { icon: NavigationIcon; label: string; path: string; allowedRoles: SessionProfile[]; children?: NavigationItem[] };
export type NavigationGroup = { label: string; items: NavigationItem[] };

const productRoles: SessionProfile[] = ["ADMIN", "CLIENT_MANAGER", "PARTNER_SPECIALIST"];
const managementRoles: SessionProfile[] = ["ADMIN", "CLIENT_MANAGER"];
const adminRoles: SessionProfile[] = ["ADMIN"];

export const dashboardItem: NavigationItem = { icon: LayoutDashboard, label: "Dashboard", path: "/dashboard", allowedRoles: productRoles };

export const navigationGroups: NavigationGroup[] = [
  {
    label: "Construção",
    items: [
      {
        icon: ClipboardList,
        label: "Avaliações",
        path: "/avaliacoes",
        allowedRoles: productRoles,
        children: [
          { icon: ClipboardList, label: "Central de Avaliações", path: "/avaliacoes", allowedRoles: productRoles },
          { icon: FileStack, label: "Biblioteca de Modelos", path: "/avaliacoes/modelos", allowedRoles: productRoles },
          { icon: Sparkles, label: "Nova Avaliação", path: "/nova", allowedRoles: productRoles },
          { icon: ListChecks, label: "Competências", path: "/competencias", allowedRoles: productRoles },
          { icon: Workflow, label: "Jornadas", path: "/jornadas", allowedRoles: productRoles },
          { icon: BriefcaseBusiness, label: "Prévias realistas", path: "/previas-realistas", allowedRoles: managementRoles },
        ],
      },
      {
        icon: UsersRound,
        label: "Jornada",
        path: "/jornadas",
        allowedRoles: productRoles,
        children: [
          { icon: Send, label: "Campanhas", path: "/participacoes/campanhas", allowedRoles: managementRoles },
          { icon: Users, label: "Participações", path: "/participacoes", allowedRoles: productRoles },
          { icon: Link2, label: "Convites", path: "/convites", allowedRoles: productRoles },
          { icon: UserRoundCheck, label: "Experiência do Participante", path: "/candidato/demo", allowedRoles: productRoles },
        ],
      },
    ],
  },
  {
    label: "Análise",
    items: [{
      icon: BarChart3,
      label: "Resultados",
      path: "/results",
      allowedRoles: productRoles,
      children: [
        { icon: BarChart3, label: "Resultados", path: "/results", allowedRoles: productRoles },
        { icon: GitCompare, label: "Talent Match", path: "/talent-match", allowedRoles: productRoles },
        { icon: TestTube2, label: "Piloto e Indicadores", path: "/piloto", allowedRoles: productRoles },
        { icon: FlaskConical, label: "Qualidade e Justiça", path: "/resultados/qualidade", allowedRoles: managementRoles },
      ],
    }],
  },
  {
    label: "Operação",
    items: [
      { icon: Gauge, label: "Central Operacional", path: "/operacao", allowedRoles: productRoles },
      {
        icon: Plug,
        label: "Integrações",
        path: "/integracoes",
        allowedRoles: managementRoles,
        children: [
          { icon: Plug, label: "Integrações", path: "/integracoes", allowedRoles: managementRoles },
          { icon: ShieldCheck, label: "Ativação Gupy", path: "/integracoes/gupy/ativacao", allowedRoles: managementRoles },
          { icon: Network, label: "Webhooks", path: "/integracoes/webhooks", allowedRoles: managementRoles },
        ],
      },
    ],
  },
  {
    label: "Gestão",
    items: [
      {
        icon: Settings,
        label: "Empresa",
        path: "/perfil-empresa",
        allowedRoles: managementRoles,
        children: [
          { icon: Building2, label: "Perfil da Empresa", path: "/perfil-empresa", allowedRoles: managementRoles },
          { icon: Users, label: "Equipe e Acessos", path: "/equipe", allowedRoles: managementRoles },
          { icon: KeyRound, label: "Parceiros", path: "/parceiros", allowedRoles: managementRoles },
        ],
      },
      {
        icon: CreditCard,
        label: "Financeiro",
        path: "/financeiro",
        allowedRoles: managementRoles,
        children: [
          { icon: CreditCard, label: "Financeiro", path: "/financeiro", allowedRoles: managementRoles },
          { icon: ReceiptText, label: "Cobrança", path: "/admin/cobranca", allowedRoles: adminRoles },
          { icon: Activity, label: "Uso e Créditos", path: "/admin/uso", allowedRoles: adminRoles },
        ],
      },
      {
        icon: ShieldCheck,
        label: "Administração",
        path: "/admin",
        allowedRoles: adminRoles,
        children: [
          { icon: ShieldCheck, label: "Administração", path: "/admin", allowedRoles: adminRoles },
          { icon: Building2, label: "Empresas", path: "/admin/empresas", allowedRoles: adminRoles },
          { icon: Target, label: "Governança", path: "/admin/governanca", allowedRoles: adminRoles },
        ],
      },
    ],
  },
];

export const footerNavigationItems: NavigationItem[] = [
  { icon: HelpCircle, label: "Ajuda", path: "/ajuda", allowedRoles: productRoles },
  { icon: FileText, label: "Manual", path: "/manual", allowedRoles: productRoles },
];

export function filterNavigationItems(items: NavigationItem[], profile: SessionProfile) {
  return items.filter((item) => item.allowedRoles.includes(profile)).map((item) => ({ ...item, children: item.children ? filterNavigationItems(item.children, profile) : undefined }));
}
