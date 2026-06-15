import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { BarChart3, Eye, FilePlus2, Filter, Search, Table2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  ScreenStateStrip,
  SkeletonRows,
  StateBanner,
  StatusBadge,
} from "@/components/praxis-ui";
import { simulations, type SimStatus } from "@/lib/mock";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Painel - Praxis" },
      {
        name: "description",
        content:
          "Painel da Renata: simulacoes ativas, qualidade, maturidade e vinculo com vagas Gupy.",
      },
    ],
  }),
  component: Dashboard,
});

const filters: Array<"todas" | SimStatus> = [
  "todas",
  "publicada",
  "piloto",
  "rascunho",
  "em-revisao",
  "expirada",
  "arquivada",
];

function Dashboard() {
  const [firstRun, setFirstRun] = useState(true);
  const [filter, setFilter] = useState<(typeof filters)[number]>("todas");
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState("");

  const totals = {
    publicadas: simulations.filter((s) => s.status === "publicada").length,
    piloto: simulations.filter((s) => s.status === "piloto").length,
    rascunhos: simulations.filter((s) => s.status === "rascunho").length,
    tentativas: simulations.reduce((a, s) => a + s.attempts, 0),
  };

  const filtered = useMemo(() => {
    return simulations.filter((simulation) => {
      const byStatus = filter === "todas" || simulation.status === filter;
      const byQuery =
        query.trim().length === 0 ||
        simulation.name.toLowerCase().includes(query.toLowerCase()) ||
        simulation.role.toLowerCase().includes(query.toLowerCase());
      return byStatus && byQuery;
    });
  }, [filter, query]);

  function loadWorkspace() {
    setLoading(true);
    window.setTimeout(() => {
      setFirstRun(false);
      setLoading(false);
    }, 450);
  }

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="workspace sem permissao ou Gupy desconectada" />
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-muted-foreground">Painel</div>
          <h1 className="mt-1 text-3xl font-semibold text-foreground">Boa tarde, Renata.</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Avaliacao situacional estruturada para recrutamento, sem IA julgando candidato, com
            score por rubrica e trilha auditavel.
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/monitoramento"
            className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
          >
            <BarChart3 className="h-4 w-4" />
            Monitoramento
          </Link>
          <Link
            to="/nova/blueprint"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            <FilePlus2 className="h-4 w-4" />
            Nova simulacao
          </Link>
        </div>
      </div>

      {firstRun ? (
        <div className="space-y-5">
          <EmptyState
            title="Comece pela primeira simulacao guiada"
            description="Conta nova nunca abre em tabela vazia. O fluxo guiado parte de um template parcialmente preenchido e leva do blueprint ao validador em poucos minutos."
            actions={
              <>
                <Link
                  to="/nova/blueprint"
                  className="inline-flex items-center justify-between rounded-md border border-primary bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                >
                  Criar minha primeira simulacao guiada
                  <FilePlus2 className="h-4 w-4" />
                </Link>
                <Link
                  to="/nova/objetivo"
                  className="inline-flex items-center justify-between rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
                >
                  Comecar de um modelo pronto
                  <Table2 className="h-4 w-4" />
                </Link>
                <button
                  type="button"
                  onClick={loadWorkspace}
                  className="inline-flex items-center justify-between rounded-md border border-border bg-card px-4 py-3 text-left text-sm hover:bg-accent"
                >
                  Ver exemplo pre-carregado: O Dia do Caos
                  <Eye className="h-4 w-4" />
                </button>
              </>
            }
          />
          <StateBanner tone="info" title="Exemplo pronto em toda conta nova">
            O Dia do Caos entra como simulacao de leitura para o RH entender o produto antes de
            criar algo do zero.
          </StateBanner>
          {loading && (
            <section className="rounded-md border border-border bg-card p-4">
              <SkeletonRows rows={3} />
            </section>
          )}
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <Stat label="Publicadas" value={totals.publicadas} hint="Em vagas ativas" />
            <Stat label="Em piloto" value={totals.piloto} hint="Ranqueiam, nao eliminam" />
            <Stat label="Rascunhos" value={totals.rascunhos} hint="Em construcao" />
            <Stat label="Tentativas" value={totals.tentativas} hint="Ultimos 30 dias" />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold">Simulacoes</h2>
              <p className="text-xs text-muted-foreground">
                Status tecnico e maturidade aparecem juntos em todas as linhas.
              </p>
            </div>
            <div className="flex min-w-0 flex-wrap gap-2">
              <label className="relative">
                <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  className="input w-64 pl-8"
                  placeholder="Buscar cargo ou simulacao"
                />
              </label>
              <div className="inline-flex flex-wrap gap-1 rounded-md border border-border bg-card p-1">
                <Filter className="m-1.5 h-4 w-4 text-muted-foreground" />
                {filters.map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => setFilter(item)}
                    className={cn(
                      "rounded px-2 py-1 text-xs capitalize hover:bg-accent",
                      filter === item && "bg-primary text-primary-foreground hover:bg-primary",
                    )}
                  >
                    {item.replace("-", " ")}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {filtered.length === 0 ? (
            <EmptyState
              title="Nenhuma simulacao neste filtro"
              description="A lista nunca fica vazia sem orientacao. Ajuste busca, limpe o filtro ou crie um novo rascunho."
              actions={
                <button
                  type="button"
                  onClick={() => {
                    setFilter("todas");
                    setQuery("");
                  }}
                  className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
                >
                  Limpar filtros
                </button>
              }
            />
          ) : (
            <div className="overflow-hidden rounded-md border border-border bg-card">
              <table className="w-full text-sm">
                <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Simulacao</th>
                    <th className="px-4 py-3 text-left font-medium">Estado</th>
                    <th className="px-4 py-3 text-left font-medium">Qualidade</th>
                    <th className="px-4 py-3 text-left font-medium">Versao</th>
                    <th className="px-4 py-3 text-left font-medium">Tentativas</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((simulation) => (
                    <tr
                      key={simulation.id}
                      className="border-b border-border last:border-0 hover:bg-accent/35"
                    >
                      <td className="px-4 py-3">
                        <div className="font-medium text-foreground">{simulation.name}</div>
                        <div className="text-xs text-muted-foreground">
                          {simulation.role} - {simulation.seniority} - atualizada{" "}
                          {simulation.updated}
                        </div>
                        <div className="mt-1 flex flex-wrap gap-1">
                          {simulation.competencies.map((competency) => (
                            <span
                              key={competency}
                              className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                            >
                              {competency}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={simulation.status} maturity={simulation.maturity} />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="h-1.5 w-24 overflow-hidden rounded-full bg-muted">
                            <div
                              className={cn(
                                "h-full rounded-full",
                                simulation.quality >= 80
                                  ? "bg-success"
                                  : simulation.quality >= 60
                                    ? "bg-warning"
                                    : "bg-danger",
                              )}
                              style={{ width: `${simulation.quality}%` }}
                            />
                          </div>
                          <span className="text-xs font-medium tabular-nums">
                            {simulation.quality}/100
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-xs tabular-nums text-muted-foreground">
                        {simulation.version}
                      </td>
                      <td className="px-4 py-3 text-xs tabular-nums">
                        {simulation.attempts.toLocaleString("pt-BR")}
                        <div className="text-[10px] text-muted-foreground">
                          {Math.round(simulation.completion * 100)}% concluem
                        </div>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Link
                          to="/nova/validador"
                          className="text-xs font-medium text-primary hover:underline"
                        >
                          Abrir
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </AppShell>
  );
}

function Stat({ label, value, hint }: { label: string; value: number; hint: string }) {
  return (
    <div className="rounded-md border border-border bg-card p-4">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums text-foreground">
        {value.toLocaleString("pt-BR")}
      </div>
      <div className="mt-1 text-[11px] text-muted-foreground">{hint}</div>
    </div>
  );
}
