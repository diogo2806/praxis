import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useCallback, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Download,
  ExternalLink,
  Flag,
  GitBranch,
  History,
  MousePointerClick,
  RefreshCw,
  Workflow,
  XCircle,
  ZoomIn,
  ZoomOut,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  NextStepContract,
  ScreenStateStrip,
  StateBanner,
  StatusBadge,
} from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  cloneSimulationVersionToDraft,
  getSimulationVersion,
  getSimulationValidation,
  listSimulations,
  type SimulationSummaryResponse,
  type SimulationVersionDetailResponse,
  type SimulationVersionNodeResponse,
  type SimulationVersionOptionResponse,
  type SimulationValidationResponse,
} from "@/lib/api/praxis";
import { canEditSimulationVersion, maturityForStatus, statusMeta } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/validador")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string" && Number.isFinite(Number(search.versionNumber))
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Validador de Qualidade - Praxis" },
      {
        name: "description",
        content: "Diagnóstico determinístico com bloqueios de publicação.",
      },
    ],
  }),
  component: ValidatorPage,
});

type CheckTone = "ok" | "warn" | "danger";

interface ValidationCheck {
  id: string;
  nodeId: string | null;
  tone: CheckTone;
  text: string;
  target: string;
}

interface DiagnosticGroup {
  id: string;
  nodeIds: string[];
  targets: string[];
  text: string;
  tone: CheckTone;
}

function ValidatorPage() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const selectNode = useCallback((nodeId: string | null) => {
    if (typeof window === "undefined") {
      setSelectedNodeId(nodeId);
      return;
    }

    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    setSelectedNodeId(nodeId);
    window.requestAnimationFrame(() => {
      window.scrollTo(scrollX, scrollY);
    });
  }, []);
  const hasValidationParams = Boolean(search.simulationId && search.versionNumber);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasValidationParams,
  });
  const validationQuery = useQuery({
    queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationValidation(search.simulationId!, search.versionNumber!),
    enabled: hasValidationParams,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasValidationParams,
  });

  const validation = validationQuery.data;
  const versionStatus = versionQuery.data?.status;
  const isEditable = versionStatus ? canEditSimulationVersion(versionStatus) : true;
  const canCloneForEdit = versionStatus === "published";
  const activeChecks = validation ? mapValidationIssues(validation) : [];
  const filteredChecks = selectedNodeId
    ? activeChecks.filter((check) => check.nodeId === selectedNodeId)
    : activeChecks;
  const diagnosticGroups = groupValidationChecks(filteredChecks);
  const firstFixableBlocker = activeChecks.find((check) => check.tone === "danger" && check.nodeId);
  const blockers = validation?.blockerCount ?? 0;
  const warnings = validation?.warningCount ?? 0;
  const qualityScore = validation?.qualityScore ?? 0;
  const canPublish = Boolean(validation) && blockers === 0;
  const scoreTone = scoreQualityTone(qualityScore);
  const scoreWidth = Math.max(0, Math.min(qualityScore, 100));
  const refreshValidation = () => {
    void validationQuery.refetch();
    void versionQuery.refetch();
  };
  const cloneDraftMutation = useMutation({
    mutationFn: (nodeId?: string) =>
      cloneSimulationVersionToDraft(search.simulationId!, search.versionNumber!).then((draft) => ({
        draft,
        nodeId,
      })),
    onSuccess: async ({ draft, nodeId }) => {
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/dialogo",
        search: {
          simulationId: draft.simulationId,
          versionNumber: draft.newVersionNumber,
          nodeId,
        },
      });
    },
  });
  const openEditor = (nodeId?: string) => {
    if (!search.simulationId || !search.versionNumber) return;

    if (isEditable) {
      void navigate({
        to: "/nova/dialogo",
        search: {
          simulationId: search.simulationId,
          versionNumber: search.versionNumber,
          nodeId,
        },
      });
      return;
    }

    if (canCloneForEdit) {
      cloneDraftMutation.mutate(nodeId);
    }
  };
  const exportDiagnostics = () => {
    if (!validation) return;

    const payload = JSON.stringify(
      {
        exportedAt: new Date().toISOString(),
        selectedNodeId,
        validation,
        version: versionQuery.data,
      },
      null,
      2,
    );
    const blob = new Blob([payload], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `diagnostico-${validation.simulationId}-v${validation.versionNumber}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  return (
    <AppShell>
      <WizardStepper current="revisao" unlockedThrough={canPublish ? "publicacao" : "revisao"} />
      <ScreenStateStrip blockedReason="qualquer bloqueio remove a ação de publicar" />
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3</div>
          <h1 className="mt-1 text-3xl font-semibold">Validador de Qualidade</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Regras fixas, histórico completo de alterações e bloqueios sem ajuste manual.
          </p>
        </div>
      </div>

      {hasValidationParams && (
        <ScoringModelPreview
          error={versionQuery.error}
          loading={versionQuery.isLoading}
          onSelectNode={selectNode}
          selectedNodeId={selectedNodeId}
          simulationId={search.simulationId}
          version={versionQuery.data}
          versionNumber={search.versionNumber}
        />
      )}

      {hasValidationParams && validationQuery.isLoading && (
        <StateBanner tone="info" title="Validador conectado">
          Buscando o diagnóstico da simulação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      )}

      {hasValidationParams && validationQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar a validação">
          {validationQuery.error instanceof Error
            ? validationQuery.error.message
            : "Verifique se o sistema está disponível e tente novamente."}
        </StateBanner>
      )}

      {hasValidationParams && cloneDraftMutation.isError && (
        <StateBanner tone="danger" title="Nao foi possivel criar o rascunho para edicao">
          {cloneDraftMutation.error instanceof Error
            ? cloneDraftMutation.error.message
            : "Tente novamente."}
        </StateBanner>
      )}

      {hasValidationParams && versionStatus && !isEditable && !canCloneForEdit && (
        <StateBanner
          tone="warn"
          title={`Versao ${statusMeta[versionStatus].label.toLowerCase()} nao pode ser editada`}
        >
          Para alterar esta versao, mova o fluxo para rascunho/reprovada ou publique uma versao para
          clonar como novo rascunho.
        </StateBanner>
      )}

      {!hasValidationParams ? (
        <EmptyState
          title="Selecione uma versão para validar"
          description="O validador usa apenas o diagnóstico calculado pelo sistema."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : (
        <>
          {blockers > 0 ? (
            <StateBanner
              tone="danger"
              title={`Publicação bloqueada - ${blockers} ${blockers === 1 ? "item crítico" : "itens críticos"}`}
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span>
                  O botão Publicar não aparece. Resolva o bloqueio ou salve como rascunho.
                </span>
                {firstFixableBlocker?.nodeId && (
                  <button
                    type="button"
                    onClick={() => openEditor(firstFixableBlocker.nodeId!)}
                    disabled={!isEditable && !canCloneForEdit}
                    className="rounded-md bg-danger px-3 py-1.5 text-xs font-medium text-danger-foreground hover:opacity-90"
                  >
                    {cloneDraftMutation.isPending
                      ? "Criando rascunho..."
                      : `Corrigir ${firstFixableBlocker.nodeId}`}
                  </button>
                )}
              </div>
            </StateBanner>
          ) : warnings > 0 ? (
            <StateBanner
              tone="warn"
              title={`Pode publicar - ${warnings} ${warnings === 1 ? "alerta registrado" : "alertas registrados"}`}
            >
              A confirmação registra os alertas no log de auditoria antes de publicar.
            </StateBanner>
          ) : (
            <StateBanner tone="ok" title="Pronta para publicar">
              Sem bloqueio ou alerta ativo. A publicação usa a versão protegida atual.
            </StateBanner>
          )}

          <details className="mt-5 rounded-md border border-border bg-card p-4">
            <summary className="cursor-pointer text-sm font-medium">
              Regras de publicação e versão
            </summary>
            <div className="mt-4">
              <NextStepContract
                primary={
                  blockers > 0
                    ? "Editar o diálogo. Piloto e publicação ficam travados."
                    : warnings > 0
                      ? "Confirmar publicação com alertas gravados no registro de auditoria."
                      : "Publicar versão protegida e seguir para piloto."
                }
                secondary="Salvar rascunho nunca publica; mantém o diagnóstico clicável para edição."
                versionRule="Depois de publicar, editar cria nova versão e preserva a versão no ar."
                lockedAfter="Não existe ajuste manual para bloqueio crítico."
              />
            </div>
          </details>

          <div className="mt-5 grid gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
            <section className="rounded-md border border-border bg-card p-5">
              <div className="text-xs uppercase text-muted-foreground">Pontuação de qualidade</div>
              <div className="mt-2 flex items-end gap-2">
                <div className={cn("text-6xl font-semibold tabular-nums", scoreTone.textClass)}>
                  {qualityScore}
                </div>
                <div className="mb-2 text-sm text-muted-foreground">/100</div>
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                Calculado pelo sistema junto com os bloqueios e alertas.
              </p>
              <div className="mt-4 h-2 overflow-hidden rounded-full bg-muted">
                <div
                  className={cn("h-full rounded-full", scoreTone.barClass)}
                  style={{ width: `${scoreWidth}%` }}
                />
              </div>
              <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-[11px] text-muted-foreground">
                <span className="text-danger">Crítico &lt; 50</span>
                <span className="text-warning-foreground">Aceitável 50-79</span>
                <span className="text-success">Bom 80+</span>
              </div>
              <div className="mt-4 rounded-md border border-border bg-background p-3 text-sm">
                {formatCount(blockers, "bloqueio", "bloqueios")} e{" "}
                {formatCount(warnings, "alerta", "alertas")}.
              </div>
            </section>

            <section className="rounded-md border border-border bg-card p-5">
              <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-sm font-semibold">Diagnóstico</h2>
                  {selectedNodeId && (
                    <div className="mt-1 text-xs text-muted-foreground">
                      Filtrado por etapa{" "}
                      <span className="font-mono text-foreground">{selectedNodeId}</span>
                    </div>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  {selectedNodeId && (
                    <button
                      type="button"
                      onClick={() => selectNode(null)}
                      className="rounded-md border border-border bg-background px-2 py-1 text-[11px] text-muted-foreground hover:bg-accent"
                    >
                      Limpar filtro
                    </button>
                  )}
                  <span className="rounded-md border border-border bg-background px-2 py-1 text-[11px] text-muted-foreground">
                    dados do sistema
                  </span>
                </div>
              </div>
              <ul className="divide-y divide-border">
                {diagnosticGroups.map((group) => (
                  <li key={group.id} className="flex items-start gap-3 py-3 text-sm">
                    <CheckIcon tone={group.tone} />
                    <div className="min-w-0 flex-1">
                      <span className="block text-foreground/90">
                        {group.nodeIds.length > 1
                          ? `${group.nodeIds.length} etapas com o mesmo problema: ${group.text}`
                          : group.text}
                      </span>
                      <div className="mt-2 flex flex-wrap items-center gap-2">
                        {group.nodeIds.length > 0 ? (
                          group.nodeIds.map((nodeId) => (
                            <button
                              key={nodeId}
                              type="button"
                              onClick={() => selectNode(nodeId)}
                              className="rounded-md border border-border bg-background px-2 py-1 font-mono text-[11px] text-primary hover:bg-accent"
                            >
                              {nodeId}
                            </button>
                          ))
                        ) : (
                          <span className="text-xs text-muted-foreground">{group.targets[0]}</span>
                        )}
                      </div>
                      <div className="mt-3 flex flex-wrap gap-2">
                        {group.nodeIds[0] && (
                          <button
                            type="button"
                            onClick={() => openEditor(group.nodeIds[0])}
                            disabled={!isEditable && !canCloneForEdit}
                            className="inline-flex items-center gap-1 rounded-md border border-border bg-card px-3 py-1.5 text-xs font-medium hover:bg-accent"
                          >
                            <ExternalLink className="h-3 w-3" />
                            {cloneDraftMutation.isPending ? "Criando..." : "Corrigir"}
                          </button>
                        )}
                        {group.tone === "warn" && (
                          <button
                            type="button"
                            className="rounded-md border border-border bg-card px-3 py-1.5 text-xs font-medium hover:bg-accent"
                          >
                            Ignorar com justificativa
                          </button>
                        )}
                      </div>
                    </div>
                  </li>
                ))}
                {diagnosticGroups.length === 0 && (
                  <li className="py-4 text-sm text-muted-foreground">
                    Nenhum item de diagnóstico para a etapa selecionada.
                  </li>
                )}
              </ul>

              <div className="mt-5 flex flex-wrap justify-between gap-3">
                <button
                  type="button"
                  onClick={() => openEditor(selectedNodeId ?? undefined)}
                  disabled={!isEditable && !canCloneForEdit}
                  className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
                >
                  {canCloneForEdit ? "Criar rascunho e editar" : "Editar diálogo"}
                </button>
                <div className="flex flex-wrap justify-end gap-2">
                  <button
                    type="button"
                    onClick={refreshValidation}
                    className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
                  >
                    <RefreshCw className="h-3.5 w-3.5" />
                    Revalidar
                  </button>
                  <button
                    type="button"
                    onClick={exportDiagnostics}
                    disabled={!validation}
                    className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Download className="h-3.5 w-3.5" />
                    Exportar
                  </button>
                  <Link
                    to="/nova/governanca"
                    search={{
                      simulationId: search.simulationId,
                      versionNumber: search.versionNumber,
                    }}
                    className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
                  >
                    <History className="h-3.5 w-3.5" />
                    Histórico
                  </Link>
                  {blockers > 0 ? (
                    <button
                      type="button"
                      onClick={() =>
                        openEditor(selectedNodeId ?? firstFixableBlocker?.nodeId ?? undefined)
                      }
                      disabled={!isEditable && !canCloneForEdit}
                      className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {canCloneForEdit ? "Criar rascunho e corrigir" : "Editar diálogo"}
                    </button>
                  ) : (
                    <Link
                      to="/nova/piloto"
                      search={{
                        simulationId: search.simulationId,
                        versionNumber: search.versionNumber,
                      }}
                      className={cn(
                        "rounded-md px-5 py-2 text-sm font-medium",
                        warnings > 0
                          ? "bg-warning text-warning-foreground hover:opacity-90"
                          : "bg-success text-success-foreground hover:opacity-90",
                      )}
                    >
                      Seguir para governança
                    </Link>
                  )}
                </div>
              </div>
            </section>
          </div>
        </>
      )}
    </AppShell>
  );
}

function ScoringModelPreview({
  error,
  loading,
  onSelectNode,
  selectedNodeId,
  simulationId,
  version,
  versionNumber,
}: {
  error: Error | null;
  loading: boolean;
  onSelectNode: (nodeId: string | null) => void;
  selectedNodeId: string | null;
  simulationId?: string;
  version?: SimulationVersionDetailResponse;
  versionNumber?: number;
}) {
  const steps = version ? buildStepScoreSummaries(version) : [];

  return (
    <section className="mb-5">
      <div className="mb-5">
        <h2 className="text-2xl font-semibold tracking-tight">Mapa e pontuação normalizada</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Dois candidatos têm o mesmo teto possível, independentemente do caminho.
        </p>
      </div>

      <div className="grid gap-5">
        <div className="rounded-md border border-border bg-card p-5">
          <NormalizedScoreMap
            onSelectNode={onSelectNode}
            selectedNodeId={selectedNodeId}
            simulationId={simulationId}
            version={version}
            versionNumber={versionNumber}
          />

          <div className="mt-6 border-t border-border pt-5">
            <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="text-xs font-semibold uppercase text-muted-foreground">
                  Fluxo de pontuação
                </div>
                <h2 className="mt-1 text-lg font-semibold">
                  {version?.name ?? simulationId ?? "Simulação"}{" "}
                  {versionNumber ? `v${versionNumber}` : ""}
                </h2>
              </div>
              <span className="rounded-md border border-border bg-background px-2 py-1 text-[11px] text-muted-foreground">
                blocos da versão
              </span>
            </div>

            {loading ? (
              <div className="rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
                Carregando etapas, tempos configurados e competências...
              </div>
            ) : error ? (
              <StateBanner tone="danger" title="Não foi possível carregar o fluxo">
                {error.message}
              </StateBanner>
            ) : steps.length > 0 ? (
              <div className="grid gap-3">
                {steps.map((step) => (
                  <article
                    key={step.node.id}
                    className={cn(
                      "flex min-h-[310px] flex-col rounded-md border bg-background p-4 text-left transition",
                      selectedNodeId === step.node.id
                        ? "border-primary ring-2 ring-primary/20"
                        : "border-border",
                    )}
                  >
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <div className="font-mono text-[11px] uppercase text-primary">
                          {step.node.id}
                        </div>
                        <div className="mt-1 text-sm font-semibold">
                          Turno {step.node.turnIndex}
                        </div>
                      </div>
                      {step.hasCriticalOption && (
                        <span className="rounded-md border border-danger/30 bg-danger/10 px-2 py-1 text-[11px] font-medium text-danger">
                          crítica
                        </span>
                      )}
                      <div className="ml-auto flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() =>
                            onSelectNode(selectedNodeId === step.node.id ? null : step.node.id)
                          }
                          className="rounded-md border border-border bg-card px-3 py-1.5 text-xs font-medium hover:bg-accent"
                        >
                          {selectedNodeId === step.node.id ? "Selecionado" : "Selecionar"}
                        </button>
                        <button
                          type="button"
                          onClick={() => onSelectNode(step.node.id)}
                          className="inline-flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                        >
                          <ZoomIn className="h-3 w-3" />
                          Detalhes
                        </button>
                      </div>
                    </div>

                    <div className="mt-3">
                      <div className="text-[11px] font-medium uppercase text-muted-foreground">
                        Texto cadastrado
                      </div>
                      <p className="mt-1 line-clamp-4 text-sm leading-6 text-foreground/85">
                        {step.node.clientMessage || "Sem texto cadastrado."}
                      </p>
                    </div>

                    <dl className="mt-4 grid grid-cols-3 gap-2 text-xs">
                      <MetricTile
                        label="Tempo"
                        value={formatTimeLimit(step.node.timeLimitSeconds)}
                      />
                      <MetricTile label="Atual" value={formatScore(step.currentScore)} />
                      <MetricTile label="Acumulada" value={formatScore(step.accumulatedScore)} />
                    </dl>

                    <div className="mt-4">
                      <div className="text-[11px] font-medium uppercase text-muted-foreground">
                        Competências
                      </div>
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {step.competencies.length > 0 ? (
                          step.competencies.map((competency) => (
                            <span
                              key={competency.name}
                              className="rounded-md border border-border bg-card px-2 py-1 text-[11px]"
                            >
                              {competency.name}:{" "}
                              <span className="font-semibold tabular-nums">
                                {formatScore(competency.value)}
                              </span>
                            </span>
                          ))
                        ) : (
                          <span className="text-xs text-muted-foreground">
                            Nenhuma competência configurada.
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="mt-4 border-t border-border pt-3">
                      <div className="text-[11px] font-medium uppercase text-muted-foreground">
                        Alternativas
                      </div>
                      <div className="mt-2 space-y-2">
                        {step.node.options.map((option) => (
                          <div
                            key={option.id}
                            className="rounded-md border border-border bg-card px-3 py-2"
                          >
                            <div className="flex flex-wrap items-center justify-between gap-2">
                              <span className="font-mono text-[11px] text-muted-foreground">
                                {option.id}
                              </span>
                              <span className="rounded bg-muted px-1.5 py-0.5 text-[11px] font-medium tabular-nums text-foreground">
                                +{formatScore(scoreOption(option, version!))} pts
                              </span>
                            </div>
                            <p className="mt-1 line-clamp-2 text-xs leading-5 text-foreground/80">
                              {option.text}
                            </p>
                            <div className="mt-1 text-[11px] text-muted-foreground">
                              Vai para {option.nextNodeId ?? "fim"}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="mt-auto pt-4 text-[11px] text-muted-foreground">
                      {step.node.options.length} opções - próximo: {step.nextTargets.join(", ")}
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <div className="rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
                Nenhuma etapa cadastrada para esta versão.
              </div>
            )}

            {version && steps.length > 0 && <FlowOutcomeSummary version={version} />}
          </div>
        </div>
      </div>
    </section>
  );
}

function MetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-md border border-border bg-card p-2">
      <dt className="truncate text-[10px] uppercase text-muted-foreground">{label}</dt>
      <dd className="mt-1 truncate text-sm font-semibold tabular-nums">{value}</dd>
    </div>
  );
}

function NormalizedScoreMap({
  onSelectNode,
  selectedNodeId,
  simulationId,
  version,
  versionNumber,
}: {
  onSelectNode: (nodeId: string | null) => void;
  selectedNodeId: string | null;
  simulationId?: string;
  version?: SimulationVersionDetailResponse;
  versionNumber?: number;
}) {
  const [zoom, setZoom] = useState(1);
  const title = version?.name ?? simulationId ?? "Simulação";
  const flow = version ? buildInteractiveScoreFlow(version) : null;
  const selectedFlowNode =
    selectedNodeId && flow ? flow.nodes.find((node) => node.id === selectedNodeId) : null;
  const selectedStep =
    selectedFlowNode?.node && version
      ? buildSingleStepScoreSummary(selectedFlowNode.node, version)
      : null;
  const selectedOutgoing = flow
    ? flow.edges.filter((edge) => edge.from === selectedNodeId && edge.option)
    : [];

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold">
            {title} {versionNumber ? `— v${versionNumber}` : ""}
          </h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Clique em blocos e alternativas para filtrar o diagnóstico e ver a função da etapa.
          </p>
        </div>
        <div className="flex items-center gap-1 rounded-md border border-border bg-background p-1">
          <button
            type="button"
            onClick={() => setZoom((current) => Math.max(0.75, Number((current - 0.1).toFixed(2))))}
            className="rounded p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
            title="Reduzir zoom"
          >
            <ZoomOut className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => setZoom(1)}
            className="rounded px-2 py-1 text-xs font-medium tabular-nums hover:bg-muted"
            title="Voltar para 100%"
          >
            {Math.round(zoom * 100)}%
          </button>
          <button
            type="button"
            onClick={() => setZoom((current) => Math.min(1.25, Number((current + 0.1).toFixed(2))))}
            className="rounded p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
            title="Aumentar zoom"
          >
            <ZoomIn className="h-4 w-4" />
          </button>
          {selectedNodeId && (
            <button
              type="button"
              onClick={() => onSelectNode(null)}
              className="ml-1 rounded px-2 py-1 text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
            >
              limpar
            </button>
          )}
        </div>
      </div>

      {flow ? (
        <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_280px]">
          <div className="overflow-auto rounded-md border border-border bg-muted/20">
            <div
              className="relative origin-top-left"
              style={{
                height: flow.height * zoom,
                minWidth: flow.width * zoom,
                width: flow.width * zoom,
              }}
            >
              <div
                className="absolute left-0 top-0"
                style={{
                  height: flow.height,
                  transform: `scale(${zoom})`,
                  transformOrigin: "top left",
                  width: flow.width,
                }}
              >
                <svg
                  aria-hidden="true"
                  className="absolute inset-0"
                  height={flow.height}
                  width={flow.width}
                >
                  <defs>
                    <marker
                      id="validator-flow-arrow"
                      markerHeight="8"
                      markerWidth="8"
                      orient="auto"
                      refX="7"
                      refY="4"
                    >
                      <path d="M0,0 L8,4 L0,8 Z" className="fill-muted-foreground" />
                    </marker>
                  </defs>
                  {flow.edges.map((edge) => (
                    <path
                      key={edge.id}
                      className={cn(
                        "fill-none stroke-muted-foreground/60",
                        edge.option?.isCritical && "stroke-danger",
                        selectedNodeId === edge.from && "stroke-primary",
                      )}
                      d={edge.path}
                      markerEnd="url(#validator-flow-arrow)"
                      strokeWidth={selectedNodeId === edge.from ? 2 : 1.5}
                    />
                  ))}
                </svg>

                {flow.edges.map((edge) =>
                  edge.option ? (
                    <button
                      key={`${edge.id}-label`}
                      type="button"
                      onClick={() => onSelectNode(edge.toKind === "end" ? edge.from : edge.to)}
                      className={cn(
                        "absolute z-10 w-[132px] rounded-md border bg-card px-2 py-1 text-left text-[11px] shadow-sm transition hover:border-primary hover:bg-accent",
                        edge.option.isCritical
                          ? "border-danger/40 text-danger"
                          : "border-border text-foreground",
                      )}
                      style={{ left: edge.labelX, top: edge.labelY }}
                      title={edge.option.text}
                    >
                      <span className="flex items-center gap-1 font-mono">
                        <GitBranch className="h-3 w-3" />
                        {edge.option.id}
                        <span className="font-sans tabular-nums">+{formatScore(edge.score)}</span>
                      </span>
                    </button>
                  ) : null,
                )}

                {flow.nodes.map((flowNode) => (
                  <button
                    key={flowNode.id}
                    type="button"
                    onClick={() =>
                      flowNode.kind === "step"
                        ? onSelectNode(selectedNodeId === flowNode.id ? null : flowNode.id)
                        : onSelectNode(flowNode.sourceNodeId ?? null)
                    }
                    className={cn(
                      "absolute z-20 flex h-[104px] w-[176px] flex-col rounded-md border bg-card p-3 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-primary hover:shadow-md",
                      flowNode.kind === "end" && "bg-muted",
                      selectedNodeId === flowNode.id || selectedNodeId === flowNode.sourceNodeId
                        ? "border-primary ring-2 ring-primary/20"
                        : "border-border",
                    )}
                    style={{ left: flowNode.x, top: flowNode.y }}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-mono text-[11px] font-semibold text-primary">
                        {flowNode.label}
                      </span>
                      {flowNode.kind === "step" ? (
                        <Workflow className="h-3.5 w-3.5 text-muted-foreground" />
                      ) : (
                        <Flag className="h-3.5 w-3.5 text-muted-foreground" />
                      )}
                    </div>
                    <div className="mt-2 line-clamp-2 text-xs font-medium leading-5">
                      {flowNode.title}
                    </div>
                    <div className="mt-auto flex items-center justify-between gap-2 text-[11px] text-muted-foreground">
                      <span>{flowNode.meta}</span>
                      <span className="font-semibold tabular-nums text-foreground">
                        {flowNode.scoreLabel}
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>

          <aside className="rounded-md border border-border bg-background p-4">
            <div className="flex items-center gap-2 text-xs font-semibold uppercase text-muted-foreground">
              <MousePointerClick className="h-4 w-4" />
              Função do bloco
            </div>

            {selectedStep ? (
              <div className="mt-3 space-y-4">
                <div>
                  <div className="font-mono text-xs text-primary">{selectedStep.node.id}</div>
                  <p className="mt-1 line-clamp-5 text-sm leading-6">
                    {selectedStep.node.clientMessage || "Sem texto cadastrado."}
                  </p>
                  <button
                    type="button"
                    onClick={() => onSelectNode(selectedStep.node.id)}
                    className="mt-3 inline-flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                  >
                    <ZoomIn className="h-3 w-3" />
                    Detalhes
                  </button>
                </div>
                <dl className="grid grid-cols-2 gap-2 text-xs">
                  <MetricTile
                    label="Tempo"
                    value={formatTimeLimit(selectedStep.node.timeLimitSeconds)}
                  />
                  <MetricTile label="Atual" value={formatScore(selectedStep.currentScore)} />
                  <MetricTile
                    label="Acumulada"
                    value={formatScore(selectedStep.accumulatedScore)}
                  />
                  <MetricTile label="Saídas" value={selectedStep.node.options.length.toString()} />
                </dl>
                <div>
                  <div className="mb-2 text-[11px] font-medium uppercase text-muted-foreground">
                    Competências
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {selectedStep.competencies.length > 0 ? (
                      selectedStep.competencies.map((competency) => (
                        <span
                          key={competency.name}
                          className="rounded-md border border-border bg-card px-2 py-1 text-[11px]"
                        >
                          {competency.name}:{" "}
                          <span className="font-semibold tabular-nums">
                            {formatScore(competency.value)}
                          </span>
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-muted-foreground">
                        Nenhuma competência configurada.
                      </span>
                    )}
                  </div>
                </div>
                <div>
                  <div className="mb-2 text-[11px] font-medium uppercase text-muted-foreground">
                    Alternativas
                  </div>
                  <div className="space-y-2">
                    {selectedOutgoing.map((edge) => (
                      <button
                        key={edge.id}
                        type="button"
                        onClick={() => onSelectNode(edge.toKind === "end" ? edge.from : edge.to)}
                        className="w-full rounded-md border border-border bg-card p-2 text-left text-xs hover:border-primary hover:bg-accent"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-mono">{edge.option?.id}</span>
                          <span className="font-semibold tabular-nums">
                            +{formatScore(edge.score)} pts
                          </span>
                        </div>
                        <p className="mt-1 line-clamp-2 text-muted-foreground">
                          {edge.option?.text}
                        </p>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <div className="mt-4 rounded-md border border-dashed border-border p-4 text-sm text-muted-foreground">
                Selecione uma etapa para ver texto, tempo, pontuação, competências e saídas sem sair da
                tela.
              </div>
            )}
          </aside>
        </div>
      ) : (
        <div className="rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
          Carregando mapa interativo do fluxo...
        </div>
      )}

      <div className="mt-4 grid grid-cols-2 gap-3 text-xs md:grid-cols-4">
        {(flow?.pathSummaries ?? ["A", "B", "C", "D"].map((path) => ({ label: path }))).map(
          (path) => (
            <div key={path.label} className="rounded-md border border-border bg-background p-3">
              <div className="font-mono text-[11px] text-muted-foreground">
                Caminho {path.label}
              </div>
              <div className="mt-1 text-xl font-semibold tabular-nums">máx 100</div>
            </div>
          ),
        )}
      </div>
    </div>
  );
}

interface InteractiveFlowNode {
  id: string;
  kind: "step" | "end";
  label: string;
  meta: string;
  node?: SimulationVersionNodeResponse;
  scoreLabel: string;
  sourceNodeId?: string;
  title: string;
  x: number;
  y: number;
}

interface InteractiveFlowEdge {
  from: string;
  id: string;
  labelX: number;
  labelY: number;
  option?: SimulationVersionOptionResponse;
  path: string;
  score: number;
  to: string;
  toKind: "step" | "end";
}

interface InteractiveScoreFlow {
  edges: InteractiveFlowEdge[];
  height: number;
  nodes: InteractiveFlowNode[];
  pathSummaries: Array<{ label: string }>;
  width: number;
}

function buildInteractiveScoreFlow(version: SimulationVersionDetailResponse): InteractiveScoreFlow {
  const nodeWidth = 176;
  const nodeHeight = 104;
  const optionLabelWidth = 132;
  const columnGap = 420;
  const rowGap = 154;
  const startX = 36;
  const startY = 36;
  const nodesById = new Map(version.nodes.map((node) => [node.id, node]));
  const summariesById = new Map(
    buildStepScoreSummaries(version).map((summary) => [summary.node.id, summary]),
  );
  const stepNodes = [...version.nodes].sort((a, b) => {
    if (a.turnIndex !== b.turnIndex) return a.turnIndex - b.turnIndex;
    return a.id.localeCompare(b.id);
  });
  const turnIndexes = Array.from(new Set(stepNodes.map((node) => node.turnIndex))).sort(
    (a, b) => a - b,
  );
  const columnByTurn = new Map(turnIndexes.map((turn, index) => [turn, index]));
  const rowsByTurn = new Map<number, SimulationVersionNodeResponse[]>();

  stepNodes.forEach((node) => {
    rowsByTurn.set(node.turnIndex, [...(rowsByTurn.get(node.turnIndex) ?? []), node]);
  });

  const flowNodes: InteractiveFlowNode[] = stepNodes.map((node) => {
    const column = columnByTurn.get(node.turnIndex) ?? 0;
    const row = rowsByTurn.get(node.turnIndex)?.findIndex((item) => item.id === node.id) ?? 0;
    const summary = summariesById.get(node.id);

    return {
      id: node.id,
      kind: "step",
      label: node.id,
      meta: `Turno ${node.turnIndex}`,
      node,
      scoreLabel: `${formatScore(summary?.accumulatedScore ?? 0)} pts`,
      title: node.clientMessage || "Sem texto cadastrado.",
      x: startX + column * columnGap,
      y: startY + row * rowGap,
    };
  });
  const endNodes: InteractiveFlowNode[] = [];
  const edges: InteractiveFlowEdge[] = [];

  stepNodes.forEach((node) => {
    const fromNode = flowNodes.find((item) => item.id === node.id);
    if (!fromNode) return;

    const terminalOptions = node.options.filter(
      (option) => !option.nextNodeId || !nodesById.has(option.nextNodeId),
    );

    node.options.forEach((option, optionIndex) => {
      const score = scoreOption(option, version);
      const targetNode = option.nextNodeId ? nodesById.get(option.nextNodeId) : undefined;
      let toNode = targetNode ? flowNodes.find((item) => item.id === targetNode.id) : undefined;
      let toKind: "step" | "end" = "step";

      if (!toNode) {
        const endId = `fim-${node.id}-${option.id}`;
        const existingEnd = endNodes.find((item) => item.id === endId);
        toNode =
          existingEnd ??
          ({
            id: endId,
            kind: "end",
            label: "FIM",
            meta: `via ${node.id}/${option.id}`,
            scoreLabel: `+${formatScore(score)} pts`,
            sourceNodeId: node.id,
            title: option.auditNote || option.text || "Encerramento",
            x: fromNode.x + columnGap,
            y: Math.max(
              startY,
              fromNode.y +
                terminalOptions.findIndex((item) => item.id === option.id) * (nodeHeight + 14),
            ),
          } satisfies InteractiveFlowNode);
        if (!existingEnd) endNodes.push(toNode);
        toKind = "end";
      }

      const start = { x: fromNode.x + nodeWidth, y: fromNode.y + nodeHeight / 2 };
      const end = { x: toNode.x, y: toNode.y + nodeHeight / 2 };
      const midX = start.x + Math.max(80, (end.x - start.x) / 2);
      const path = `M ${start.x} ${start.y} C ${midX} ${start.y}, ${midX} ${end.y}, ${end.x} ${end.y}`;
      const labelX = Math.min(end.x - optionLabelWidth - 20, start.x + 40);

      edges.push({
        from: node.id,
        id: `${node.id}-${option.id}-${toNode.id}`,
        labelX: Math.max(start.x + 12, labelX),
        labelY: (start.y + end.y) / 2 - 18,
        option,
        path,
        score,
        to: toNode.id,
        toKind,
      });
    });
  });

  const allNodes = [...flowNodes, ...endNodes];
  const width = Math.max(760, Math.max(...allNodes.map((node) => node.x + nodeWidth + 36), 760));
  const height = Math.max(330, Math.max(...allNodes.map((node) => node.y + nodeHeight + 36), 330));
  const pathSummaries = collectEndingTraces(version)
    .slice(0, 4)
    .map((ending, index) => ({
      label: ending.optionId || String.fromCharCode(65 + index),
    }));

  return {
    edges,
    height,
    nodes: allNodes,
    pathSummaries:
      pathSummaries.length > 0 ? pathSummaries : ["A", "B", "C", "D"].map((label) => ({ label })),
    width,
  };
}

function buildSingleStepScoreSummary(
  node: SimulationVersionNodeResponse,
  version: SimulationVersionDetailResponse,
): StepScoreSummary {
  const summary = buildStepScoreSummaries(version).find((item) => item.node.id === node.id);
  return (
    summary ?? {
      accumulatedScore: 0,
      competencies: summarizeCompetencies(node),
      currentScore: 0,
      hasCriticalOption: node.options.some((option) => option.isCritical),
      nextTargets: summarizeNextTargets(node),
      node,
    }
  );
}

function FlowOutcomeSummary({ version }: { version: SimulationVersionDetailResponse }) {
  const endings = collectEndingTraces(version);
  const maxScore = endings.length
    ? Math.max(...endings.map((ending) => ending.accumulatedScore))
    : 0;

  return (
    <div className="mt-5 grid gap-4 lg:grid-cols-2">
      <section className="rounded-md border border-border bg-background p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-sm font-semibold">Finais alcançáveis</h3>
          <span className="rounded-md bg-muted px-2 py-1 text-[11px] text-muted-foreground">
            {endings.length} {endings.length === 1 ? "final" : "finais"}
          </span>
        </div>

        {endings.length > 0 ? (
          <ul className="space-y-2">
            {endings.map((ending) => (
              <li
                key={`${ending.nodeId}-${ending.optionId}-${ending.path.join("-")}`}
                className="flex items-start gap-3 rounded-md border border-border bg-card p-3 text-sm"
              >
                <Flag className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium">{ending.label}</span>
                    <span className="rounded bg-muted px-1.5 py-0.5 text-[11px] tabular-nums text-muted-foreground">
                      {formatScore(ending.accumulatedScore)} pts acumulados
                    </span>
                  </div>
                  <div className="mt-1 text-[11px] text-muted-foreground">
                    via {ending.path.join(" -> ")}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-muted-foreground">
            Nenhum final alcançável foi encontrado a partir da etapa inicial.
          </p>
        )}
      </section>

      <section className="rounded-md border border-border bg-background p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-sm font-semibold">Texto do relatório final</h3>
          <span className="rounded-md bg-muted px-2 py-1 text-[11px] text-muted-foreground">
            máximo {formatScore(maxScore)} pts
          </span>
        </div>

        {endings.length > 0 ? (
          <div className="space-y-2">
            {endings.map((ending) => (
              <article
                key={`${ending.nodeId}-${ending.optionId}-report-${ending.path.join("-")}`}
                className="rounded-md border border-border bg-card p-3 text-sm"
              >
                <div className="mb-2 flex flex-wrap items-center justify-between gap-2 border-b border-border pb-2">
                  <span className="font-medium">{ending.label}</span>
                  <span className="text-[11px] text-muted-foreground">
                    {formatScore(ending.accumulatedScore)} de {formatScore(maxScore)}
                  </span>
                </div>
                <p className="leading-6 text-foreground/85">
                  {ending.reportText || "Sem texto de relatório cadastrado para este final."}
                </p>
              </article>
            ))}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            Cadastre uma alternativa final para ver o texto do relatório.
          </p>
        )}
      </section>
    </div>
  );
}

interface StepScoreSummary {
  accumulatedScore: number;
  competencies: Array<{ name: string; value: number }>;
  currentScore: number;
  hasCriticalOption: boolean;
  nextTargets: string[];
  node: SimulationVersionNodeResponse;
}

interface EndingTrace {
  accumulatedScore: number;
  label: string;
  nodeId: string;
  optionId: string;
  path: string[];
  reportText: string;
}

function buildStepScoreSummaries(version: SimulationVersionDetailResponse): StepScoreSummary[] {
  const nodes = [...version.nodes].sort((a, b) => a.turnIndex - b.turnIndex);
  const bestBefore = new Map<string, number>([[version.blueprint.rootNodeId, 0]]);

  return nodes.map((node) => {
    const before = bestBefore.get(node.id) ?? 0;
    const optionScores = node.options.map((option) => scoreOption(option, version));
    const currentScore = optionScores.length > 0 ? Math.max(...optionScores) : 0;
    const accumulatedScore = before + currentScore;

    node.options.forEach((option) => {
      if (!option.nextNodeId) return;
      const nextScore = before + scoreOption(option, version);
      const previousScore = bestBefore.get(option.nextNodeId) ?? Number.NEGATIVE_INFINITY;
      if (nextScore > previousScore) {
        bestBefore.set(option.nextNodeId, nextScore);
      }
    });

    return {
      accumulatedScore,
      competencies: summarizeCompetencies(node),
      currentScore,
      hasCriticalOption: node.options.some((option) => option.isCritical),
      nextTargets: summarizeNextTargets(node),
      node,
    };
  });
}

function scoreOption(
  option: SimulationVersionOptionResponse,
  version: SimulationVersionDetailResponse,
) {
  const weights = version.blueprint.competencies;
  if (weights.length > 0) {
    const totalWeight = weights.reduce((sum, competency) => sum + competency.weight, 0);
    if (totalWeight > 0) {
      return (
        weights.reduce(
          (sum, competency) =>
            sum + (option.competencyLevels[competency.name] ?? 0) * competency.weight,
          0,
        ) / totalWeight
      );
    }
  }

  const values = Object.values(option.competencyLevels);
  return values.length > 0 ? values.reduce((sum, value) => sum + value, 0) / values.length : 0;
}

function collectEndingTraces(version: SimulationVersionDetailResponse): EndingTrace[] {
  const nodesById = new Map(version.nodes.map((node) => [node.id, node]));
  const endings: EndingTrace[] = [];
  const maxDepth = version.nodes.length + 1;

  function visit(nodeId: string, accumulatedScore: number, path: string[], depth: number) {
    if (depth > maxDepth || path.includes(nodeId)) return;

    const node = nodesById.get(nodeId);
    if (!node) return;

    const nextPath = [...path, node.id];
    node.options.forEach((option) => {
      const optionScore = scoreOption(option, version);
      const nextScore = accumulatedScore + optionScore;

      if (!option.nextNodeId) {
        endings.push({
          accumulatedScore: nextScore,
          label: `Final via ${node.id}/${option.id}`,
          nodeId: node.id,
          optionId: option.id,
          path: nextPath,
          reportText: option.auditNote || option.text,
        });
        return;
      }

      visit(option.nextNodeId, nextScore, nextPath, depth + 1);
    });
  }

  visit(version.blueprint.rootNodeId, 0, [], 0);
  return endings.sort((a, b) => b.accumulatedScore - a.accumulatedScore);
}

function summarizeCompetencies(node: SimulationVersionNodeResponse) {
  const valuesByName = new Map<string, number[]>();

  node.options.forEach((option) => {
    Object.entries(option.competencyLevels).forEach(([name, value]) => {
      valuesByName.set(name, [...(valuesByName.get(name) ?? []), value]);
    });
  });

  return Array.from(valuesByName.entries())
    .map(([name, values]) => ({
      name,
      value: Math.max(...values),
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function summarizeNextTargets(node: SimulationVersionNodeResponse) {
  const targets = Array.from(
    new Set(node.options.map((option) => option.nextNodeId ?? "fim")),
  ).sort((a, b) => a.localeCompare(b));

  return targets.length > 0 ? targets : ["fim"];
}

function formatTimeLimit(seconds: number | null) {
  if (seconds === null) return "Sem limite";
  if (seconds < 60) return `${seconds}s`;

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return remainingSeconds === 0 ? `${minutes}min` : `${minutes}m ${remainingSeconds}s`;
}

function formatScore(score: number) {
  return Math.round(score).toString();
}

function groupValidationChecks(checks: ValidationCheck[]): DiagnosticGroup[] {
  const groups = new Map<string, DiagnosticGroup>();

  checks.forEach((check) => {
    const key = `${check.tone}-${check.text}`;
    const current = groups.get(key);
    if (!current) {
      groups.set(key, {
        id: key,
        nodeIds: check.nodeId ? [check.nodeId] : [],
        targets: [check.target],
        text: check.text,
        tone: check.tone,
      });
      return;
    }

    if (check.nodeId && !current.nodeIds.includes(check.nodeId)) {
      current.nodeIds.push(check.nodeId);
    }
    if (!current.targets.includes(check.target)) {
      current.targets.push(check.target);
    }
  });

  return Array.from(groups.values());
}

function formatCount(count: number, singular: string, plural: string) {
  return `${count} ${count === 1 ? singular : plural}`;
}

function scoreQualityTone(score: number) {
  if (score < 50) {
    return { barClass: "bg-danger", textClass: "text-danger" };
  }
  if (score < 80) {
    return { barClass: "bg-warning", textClass: "text-warning-foreground" };
  }
  return { barClass: "bg-success", textClass: "text-success" };
}

function mapValidationIssues(validation: SimulationValidationResponse): ValidationCheck[] {
  if (validation.issues.length === 0) {
    return [
      {
        id: "publishable",
        nodeId: null,
        tone: "ok",
        text: "Nenhum bloqueio ou alerta encontrado nesta versão",
        target: `Simulação ${validation.simulationId} v${validation.versionNumber}`,
      },
    ];
  }

  return validation.issues.map((issue, index) => ({
    id: `${issue.severity}-${issue.nodeId ?? "global"}-${index}`,
    nodeId: issue.nodeId,
    tone: issue.severity === "blocker" ? "danger" : "warn",
    text: issue.message,
    target: issue.nodeId ? `Editor: ${issue.nodeId}` : "Simulação",
  }));
}

function SimulationLinks({
  simulations,
  loading,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
}) {
  if (loading) {
    return (
      <div className="rounded-md border border-border bg-card px-4 py-3 text-sm">
        Carregando simulações...
      </div>
    );
  }

  if (simulations.length === 0) {
    return (
      <Link
        to="/nova/blueprint"
        className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
      >
        Criar simulação
      </Link>
    );
  }

  return (
    <>
      {simulations.slice(0, 3).map((simulation) => (
        <Link
          key={simulation.id}
          to="/nova/validador"
          search={{
            simulationId: simulation.id,
            versionNumber: simulation.versionNumber,
          }}
          className="rounded-md border border-border bg-card px-4 py-3 text-sm hover:bg-accent"
        >
          <span className="block font-medium">{simulation.name}</span>
          <span className="mt-1 block">
            <StatusBadge
              status={simulation.status}
              maturity={maturityForStatus(simulation.status)}
            />
          </span>
        </Link>
      ))}
    </>
  );
}

function CheckIcon({ tone }: { tone: "ok" | "warn" | "danger" }) {
  const cls =
    tone === "ok"
      ? "bg-success/15 text-success"
      : tone === "warn"
        ? "bg-warning/20 text-warning-foreground"
        : "bg-danger/15 text-danger";
  const Icon = tone === "ok" ? CheckCircle2 : tone === "warn" ? AlertTriangle : XCircle;
  return (
    <span
      className={cn("mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full", cls)}
    >
      <Icon className="h-4 w-4" />
    </span>
  );
}
