import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { AdminShell, StatusBadge, planLabel } from "@/components/admin-shell";
import { getAdminDashboard } from "@/lib/api/praxis";

export const Route = createFileRoute("/admin/")({
  head: () => ({
    meta: [{ title: "Dashboard · Admin Praxis" }],
  }),
  component: AdminDashboardPage,
});

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

  if (dashboard.isLoading) {
    return (
      <AdminShell>
        <p className="text-slate-500">Carregando indicadores…</p>
      </AdminShell>
    );
  }

  if (dashboard.isError || !dashboard.data) {
    return (
      <AdminShell>
        <p className="text-rose-600">Não foi possível carregar o dashboard.</p>
      </AdminShell>
    );
  }

  const data = dashboard.data;

  return (
    <AdminShell>
      <h1 className="text-2xl font-semibold">Dashboard</h1>
      <p className="mt-1 text-sm text-slate-500">
        Visão consolidada da plataforma no período corrente.
      </p>

      <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
        <Card label="Total de clientes" value={data.totalTenants} />
        <Card label="Ativos" value={data.activeTenants} accent="text-emerald-600" />
        <Card label="Em teste" value={data.trialTenants} accent="text-sky-600" />
        <Card label="Suspensos" value={data.suspendedTenants} accent="text-amber-600" />
        <Card label="Cancelados" value={data.canceledTenants} accent="text-rose-600" />
        <Card label="Uso no período" value={data.totalCompletedAttempts} accent="text-primary" />
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold">Clientes que exigem atenção</h2>
          <p className="mb-3 text-xs text-slate-500">Suspensos e cancelados recentemente.</p>
          {data.attentionTenants.length === 0 ? (
            <p className="text-sm text-slate-500">Nenhum cliente em alerta.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.attentionTenants.map((tenant) => (
                <li key={tenant.tenantId} className="flex items-center justify-between py-2.5">
                  <Link
                    to="/admin/tenants/$tenantId"
                    params={{ tenantId: tenant.tenantId }}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {tenant.name}
                  </Link>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-slate-500">{planLabel(tenant.commercialPlanType)}</span>
                    <StatusBadge status={tenant.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold">Clientes com maior uso</h2>
          <p className="mb-3 text-xs text-slate-500">Avaliações concluídas no período.</p>
          {data.topUsageTenants.length === 0 ? (
            <p className="text-sm text-slate-500">Sem uso registrado no período.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {data.topUsageTenants.map((tenant) => (
                <li key={tenant.tenantId} className="flex items-center justify-between py-2.5">
                  <Link
                    to="/admin/tenants/$tenantId"
                    params={{ tenantId: tenant.tenantId }}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {tenant.name}
                  </Link>
                  <span className="text-sm font-semibold">{tenant.completedAttempts}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="mt-6 rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="font-semibold">Clientes criados recentemente</h2>
        {data.recentTenants.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">Nenhum cliente cadastrado ainda.</p>
        ) : (
          <ul className="mt-3 divide-y divide-slate-100">
            {data.recentTenants.map((tenant) => (
              <li key={tenant.tenantId} className="flex items-center justify-between py-2.5">
                <Link
                  to="/admin/tenants/$tenantId"
                  params={{ tenantId: tenant.tenantId }}
                  className="text-sm font-medium text-primary hover:underline"
                >
                  {tenant.name}
                </Link>
                <div className="flex items-center gap-3 text-xs text-slate-500">
                  <span>{planLabel(tenant.commercialPlanType)}</span>
                  <StatusBadge status={tenant.status} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </AdminShell>
  );
}
