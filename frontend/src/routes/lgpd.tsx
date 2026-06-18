import { useQuery } from "@tanstack/react-query";
import { useLanguage } from "@/lib/language-context";
import { createFileRoute, Link } from "@tanstack/react-router";
import { FileSearch, ShieldCheck } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Termo } from "@/components/glossario";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  getPrivacyCompliance,
  getSimulationVersion,
  listSimulationVersionAuditEvents,
  listSimulations,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/lgpd")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string"
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "LGPD & Transparência do resultado - Praxis" },
      { name: "description", content: "Explicação de score e revisão humana." },
    ],
  }),
  component: LgpdPage,
});

function LgpdPage() {
  const { t } = useLanguage();
  const search = Route.useSearch();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasContext,
  });
  const privacyQuery = useQuery({
    queryKey: ["privacy-compliance"],
    queryFn: getPrivacyCompliance,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const auditQuery = useQuery({
    queryKey: ["simulation-audit", search.simulationId, search.versionNumber],
    queryFn: () => listSimulationVersionAuditEvents(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const version = versionQuery.data;
  const optionsCount =
    version?.nodes.reduce((total, node) => total + node.options.length, 0) ?? 0;
  const criticalOptionsCount =
    version?.nodes.reduce(
      (total, node) => total + node.options.filter((option) => option.isCritical).length,
      0,
    ) ?? 0;

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="canal de revisão humana não configurado" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">{t.common.compliance}</div>
          <h1 className="mt-1 text-3xl font-semibold">{t.lgpd.heading}</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            O candidato pode pedir revisão. A explicação usa <Termo id="rubrica">rubrica</Termo>,
            escolha e caminho, sem <Termo id="caixa-preta">caixa-preta</Termo>.
          </p>
        </div>
        {version && <StatusBadge status={version.status} />}
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        {[
          {
            key: "normalizada",
            title: (
              <>
                <Termo id="pontuacao-normalizada">Pontuação normalizada</Termo> por caminho
              </>
            ),
          },
          {
            key: "erro-critico",
            title: (
              <>
                <Termo id="erro-critico">Erro crítico</Termo> gera revisão humana
              </>
            ),
          },
          { key: "sem-ia", title: <>Sem IA julgando candidato</> },
        ].map((item) => (
          <div key={item.key} className="rounded-md border border-border bg-card p-5">
            <ShieldCheck className="h-5 w-5 text-success" />
            <div className="mt-3 text-sm font-semibold">{item.title}</div>
            <p className="mt-1 text-sm text-muted-foreground">
              Evidência rastreável por versão, tentativa, turno e alternativa escolhida.
            </p>
          </div>
        ))}
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_340px]">
        <section className="rounded-md border border-border bg-card p-5">
          <div className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <FileSearch className="h-4 w-4" />
            Explicação baseada em contrato real
          </div>
          {!hasContext ? (
            <EmptyState
              title="Selecione uma simulação para ver a explicação"
              description="Escolha uma simulação abaixo para entender como funciona a explicação da nota que o candidato recebe. Você verá o planejamento, as alternativas e o histórico de eventos."
              actions={<SimulationLinks loading={simulationsQuery.isLoading} simulations={simulationsQuery.data ?? []} />}
            />
          ) : versionQuery.isLoading || auditQuery.isLoading ? (
            <StateBanner tone="info" title="Carregando transparência do resultado">
              Buscando versão {search.simulationId} v{search.versionNumber}.
            </StateBanner>
          ) : versionQuery.isError || auditQuery.isError ? (
            <StateBanner tone="danger" title="Não foi possível carregar dados LGPD">
              {versionQuery.error instanceof Error
                ? versionQuery.error.message
                : auditQuery.error instanceof Error
                  ? auditQuery.error.message
                  : "Verifique sua conexão e tente novamente."}
            </StateBanner>
          ) : version ? (
            <div>
              <h2 className="text-xl font-semibold">{version.name}</h2>
              <p className="mt-2 text-sm text-muted-foreground">{version.description}</p>
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <Metric label="Turnos" value={version.nodes.length} />
                <Metric label="Alternativas" value={optionsCount} />
                <Metric label="Críticas" value={criticalOptionsCount} />
              </div>
              <div className="mt-5 space-y-3">
                {version.nodes.slice(0, 3).map((node) => (
                  <div key={node.id} className="rounded-md border border-border bg-background p-3">
                    <div className="text-xs font-semibold uppercase text-muted-foreground">
                      {node.id} - turno {node.turnIndex}
                    </div>
                    <p className="mt-1 text-sm">{node.clientMessage}</p>
                    <div className="mt-2 flex flex-wrap gap-1.5 text-[11px] text-muted-foreground">
                      {node.options.slice(0, 3).map((option) => (
                        <span key={option.id} className="rounded border border-border px-2 py-1">
                          {option.id}: {Object.keys(option.competencyLevels).join(", ") || "sem rubrica"}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </section>

        <aside className="space-y-4">
          {privacyQuery.isLoading ? (
            <StateBanner tone="info" title="Carregando política LGPD">
              Buscando bases legais, retenção e canal de revisão.
            </StateBanner>
          ) : privacyQuery.isError ? (
            <StateBanner tone="danger" title="Não foi possível carregar política LGPD">
              {privacyQuery.error instanceof Error ? privacyQuery.error.message : "Verifique sua conexão e tente novamente."}
            </StateBanner>
          ) : privacyQuery.data ? (
            <div className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Política operacional
              </div>
              <div className="mt-3 space-y-3 text-sm">
                <div className="rounded-md border border-border bg-background p-3">
                  <div className="font-medium">Retenção: {privacyQuery.data.retentionDays} dias</div>
                  <p className="mt-1 text-xs text-muted-foreground">{privacyQuery.data.retentionPolicy}</p>
                </div>
                <div className="rounded-md border border-border bg-background p-3">
                  <div className="font-medium">Revisão humana</div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {privacyQuery.data.reviewChannel} - SLA {privacyQuery.data.reviewSla}
                  </p>
                </div>
                <div className="rounded-md border border-border bg-background p-3">
                  <div className="font-medium">Decisão automatizada sem revisão</div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {privacyQuery.data.automatedDecisionWithoutReviewAllowed ? "Permitida" : "Não permitida"}
                  </p>
                </div>
              </div>
            </div>
          ) : null}
          <StateBanner tone="warn" title="Canal de revisão obrigatório">
            Uso eliminatório só é permitido para simulação validada, com aprovação e revisão humana.
          </StateBanner>
          {privacyQuery.data && (
            <div className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Bases legais
              </div>
              <div className="mt-3 space-y-3">
                {privacyQuery.data.legalBases.map((basis) => (
                  <div key={basis.name} className="rounded-md border border-border bg-background p-3 text-sm">
                    <div className="font-medium">{basis.name}</div>
                    <p className="mt-1 text-xs text-muted-foreground">{basis.description}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
          {hasContext && (
            <div className="rounded-md border border-border bg-card p-5">
              <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Evidências auditáveis
              </div>
              <div className="mt-3 space-y-3">
                {(auditQuery.data ?? []).slice(0, 4).map((event) => (
                  <div key={event.id} className="rounded-md border border-border bg-background p-3 text-sm">
                    <div className="font-medium">{event.message}</div>
                    <div className="mt-1 text-xs text-muted-foreground">
                      {new Date(event.createdAt).toLocaleString("pt-BR")}
                    </div>
                  </div>
                ))}
                {(auditQuery.data ?? []).length === 0 && (
                  <p className="text-sm text-muted-foreground">Nenhum evento registrado.</p>
                )}
              </div>
            </div>
          )}
        </aside>
      </div>

      <div className="mt-6">
        <Link
          to="/"
          className="inline-flex rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>
    </AppShell>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 text-2xl font-semibold tabular-nums">{value}</div>
    </div>
  );
}

function SimulationLinks({
  loading,
  simulations,
}: {
  loading: boolean;
  simulations: SimulationSummaryResponse[];
}) {
  if (loading) return <span className="text-sm text-muted-foreground">Carregando...</span>;
  return (
    <div className="flex flex-wrap gap-2">
      {simulations.map((simulation) => (
        <Link
          key={`${simulation.id}-${simulation.versionNumber}`}
          to="/lgpd"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
