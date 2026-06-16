import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { GitBranch, Plus, Save, Trash2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, NextStepContract, ScreenStateStrip, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  createSimulationNode,
  createSimulationOption,
  deleteSimulationNode,
  deleteSimulationOption,
  getSimulationVersion,
  listSimulations,
  updateSimulationNode,
  updateSimulationOption,
  type SimulationSummaryResponse,
  type SimulationVersionNodeResponse,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/nova/dialogo")({
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
      { title: "Editor de Simulacao - Praxis" },
      { name: "description", content: "Editor conectado ao grafo persistido no backend." },
    ],
  }),
  component: DialogEditor,
});

function DialogEditor() {
  const search = Route.useSearch();
  const queryClient = useQueryClient();
  const hasContext = Boolean(search.simulationId && search.versionNumber);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [draftMessage, setDraftMessage] = useState("");
  const [draftOption, setDraftOption] = useState("");
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
  const selected = nodes.find((node) => node.id === selectedId) ?? nodes[0];
  const competencies = versionQuery.data?.blueprint.competencies ?? [];
  const competencyLevels = useMemo(
    () => Object.fromEntries(competencies.map((competency) => [competency.name, 50])),
    [competencies],
  );

  const invalidateVersion = async () => {
    await queryClient.invalidateQueries({
      queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    });
  };
  const addNodeMutation = useMutation({
    mutationFn: () =>
      createSimulationNode(search.simulationId!, search.versionNumber!, {
        clientMessage: draftMessage.trim(),
        timeLimitSeconds: 45,
      }),
    onSuccess: async (nodeId) => {
      setDraftMessage("");
      setSelectedId(nodeId);
      await invalidateVersion();
    },
  });
  const saveNodeMutation = useMutation({
    mutationFn: (node: SimulationVersionNodeResponse) =>
      updateSimulationNode(search.simulationId!, search.versionNumber!, node.id, {
        clientMessage: node.clientMessage,
        timeLimitSeconds: node.timeLimitSeconds,
      }),
    onSuccess: invalidateVersion,
  });
  const deleteNodeMutation = useMutation({
    mutationFn: (nodeId: string) => deleteSimulationNode(search.simulationId!, search.versionNumber!, nodeId),
    onSuccess: async () => {
      setSelectedId(null);
      await invalidateVersion();
    },
  });
  const addOptionMutation = useMutation({
    mutationFn: () =>
      createSimulationOption(search.simulationId!, search.versionNumber!, selected!.id, {
        text: draftOption.trim(),
        competencyLevels,
        isCritical: false,
        nextNodeId: null,
      }),
    onSuccess: async () => {
      setDraftOption("");
      await invalidateVersion();
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
    }: {
      nodeId: string;
      optionId: string;
      text?: string;
      nextNodeId?: string | null;
      isCritical?: boolean;
      competencyLevels?: Record<string, number>;
    }) =>
      updateSimulationOption(search.simulationId!, search.versionNumber!, nodeId, optionId, {
        text,
        nextNodeId: nextNodeId === null ? "" : nextNodeId,
        isCritical,
        competencyLevels: levels,
      }),
    onSuccess: invalidateVersion,
  });
  const deleteOptionMutation = useMutation({
    mutationFn: ({ nodeId, optionId }: { nodeId: string; optionId: string }) =>
      deleteSimulationOption(search.simulationId!, search.versionNumber!, nodeId, optionId),
    onSuccess: invalidateVersion,
  });

  return (
    <AppShell>
      <WizardStepper current="dialogo" />
      <ScreenStateStrip blockedReason="grafo precisa existir no backend e passar pelo validador" />
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-xs uppercase text-primary">Passo 3</div>
          <h1 className="mt-1 text-3xl font-semibold">Editor de dialogo</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Nos e alternativas sao lidos e gravados pela API administrativa.
          </p>
        </div>
        {versionQuery.data && <StatusBadge status={versionQuery.data.status} />}
      </div>

      {!hasContext ? (
        <EmptyState
          title="Selecione uma versao para editar"
          description="O editor nao possui grafo local de exemplo."
          actions={
            <SimulationLinks loading={simulationsQuery.isLoading} simulations={simulationsQuery.data ?? []} />
          }
        />
      ) : versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando grafo">
          Buscando simulacao {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : versionQuery.isError ? (
        <StateBanner tone="danger" title="Nao foi possivel carregar o grafo">
          {versionQuery.error instanceof Error ? versionQuery.error.message : "Verifique a API."}
        </StateBanner>
      ) : (
        <>
          <NextStepContract
            primary="Validar qualidade quando o grafo estiver completo."
            secondary="Voltar a personagem ou blueprint continua permitido antes de publicar."
            versionRule="Depois de publicar, editar cria nova versao."
            lockedAfter="Versao publicada nao altera tentativas em andamento."
          />
          <div className="mt-5 grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
            <aside className="rounded-md border border-border bg-card p-4">
              <div className="mb-3 text-sm font-semibold">Nos persistidos</div>
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
                <span className="mb-1 block text-xs text-muted-foreground">Nova fala do cliente</span>
                <textarea className="input min-h-20" value={draftMessage} onChange={(event) => setDraftMessage(event.target.value)} />
              </label>
              <button
                type="button"
                onClick={() => addNodeMutation.mutate()}
                disabled={!draftMessage.trim() || addNodeMutation.isPending}
                className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Plus className="h-4 w-4" />
                Adicionar no
              </button>
            </aside>

            {selected ? (
              <section className="rounded-md border border-border bg-card p-5">
                <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">{selected.id}</div>
                    <h2 className="text-xl font-semibold">Turno {selected.turnIndex}</h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => deleteNodeMutation.mutate(selected.id)}
                    disabled={selected.id === versionQuery.data?.blueprint.rootNodeId || deleteNodeMutation.isPending}
                    className="inline-flex items-center gap-2 rounded-md border border-danger/25 bg-danger/5 px-3 py-2 text-xs text-danger hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                    Remover no
                  </button>
                </div>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-medium text-muted-foreground">Mensagem do cliente</span>
                  <textarea
                    key={`${selected.id}-message`}
                    className="input min-h-24"
                    defaultValue={selected.clientMessage}
                    onBlur={(event) =>
                      saveNodeMutation.mutate({ ...selected, clientMessage: event.target.value })
                    }
                  />
                </label>
                <label className="mt-3 block max-w-40">
                  <span className="mb-1.5 block text-xs font-medium text-muted-foreground">Tempo</span>
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
                    <option value="none">Sem limite</option>
                    <option value="30">30 s</option>
                    <option value="45">45 s</option>
                    <option value="60">60 s</option>
                  </select>
                </label>
                <div className="mt-5 flex items-center justify-between">
                  <div className="text-sm font-semibold">Alternativas</div>
                  <span className="text-xs text-muted-foreground">{selected.options.length} registradas</span>
                </div>
                <div className="mt-3 space-y-3">
                  {selected.options.map((option) => (
                    <div key={option.id} className="rounded-md border border-border bg-background p-3">
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
                            .filter((node) => node.id !== selected.id)
                            .map((node) => (
                              <option key={node.id} value={node.id}>
                                Vai para {node.id}
                              </option>
                            ))}
                          <option value="FIM">Vai para FIM</option>
                        </select>
                        <button
                          type="button"
                          onClick={() => deleteOptionMutation.mutate({ nodeId: selected.id, optionId: option.id })}
                          className="rounded-md border border-danger/25 bg-danger/5 p-2 text-danger hover:bg-danger/10"
                          aria-label="Remover alternativa"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                      <div className="mt-3 flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
                        {Object.entries(option.competencyLevels).map(([name, value]) => (
                          <label key={name} className="inline-flex items-center gap-1 rounded border border-border px-2 py-1">
                            {name}
                            <input
                              className="w-12 rounded border border-border bg-card px-1 py-0.5"
                              type="number"
                              min={0}
                              max={100}
                              defaultValue={value}
                              onBlur={(event) =>
                                updateOptionMutation.mutate({
                                  nodeId: selected.id,
                                  optionId: option.id,
                                  competencyLevels: {
                                    ...option.competencyLevels,
                                    [name]: Number(event.target.value),
                                  },
                                })
                              }
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
                          critica
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="mt-4 grid gap-2 md:grid-cols-[1fr_auto]">
                  <input className="input" value={draftOption} onChange={(event) => setDraftOption(event.target.value)} placeholder="Texto da nova alternativa" />
                  <button
                    type="button"
                    onClick={() => addOptionMutation.mutate()}
                    disabled={!draftOption.trim() || competencies.length === 0 || addOptionMutation.isPending}
                    className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Save className="h-4 w-4" />
                    Salvar alternativa
                  </button>
                </div>
              </section>
            ) : (
              <EmptyState title="Nenhum no encontrado" description="Crie o primeiro no para iniciar o grafo persistido." />
            )}
          </div>
          <div className="mt-8 flex justify-between">
            <Link
              to="/nova/personagem"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar
            </Link>
            <Link
              to="/nova/validador"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Validar qualidade
            </Link>
          </div>
        </>
      )}
    </AppShell>
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
