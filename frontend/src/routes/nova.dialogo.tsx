import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState } from "react";
import { GitBranch, ImagePlus, Music, Plus, Save, Trash2, X } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  cloneSimulationVersionToDraft,
  createSimulationNode,
  createSimulationOption,
  deleteSimulationNode,
  deleteSimulationOption,
  getSimulationVersion,
  listSimulations,
  updateSimulationNode,
  updateSimulationOption,
  uploadMedia,
  type MediaType,
  type SimulationSummaryResponse,
  type SimulationVersionNodeResponse,
} from "@/lib/api/praxis";
import { canEditSimulationVersion, statusMeta } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";
import { defaultAnswerTimeLimitSeconds, useTenantConfig } from "@/lib/tenant-config";

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
      { name: "description", content: "Edição do diálogo usada na revisão do teste." },
    ],
  }),
  component: DialogEditor,
});

function DialogEditor() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const {
    config,
    isLoading: tenantConfigLoading,
    isError: tenantConfigError,
    error: tenantConfigQueryError,
  } = useTenantConfig();
  const answerTimeLimits = config?.answerTimeLimits ?? [];
  const [selectedId, setSelectedId] = useState<string | null>(null);
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
  const nodes = versionQuery.data?.nodes ?? [];
  const versionStatus = versionQuery.data?.status;
  const isEditable = versionStatus ? canEditSimulationVersion(versionStatus) : true;
  const selected = nodes.find((node) => node.id === selectedId) ?? nodes[0];
  const competencies = useMemo(
    () => versionQuery.data?.blueprint.competencies ?? [],
    [versionQuery.data?.blueprint.competencies],
  );
  const canReview =
    nodes.length > 0 &&
    nodes.every(
      (node) =>
        node.options.length >= 2 &&
        node.options.length <= 4 &&
        node.options.every(
          (option) =>
            option.text.trim().length > 0 && Object.keys(option.competencyLevels).length > 0,
        ),
    );
  const competencyLevels = useMemo(
    () => Object.fromEntries(competencies.map((competency) => [competency.name, 50])),
    [competencies],
  );

  useEffect(() => {
    setSelectedMessage(selected?.clientMessage ?? "");
  }, [selected?.id, selected?.clientMessage]);

  useEffect(() => {
    if (!search.nodeId || nodes.length === 0) return;

    if (nodes.some((node) => node.id === search.nodeId)) {
      setSelectedId(search.nodeId);
    }
  }, [nodes, search.nodeId]);

  const refetchVersion = async () => {
    await queryClient.refetchQueries({
      queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      type: "active",
    });
  };
  const assertEditable = () => {
    if (!isEditable) {
      throw new Error("Esta versao nao pode ser editada. Crie um rascunho antes de alterar.");
    }
  };
  const cloneDraftMutation = useMutation({
    mutationFn: () => cloneSimulationVersionToDraft(search.simulationId!, search.versionNumber!),
    onSuccess: async (draft) => {
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/dialogo",
        search: { simulationId: draft.simulationId, versionNumber: draft.newVersionNumber },
      });
    },
  });
  const addNodeMutation = useMutation({
    mutationFn: () => {
      assertEditable();
      if (!config) {
        throw new Error("A configuração da empresa ainda não foi carregada pelo sistema.");
      }
      return createSimulationNode(search.simulationId!, search.versionNumber!, {
        clientMessage: draftMessage.trim(),
        timeLimitSeconds: defaultAnswerTimeLimitSeconds(config),
      });
    },
    onSuccess: async (nodeId) => {
      setDraftMessage("");
      setSelectedId(nodeId);
      setFeedbackMessage(`Etapa ${nodeId} adicionada.`);
      await refetchVersion();
    },
  });
  const saveNodeMutation = useMutation({
    mutationFn: (node: SimulationVersionNodeResponse) => {
      assertEditable();
      return updateSimulationNode(search.simulationId!, search.versionNumber!, node.id, {
        clientMessage: node.clientMessage,
        timeLimitSeconds: node.timeLimitSeconds,
      });
    },
    onSuccess: refetchVersion,
  });
  const deleteNodeMutation = useMutation({
    mutationFn: (nodeId: string) => {
      assertEditable();
      return deleteSimulationNode(search.simulationId!, search.versionNumber!, nodeId);
    },
    onSuccess: async () => {
      setSelectedId(null);
      setFeedbackMessage("Etapa removida.");
      await refetchVersion();
    },
  });
  const addOptionMutation = useMutation({
    mutationFn: () => {
      assertEditable();
      return createSimulationOption(search.simulationId!, search.versionNumber!, selected!.id, {
        text: draftOption.trim(),
        competencyLevels,
        isCritical: false,
        nextNodeId: null,
      });
    },
    onSuccess: async () => {
      setDraftOption("");
      setFeedbackMessage("Alternativa adicionada.");
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
      mediaUrl,
      mediaType,
    }: {
      nodeId: string;
      optionId: string;
      text?: string;
      nextNodeId?: string | null;
      isCritical?: boolean;
      competencyLevels?: Record<string, number>;
      mediaUrl?: string;
      mediaType?: MediaType | null;
    }) => {
      assertEditable();
      return updateSimulationOption(search.simulationId!, search.versionNumber!, nodeId, optionId, {
        text,
        nextNodeId: nextNodeId === null ? "" : nextNodeId,
        isCritical,
        competencyLevels: levels,
        mediaUrl,
        mediaType,
      });
    },
    onSuccess: refetchVersion,
  });
  const updateNodeMediaMutation = useMutation({
    mutationFn: ({
      nodeId,
      mediaUrl,
      mediaType,
    }: {
      nodeId: string;
      mediaUrl: string;
      mediaType: MediaType | null;
    }) => {
      assertEditable();
      return updateSimulationNode(search.simulationId!, search.versionNumber!, nodeId, {
        mediaUrl,
        mediaType,
      });
    },
    onSuccess: refetchVersion,
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
    saveNodeMutation.error ??
    deleteNodeMutation.error ??
    addOptionMutation.error ??
    updateOptionMutation.error ??
    updateNodeMediaMutation.error ??
    deleteOptionMutation.error ??
    cloneDraftMutation.error;

  return (
    <AppShell>
      <WizardStepper current="revisao" unlockedThrough={canReview ? "revisao" : "cenario"} />
      <ScreenStateStrip blockedReason="o fluxo da conversa precisa existir no sistema e passar pelo validador" />
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3</div>
          <h1 className="mt-1 text-3xl font-semibold">Editar diálogo do cenário</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Ajuste as etapas e alternativas que serão avaliadas pelo validador.
          </p>
        </div>
        {versionQuery.data && <StatusBadge status={versionQuery.data.status} />}
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma versão para editar"
          description="O editor não possui fluxo da conversa local de exemplo."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : tenantConfigLoading || versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando fluxo da conversa">
          Buscando teste {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : tenantConfigError ? (
        <StateBanner tone="danger" title="Não foi possível carregar a configuração">
          {tenantConfigQueryError instanceof Error
            ? tenantConfigQueryError.message
            : "Verifique se o sistema está disponível antes de editar o fluxo."}
        </StateBanner>
      ) : versionQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar o fluxo da conversa">
          {versionQuery.error instanceof Error
            ? versionQuery.error.message
            : "Não foi possível carregar agora. Verifique sua conexão e tente novamente."}
        </StateBanner>
      ) : (
        <>
          {feedbackMessage && (
            <div className="mt-5">
              <StateBanner tone="info" title="Alteração salva">
                {feedbackMessage}
              </StateBanner>
            </div>
          )}
          {versionStatus && !isEditable && (
            <div className="mt-5">
              <StateBanner
                tone="warn"
                title={`Versao ${statusMeta[versionStatus].label.toLowerCase()} nao pode ser editada`}
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
                {versionStatus === "published"
                  ? "A versão no ar fica protegida. Crie um rascunho para editar sem afetar candidatos em andamento."
                  : "Atualize a etapa atual da versao antes de alterar o dialogo."}
              </StateBanner>
            </div>
          )}
          {mutationError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível salvar a alteração">
                {mutationError instanceof Error ? mutationError.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          <fieldset
            disabled={!isEditable}
            className="mt-5 grid gap-5 disabled:opacity-75 lg:grid-cols-[320px_minmax(0,1fr)]"
          >
            <aside className="rounded-md border border-border bg-card p-4">
              <div className="mb-3 text-sm font-semibold">Etapas salvas</div>
              <div className="space-y-2">
                {nodes.map((node) => (
                  <button
                    key={node.id}
                    type="button"
                    onClick={() => setSelectedId(node.id)}
                    className={cn(
                      "w-full rounded-md border p-3 text-left text-sm hover:bg-accent",
                      selected?.id === node.id && "border-primary bg-primary/5",
                    )}
                  >
                    <div className="font-medium">{node.id}</div>
                    <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                      {node.clientMessage}
                    </div>
                  </button>
                ))}
              </div>
              <label className="mt-4 block">
                <span className="mb-1 block text-xs text-muted-foreground">
                  Nova fala do cliente
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
                {addNodeMutation.isPending ? "Adicionando..." : "Adicionar etapa"}
              </button>
            </aside>

            {selected ? (
              <section className="rounded-md border border-border bg-card p-5">
                <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">{selected.id}</div>
                    <h2 className="text-xl font-semibold">Etapa {selected.turnIndex}</h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      if (
                        window.confirm(
                          `Remover a etapa ${selected.id}? Esta ação não pode ser desfeita.`,
                        )
                      ) {
                        setFeedbackMessage(null);
                        deleteNodeMutation.mutate(selected.id);
                      }
                    }}
                    disabled={
                      selected.id === versionQuery.data?.blueprint.rootNodeId ||
                      deleteNodeMutation.isPending
                    }
                    className="inline-flex items-center gap-2 rounded-md border border-danger/25 bg-danger/5 px-3 py-2 text-xs text-danger hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                    {deleteNodeMutation.isPending ? "Removendo..." : "Remover etapa"}
                  </button>
                </div>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                    Mensagem do cliente
                  </span>
                  <textarea
                    key={`${selected.id}-message`}
                    className="input min-h-24"
                    value={selectedMessage}
                    onChange={(event) => setSelectedMessage(event.target.value)}
                  />
                </label>
                <div className="mt-3 flex justify-end">
                  <button
                    type="button"
                    onClick={() => {
                      setFeedbackMessage(null);
                      saveNodeMutation.mutate({ ...selected, clientMessage: selectedMessage });
                    }}
                    disabled={
                      saveNodeMutation.isPending ||
                      selectedMessage.trim() === selected.clientMessage.trim()
                    }
                    className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Save className="h-4 w-4" />
                    {saveNodeMutation.isPending ? "Salvando..." : "Salvar etapa"}
                  </button>
                </div>
                <label className="mt-3 block max-w-40">
                  <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                    Tempo
                  </span>
                  <select
                    key={`${selected.id}-time`}
                    className="input"
                    defaultValue={selected.timeLimitSeconds ?? "none"}
                    onChange={(event) =>
                      saveNodeMutation.mutate({
                        ...selected,
                        timeLimitSeconds:
                          event.target.value === "none" ? null : Number(event.target.value),
                      })
                    }
                  >
                    {answerTimeLimits.map((limit) => (
                      <option key={limit.value} value={limit.value === "0" ? "none" : limit.value}>
                        {limit.label}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="mt-3">
                  <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                    Mídia da etapa (opcional)
                  </span>
                  <MediaAttachment
                    mediaUrl={selected.mediaUrl}
                    mediaType={selected.mediaType}
                    disabled={updateNodeMediaMutation.isPending}
                    onChange={(next) => {
                      setFeedbackMessage(null);
                      updateNodeMediaMutation.mutate({
                        nodeId: selected.id,
                        mediaUrl: next ? next.mediaUrl : "",
                        mediaType: next ? next.mediaType : null,
                      });
                    }}
                  />
                </div>
                <div className="mt-5 flex items-center justify-between">
                  <div className="text-sm font-semibold">Alternativas</div>
                  <span className="text-xs text-muted-foreground">
                    {selected.options.length} registradas
                  </span>
                </div>
                {selected.options.length < 2 && (
                  <p className="mt-2 rounded-md border border-warning/30 bg-warning/10 px-3 py-2 text-xs text-warning-foreground">
                    Esta etapa precisa de pelo menos 2 alternativas antes da revisão.
                  </p>
                )}
                <div className="mt-3 space-y-3">
                  {selected.options.map((option) => (
                    <div
                      key={option.id}
                      className="rounded-md border border-border bg-background p-3"
                    >
                      <div className="grid gap-3 md:grid-cols-[20px_1fr_160px_auto]">
                        <GitBranch className="mt-2 h-4 w-4 text-muted-foreground" />
                        <input
                          key={`${selected.id}-${option.id}-text`}
                          className="input"
                          defaultValue={option.text}
                          onBlur={(event) =>
                            updateOptionMutation.mutate({
                              nodeId: selected.id,
                              optionId: option.id,
                              text: event.target.value,
                            })
                          }
                        />
                        <select
                          key={`${selected.id}-${option.id}-next`}
                          className="input"
                          defaultValue={option.nextNodeId ?? "FIM"}
                          onChange={(event) =>
                            updateOptionMutation.mutate({
                              nodeId: selected.id,
                              optionId: option.id,
                              nextNodeId: event.target.value === "FIM" ? null : event.target.value,
                            })
                          }
                        >
                          {nodes
                            .filter((node) => node.turnIndex > selected.turnIndex)
                            .map((node) => (
                              <option key={node.id} value={node.id}>
                                Vai para {node.id}
                              </option>
                            ))}
                          <option value="FIM">Vai para FIM</option>
                        </select>
                        <button
                          type="button"
                          onClick={() => {
                            if (
                              window.confirm(
                                `Remover a alternativa ${option.id} da etapa ${selected.id}?`,
                              )
                            ) {
                              setFeedbackMessage(null);
                              deleteOptionMutation.mutate({
                                nodeId: selected.id,
                                optionId: option.id,
                              });
                            }
                          }}
                          disabled={deleteOptionMutation.isPending}
                          className="rounded-md border border-danger/25 bg-danger/5 p-2 text-danger hover:bg-danger/10"
                          aria-label="Remover alternativa"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      <div className="mt-3 flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
                        {Object.entries(option.competencyLevels).map(([name, value]) => (
                          <label
                            key={name}
                            className="inline-flex items-center gap-1 rounded border border-border px-2 py-1"
                          >
                            {name}
                            <input
                              className="w-12 rounded border border-border bg-card px-1 py-0.5"
                              type="text"
                              inputMode="numeric"
                              pattern="[0-9]*"
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
                                    `Peso inválido para ${name}. Use um número inteiro de 0 a 100.`,
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
                        <label className="inline-flex items-center gap-1 rounded border border-danger/30 px-2 py-1 text-danger">
                          <input
                            type="checkbox"
                            defaultChecked={option.isCritical}
                            onChange={(event) =>
                              updateOptionMutation.mutate({
                                nodeId: selected.id,
                                optionId: option.id,
                                isCritical: event.target.checked,
                              })
                            }
                          />
                          crítica
                        </label>
                      </div>
                      <MediaAttachment
                        mediaUrl={option.mediaUrl}
                        mediaType={option.mediaType}
                        disabled={updateOptionMutation.isPending}
                        label="Anexar imagem ou áudio à alternativa"
                        onChange={(next) => {
                          setFeedbackMessage(null);
                          updateOptionMutation.mutate({
                            nodeId: selected.id,
                            optionId: option.id,
                            mediaUrl: next ? next.mediaUrl : "",
                            mediaType: next ? next.mediaType : null,
                          });
                        }}
                      />
                    </div>
                  ))}
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
                    onClick={() => {
                      setFeedbackMessage(null);
                      addOptionMutation.mutate();
                    }}
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
                description="Crie a primeira etapa para iniciar o fluxo da conversa."
              />
            )}
          </fieldset>
          <div className="mt-8 flex justify-between">
            <Link
              to="/nova/validador"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar ao validador
            </Link>
            {canReview ? (
              <Link
                to="/nova/validador"
                search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
                className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Concluir edição
              </Link>
            ) : (
              <button
                type="button"
                disabled
                title="Cada etapa precisa ter de 2 a 4 alternativas com critérios de pontuação antes da revisão"
                className="cursor-not-allowed rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground opacity-50"
              >
                Concluir edição
              </button>
            )}
          </div>
        </>
      )}
    </AppShell>
  );
}

export function MediaPreview({
  mediaUrl,
  mediaType,
  className,
}: {
  mediaUrl: string;
  mediaType: MediaType | null;
  className?: string;
}) {
  if (mediaType === "AUDIO") {
    return (
      <audio controls src={mediaUrl} className={cn("w-full", className)}>
        Seu navegador não suporta áudio.
      </audio>
    );
  }
  return (
    <img
      src={mediaUrl}
      alt="Mídia anexada"
      className={cn("max-h-40 w-auto rounded-md border border-border object-contain", className)}
    />
  );
}

function MediaAttachment({
  mediaUrl,
  mediaType,
  onChange,
  disabled,
  label = "Anexar imagem ou áudio",
}: {
  mediaUrl: string | null;
  mediaType: MediaType | null;
  onChange: (next: { mediaUrl: string; mediaType: MediaType } | null) => void;
  disabled?: boolean;
  label?: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFile = async (file: File | undefined) => {
    if (!file) return;
    if (!file.type.startsWith("image/") && !file.type.startsWith("audio/")) {
      setError("Apenas imagens ou áudios são suportados.");
      return;
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
  };

  return (
    <div className="mt-3 rounded-md border border-dashed border-border bg-background/60 p-3">
      <input
        ref={inputRef}
        type="file"
        accept="image/*,audio/*"
        className="hidden"
        onChange={(event) => handleFile(event.target.files?.[0])}
      />
      {mediaUrl ? (
        <div className="space-y-2">
          <MediaPreview mediaUrl={mediaUrl} mediaType={mediaType} />
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={disabled || uploading}
              className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1.5 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ImagePlus className="h-3.5 w-3.5" />
              {uploading ? "Enviando..." : "Trocar mídia"}
            </button>
            <button
              type="button"
              onClick={() => onChange(null)}
              disabled={disabled || uploading}
              className="inline-flex items-center gap-1.5 rounded-md border border-danger/25 bg-danger/5 px-2.5 py-1.5 text-xs text-danger hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50"
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
          className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1.5 text-xs hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Music className="h-3.5 w-3.5" />
          {uploading ? "Enviando..." : label}
        </button>
      )}
      {error && <p className="mt-2 text-xs text-danger">{error}</p>}
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
          to="/nova/dialogo"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
