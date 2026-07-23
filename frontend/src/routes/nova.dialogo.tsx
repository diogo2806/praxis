import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState } from "react";
import { Film, GitBranch, ImagePlus, Music, Plus, Save, Trash2, X } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { TerminalOutcomeCard } from "@/components/simulation/terminal-outcome-card";
import {
  groupIssuesByNode,
  ValidationBadge,
  ValidationSummary,
} from "@/components/simulation/validation-badge";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  cloneSimulationVersionToDraft,
  createSimulationBranchNode,
  createSimulationNode,
  createSimulationOption,
  deleteSimulationNode,
  deleteSimulationOption,
  getSimulationValidation,
  getSimulationVersion,
  listSimulations,
  updateSimulationNode,
  updateSimulationOption,
  uploadMedia,
  type MediaType,
  type SimulationSummaryResponse,
  type SimulationVersionNodeResponse,
  type SimulationVersionOptionResponse,
} from "@/lib/api/praxis";
import { defaultAnswerTimeLimitSeconds, useEmpresaConfig } from "@/lib/empresa-config";
import {
  buildNodeDisplayCodes,
  compareDisplayCodes,
  displayCodeDepth,
} from "@/lib/simulation-node-hierarchy";
import { canEditSimulationVersion, statusMeta } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

const CREATE_BRANCH_VALUE = "__CREATE_BRANCH__";

export const Route = createFileRoute("/nova/dialogo")({
  validateSearch: (search: Record<string, unknown>) => ({
    simulationId: typeof search.simulationId === "string" ? search.simulationId : undefined,
    nodeId: typeof search.nodeId === "string" ? search.nodeId : undefined,
    versionNumber:
      typeof search.versionNumber === "number"
        ? search.versionNumber
        : typeof search.versionNumber === "string"
          ? Number(search.versionNumber)
          : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Editor de Diálogo - Práxis" },
      {
        name: "description",
        content: "Edição das etapas posteriores, alternativas, mídia e pontuação da avaliação.",
      },
    ],
  }),
  component: DialogEditor,
});

function DialogEditor() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const messageInputRef = useRef<HTMLTextAreaElement>(null);
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const {
    config,
    isLoading: empresaConfigLoading,
    isError: empresaConfigError,
    error: empresaConfigQueryError,
  } = useEmpresaConfig();
  const answerTimeLimits = config?.answerTimeLimits ?? [];

  const [selectedId, setSelectedId] = useState<string | null>(search.nodeId ?? null);
  const [draftMessage, setDraftMessage] = useState("");
  const [selectedMessage, setSelectedMessage] = useState("");
  const [draftOption, setDraftOption] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasContext,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });
  const validationQuery = useQuery({
    queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationValidation(search.simulationId!, search.versionNumber!),
    enabled: hasContext,
  });

  const nodes = versionQuery.data?.nodes ?? [];
  const rootNodeId = versionQuery.data?.blueprint.rootNodeId;
  const displayCodes = useMemo(() => buildNodeDisplayCodes(nodes, rootNodeId), [nodes, rootNodeId]);
  const dialogueNodes = useMemo(
    () =>
      nodes
        .filter((node) => !node.isFinal)
        .sort((left, right) =>
          compareDisplayCodes(
            displayCodes.get(left.id) ?? String(left.turnIndex),
            displayCodes.get(right.id) ?? String(right.turnIndex),
          ),
        ),
    [nodes, displayCodes],
  );
  const nodeById = useMemo(() => new Map(nodes.map((node) => [node.id, node])), [nodes]);
  const selected = dialogueNodes.find((node) => node.id === selectedId) ?? dialogueNodes[0];
  const selectedDisplayCode = selected
    ? (displayCodes.get(selected.id) ?? String(selected.turnIndex))
    : "";
  const isRootSelected = Boolean(selected && selected.id === rootNodeId);
  const versionStatus = versionQuery.data?.status;
  const isEditable = versionStatus ? canEditSimulationVersion(versionStatus) : true;
  const competencies = useMemo(
    () => versionQuery.data?.blueprint.competencies ?? [],
    [versionQuery.data?.blueprint.competencies],
  );
  const competencyLevels = useMemo(
    () => Object.fromEntries(competencies.map((competency) => [competency.name, 50])),
    [competencies],
  );
  const issuesByNode = useMemo(
    () => groupIssuesByNode(validationQuery.data?.issues),
    [validationQuery.data?.issues],
  );

  function nodeLabel(node: SimulationVersionNodeResponse) {
    return displayCodes.get(node.id) ?? String(node.turnIndex);
  }

  function terminalNodeFor(option: SimulationVersionOptionResponse) {
    if (!option.nextNodeId) return null;
    const target = nodeById.get(option.nextNodeId);
    return target?.isFinal ? target : null;
  }

  function isTerminalOption(option: SimulationVersionOptionResponse) {
    return option.nextNodeId == null || terminalNodeFor(option) != null;
  }

  function reportFor(option: SimulationVersionOptionResponse) {
    return terminalNodeFor(option)?.reportText ?? option.auditNote ?? "";
  }

  function terminalNodeForTimeout(node: SimulationVersionNodeResponse) {
    if (!node.timeoutNextNodeId) return null;
    const target = nodeById.get(node.timeoutNextNodeId);
    return target?.isFinal ? target : null;
  }

  function isTerminalTimeout(node: SimulationVersionNodeResponse) {
    return (
      (node.timeLimitSeconds ?? 0) > 0 &&
      (node.timeoutNextNodeId == null || terminalNodeForTimeout(node) != null)
    );
  }

  function timeoutReportFor(node: SimulationVersionNodeResponse) {
    return terminalNodeForTimeout(node)?.reportText ?? node.reportText ?? "";
  }

  const canReview =
    dialogueNodes.length > 0 &&
    dialogueNodes.every(
      (node) =>
        node.clientMessage.trim().length > 0 &&
        node.options.length >= 2 &&
        node.options.length <= 4 &&
        (!isTerminalTimeout(node) || timeoutReportFor(node).trim().length > 0) &&
        node.options.every(
          (option) =>
            option.text.trim().length > 0 &&
            Object.keys(option.competencyLevels).length > 0 &&
            (!isTerminalOption(option) || reportFor(option).trim().length > 0),
        ),
    );

  useEffect(() => {
    setSelectedMessage(selected?.clientMessage ?? "");
  }, [selected?.id, selected?.clientMessage]);

  useEffect(() => {
    if (dialogueNodes.length === 0) return;
    if (search.nodeId && dialogueNodes.some((node) => node.id === search.nodeId)) {
      setSelectedId(search.nodeId);
      return;
    }
    setSelectedId((current) =>
      current && dialogueNodes.some((node) => node.id === current)
        ? current
        : (rootNodeId ?? dialogueNodes[0].id),
    );
  }, [dialogueNodes, rootNodeId, search.nodeId]);

  useEffect(() => {
    if (!hasContext) return;
    const timer = window.setTimeout(() => void validationQuery.refetch(), 800);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [versionQuery.dataUpdatedAt, hasContext]);

  async function refetchVersion() {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      }),
      queryClient.invalidateQueries({
        queryKey: ["simulation-validation", search.simulationId, search.versionNumber],
      }),
    ]);
  }

  function assertEditable() {
    if (!isEditable) {
      throw new Error("Esta versão não pode ser editada. Crie um rascunho antes de alterar.");
    }
  }

  const cloneDraftMutation = useMutation({
    mutationFn: () => cloneSimulationVersionToDraft(search.simulationId!, search.versionNumber!),
    onSuccess: async (draft) => {
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/dialogo",
        search: {
          simulationId: draft.simulationId,
          nodeId: undefined,
          versionNumber: draft.newVersionNumber,
        },
      });
    },
  });

  const addNodeMutation = useMutation({
    mutationFn: () => {
      assertEditable();
      if (!config) throw new Error("A configuração da empresa ainda não foi carregada.");
      return createSimulationNode(search.simulationId!, search.versionNumber!, {
        clientMessage: draftMessage.trim(),
        timeLimitSeconds: defaultAnswerTimeLimitSeconds(config),
      });
    },
    onSuccess: async (nodeId) => {
      setDraftMessage("");
      await refetchVersion();
      setSelectedId(nodeId);
      setFeedbackMessage("Etapa adicionada.");
    },
  });

  const createBranchMutation = useMutation({
    mutationFn: ({ nodeId, optionId }: { nodeId: string; optionId: string }) => {
      assertEditable();
      return createSimulationBranchNode(
        search.simulationId!,
        search.versionNumber!,
        nodeId,
        optionId,
      );
    },
    onSuccess: async (nodeId) => {
      await refetchVersion();
      setSelectedId(nodeId);
      setFeedbackMessage("Nova etapa ramificada criada e vinculada à alternativa.");
      void navigate({
        to: "/nova/dialogo",
        search: {
          simulationId: search.simulationId,
          versionNumber: search.versionNumber,
          nodeId,
        },
        replace: true,
      });
      window.setTimeout(() => messageInputRef.current?.focus(), 0);
    },
  });

  const saveNodeMessageMutation = useMutation({
    mutationFn: ({ nodeId, message }: { nodeId: string; message: string }) => {
      assertEditable();
      if (nodeId === rootNodeId) {
        throw new Error("A mensagem inicial só pode ser alterada na tela Personagem.");
      }
      return updateSimulationNode(search.simulationId!, search.versionNumber!, nodeId, {
        clientMessage: message.trim(),
      });
    },
    onSuccess: async () => {
      setFeedbackMessage("Mensagem da etapa salva.");
      await refetchVersion();
    },
  });

  const updateNodeTimeMutation = useMutation({
    mutationFn: ({
      nodeId,
      timeLimitSeconds,
    }: {
      nodeId: string;
      timeLimitSeconds: number | null;
    }) => {
      assertEditable();
      return updateSimulationNode(search.simulationId!, search.versionNumber!, nodeId, {
        timeLimitSeconds,
        ...(timeLimitSeconds === null ? { timeoutNextNodeId: "" } : {}),
      });
    },
    onSuccess: refetchVersion,
  });

  const updateNodeTimeoutMutation = useMutation({
    mutationFn: ({
      nodeId,
      timeoutNextNodeId,
    }: {
      nodeId: string;
      timeoutNextNodeId: string | null;
    }) => {
      assertEditable();
      return updateSimulationNode(search.simulationId!, search.versionNumber!, nodeId, {
        timeoutNextNodeId: timeoutNextNodeId ?? "",
      });
    },
    onSuccess: refetchVersion,
  });

  const updateNodeMediaMutation = useMutation({
    mutationFn: ({
      nodeId,
      mediaUrl,
      mediaType,
      mediaTranscript,
      mediaCaptionsUrl,
      mediaVersion,
    }: {
      nodeId: string;
      mediaUrl: string;
      mediaType: MediaType | null;
      mediaTranscript?: string | null;
      mediaCaptionsUrl?: string | null;
      mediaVersion?: string | null;
    }) => {
      assertEditable();
      return updateSimulationNode(search.simulationId!, search.versionNumber!, nodeId, {
        mediaUrl,
        mediaType,
        mediaTranscript,
        mediaCaptionsUrl,
        mediaVersion,
      });
    },
    onSuccess: refetchVersion,
  });

  const deleteNodeMutation = useMutation({
    mutationFn: (nodeId: string) => {
      assertEditable();
      if (nodeId === rootNodeId) throw new Error("A etapa inicial não pode ser removida.");
      return deleteSimulationNode(search.simulationId!, search.versionNumber!, nodeId);
    },
    onSuccess: async () => {
      setSelectedId(rootNodeId ?? null);
      setFeedbackMessage("Etapa removida.");
      await refetchVersion();
    },
  });

  const addOptionMutation = useMutation({
    mutationFn: () => {
      assertEditable();
      if (!selected) throw new Error("Selecione uma etapa antes de adicionar alternativas.");
      return createSimulationOption(search.simulationId!, search.versionNumber!, selected.id, {
        text: draftOption.trim(),
        competencyLevels,
        isCritical: false,
        nextNodeId: null,
        resultingTone: "",
      });
    },
    onSuccess: async () => {
      setDraftOption("");
      setFeedbackMessage(
        "Alternativa adicionada. Defina o destino ou preencha o relatório do encerramento.",
      );
      await refetchVersion();
    },
  });

  const updateOptionMutation = useMutation({
    mutationFn: ({
      nodeId,
      optionId,
      text,
      nextNodeId,
      isCritical,
      competencyLevels: levels,
      resultingTone,
      mediaUrl,
      mediaType,
      mediaTranscript,
      mediaCaptionsUrl,
      mediaVersion,
    }: {
      nodeId: string;
      optionId: string;
      text?: string;
      nextNodeId?: string | null;
      isCritical?: boolean;
      competencyLevels?: Record<string, number>;
      resultingTone?: string;
      mediaUrl?: string;
      mediaType?: MediaType | null;
      mediaTranscript?: string | null;
      mediaCaptionsUrl?: string | null;
      mediaVersion?: string | null;
    }) => {
      assertEditable();
      return updateSimulationOption(search.simulationId!, search.versionNumber!, nodeId, optionId, {
        text,
        nextNodeId: nextNodeId === null ? "" : nextNodeId,
        isCritical,
        competencyLevels: levels,
        resultingTone,
        mediaUrl,
        mediaType,
        mediaTranscript,
        mediaCaptionsUrl,
        mediaVersion,
      });
    },
    onSuccess: refetchVersion,
  });

  const saveTerminalReportMutation = useMutation({
    mutationFn: async ({
      nodeId,
      optionId,
      finalNodeId,
      reportText,
    }: {
      nodeId: string;
      optionId: string | null;
      finalNodeId: string | null;
      reportText: string;
    }) => {
      assertEditable();
      if (finalNodeId) {
        return updateSimulationNode(search.simulationId!, search.versionNumber!, finalNodeId, {
          reportText,
        });
      }
      if (optionId) {
        return updateSimulationOption(
          search.simulationId!,
          search.versionNumber!,
          nodeId,
          optionId,
          {
            resultingTone: reportText,
          },
        );
      }
      return updateSimulationNode(search.simulationId!, search.versionNumber!, nodeId, {
        reportText,
      });
    },
    onSuccess: async () => {
      setFeedbackMessage("Texto do relatório salvo.");
      await refetchVersion();
    },
  });

  const deleteOptionMutation = useMutation({
    mutationFn: ({ nodeId, optionId }: { nodeId: string; optionId: string }) => {
      assertEditable();
      return deleteSimulationOption(search.simulationId!, search.versionNumber!, nodeId, optionId);
    },
    onSuccess: refetchVersion,
  });

  const mutationError =
    addNodeMutation.error ??
    createBranchMutation.error ??
    saveNodeMessageMutation.error ??
    updateNodeTimeMutation.error ??
    updateNodeTimeoutMutation.error ??
    updateNodeMediaMutation.error ??
    deleteNodeMutation.error ??
    addOptionMutation.error ??
    updateOptionMutation.error ??
    saveTerminalReportMutation.error ??
    deleteOptionMutation.error ??
    cloneDraftMutation.error;

  return (
    <AppShell>
      <WizardStepper current="revisao" unlockedThrough={canReview ? "revisao" : "cenario"} />

      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3</div>
          <h1 className="mt-1 text-3xl font-semibold">Editar diálogo do cenário</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Crie as etapas posteriores, alternativas, mídia e pontuação. O contexto e a mensagem
            inicial pertencem à tela Personagem.
          </p>
        </div>
        <div className="flex flex-col items-end gap-2">
          {versionQuery.data && <StatusBadge status={versionQuery.data.status} />}
          {versionQuery.data && validationQuery.data && (
            <ValidationSummary
              blockerCount={validationQuery.data.blockerCount}
              warningCount={validationQuery.data.warningCount}
            />
          )}
        </div>
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma versão para editar"
          description="Escolha uma avaliação para abrir o conteúdo do diálogo."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : empresaConfigLoading || versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando fluxo da conversa">
          Buscando avaliação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : empresaConfigError ? (
        <StateBanner tone="danger" title="Não foi possível carregar a configuração">
          {empresaConfigQueryError instanceof Error
            ? empresaConfigQueryError.message
            : "Verifique se o sistema está disponível antes de editar o fluxo."}
        </StateBanner>
      ) : versionQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar o fluxo da conversa">
          {versionQuery.error instanceof Error
            ? versionQuery.error.message
            : "Verifique sua conexão e tente novamente."}
        </StateBanner>
      ) : (
        <>
          {feedbackMessage && (
            <div className="mb-5">
              <StateBanner tone="info" title="Alteração salva">
                {feedbackMessage}
              </StateBanner>
            </div>
          )}

          {versionStatus && !isEditable && (
            <div className="mb-5">
              <StateBanner
                tone="warn"
                title={`Versão ${statusMeta[versionStatus].label.toLowerCase()} não pode ser editada`}
                action={
                  versionStatus === "published" ? (
                    <button
                      type="button"
                      onClick={() => cloneDraftMutation.mutate()}
                      disabled={cloneDraftMutation.isPending}
                      className="shrink-0 rounded-md border border-current/20 bg-background/70 px-3 py-2 text-xs font-medium hover:bg-background disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {cloneDraftMutation.isPending ? "Criando..." : "Criar rascunho"}
                    </button>
                  ) : undefined
                }
              >
                Crie ou abra uma versão em rascunho para alterar o diálogo.
              </StateBanner>
            </div>
          )}

          {mutationError && (
            <div className="mb-5">
              <StateBanner tone="danger" title="Não foi possível salvar a alteração">
                {mutationError instanceof Error ? mutationError.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          <fieldset
            disabled={!isEditable}
            className="grid gap-5 lg:grid-cols-[300px_minmax(0,1fr)] disabled:opacity-75"
          >
            <aside className="rounded-md border border-border bg-card p-4">
              <div className="mb-3 text-sm font-semibold">Etapas salvas</div>
              <div className="space-y-2">
                {dialogueNodes.map((node) => {
                  const code = nodeLabel(node);
                  return (
                    <button
                      key={node.id}
                      type="button"
                      onClick={() => setSelectedId(node.id)}
                      className={cn(
                        "w-full rounded-md border p-3 text-left text-sm hover:bg-accent",
                        selected?.id === node.id && "border-primary bg-primary/5",
                      )}
                      style={{ marginLeft: Math.min(displayCodeDepth(code), 3) * 10 }}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-medium">
                          Etapa {code}
                          {node.id === rootNodeId ? " · inicial" : ""}
                        </span>
                        <ValidationBadge issues={issuesByNode.get(node.id) ?? []} />
                      </div>
                      <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                        {node.clientMessage || "Mensagem ainda não preenchida"}
                      </div>
                    </button>
                  );
                })}
              </div>

              <label className="mt-4 block">
                <span className="mb-1 block text-xs text-muted-foreground">
                  Fala de uma nova etapa independente
                </span>
                <textarea
                  className="input min-h-20"
                  value={draftMessage}
                  onChange={(event) => {
                    setDraftMessage(event.target.value);
                    setFeedbackMessage(null);
                  }}
                />
              </label>
              <button
                type="button"
                onClick={() => {
                  setFeedbackMessage(null);
                  addNodeMutation.mutate();
                }}
                disabled={!draftMessage.trim() || addNodeMutation.isPending}
                className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Plus className="h-4 w-4" />
                {addNodeMutation.isPending ? "Adicionando..." : "Adicionar etapa independente"}
              </button>
            </aside>

            {selected ? (
              <section className="rounded-md border border-border bg-card p-5">
                <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">{selected.id}</div>
                    <h2 className="text-xl font-semibold">
                      Etapa {selectedDisplayCode}
                      {isRootSelected ? " · mensagem inicial" : ""}
                    </h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`Remover a etapa ${selectedDisplayCode}?`)) {
                        setFeedbackMessage(null);
                        deleteNodeMutation.mutate(selected.id);
                      }
                    }}
                    disabled={isRootSelected || deleteNodeMutation.isPending}
                    className="inline-flex items-center gap-2 rounded-md border border-danger/25 bg-danger/5 px-3 py-2 text-xs text-danger hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                    {deleteNodeMutation.isPending ? "Removendo..." : "Remover etapa"}
                  </button>
                </div>

                {isRootSelected ? (
                  <StateBanner
                    tone="info"
                    title="Mensagem inicial administrada em Personagem"
                    action={
                      <Link
                        to="/nova/personagem"
                        search={{
                          simulationId: search.simulationId,
                          versionNumber: search.versionNumber,
                        }}
                        className="shrink-0 rounded-md border border-current/20 bg-background/70 px-3 py-2 text-xs font-medium hover:bg-background"
                      >
                        Editar personagem
                      </Link>
                    }
                  >
                    <div className="whitespace-pre-wrap">{selected.clientMessage}</div>
                  </StateBanner>
                ) : (
                  <>
                    <label className="block">
                      <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                        Mensagem do cliente
                      </span>
                      <textarea
                        ref={messageInputRef}
                        key={`${selected.id}-message`}
                        className="input min-h-24"
                        value={selectedMessage}
                        placeholder="Descreva o que acontece nesta nova etapa."
                        onChange={(event) => setSelectedMessage(event.target.value)}
                      />
                    </label>
                    <div className="mt-3 flex justify-end">
                      <button
                        type="button"
                        onClick={() => {
                          setFeedbackMessage(null);
                          saveNodeMessageMutation.mutate({
                            nodeId: selected.id,
                            message: selectedMessage,
                          });
                        }}
                        disabled={
                          !selectedMessage.trim() ||
                          saveNodeMessageMutation.isPending ||
                          selectedMessage.trim() === selected.clientMessage.trim()
                        }
                        className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <Save className="h-4 w-4" />
                        {saveNodeMessageMutation.isPending ? "Salvando..." : "Salvar mensagem"}
                      </button>
                    </div>
                  </>
                )}

                <div className="mt-4 grid gap-4 md:grid-cols-2">
                  <label className="block">
                    <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                      Tempo
                    </span>
                    <select
                      key={`${selected.id}-time`}
                      className="input"
                      defaultValue={selected.timeLimitSeconds ?? "none"}
                      onChange={(event) =>
                        updateNodeTimeMutation.mutate({
                          nodeId: selected.id,
                          timeLimitSeconds:
                            event.target.value === "none" ? null : Number(event.target.value),
                        })
                      }
                    >
                      {answerTimeLimits.map((limit) => (
                        <option
                          key={limit.value}
                          value={limit.value === "0" ? "none" : limit.value}
                        >
                          {limit.label}
                        </option>
                      ))}
                    </select>
                  </label>

                  {(selected.timeLimitSeconds ?? 0) > 0 && (
                    <label className="block">
                      <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                        Quando o tempo acabar
                      </span>
                      <select
                        key={`${selected.id}-timeout-${selected.timeoutNextNodeId ?? "FIM"}`}
                        className="input"
                        defaultValue={
                          selected.timeoutNextNodeId &&
                          nodeById.get(selected.timeoutNextNodeId)?.isFinal
                            ? "FIM"
                            : (selected.timeoutNextNodeId ?? "FIM")
                        }
                        onChange={(event) =>
                          updateNodeTimeoutMutation.mutate({
                            nodeId: selected.id,
                            timeoutNextNodeId:
                              event.target.value === "FIM" ? null : event.target.value,
                          })
                        }
                      >
                        {dialogueNodes
                          .filter((node) => node.turnIndex > selected.turnIndex)
                          .map((node) => (
                            <option key={node.id} value={node.id}>
                              Vai para etapa {nodeLabel(node)}
                            </option>
                          ))}
                        <option value="FIM">Vai para FIM</option>
                      </select>
                      <span className="mt-1 block text-xs text-muted-foreground">
                        Escolha uma etapa posterior ou finalize a avaliação quando o tempo acabar.
                      </span>
                    </label>
                  )}
                </div>

                {isTerminalTimeout(selected) && (
                  <TerminalOutcomeCard
                    nodes={nodes}
                    rootNodeId={rootNodeId}
                    node={selected}
                    outcome="timeout"
                    competencies={competencies}
                    reportText={timeoutReportFor(selected)}
                    disabled={!isEditable}
                    saving={saveTerminalReportMutation.isPending}
                    onSaveReport={(reportText) =>
                      saveTerminalReportMutation.mutate({
                        nodeId: selected.id,
                        optionId: null,
                        finalNodeId: terminalNodeForTimeout(selected)?.id ?? null,
                        reportText,
                      })
                    }
                  />
                )}

                <div className="mt-4">
                  <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                    Mídia da etapa (opcional)
                  </span>
                  <MediaAttachment
                    mediaUrl={selected.mediaUrl}
                    mediaType={selected.mediaType}
                    mediaTranscript={selected.mediaTranscript}
                    mediaCaptionsUrl={selected.mediaCaptionsUrl}
                    mediaVersion={selected.mediaVersion}
                    disabled={updateNodeMediaMutation.isPending}
                    onChange={(next) => {
                      setFeedbackMessage(null);
                      updateNodeMediaMutation.mutate({
                        nodeId: selected.id,
                        mediaUrl: next?.mediaUrl ?? "",
                        mediaType: next?.mediaType ?? null,
                        mediaTranscript: next?.mediaTranscript ?? "",
                        mediaCaptionsUrl: next?.mediaCaptionsUrl ?? "",
                        mediaVersion: next?.mediaVersion ?? "",
                      });
                    }}
                  />
                </div>

                <div className="mt-6 flex items-center justify-between">
                  <div className="text-sm font-semibold">Alternativas</div>
                  <span className="text-xs text-muted-foreground">
                    {selected.options.length} registradas
                  </span>
                </div>

                {selected.options.length < 2 && (
                  <p className="mt-2 rounded-md border border-warning/30 bg-warning/10 px-3 py-2 text-xs text-warning-foreground">
                    Esta etapa precisa de pelo menos duas alternativas antes da revisão.
                  </p>
                )}

                <div className="mt-3 space-y-3">
                  {selected.options.map((option) => {
                    const finalNode = terminalNodeFor(option);
                    const terminal = isTerminalOption(option);
                    const destinationValue = terminal ? "FIM" : (option.nextNodeId ?? "FIM");
                    return (
                      <div
                        key={option.id}
                        className="rounded-md border border-border bg-background p-3"
                      >
                        <div className="grid gap-3 md:grid-cols-[20px_1fr_220px_auto]">
                          <GitBranch className="mt-2 h-4 w-4 text-muted-foreground" />
                          <input
                            key={`${selected.id}-${option.id}-text`}
                            className="input"
                            defaultValue={option.text}
                            onBlur={(event) =>
                              updateOptionMutation.mutate({
                                nodeId: selected.id,
                                optionId: option.id,
                                text: event.target.value.trim(),
                              })
                            }
                          />
                          <select
                            key={`${selected.id}-${option.id}-next-${destinationValue}`}
                            className="input"
                            defaultValue={destinationValue}
                            disabled={
                              createBranchMutation.isPending || updateOptionMutation.isPending
                            }
                            aria-label={`Destino da alternativa ${option.id}`}
                            onChange={(event) => {
                              const target = event.target.value;
                              if (target === CREATE_BRANCH_VALUE) {
                                setFeedbackMessage(null);
                                createBranchMutation.mutate({
                                  nodeId: selected.id,
                                  optionId: option.id,
                                });
                                return;
                              }
                              updateOptionMutation.mutate({
                                nodeId: selected.id,
                                optionId: option.id,
                                nextNodeId: target === "FIM" ? null : target,
                              });
                            }}
                          >
                            {dialogueNodes
                              .filter((node) => node.turnIndex > selected.turnIndex)
                              .map((node) => (
                                <option key={node.id} value={node.id}>
                                  Vai para etapa {nodeLabel(node)}
                                </option>
                              ))}
                            <option value="FIM">Vai para FIM</option>
                            <option disabled>──────────</option>
                            <option value={CREATE_BRANCH_VALUE}>
                              + Criar nova etapa ramificada
                            </option>
                          </select>
                          <button
                            type="button"
                            onClick={() => {
                              if (window.confirm(`Remover a alternativa ${option.id}?`)) {
                                deleteOptionMutation.mutate({
                                  nodeId: selected.id,
                                  optionId: option.id,
                                });
                              }
                            }}
                            disabled={deleteOptionMutation.isPending}
                            className="rounded-md border border-danger/25 bg-danger/5 p-2 text-danger hover:bg-danger/10 disabled:opacity-50"
                            aria-label="Remover alternativa"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>

                        <p className="mt-2 text-[11px] text-muted-foreground">
                          Use “Criar nova etapa ramificada” para gerar e abrir automaticamente a
                          próxima etapa deste caminho.
                        </p>

                        <div className="mt-3 flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
                          {Object.entries(option.competencyLevels).map(([name, value]) => (
                            <label
                              key={name}
                              className="inline-flex items-center gap-1 rounded border border-border px-2 py-1"
                            >
                              {name}
                              <input
                                className="w-12 rounded border border-border bg-card px-1 py-0.5"
                                type="number"
                                min={0}
                                max={100}
                                step={1}
                                defaultValue={value}
                                onBlur={(event) => {
                                  const nextValue = Number(event.target.value);
                                  if (
                                    !Number.isInteger(nextValue) ||
                                    nextValue < 0 ||
                                    nextValue > 100
                                  ) {
                                    event.target.value = String(value);
                                    setFeedbackMessage(
                                      `Pontuação inválida para ${name}. Use um inteiro de 0 a 100.`,
                                    );
                                    return;
                                  }
                                  setFeedbackMessage(null);
                                  updateOptionMutation.mutate({
                                    nodeId: selected.id,
                                    optionId: option.id,
                                    competencyLevels: {
                                      ...option.competencyLevels,
                                      [name]: nextValue,
                                    },
                                  });
                                }}
                              />
                            </label>
                          ))}
                        </div>

                        <label
                          className={cn(
                            "mt-3 flex max-w-2xl items-start gap-2 rounded-md border px-3 py-2",
                            option.isCritical
                              ? "border-danger/30 bg-danger/5"
                              : "border-border bg-card",
                          )}
                        >
                          <input
                            type="checkbox"
                            className="mt-0.5"
                            defaultChecked={option.isCritical}
                            aria-describedby={`${option.id}-critical-help`}
                            onChange={(event) =>
                              updateOptionMutation.mutate({
                                nodeId: selected.id,
                                optionId: option.id,
                                isCritical: event.target.checked,
                              })
                            }
                          />
                          <span>
                            <span className="block text-xs font-semibold text-foreground">
                              Resposta crítica — exige revisão humana
                            </span>
                            <span
                              id={`${option.id}-critical-help`}
                              className="mt-0.5 block text-[11px] leading-4 text-muted-foreground"
                            >
                              Marque quando esta resposta representar erro grave ou comportamento de
                              risco. A pontuação continua sendo calculada, mas o resultado exige
                              análise da equipe e não reprova automaticamente.
                            </span>
                          </span>
                        </label>

                        <MediaAttachment
                          mediaUrl={option.mediaUrl}
                          mediaType={option.mediaType}
                          mediaTranscript={option.mediaTranscript}
                          mediaCaptionsUrl={option.mediaCaptionsUrl}
                          mediaVersion={option.mediaVersion}
                          disabled={updateOptionMutation.isPending}
                          label="Anexar imagem, áudio ou vídeo à alternativa"
                          onChange={(next) =>
                            updateOptionMutation.mutate({
                              nodeId: selected.id,
                              optionId: option.id,
                              mediaUrl: next?.mediaUrl ?? "",
                              mediaType: next?.mediaType ?? null,
                        mediaTranscript: next?.mediaTranscript ?? "",
                        mediaCaptionsUrl: next?.mediaCaptionsUrl ?? "",
                        mediaVersion: next?.mediaVersion ?? "",
                            })
                          }
                        />

                        {terminal && (
                          <TerminalOutcomeCard
                            nodes={nodes}
                            rootNodeId={rootNodeId}
                            node={selected}
                            option={option}
                            competencies={competencies}
                            reportText={finalNode?.reportText ?? option.auditNote ?? ""}
                            disabled={!isEditable}
                            saving={saveTerminalReportMutation.isPending}
                            onSaveReport={(reportText) =>
                              saveTerminalReportMutation.mutate({
                                nodeId: selected.id,
                                optionId: option.id,
                                finalNodeId: finalNode?.id ?? null,
                                reportText,
                              })
                            }
                          />
                        )}
                      </div>
                    );
                  })}
                </div>

                <div className="mt-4 grid gap-2 md:grid-cols-[1fr_auto]">
                  <input
                    className="input"
                    value={draftOption}
                    onChange={(event) => {
                      setDraftOption(event.target.value);
                      setFeedbackMessage(null);
                    }}
                    placeholder="Texto da nova alternativa"
                  />
                  <button
                    type="button"
                    onClick={() => addOptionMutation.mutate()}
                    disabled={
                      !draftOption.trim() ||
                      competencies.length === 0 ||
                      selected.options.length >= 4 ||
                      addOptionMutation.isPending
                    }
                    className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Save className="h-4 w-4" />
                    {addOptionMutation.isPending ? "Salvando..." : "Salvar alternativa"}
                  </button>
                </div>
              </section>
            ) : (
              <EmptyState
                title="Nenhuma etapa encontrada"
                description="Configure o personagem para criar a etapa inicial da avaliação."
                actions={
                  <Link
                    to="/nova/personagem"
                    search={{
                      simulationId: search.simulationId,
                      versionNumber: search.versionNumber,
                    }}
                    className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
                  >
                    Configurar personagem
                  </Link>
                }
              />
            )}
          </fieldset>

          <div className="mt-8 flex flex-wrap justify-between gap-3">
            <Link
              to="/nova/personagem"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Personagem
            </Link>
            <div className="flex flex-wrap gap-2">
              <Link
                to="/nova/mapa"
                search={{
                  simulationId: search.simulationId,
                  nodeId: search.nodeId,
                  versionNumber: search.versionNumber,
                }}
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
              >
                Abrir mapa do fluxo
              </Link>
              <Link
                to="/nova/validador"
                search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
                className={cn(
                  "rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90",
                  !canReview && "pointer-events-none opacity-50",
                )}
                aria-disabled={!canReview}
              >
                Concluir edição
              </Link>
            </div>
          </div>
        </>
      )}
    </AppShell>
  );
}

const MAX_VIDEO_DURATION_SECONDS = 10 * 60;

function MediaAttachment({
  mediaUrl,
  mediaType,
  mediaTranscript,
  mediaCaptionsUrl,
  mediaVersion,
  onChange,
  disabled,
  label = "Anexar imagem, áudio ou vídeo",
}: {
  mediaUrl: string | null;
  mediaType: MediaType | null;
  mediaTranscript?: string | null;
  mediaCaptionsUrl?: string | null;
  mediaVersion?: string | null;
  onChange: (next: { mediaUrl: string; mediaType: MediaType; mediaTranscript?: string | null; mediaCaptionsUrl?: string | null; mediaVersion?: string | null } | null) => void;
  disabled?: boolean;
  label?: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleFile(file: File | undefined) {
    if (!file) return;
    if (!file.type.startsWith("image/") && !file.type.startsWith("audio/") && !file.type.startsWith("video/")) {
      setError("Apenas imagens, áudios ou vídeos são suportados.");
      return;
    }
    if (file.type.startsWith("video/")) {
      try {
        const durationSeconds = await readVideoDuration(file);
        if (durationSeconds > MAX_VIDEO_DURATION_SECONDS) {
          setError("O vídeo deve ter no máximo 10 minutos.");
          return;
        }
      } catch {
        setError("Não foi possível validar a duração do vídeo.");
        return;
      }
    }
    setError(null);
    setUploading(true);
    try {
      const result = await uploadMedia(file);
      onChange({ mediaUrl: result.url, mediaType: result.mediaType });
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : "Falha no upload da mídia.");
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  }

  return (
    <div className="mt-3 rounded-md border border-dashed border-border bg-background/60 p-3">
      <input
        ref={inputRef}
        type="file"
        accept="image/*,audio/*,video/mp4,video/webm,video/ogg,video/quicktime"
        className="hidden"
        onChange={(event) => void handleFile(event.target.files?.[0])}
      />
      {mediaUrl ? (
        <div className="space-y-2">
          <MediaPreview mediaUrl={mediaUrl} mediaType={mediaType} />
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={disabled || uploading}
              className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1.5 text-xs hover:bg-accent disabled:opacity-50"
            >
              <ImagePlus className="h-3.5 w-3.5" />
              {uploading ? "Enviando..." : "Trocar mídia"}
            </button>
            <button
              type="button"
              onClick={() => onChange(null)}
              disabled={disabled || uploading}
              className="inline-flex items-center gap-1.5 rounded-md border border-danger/25 bg-danger/5 px-2.5 py-1.5 text-xs text-danger hover:bg-danger/10 disabled:opacity-50"
            >
              <X className="h-3.5 w-3.5" />
              Remover mídia
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={disabled || uploading}
          className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1.5 text-xs hover:bg-accent disabled:opacity-50"
        >
          {mediaType === "VIDEO" ? <Film className="h-3.5 w-3.5" /> : <Music className="h-3.5 w-3.5" />}
          {uploading ? "Enviando..." : label}
        </button>
      )}
      {mediaUrl && (mediaType === "AUDIO" || mediaType === "VIDEO") && (
        <label className="mt-3 block text-xs">
          Transcrição acessível
          <textarea className="input mt-1 min-h-20" defaultValue={mediaTranscript ?? ""} onBlur={(event) => onChange({ mediaUrl, mediaType: mediaType!, mediaTranscript: event.target.value, mediaCaptionsUrl, mediaVersion })} />
        </label>
      )}
      {mediaUrl && mediaType === "VIDEO" && (
        <label className="mt-3 block text-xs">
          URL da legenda WebVTT
          <input className="input mt-1" type="url" defaultValue={mediaCaptionsUrl ?? ""} onBlur={(event) => onChange({ mediaUrl, mediaType, mediaTranscript, mediaCaptionsUrl: event.target.value, mediaVersion })} />
        </label>
      )}
      {mediaUrl && (
        <label className="mt-3 block text-xs">
          Versão da mídia
          <input className="input mt-1" defaultValue={mediaVersion ?? ""} placeholder="Gerada automaticamente quando vazia" onBlur={(event) => onChange({ mediaUrl, mediaType: mediaType!, mediaTranscript, mediaCaptionsUrl, mediaVersion: event.target.value })} />
        </label>
      )}
      {error && <p className="mt-2 text-xs text-danger">{error}</p>}
    </div>
  );
}

function readVideoDuration(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const video = document.createElement("video");
    video.preload = "metadata";
    video.onloadedmetadata = () => {
      const duration = video.duration;
      URL.revokeObjectURL(objectUrl);
      video.remove();
      if (Number.isFinite(duration) && duration > 0) {
    resolve(duration);
  } else {
    reject(new Error("Duração inválida"));
  }
    };
    video.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      video.remove();
      reject(new Error("Metadados indisponíveis"));
    };
    video.src = objectUrl;
  });
}

function MediaPreview({ mediaUrl, mediaType }: { mediaUrl: string; mediaType: MediaType | null }) {
  if (mediaType === "VIDEO") {
    return (
      <video controls preload="metadata" className="max-h-64 w-full rounded-md border border-border bg-black">
        <source src={mediaUrl} />
        Seu navegador não suporta vídeo.
      </video>
    );
  }
  if (mediaType === "AUDIO") {
    return (
      <audio controls src={mediaUrl} className="w-full">
        Seu navegador não suporta áudio.
      </audio>
    );
  }
  return (
    <img
      src={mediaUrl}
      alt="Mídia anexada"
      className="max-h-40 w-auto rounded-md border border-border object-contain"
    />
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
          to="/nova/dialogo"
          search={{
            simulationId: simulation.id,
            nodeId: undefined,
            versionNumber: simulation.versionNumber,
          }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
