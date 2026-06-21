import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { ArchiveRestore, Lock, RefreshCw } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { ComplianceScope } from "@/components/compliance-scope";
import { Termo } from "@/components/glossario";
import { ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useLanguage } from "@/lib/language-context";
import {
  cloneSimulationVersionToDraft,
  listSimulations,
  listSimulationVersionAuditEvents,
  type AuditEventResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";

export const Route = createFileRoute("/governanca")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
        ? Number(search.versionNumber)
        : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Governança & Auditoria - Praxis" },
      { name: "description", content: "Controles operacionais, auditoria e versionamento." },
    ],
  }),
  component: GovernanceHub,
});

function GovernanceHub() {
  const { t } = useLanguage();
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const [versionDialogOpen, setVersionDialogOpen] = useState(false);
  const [simulationSearch, setSimulationSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"all" | "draft" | "published" | "archived">(
    "all",
  );
  const hasGovernanceParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasGovernanceParams,
  });
  const auditQuery = useQuery({
    queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
    queryFn: () => listSimulationVersionAuditEvents(search.simulationId!, search.versionNumber!),
    enabled: hasGovernanceParams,
  });
  const cloneMutation = useMutation({
    mutationFn: () => cloneSimulationVersionToDraft(search.simulationId!, search.versionNumber!),
    onSuccess: async () => {
      setVersionDialogOpen(false);
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version-audit", search.simulationId, search.versionNumber],
      });
    },
  });

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="papel atual não tem permissão para esta transição" />
      <div className="mb-5">
        <div className="text-xs uppercase text-primary">{t.common.compliance}</div>
        <h1 className="mt-1 text-3xl font-semibold">{t.governance.heading}</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Consulte o histórico das versões, abra o registro de auditoria e crie um novo rascunho
          quando uma versão no ar precisar mudar. Aprovação e publicação continuam no fluxo guiado
          de governança.
        </p>
      </div>
      <ComplianceScope current="governanca" />

      {hasGovernanceParams && auditQuery.isLoading && (
        <StateBanner tone="info" title={t.governance.auditRecordConnected}>
          Buscando eventos do teste {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      )}

      {hasGovernanceParams && auditQuery.isError && (
        <StateBanner tone="danger" title={t.governance.couldNotLoadAudit}>
          {auditQuery.error instanceof Error
            ? auditQuery.error.message
            : "Verifique sua conexão e tente novamente. Confira também se a versão existe."}
        </StateBanner>
      )}

      {cloneMutation.isSuccess && (
        <StateBanner tone="ok" title={t.governance.newVersionCreated}>
          Rascunho v{cloneMutation.data.newVersionNumber} criado a partir da v
          {cloneMutation.data.sourceVersionNumber}.
        </StateBanner>
      )}

      {cloneMutation.isError && (
        <StateBanner tone="danger" title="Não foi possível criar a versão">
          {cloneMutation.error instanceof Error
            ? cloneMutation.error.message
            : "A mudança não foi permitida pelo sistema."}
        </StateBanner>
      )}

      <div className="mt-5 space-y-5">
        <section className="rounded-md border border-border bg-card p-5">
          <h2 className="text-sm font-semibold">
            {hasGovernanceParams ? "Registro de auditoria imutável" : "Versões disponíveis"}
          </h2>
          {!hasGovernanceParams && (
            <p className="mt-1 text-xs text-muted-foreground">
              Selecione uma versão para ver a trilha de decisões ou criar um rascunho derivado.
            </p>
          )}
          {hasGovernanceParams ? (
            <AuditEventList events={auditQuery.data ?? []} loading={auditQuery.isLoading} />
          ) : (
            <>
              <SimulationLinks
                loading={simulationsQuery.isLoading}
                simulations={simulationsQuery.data ?? []}
                simulationSearch={simulationSearch}
                statusFilter={statusFilter}
              />
              <div className="mt-4 grid gap-3 rounded-md border border-border bg-card p-3 md:grid-cols-[1fr_220px]">
                <input
                  type="search"
                  autoComplete="off"
                  name="governance-version-search"
                  value={simulationSearch}
                  onChange={(event) => setSimulationSearch(event.target.value)}
                  placeholder="Buscar teste"
                  className="input"
                />
                <select
                  value={statusFilter}
                  onChange={(event) =>
                    setStatusFilter(
                      event.target.value as "all" | "draft" | "published" | "archived",
                    )
                  }
                  className="rounded-md border border-border bg-background px-3 py-2 text-sm"
                >
                  <option value="all">Todos os status</option>
                  <option value="draft">Rascunho</option>
                  <option value="published">No ar</option>
                  <option value="archived">Arquivada</option>
                </select>
              </div>
            </>
          )}
        </section>
        <aside className="space-y-3">
          <StateBanner tone="warn" title="Editar teste no ar cria nova versão">
            Candidatos em andamento continuam na versão atual.
          </StateBanner>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <button
              type="button"
              onClick={() => setVersionDialogOpen(true)}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60"
            >
              <RefreshCw className="h-4 w-4 shrink-0" />
              <span className="min-w-0 truncate">
                {hasGovernanceParams
                  ? `Criar nova versão a partir da v${search.versionNumber}`
                  : "Selecione uma versão"}
              </span>
            </button>
            <button
              disabled
              className="inline-flex min-h-10 cursor-not-allowed items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm opacity-60"
            >
              <ArchiveRestore className="h-4 w-4 shrink-0" />
              Restaurar arquivada
            </button>
            <Link
              to="/nova/governanca"
              search={{
                simulationId: search.simulationId,
                versionNumber: search.versionNumber,
              }}
              className="inline-flex min-h-10 items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
            >
              Abrir aprovação/publicação
            </Link>
            <Link
              to="/"
              className="inline-flex min-h-10 items-center justify-center rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar ao painel
            </Link>
          </div>
          <div className="flex items-start gap-2 rounded-md border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
            <Lock className="mt-0.5 h-3.5 w-3.5" />
            <span>
              Esta tela é consulta operacional. Publicar, aprovar ou reprovar uma versão é feito no
              fluxo guiado, onde <Termo id="blocker">bloqueios</Termo> são validados antes da
              mudança de estado.
            </span>
          </div>
        </aside>
      </div>
      {versionDialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/30 p-4">
          <div className="w-full max-w-md rounded-md border border-border bg-card p-5 shadow-xl">
            <div className="text-sm font-semibold">Criar nova versão?</div>
            <p className="mt-2 text-sm text-muted-foreground">
              {hasGovernanceParams
                ? `Isto cria um rascunho a partir da versão ${search.versionNumber}. Candidatos em andamento continuam na versão atual.`
                : "Escolha um teste real antes de criar nova versão."}
            </p>
            <div className="mt-4 rounded-md border border-border bg-muted/45 p-3 text-xs text-muted-foreground">
              Um erro de digitação conta como mudança menor. Mudanças em pontuação, peso ou no fluxo
              da conversa contam como mudança maior.
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setVersionDialogOpen(false)}
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  if (hasGovernanceParams) {
                    cloneMutation.mutate();
                  } else {
                    setVersionDialogOpen(false);
                  }
                }}
                disabled={cloneMutation.isPending}
                className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-60"
              >
                {cloneMutation.isPending ? "Criando..." : "Confirmar nova versão"}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}

function AuditEventList({ events, loading }: { events: AuditEventResponse[]; loading: boolean }) {
  if (loading) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm">
        Carregando eventos...
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhum evento de auditoria registrado para esta versão.
      </div>
    );
  }

  return (
    <div className="mt-4 rounded-md border border-border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-16">#</TableHead>
            <TableHead>Evento</TableHead>
            <TableHead>Origem</TableHead>
            <TableHead className="text-right">Data</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {events.map((event, index) => (
            <TableRow key={event.id}>
              <TableCell className="text-xs tabular-nums text-muted-foreground">
                {index + 1}
              </TableCell>
              <TableCell className="min-w-[240px] font-medium">{event.message}</TableCell>
              <TableCell className="min-w-[220px] text-xs text-muted-foreground">
                {formatEventType(event.eventType)} - {event.aggregateId}
              </TableCell>
              <TableCell className="text-right text-xs tabular-nums text-muted-foreground">
                {formatDateTime(event.createdAt)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function SimulationLinks({
  simulations,
  loading,
  simulationSearch,
  statusFilter,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
  simulationSearch: string;
  statusFilter: "all" | "draft" | "published" | "archived";
}) {
  const normalizedSearch = simulationSearch.trim().toLowerCase();
  const filteredSimulations = useMemo(() => {
    return simulations.filter((simulation) => {
      if (statusFilter !== "all" && simulation.status !== statusFilter) return false;
      return !normalizedSearch || simulation.name.toLowerCase().includes(normalizedSearch);
    });
  }, [normalizedSearch, simulations, statusFilter]);

  if (loading) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm">
        Carregando testes...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhum teste ativo encontrado.
      </div>
    );
  }

  if (filteredSimulations.length === 0) {
    return (
      <div className="mt-4 rounded-md border border-border bg-background p-4 text-sm text-muted-foreground">
        Nenhum teste encontrado com os filtros atuais.
      </div>
    );
  }

  return (
    <div className="mt-4 rounded-md border border-border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Teste</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Versão</TableHead>
            <TableHead className="text-right">Tentativas</TableHead>
            <TableHead className="text-right">Ação</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {filteredSimulations.map((simulation) => {
            return (
              <TableRow key={simulation.id}>
                <TableCell className="min-w-[220px] font-medium">{simulation.name}</TableCell>
                <TableCell>
                  <StatusBadge
                    status={simulation.status}
                    maturity={maturityForStatus(simulation.status)}
                  />
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  v{simulation.versionNumber}
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  {simulation.attemptsCreated.toLocaleString("pt-BR")}
                </TableCell>
                <TableCell className="text-right">
                  <Link
                    to="/governanca"
                    search={{
                      simulationId: simulation.id,
                      versionNumber: simulation.versionNumber,
                    }}
                    className="inline-flex items-center justify-center rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                  >
                    Ver auditoria
                  </Link>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

function formatEventType(value: string) {
  return value.replace(/([A-Z])/g, " $1").toLowerCase();
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
