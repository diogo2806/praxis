import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { AdminShell, StatusBadge } from "@/components/admin-shell";
import { getAdminDashboard } from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";

export const Route = createFileRoute("/admin/")({
  head: () => ({
    meta: [{ title: "Dashboard · Admin Praxis" }],
  }),
  component: AdminDashboardPage,
});

type DashboardCopy = {
  loading: string;
  loadError: string;
  title: string;
  subtitle: string;
  totalClients: string;
  activeClients: string;
  trialClients: string;
  suspendedClients: string;
  cancelledClients: string;
  usageInPeriod: string;
  attentionClients: string;
  attentionClientsDescription: string;
  noAttentionClients: string;
  topUsageClients: string;
  topUsageClientsDescription: string;
  noUsageInPeriod: string;
  recentClients: string;
  noRecentClients: string;
  planLabels: Record<string, string>;
};

const DASHBOARD_COPY: Record<"pt-BR" | "en" | "es-MX", DashboardCopy> = {
  "pt-BR": {
    loading: "Carregando indicadores…",
    loadError: "Não foi possível carregar o painel administrativo.",
    title: "Painel administrativo",
    subtitle: "Visão consolidada da plataforma no período atual.",
    totalClients: "Total de clientes",
    activeClients: "Ativos",
    trialClients: "Em teste",
    suspendedClients: "Suspensos",
    cancelledClients: "Cancelados",
    usageInPeriod: "Uso no período",
    attentionClients: "Clientes que exigem atenção",
    attentionClientsDescription: "Suspensos e cancelados recentemente.",
    noAttentionClients: "Nenhum cliente em alerta.",
    topUsageClients: "Clientes com maior uso",
    topUsageClientsDescription: "Avaliações concluídas no período.",
    noUsageInPeriod: "Sem uso registrado no período.",
    recentClients: "Clientes criados recentemente",
    noRecentClients: "Nenhum cliente cadastrado ainda.",
    planLabels: {
      AVULSO: "Avulso (crédito pré-pago)",
      PROFISSIONAL: "Profissional (assinatura)",
      ENTERPRISE: "Enterprise (contrato)",
    },
  },
  en: {
    loading: "Loading metrics…",
    loadError: "Could not load the administration dashboard.",
    title: "Administration dashboard",
    subtitle: "Consolidated platform view for the current period.",
    totalClients: "Total clients",
    activeClients: "Active",
    trialClients: "In trial",
    suspendedClients: "Suspended",
    cancelledClients: "Cancelled",
    usageInPeriod: "Usage in period",
    attentionClients: "Clients requiring attention",
    attentionClientsDescription: "Recently suspended and cancelled clients.",
    noAttentionClients: "No clients requiring attention.",
    topUsageClients: "Highest-usage clients",
    topUsageClientsDescription: "Assessments completed in the period.",
    noUsageInPeriod: "No usage recorded in the period.",
    recentClients: "Recently created clients",
    noRecentClients: "No clients registered yet.",
    planLabels: {
      AVULSO: "Single purchase (prepaid credits)",
      PROFISSIONAL: "Professional (subscription)",
      ENTERPRISE: "Enterprise (contract)",
    },
  },
  "es-MX": {
    loading: "Cargando indicadores…",
    loadError: "No fue posible cargar el panel de administración.",
    title: "Panel de administración",
    subtitle: "Vista consolidada de la plataforma en el período actual.",
    totalClients: "Total de clientes",
    activeClients: "Activos",
    trialClients: "En prueba",
    suspendedClients: "Suspendidos",
    cancelledClients: "Cancelados",
    usageInPeriod: "Uso en el período",
    attentionClients: "Clientes que requieren atención",
    attentionClientsDescription: "Clientes suspendidos y cancelados recientemente.",
    noAttentionClients: "No hay clientes en alerta.",
    topUsageClients: "Clientes con mayor uso",
    topUsageClientsDescription: "Evaluaciones completadas en el período.",
    noUsageInPeriod: "No hay uso registrado en el período.",
    recentClients: "Clientes creados recientemente",
    noRecentClients: "Aún no hay clientes registrados.",
    planLabels: {
      AVULSO: "Compra única (crédito prepago)",
      PROFISSIONAL: "Profesional (suscripción)",
      ENTERPRISE: "Enterprise (contrato)",
    },
  },
};

function Card({ label, value, accent }: { label: string; value: number; accent?: string }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <p className="text-sm text-slate-500">{label}</p>
      <p className={`mt-2 text-3xl font-semibold ${accent ?? "text-slate-900"}`}>{value}</p>
    </div>
  );
}

function AdminDashboardPage() {
  const dashboard = useQuery({ queryKey: ["admin-dashboard"], queryFn: getAdminDashboard });
  const { language } = useLanguage();
  const copy = DASHBOARD_COPY[language];

  if (dashboard.isLoading) {
    return (
      <AdminShell>
        <p className="text-slate-500">{copy.loading}</p>
      </AdminShell>
    );
  }

  if (dashboard.isError || !dashboard.data) {
    return (
      <AdminShell>
        <p className="text-rose-600">{copy.loadError}</p>
      </AdminShell>
    );
  }

  const data = dashboard.data;
  const planLabel = (plan: string) => copy.planLabels[plan] ?? plan;

  return (
    <AdminShell>
      <h1 className="text-2xl font-semibold">{copy.title}</h1>
      <p className="mt-1 text-sm text-slate-500">{copy.subtitle}</p>

      <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
        <Card label={copy.totalClients} value={data.totalEmpresas} />
        <Card label={copy.activeClients} value={data.activeEmpresas} accent="text-emerald-600" />
        <Card label={copy.trialClients} value={data.trialEmpresas} accent="text-sky-600" />
        <Card label={copy.suspendedClients} value={data.suspendedEmpresas} accent="text-amber-600" />
        <Card label={copy.cancelledClients} value={data.canceledEmpresas} accent="text-rose-600" />
        <Card label={copy.usageInPeriod} value={data.totalCompletedAttempts} accent="text-primary" />
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold">{copy.attentionClients}</h2>
          <p className="mb-3 text-xs text-slate-500">{copy.attentionClientsDescription}</p>
          {data.attentionEmpresas.length === 0 ? (
            <p className="text-sm text-slate-500">{copy.noAttentionClients}</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.attentionEmpresas.map((empresa) => (
                <li key={empresa.empresaId} className="flex items-center justify-between py-2.5">
                  <Link
                    to="/admin/empresas/$empresaId"
                    params={{ empresaId: empresa.empresaId }}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {empresa.name}
                  </Link>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-slate-500">{planLabel(empresa.commercialPlanType)}</span>
                    <StatusBadge status={empresa.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold">{copy.topUsageClients}</h2>
          <p className="mb-3 text-xs text-slate-500">{copy.topUsageClientsDescription}</p>
          {data.topUsageEmpresas.length === 0 ? (
            <p className="text-sm text-slate-500">{copy.noUsageInPeriod}</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.topUsageEmpresas.map((empresa) => (
                <li key={empresa.empresaId} className="flex items-center justify-between py-2.5">
                  <Link
                    to="/admin/empresas/$empresaId"
                    params={{ empresaId: empresa.empresaId }}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {empresa.name}
                  </Link>
                  <span className="text-sm font-semibold">{empresa.completedAttempts}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="mt-6 rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="font-semibold">{copy.recentClients}</h2>
        {data.recentEmpresas.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">{copy.noRecentClients}</p>
        ) : (
          <ul className="mt-3 divide-y divide-slate-100">
            {data.recentEmpresas.map((empresa) => (
              <li key={empresa.empresaId} className="flex items-center justify-between py-2.5">
                <Link
                  to="/admin/empresas/$empresaId"
                  params={{ empresaId: empresa.empresaId }}
                  className="text-sm font-medium text-primary hover:underline"
                >
                  {empresa.name}
                </Link>
                <div className="flex items-center gap-3 text-xs text-slate-500">
                  <span>{planLabel(empresa.commercialPlanType)}</span>
                  <StatusBadge status={empresa.status} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </AdminShell>
  );
}
