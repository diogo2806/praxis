import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  Bell,
  CheckCircle2,
  Clock3,
  Eye,
  PlayCircle,
  RefreshCw,
  RotateCcw,
  Send,
  XCircle,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import {
  listCandidateLinks,
  listLiveAttempts,
  listResultDeliveries,
  listSimulations,
  type AttemptStatus,
  type CandidateAttemptMonitoringResponse,
  type CandidateLinkResponse,
  type ResultDeliveryResponse,
  type ResultDeliveryStatus,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import {
  listNotifications,
  processReadyResultDeliveries,
  reprocessResultDelivery,
  type InAppNotificationResponse,
} from "@/lib/api/monitoring-operations";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/monitoramento")({
  head: () => ({
    meta: [
      { title: "Monitoramento - Práxis" },
      {
        name: "description",
        content: "Central operacional para acompanhar convites, tentativas e entregas.",
      },
    ],
  }),
  component: MonitoringPage,
});

type AttemptFilter = "todos" | "ativos" | "finalizadas" | "sem-sinal";
type DeliveryFilter = "todos" | ResultDeliveryStatus;

function MonitoringPage() {
  const queryClient = useQueryClient();
  const [revealed, setRevealed] = useState(false);
  const [attemptFilter, setAttemptFilter] = useState<AttemptFilter>("todos");
  const [deliveryFilter, setDeliveryFilter] = useState<DeliveryFilter>("dlq");
  const [simulationFilter, setSimulationFilter] = useState<string>("todos");

  const simulationsQuery = useQuery({ queryKey: ["simulations"], queryFn: listSimulations });
  const deliveriesQuery = useQuery({ queryKey: ["result-deliveries"], queryFn: () => listResultDeliveries() });
  const notificationsQuery = useQuery({ queryKey: ["notifications"], queryFn: listNotifications });
  const liveAttemptsQuery = useQuery({ queryKey: ["live-attempts"], queryFn: listLiveAttempts });
  const candidateLinksQuery = useQuery({ queryKey: ["candidate-links"], queryFn: () => listCandidateLinks() });

  const processReadyMutation = useMutation({
    mutationFn: processReadyResultDeliveries,
    onSuccess: () => refreshOperationalQueries(queryClient),
  });
  const reprocessMutation = useMutation({
    mutationFn: reprocessResultDelivery,
    onSuccess: () => refreshOperationalQueries(queryClient),
  });

  const simulations = useMemo(
    () => (simulationsQuery.data ?? []).filter((simulation) => simulation.status === "published"),
    [simulationsQuery.data],
  );
  const deliveries = deliveriesQuery.data ?? [];
  const notifications = notificationsQuery.data ?? [];
  const liveAttempts = liveAttemptsQuery.data ?? [];
  const candidateLinks = candidateLinksQuery.data ?? [];
  const failedDeliveries = deliveries.filter((delivery) => delivery.status === "dlq").length;
  const unreadNotifications = notifications.filter((notification) => !notification.readAt).length;
  const totals = buildTotals(candidateLinks);
  const filteredAttempts = liveAttempts.filter((attempt) => {
    const openAttempt = attempt.status === "inProgress";
    if (attemptFilter === "ativos" && !attempt.active) return false;
    if (attemptFilter === "finalizadas" && attempt.status !== "completed") return false;
    if (attemptFilter === "sem-sinal" && (!openAttempt || attempt.active)) return false;
    if (simulationFilter !== "todos" && attempt.simulationId !== simulationFilter) return false;
    return true;
  });
  const filteredDeliveries = deliveries.filter((delivery) =>
    deliveryFilter === "todos" ? true : delivery.status === deliveryFilter,
  );

  function refreshAll() {
    void simulationsQuery.refetch();
    void deliveriesQuery.refetch();
    void notificationsQuery.refetch();
    void liveAttemptsQuery.refetch();
    void candidateLinksQuery.refetch();
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-normal text-foreground">Monitoramento</div>
            <h1 className="mt-1 font-display text-3xl leading-tight text-foreground">Central operacional</h1>
            <p className="mt-3 text-sm leading-6 text-muted-foreground">
              Convites, tentativas em andamento, resultados, notificações e falhas operacionais que
              a automação não conseguiu resolver sozinha.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button type="button" variant="outline" size="sm" className="h-8 min-h-8 gap-1.5 bg-card text-xs" onClick={() => setRevealed((current) => !current)}>
              <Eye className="h-3.5 w-3.5" />
              {revealed ? "Ocultar nomes" : "Revelar nomes"}
            </Button>
            <Button type="button" variant="outline" size="sm" className="h-8 min-h-8 gap-1.5 bg-card text-xs" onClick={refreshAll}>
              <RefreshCw className="h-3.5 w-3.5" />
              Atualizar
            </Button>
          </div>
        </section>

        <section className="grid grid-cols-2 gap-3 md:grid-cols-5">
          <MetricCard icon={<Send className="h-4 w-4" />} label="Convites enviados" value={totals.invites} detail="Total histórico" tone="ok" />
          <MetricCard icon={<Activity className="h-4 w-4" />} label="Tentativas iniciadas" value={totals.started} detail={`${totals.adherence}% de adesão`} tone="ok" />
          <MetricCard icon={<CheckCircle2 className="h-4 w-4" />} label="Conclusões" value={totals.completed} detail={`${totals.completion}% de finalização`} tone="ok" />
          <MetricCard icon={<Bell className="h-4 w-4" />} label="Notificações" value={unreadNotifications} detail="Não lidas" tone={unreadNotifications > 0 ? "danger" : "ok"} />
          <MetricCard icon={<XCircle className="h-4 w-4" />} label="Falhas de entrega" value={failedDeliveries} detail="DLQ pendente" tone={failedDeliveries > 0 ? "danger" : "ok"} />
        </section>

        <OperationalChecklist publishedSimulations={simulations.length} generatedLinks={totals.invites} completedAttempts={totals.completed} failedDeliveries={failedDeliveries} />

        <div className="grid gap-5 xl:grid-cols-[1fr_0.9fr]">
          <NotificationsPanel notifications={notifications} loading={notificationsQuery.isLoading} error={notificationsQuery.isError} />
          <DeliveryQueuePanel deliveries={filteredDeliveries} allDeliveries={deliveries} filter={deliveryFilter} onFilterChange={setDeliveryFilter} loading={deliveriesQuery.isLoading} error={deliveriesQuery.isError} processingReady={processReadyMutation.isPending} reprocessingId={reprocessMutation.variables ?? null} onProcessReady={() => processReadyMutation.mutate()} onReprocess={(deliveryId) => reprocessMutation.mutate(deliveryId)} />
        </div>

        <div className="space-y-5">
          <section className="overflow-hidden rounded-xl border border-border bg-card">
            <div className="flex flex-col gap-3 border-b border-border px-4 py-4 md:flex-row md:items-center md:justify-between">
              <div>
                <div className="flex items-center gap-2 text-sm font-semibold"><span className="h-2 w-2 rounded-full bg-success" />Tentativas monitoradas</div>
                <p className="mt-1 text-xs text-muted-foreground">{filteredAttempts.length} de {liveAttempts.length} participações acompanhadas</p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <select value={simulationFilter} onChange={(event) => setSimulationFilter(event.target.value)} className="h-8 min-h-8 rounded-md border border-border bg-background px-3 text-xs">
                  <option value="todos">Todas as avaliações</option>
                  {simulations.map((simulation) => <option key={simulation.id} value={simulation.id}>{simulation.name}</option>)}
                </select>
                <AttemptToggle value={attemptFilter} onChange={setAttemptFilter} />
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead className="border-b border-border bg-muted/50 text-[11px] uppercase text-muted-foreground"><tr><th className="px-4 py-3 font-medium">Participante</th><th className="px-4 py-3 font-medium">Avaliação</th><th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 font-medium">Etapa</th><th className="px-4 py-3 font-medium">Tempo</th><th className="px-4 py-3 font-medium">Último sinal</th><th className="px-4 py-3 text-right font-medium">Ação</th></tr></thead>
                <tbody>
                  {filteredAttempts.map((attempt) => <tr key={attempt.attemptId} className="border-b border-border last:border-0"><td className="px-4 py-3 font-medium">{revealed ? attempt.candidateName : maskName(attempt.candidateName)}</td><td className="px-4 py-3 text-muted-foreground">{attempt.simulationName} - v{attempt.versionNumber}</td><td className="px-4 py-3"><StatusBadge status={attempt.status} active={attempt.active} /></td><td className="px-4 py-3 text-muted-foreground">{attempt.currentTurn}/{attempt.estimatedTurns}</td><td className="px-4 py-3 font-mono text-xs text-muted-foreground">{formatElapsed(attempt.elapsedSeconds)}</td><td className="px-4 py-3 text-muted-foreground">{formatRelativeTime(attempt.lastSignalAt)}</td><td className="px-4 py-3 text-right"><AttemptAction attempt={attempt} /></td></tr>)}
                  {filteredAttempts.length === 0 && <tr><td colSpan={7} className="px-4 py-8 text-center text-sm text-muted-foreground">Nenhuma tentativa encontrada para os filtros atuais.</td></tr>}
                </tbody>
              </table>
            </div>
          </section>

          <section className="overflow-hidden rounded-xl border border-border bg-card">
            <div className="border-b border-border px-4 py-4"><h2 className="text-sm font-semibold">Avaliações no ar</h2><p className="mt-1 text-xs text-muted-foreground">Versões publicadas com dados de tentativas</p></div>
            <div>{simulations.slice(0, 4).map((simulation) => <ProductionSimulation key={simulation.id} simulation={simulation} />)}{simulations.length === 0 && <div className="px-4 py-8 text-sm text-muted-foreground">Nenhuma avaliação publicada encontrada.</div>}</div>
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function AttemptAction({ attempt }: { attempt: CandidateAttemptMonitoringResponse }) {
  if (attempt.status === "completed") {
    return <Button asChild variant="outline" size="sm" className="h-8 min-h-8 bg-background text-xs"><a href={`/results/${encodeURIComponent(attempt.attemptId)}`}>Ver resultado</a></Button>;
  }
  const search = new URLSearchParams({ simulationId: attempt.simulationId, versionNumber: String(attempt.versionNumber) });
  return <Button asChild variant="outline" size="sm" className="h-8 min-h-8 bg-background text-xs"><a href={`/compliance?${search.toString()}`}>Acompanhar</a></Button>;
}

function refreshOperationalQueries(queryClient: ReturnType<typeof useQueryClient>) {
  void queryClient.invalidateQueries({ queryKey: ["result-deliveries"] });
  void queryClient.invalidateQueries({ queryKey: ["notifications"] });
  void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
}

function OperationalChecklist({ publishedSimulations, generatedLinks, completedAttempts, failedDeliveries }: { publishedSimulations: number; generatedLinks: number; completedAttempts: number; failedDeliveries: number }) {
  const items = [
    { label: "Criar e publicar avaliação", done: publishedSimulations > 0, detail: `${publishedSimulations} avaliação(ões) publicada(s)`, href: "/nova/avaliacao", action: publishedSimulations > 0 ? "Criar outra" : "Criar agora" },
    { label: "Gerar link para candidato", done: generatedLinks > 0, detail: `${generatedLinks} convite(s) gerado(s)`, href: "/enviar-link", action: "Gerar link" },
    { label: "Candidato conclui e resultado aparece", done: completedAttempts > 0, detail: `${completedAttempts} conclusão(ões) com resultado`, href: "/results", action: "Ver resultados" },
    { label: "Fila de entrega sem DLQ pendente", done: failedDeliveries === 0, detail: failedDeliveries === 0 ? "Sem falhas definitivas" : `${failedDeliveries} falha(s) para tratar`, href: "/monitoramento", action: "Tratar fila" },
  ];
  return <section className="rounded-xl border border-border bg-card p-4"><div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between"><div><h2 className="text-sm font-semibold">Happy path operacional</h2><p className="mt-1 text-xs text-muted-foreground">Caminho mínimo para garantir que empresa cria, publica, candidato responde e o resultado aparece.</p></div><a href="/comecar" className="text-xs font-medium text-primary hover:underline">Ver guia completo -&gt;</a></div><div className="mt-4 grid gap-3 md:grid-cols-4">{items.map((item) => <div key={item.label} className="rounded-lg border border-border bg-background p-3"><div className="flex items-start gap-2">{item.done ? <CheckCircle2 className="mt-0.5 h-4 w-4 text-success" /> : <Clock3 className="mt-0.5 h-4 w-4 text-warning" />}<div className="min-w-0"><div className="text-sm font-medium">{item.label}</div><div className="mt-1 text-xs text-muted-foreground">{item.detail}</div><a href={item.href} className="mt-2 inline-block text-xs font-medium text-primary hover:underline">{item.action}</a></div></div></div>)}</div></section>;
}

function NotificationsPanel({ notifications, loading, error }: { notifications: InAppNotificationResponse[]; loading: boolean; error: boolean }) {
  const recentNotifications = notifications.slice(0, 6);
  return <section className="overflow-hidden rounded-xl border border-border bg-card"><div className="flex items-center justify-between border-b border-border px-4 py-4"><div><h2 className="flex items-center gap-2 text-sm font-semibold"><Bell className="h-4 w-4 text-primary" />Notificações operacionais</h2><p className="mt-1 text-xs text-muted-foreground">Alertas internos gerados pela operação, incluindo falhas definitivas de entrega.</p></div></div>{loading ? <div className="px-4 py-8 text-sm text-muted-foreground">Carregando notificações...</div> : error ? <div className="px-4 py-8 text-sm text-danger">Não foi possível carregar notificações.</div> : recentNotifications.length === 0 ? <div className="px-4 py-8 text-sm text-muted-foreground">Nenhuma notificação operacional.</div> : <div className="divide-y divide-border">{recentNotifications.map((notification) => <article key={notification.id} className="px-4 py-3"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><div className="flex items-center gap-2 text-sm font-medium">{!notification.readAt && <span className="h-2 w-2 rounded-full bg-primary" />}{notification.title}</div><p className="mt-1 line-clamp-2 text-xs leading-5 text-muted-foreground">{notification.message}</p>{notification.candidateName && <p className="mt-1 text-xs text-muted-foreground">Candidato: {notification.candidateName}</p>}</div><time className="shrink-0 text-[11px] text-muted-foreground">{formatRelativeTime(notification.createdAt)}</time></div>{notification.candidateAttemptId && <a href={`/results/${encodeURIComponent(notification.candidateAttemptId)}`} className="mt-2 inline-flex text-xs font-medium text-primary hover:underline">Ver resultado -&gt;</a>}</article>)}</div>}</section>;
}

function DeliveryQueuePanel({ deliveries, allDeliveries, filter, onFilterChange, loading, error, processingReady, reprocessingId, onProcessReady, onReprocess }: { deliveries: ResultDeliveryResponse[]; allDeliveries: ResultDeliveryResponse[]; filter: DeliveryFilter; onFilterChange: (filter: DeliveryFilter) => void; loading: boolean; error: boolean; processingReady: boolean; reprocessingId: number | null; onProcessReady: () => void; onReprocess: (deliveryId: number) => void }) {
  const readyCount = allDeliveries.filter((delivery) => isReadyForRetry(delivery)).length;
  const dlqCount = allDeliveries.filter((delivery) => delivery.status === "dlq").length;
  const filters: Array<[DeliveryFilter, string]> = [["dlq", `DLQ (${dlqCount})`], ["retrying", "Retentativa"], ["pending", "Pendente"], ["sent", "Enviada"], ["todos", "Todas"]];
  return <section className="overflow-hidden rounded-xl border border-border bg-card"><div className="flex flex-col gap-3 border-b border-border px-4 py-4 md:flex-row md:items-center md:justify-between"><div><h2 className="flex items-center gap-2 text-sm font-semibold"><AlertTriangle className={cn("h-4 w-4", dlqCount > 0 ? "text-danger" : "text-primary")} />Entregas e DLQ</h2><p className="mt-1 text-xs text-muted-foreground">Reprocessamento manual de resultados que falharam ou já estão prontos para nova tentativa.</p></div><Button type="button" variant="outline" size="sm" className="h-8 min-h-8 gap-1.5 bg-background text-xs" disabled={processingReady || readyCount === 0} onClick={onProcessReady}><PlayCircle className="h-3.5 w-3.5" />{processingReady ? "Processando..." : `Processar prontas (${readyCount})`}</Button></div><div className="border-b border-border px-4 py-3"><div className="flex flex-wrap gap-2">{filters.map(([id, label]) => <button key={id} type="button" onClick={() => onFilterChange(id)} className={cn("rounded-md border px-2.5 py-1 text-xs transition", filter === id ? "border-primary bg-primary text-primary-foreground" : "border-border bg-background text-muted-foreground hover:bg-accent")}>{label}</button>)}</div></div>{loading ? <div className="px-4 py-8 text-sm text-muted-foreground">Carregando entregas...</div> : error ? <div className="px-4 py-8 text-sm text-danger">Não foi possível carregar entregas.</div> : deliveries.length === 0 ? <div className="px-4 py-8 text-sm text-muted-foreground">Nenhuma entrega para este filtro.</div> : <div className="max-h-[420px] overflow-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="sticky top-0 border-b border-border bg-muted/80 text-[11px] uppercase text-muted-foreground backdrop-blur"><tr><th className="px-4 py-3 font-medium">Entrega</th><th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 font-medium">Tentativas</th><th className="px-4 py-3 font-medium">Último erro</th><th className="px-4 py-3 text-right font-medium">Ação</th></tr></thead><tbody>{deliveries.map((delivery) => <tr key={delivery.id} className="border-b border-border last:border-0"><td className="px-4 py-3"><div className="font-medium">#{delivery.id}</div><div className="mt-0.5 text-xs text-muted-foreground">Attempt: {delivery.attemptId}</div><div className="mt-0.5 text-xs text-muted-foreground">Result: {delivery.resultId}</div></td><td className="px-4 py-3"><DeliveryStatusBadge status={delivery.status} />{delivery.nextAttemptAt && <div className="mt-1 text-[11px] text-muted-foreground">Próxima: {formatDateTime(delivery.nextAttemptAt)}</div>}</td><td className="px-4 py-3 tabular-nums text-muted-foreground">{delivery.attemptCount}</td><td className="max-w-[260px] px-4 py-3 text-xs text-muted-foreground"><span className="line-clamp-2">{delivery.lastError ?? "-"}</span></td><td className="px-4 py-3 text-right"><div className="flex justify-end gap-2"><Button asChild variant="outline" size="sm" className="h-8 min-h-8 bg-background text-xs"><a href={`/results/${encodeURIComponent(delivery.attemptId)}`}>Resultado</a></Button><Button type="button" variant="outline" size="sm" className="h-8 min-h-8 gap-1.5 bg-background text-xs" disabled={reprocessingId === delivery.id} onClick={() => onReprocess(delivery.id)}><RotateCcw className="h-3.5 w-3.5" />{reprocessingId === delivery.id ? "Reenviando..." : "Reprocessar"}</Button></div></td></tr>)}</tbody></table></div>}</section>;
}

function AttemptToggle({ value, onChange }: { value: AttemptFilter; onChange: (value: AttemptFilter) => void }) { const items: Array<[AttemptFilter, string]> = [["todos", "Todos"], ["ativos", "Ativos"], ["finalizadas", "Finalizadas"], ["sem-sinal", "Sem sinal"]]; return <div className="inline-flex h-8 min-h-8 rounded-md border border-border bg-card p-0.5 text-xs">{items.map(([id, label]) => <button key={id} type="button" onClick={() => onChange(id)} className={cn("min-h-0 rounded px-2 py-0.5 transition", value === id ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-accent")}>{label}</button>)}</div>; }
function StatusBadge({ status, active }: { status: AttemptStatus; active: boolean }) { const label = attemptStatusLabel(status, active); const tone = status === "completed" ? "border-success/30 bg-success/10 text-success" : active ? "border-primary/30 bg-primary/10 text-primary" : "border-warning/30 bg-warning/10 text-warning"; return <span className={cn("inline-flex rounded-full border px-2 py-0.5 text-[11px]", tone)}>{label}</span>; }
function DeliveryStatusBadge({ status }: { status: ResultDeliveryStatus }) { const config: Record<ResultDeliveryStatus, { label: string; className: string }> = { pending: { label: "Pendente", className: "border-warning/30 bg-warning/10 text-warning" }, retrying: { label: "Retentativa", className: "border-primary/30 bg-primary/10 text-primary" }, sent: { label: "Enviada", className: "border-success/30 bg-success/10 text-success" }, dlq: { label: "DLQ", className: "border-danger/30 bg-danger/10 text-danger" } }; return <span className={cn("inline-flex rounded-full border px-2 py-0.5 text-[11px]", config[status].className)}>{config[status].label}</span>; }
function attemptStatusLabel(status: AttemptStatus, active: boolean) { if (status === "completed") return "Finalizada"; if (status === "inProgress" && active) return "Ativa"; if (status === "inProgress") return "Sem sinal"; if (status === "abandoned") return "Abandonada"; if (status === "expired") return "Expirada"; return "Não iniciada"; }
function MetricCard({ icon, label, value, detail, tone }: { icon: ReactNode; label: string; value: number; detail?: string; tone: "ok" | "danger" }) { return <div className="rounded-xl border border-border bg-card p-4"><div className="flex items-center gap-2 text-[11px] font-medium uppercase text-muted-foreground">{icon}{label}</div><div className="mt-4 flex items-end gap-2"><div className="font-display text-3xl leading-none tabular-nums">{value}</div></div>{detail && <div className={cn("mt-2 text-xs", tone === "danger" ? "text-danger" : "text-muted-foreground")}>{detail}</div>}</div>; }
function ProductionSimulation({ simulation }: { simulation: SimulationSummaryResponse }) { const completion = Math.round(simulation.completionRatePercent || 0); const volume = simulation.attemptsCreated || 0; const pending = Math.max(0, 100 - completion); const complianceSearch = new URLSearchParams({ simulationId: simulation.id, versionNumber: String(simulation.versionNumber) }); return <article className="border-b border-border px-4 py-4 last:border-0"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><h3 className="truncate text-sm font-semibold">{simulation.name}</h3><div className="mt-1 text-[11px] text-muted-foreground">v{simulation.versionNumber}</div></div><div className="flex shrink-0 gap-3 text-[11px]"><a href="/enviar-link" className="text-primary hover:underline">Enviar link</a><a href={`/compliance?${complianceSearch.toString()}`} className="text-primary hover:underline">Compliance -&gt;</a></div></div><div className="mt-3 grid grid-cols-3 gap-3 text-[11px] uppercase text-muted-foreground"><div>Volume<strong className="mt-1 block text-sm text-foreground">{volume}</strong></div><div>Conclusão<strong className="mt-1 block text-sm text-foreground">{completion}%</strong></div><div>Concluídas<strong className="mt-1 block text-sm text-foreground">{simulation.attemptsCompleted}</strong></div></div><div className="mt-3 flex h-1.5 overflow-hidden rounded-full bg-muted"><span className="bg-success" style={{ width: `${completion}%` }} /><span className="bg-muted-foreground/20" style={{ width: `${pending}%` }} /></div><div className="mt-2 grid grid-cols-2 text-[10px] text-muted-foreground"><span>Finalização {completion}%</span><span className="text-right">Em aberto {pending}%</span></div></article>; }
function buildTotals(candidateLinks: CandidateLinkResponse[]) { const invites = candidateLinks.length; const started = candidateLinks.filter((link) => link.status !== "notStarted").length; const completed = candidateLinks.filter((link) => link.status === "completed").length; return { invites, started, completed, adherence: Math.round((started / Math.max(invites, 1)) * 100), completion: Math.round((completed / Math.max(started, 1)) * 100) }; }
function maskName(name: string) { const [first = "", second = ""] = name.split(" "); return `${first.charAt(0)}${"*".repeat(Math.max(4, first.length - 1))} ${second.charAt(0)}.`; }
function formatElapsed(totalSeconds: number) { const safeSeconds = Math.max(0, totalSeconds); const minutes = Math.floor(safeSeconds / 60); const seconds = safeSeconds % 60; const hours = Math.floor(minutes / 60); const remainingMinutes = minutes % 60; if (hours > 0) return `${String(hours).padStart(2, "0")}:${String(remainingMinutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`; return `${String(remainingMinutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`; }
function formatRelativeTime(value: string) { const timestamp = new Date(value).getTime(); if (Number.isNaN(timestamp)) return "-"; const seconds = Math.max(0, Math.round((Date.now() - timestamp) / 1000)); if (seconds < 60) return `há ${seconds}s`; const minutes = Math.round(seconds / 60); if (minutes < 60) return `há ${minutes} min`; const hours = Math.round(minutes / 60); return `há ${hours} h`; }
function formatDateTime(value: string) { const date = new Date(value); if (Number.isNaN(date.getTime())) return "-"; return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" }).format(date); }
function isReadyForRetry(delivery: ResultDeliveryResponse) { if (delivery.status !== "pending" && delivery.status !== "retrying") return false; if (!delivery.nextAttemptAt) return true; return new Date(delivery.nextAttemptAt).getTime() <= Date.now(); }
