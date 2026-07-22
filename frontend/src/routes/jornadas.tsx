import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  Archive,
  CheckCircle2,
  Edit3,
  Plus,
  RefreshCw,
  Save,
  Send,
  Trash2,
  Workflow,
  X,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, SkeletonRows, StateBanner } from "@/components/praxis-ui";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  addAssessmentJourneyStep,
  archiveAssessmentJourney,
  createAssessmentJourney,
  deleteAssessmentJourneyStep,
  getAssessmentJourney,
  listAssessmentJourneys,
  listSimulations,
  publishAssessmentJourney,
  updateAssessmentJourney,
  updateAssessmentJourneyStep,
  type AssessmentJourneyDetailResponse,
  type AssessmentJourneyStatus,
  type JourneyStepResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/jornadas")({
  head: () => ({
    meta: [
      { title: "Jornadas de avaliação - Práxis" },
      {
        name: "description",
        content: "Monte, ordene e publique sequências de avaliações.",
      },
    ],
  }),
  component: AssessmentJourneysPage,
});

const DEFAULT_SEQUENCE = "principal";

type JourneyConfirmation =
  | { action: "publish"; journeyId: string; journeyName: string }
  | { action: "archive"; journeyId: string; journeyName: string }
  | {
      action: "remove-step";
      journeyId: string;
      journeyName: string;
      stepId: number;
      simulationName: string;
    };

function AssessmentJourneysPage() {
  const queryClient = useQueryClient();
  const [selectedJourneyId, setSelectedJourneyId] = useState<string | null>(null);
  const [journeyName, setJourneyName] = useState("");
  const [journeyDescription, setJourneyDescription] = useState("");
  const [selectedSimulationId, setSelectedSimulationId] = useState("");
  const [required, setRequired] = useState(true);
  const [sequenceKey, setSequenceKey] = useState(DEFAULT_SEQUENCE);
  const [confirmation, setConfirmation] = useState<JourneyConfirmation | null>(null);

  const journeysQuery = useQuery({
    queryKey: ["assessment-journeys"],
    queryFn: listAssessmentJourneys,
    retry: false,
  });
  const selectedJourneyQuery = useQuery({
    queryKey: ["assessment-journey", selectedJourneyId],
    queryFn: () => getAssessmentJourney(selectedJourneyId!),
    enabled: Boolean(selectedJourneyId),
    retry: false,
  });
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });

  const journeys = journeysQuery.data ?? [];
  const selectedJourney = selectedJourneyQuery.data ?? null;
  const liveSimulations = useMemo(
    () =>
      (simulationsQuery.data ?? []).filter(
        (simulation) =>
          simulation.status === "published" || simulation.livePublishedVersionNumber != null,
      ),
    [simulationsQuery.data],
  );

  const createMutation = useMutation({
    mutationFn: createAssessmentJourney,
    onSuccess: async (journey) => {
      setJourneyName("");
      setJourneyDescription("");
      setSelectedJourneyId(journey.id);
      await queryClient.invalidateQueries({ queryKey: ["assessment-journeys"] });
    },
  });
  const addStepMutation = useMutation({
    mutationFn: () =>
      addAssessmentJourneyStep(selectedJourneyId!, {
        simulationId: selectedSimulationId,
        sequenceKey: sequenceKey.trim() || DEFAULT_SEQUENCE,
        required,
      }),
    onSuccess: async (journey) => {
      setSelectedSimulationId("");
      setSelectedJourneyId(journey.id);
      await invalidateJourney(queryClient, journey.id);
    },
  });
  const publishMutation = useMutation({
    mutationFn: publishAssessmentJourney,
    onSuccess: async (journey) => {
      setConfirmation(null);
      await invalidateJourney(queryClient, journey.id);
    },
  });
  const archiveMutation = useMutation({
    mutationFn: archiveAssessmentJourney,
    onSuccess: async (journey) => {
      setConfirmation(null);
      await invalidateJourney(queryClient, journey.id);
    },
  });
  const removeStepMutation = useMutation({
    mutationFn: ({ journeyId, stepId }: { journeyId: string; stepId: number }) =>
      deleteAssessmentJourneyStep(journeyId, stepId),
    onSuccess: async (_journey, variables) => {
      setConfirmation(null);
      await invalidateJourney(queryClient, variables.journeyId);
    },
  });
  const reorderStepMutation = useMutation({
    mutationFn: ({ step, direction }: { step: JourneyStepResponse; direction: -1 | 1 }) =>
      updateAssessmentJourneyStep(selectedJourneyId!, step.id, {
        orderIndex: step.orderIndex + direction,
      }),
    onSuccess: async () => invalidateJourney(queryClient, selectedJourneyId),
  });

  const mutationError = createMutation.error ?? addStepMutation.error ?? reorderStepMutation.error;
  const confirmationPending =
    confirmation?.action === "publish"
      ? publishMutation.isPending
      : confirmation?.action === "archive"
        ? archiveMutation.isPending
        : confirmation?.action === "remove-step"
          ? removeStepMutation.isPending
          : false;
  const confirmationError =
    confirmation?.action === "publish"
      ? publishMutation.error
      : confirmation?.action === "archive"
        ? archiveMutation.error
        : confirmation?.action === "remove-step"
          ? removeStepMutation.error
          : null;

  function createJourney() {
    if (!journeyName.trim()) return;
    createMutation.mutate({
      name: journeyName.trim(),
      description: journeyDescription.trim() || null,
    });
  }

  function addStep() {
    if (!selectedJourneyId || !selectedSimulationId || !sequenceKey.trim()) return;
    addStepMutation.mutate();
  }

  function confirmJourneyAction() {
    if (!confirmation) return;
    if (confirmation.action === "publish") {
      publishMutation.mutate(confirmation.journeyId);
      return;
    }
    if (confirmation.action === "archive") {
      archiveMutation.mutate(confirmation.journeyId);
      return;
    }
    removeStepMutation.mutate({
      journeyId: confirmation.journeyId,
      stepId: confirmation.stepId,
    });
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase text-primary">
              Jornadas de avaliação
            </div>
            <h1 className="mt-1 text-3xl font-semibold">Composição das jornadas</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
              Organize avaliações publicadas em sequências. Convites, validade e acompanhamento
              ficam centralizados em Participações.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button asChild className="gap-2">
              <Link to="/participacoes/jornada">
                <Send className="h-4 w-4" />
                Criar convite
              </Link>
            </Button>
            <Button
              type="button"
              variant="outline"
              className="gap-2 bg-card"
              onClick={() => {
                void journeysQuery.refetch();
                void selectedJourneyQuery.refetch();
                void simulationsQuery.refetch();
              }}
            >
              <RefreshCw className="h-4 w-4" />
              Atualizar
            </Button>
          </div>
        </header>

        {mutationError && (
          <StateBanner tone="danger" title="Não foi possível concluir a ação">
            {mutationError instanceof Error ? mutationError.message : "Tente novamente."}
          </StateBanner>
        )}

        <div className="grid gap-5 lg:grid-cols-[300px_minmax(0,1fr)]">
          <aside className="space-y-5">
            <section className="rounded-xl border border-border bg-card p-4">
              <h2 className="text-sm font-semibold">Nova jornada</h2>
              <div className="mt-4 space-y-3">
                <input
                  className="input w-full"
                  placeholder="Ex.: Processo Trainee 2026"
                  value={journeyName}
                  onChange={(event) => setJourneyName(event.target.value)}
                />
                <textarea
                  className="input min-h-24 w-full resize-y"
                  placeholder="Descrição opcional"
                  value={journeyDescription}
                  onChange={(event) => setJourneyDescription(event.target.value)}
                />
                <Button
                  type="button"
                  className="w-full gap-2"
                  disabled={!journeyName.trim() || createMutation.isPending}
                  onClick={createJourney}
                >
                  <Plus className="h-4 w-4" />
                  Criar rascunho
                </Button>
              </div>
            </section>

            <section className="overflow-hidden rounded-xl border border-border bg-card">
              <div className="border-b border-border p-4">
                <h2 className="text-sm font-semibold">Jornadas</h2>
              </div>
              {journeysQuery.isLoading ? (
                <div className="p-4">
                  <SkeletonRows rows={4} />
                </div>
              ) : journeys.length === 0 ? (
                <div className="p-4 text-sm text-muted-foreground">Nenhuma jornada criada.</div>
              ) : (
                <div className="divide-y divide-border">
                  {journeys.map((journey) => (
                    <button
                      key={journey.id}
                      type="button"
                      onClick={() => setSelectedJourneyId(journey.id)}
                      className={cn(
                        "block w-full px-4 py-3 text-left hover:bg-accent",
                        selectedJourneyId === journey.id && "bg-accent",
                      )}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="truncate text-sm font-medium">{journey.name}</div>
                          <div className="mt-1 text-xs text-muted-foreground">
                            {journey.stepCount} avaliação(ões) · {journey.sequenceCount}{" "}
                            sequência(s)
                          </div>
                        </div>
                        <JourneyStatusBadge status={journey.status} />
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </section>
          </aside>

          <section className="space-y-5">
            {!selectedJourneyId ? (
              <EmptyState
                title="Selecione uma jornada"
                description="Crie ou escolha uma jornada para montar sua sequência de avaliações."
              />
            ) : selectedJourneyQuery.isLoading ? (
              <div className="rounded-xl border border-border bg-card p-5">
                <SkeletonRows rows={6} />
              </div>
            ) : selectedJourneyQuery.isError || !selectedJourney ? (
              <StateBanner tone="danger" title="Não foi possível carregar a jornada">
                {selectedJourneyQuery.error instanceof Error
                  ? selectedJourneyQuery.error.message
                  : "Tente novamente."}
              </StateBanner>
            ) : (
              <>
                <JourneyComposer
                  journey={selectedJourney}
                  simulations={liveSimulations}
                  selectedSimulationId={selectedSimulationId}
                  required={required}
                  sequenceKey={sequenceKey}
                  onSimulationChange={setSelectedSimulationId}
                  onRequiredChange={setRequired}
                  onSequenceKeyChange={setSequenceKey}
                  onAddStep={addStep}
                  addPending={addStepMutation.isPending}
                  onPublish={() =>
                    setConfirmation({
                      action: "publish",
                      journeyId: selectedJourney.id,
                      journeyName: selectedJourney.name,
                    })
                  }
                  publishPending={publishMutation.isPending}
                  onArchive={() =>
                    setConfirmation({
                      action: "archive",
                      journeyId: selectedJourney.id,
                      journeyName: selectedJourney.name,
                    })
                  }
                  archivePending={archiveMutation.isPending}
                  onRemoveStep={(step) =>
                    setConfirmation({
                      action: "remove-step",
                      journeyId: selectedJourney.id,
                      journeyName: selectedJourney.name,
                      stepId: step.id,
                      simulationName: step.simulationName,
                    })
                  }
                  onMoveStep={(step, direction) => reorderStepMutation.mutate({ step, direction })}
                />
                <ParticipationOwnerCard published={selectedJourney.status === "published"} />
              </>
            )}
          </section>
        </div>
      </main>
      <JourneyConfirmationDialog
        confirmation={confirmation}
        pending={confirmationPending}
        error={confirmationError}
        onCancel={() => setConfirmation(null)}
        onConfirm={confirmJourneyAction}
      />
    </AppShell>
  );
}

function JourneyComposer({
  journey,
  simulations,
  selectedSimulationId,
  required,
  sequenceKey,
  onSimulationChange,
  onRequiredChange,
  onSequenceKeyChange,
  onAddStep,
  addPending,
  onPublish,
  publishPending,
  onArchive,
  archivePending,
  onRemoveStep,
  onMoveStep,
}: {
  journey: AssessmentJourneyDetailResponse;
  simulations: SimulationSummaryResponse[];
  selectedSimulationId: string;
  required: boolean;
  sequenceKey: string;
  onSimulationChange: (value: string) => void;
  onRequiredChange: (value: boolean) => void;
  onSequenceKeyChange: (value: string) => void;
  onAddStep: () => void;
  addPending: boolean;
  onPublish: () => void;
  publishPending: boolean;
  onArchive: () => void;
  archivePending: boolean;
  onRemoveStep: (step: JourneyStepResponse) => void;
  onMoveStep: (step: JourneyStepResponse, direction: -1 | 1) => void;
}) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState(journey.name);
  const [editDescription, setEditDescription] = useState(journey.description ?? "");

  useEffect(() => {
    setEditing(false);
    setEditName(journey.name);
    setEditDescription(journey.description ?? "");
  }, [journey.id, journey.name, journey.description]);

  const updateMutation = useMutation({
    mutationFn: () =>
      updateAssessmentJourney(journey.id, {
        name: editName.trim(),
        description: editDescription.trim() || null,
      }),
    onSuccess: async (updatedJourney) => {
      setEditing(false);
      await invalidateJourney(queryClient, updatedJourney.id);
    },
  });

  const sequences = journey.sequences;
  const totalSteps = sequences.reduce((total, sequence) => total + sequence.steps.length, 0);
  const canEdit = journey.status !== "archived";
  const canPublish = journey.status === "draft" && totalSteps > 0;
  const normalizedSequence = sequenceKey.trim() || DEFAULT_SEQUENCE;
  const simulationsInSequence = new Set(
    sequences
      .find((sequence) => sequence.sequenceKey === normalizedSequence)
      ?.steps.map((step) => step.simulationId) ?? [],
  );
  const availableSimulations = simulations.filter(
    (simulation) => !simulationsInSequence.has(simulation.id),
  );

  return (
    <section className="rounded-xl border border-border bg-card">
      <header className="flex flex-col gap-3 border-b border-border p-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <Workflow className="h-4 w-4 text-primary" />
            <h2 className="text-xl font-semibold">{journey.name}</h2>
            <JourneyStatusBadge status={journey.status} />
          </div>
          {journey.description && (
            <p className="mt-1 text-sm text-muted-foreground">{journey.description}</p>
          )}
          <p className="mt-2 text-xs text-muted-foreground">
            {totalSteps} avaliação(ões) em {sequences.length} sequência(s)
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {canEdit && (
            <Button variant="outline" size="sm" onClick={() => setEditing((current) => !current)}>
              <Edit3 className="mr-1.5 h-4 w-4" />
              {editing ? "Fechar edição" : "Editar"}
            </Button>
          )}
          <Button
            variant="outline"
            size="sm"
            disabled={!canPublish || publishPending}
            onClick={onPublish}
          >
            <CheckCircle2 className="mr-1.5 h-4 w-4" />
            Publicar
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={!canEdit || archivePending}
            onClick={onArchive}
          >
            <Archive className="mr-1.5 h-4 w-4" />
            Arquivar
          </Button>
        </div>
      </header>

      {editing && canEdit && (
        <div className="border-b border-border bg-muted/20 p-5">
          <div className="grid gap-3 lg:grid-cols-[1fr_1fr_auto]">
            <input
              className="input"
              value={editName}
              onChange={(event) => setEditName(event.target.value)}
            />
            <textarea
              className="input min-h-10"
              value={editDescription}
              onChange={(event) => setEditDescription(event.target.value)}
            />
            <div className="flex gap-2">
              <Button
                disabled={!editName.trim() || updateMutation.isPending}
                onClick={() => updateMutation.mutate()}
              >
                <Save className="mr-1.5 h-4 w-4" />
                Salvar
              </Button>
              <Button
                variant="outline"
                disabled={updateMutation.isPending}
                onClick={() => setEditing(false)}
              >
                <X className="mr-1.5 h-4 w-4" />
                Cancelar
              </Button>
            </div>
          </div>
        </div>
      )}

      {canEdit && (
        <div className="border-b border-border p-5">
          {simulations.length === 0 ? (
            <EmptyState
              title="Nenhuma avaliação publicada"
              description="Publique uma avaliação antes de adicioná-la à jornada."
              actions={
                <Button asChild>
                  <Link to="/avaliacoes">Abrir avaliações</Link>
                </Button>
              }
            />
          ) : (
            <div className="grid gap-3 lg:grid-cols-[1fr_220px_150px_130px]">
              <select
                className="input"
                value={selectedSimulationId}
                onChange={(event) => onSimulationChange(event.target.value)}
              >
                <option value="">Selecione uma avaliação</option>
                {availableSimulations.map((simulation) => (
                  <option key={simulation.id} value={simulation.id}>
                    {simulation.name} · v
                    {simulation.livePublishedVersionNumber ?? simulation.versionNumber}
                  </option>
                ))}
              </select>
              <input
                className="input"
                list="journey-sequences"
                value={sequenceKey}
                onChange={(event) => onSequenceKeyChange(event.target.value)}
                placeholder="Sequência"
              />
              <datalist id="journey-sequences">
                {sequences.map((sequence) => (
                  <option key={sequence.sequenceKey} value={sequence.sequenceKey} />
                ))}
              </datalist>
              <label className="inline-flex items-center gap-2 rounded-md border border-border px-3 text-sm">
                <input
                  type="checkbox"
                  checked={required}
                  onChange={(event) => onRequiredChange(event.target.checked)}
                />
                Obrigatória
              </label>
              <Button
                disabled={!selectedSimulationId || !sequenceKey.trim() || addPending}
                onClick={onAddStep}
              >
                <Plus className="mr-1.5 h-4 w-4" />
                Adicionar
              </Button>
            </div>
          )}
        </div>
      )}

      {totalSteps === 0 ? (
        <div className="p-5 text-sm text-muted-foreground">
          Adicione pelo menos uma avaliação para publicar a jornada.
        </div>
      ) : (
        <div className="divide-y divide-border">
          {sequences.map((sequence) => (
            <div key={sequence.sequenceKey} className="p-5">
              <div className="mb-3 flex items-center gap-2">
                <span className="rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                  {sequence.sequenceKey}
                </span>
                <span className="text-xs text-muted-foreground">
                  {sequence.steps.length} etapa(s)
                </span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[680px] text-left text-sm">
                  <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3">Ordem</th>
                      <th className="px-4 py-3">Avaliação</th>
                      <th className="px-4 py-3">Versão</th>
                      <th className="px-4 py-3">Obrigatória</th>
                      <th className="px-4 py-3 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sequence.steps.map((step, index) => (
                      <tr key={step.id} className="border-b border-border last:border-0">
                        <td className="px-4 py-3">{index + 1}</td>
                        <td className="px-4 py-3 font-medium">{step.simulationName}</td>
                        <td className="px-4 py-3">v{step.simulationVersionNumber}</td>
                        <td className="px-4 py-3">{step.required ? "Sim" : "Não"}</td>
                        <td className="px-4 py-3 text-right">
                          {canEdit && (
                            <div className="flex justify-end gap-2">
                              <Button
                                variant="outline"
                                size="sm"
                                disabled={index === 0}
                                onClick={() => onMoveStep(step, -1)}
                              >
                                Subir
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                disabled={index === sequence.steps.length - 1}
                                onClick={() => onMoveStep(step, 1)}
                              >
                                Descer
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                className="text-danger"
                                onClick={() => onRemoveStep(step)}
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function JourneyConfirmationDialog({
  confirmation,
  pending,
  error,
  onCancel,
  onConfirm,
}: {
  confirmation: JourneyConfirmation | null;
  pending: boolean;
  error: unknown;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const title =
    confirmation?.action === "publish"
      ? "Publicar jornada?"
      : confirmation?.action === "archive"
        ? "Arquivar jornada?"
        : "Remover avaliação da jornada?";
  const description =
    confirmation?.action === "publish"
      ? `A jornada “${confirmation.journeyName}” ficará disponível para novos convites.`
      : confirmation?.action === "archive"
        ? `A jornada “${confirmation.journeyName}” será arquivada e não poderá mais ser editada.`
        : confirmation?.action === "remove-step"
          ? `A avaliação “${confirmation.simulationName}” será removida da jornada “${confirmation.journeyName}”.`
          : "";
  const actionLabel =
    confirmation?.action === "publish"
      ? "Publicar jornada"
      : confirmation?.action === "archive"
        ? "Arquivar jornada"
        : "Remover avaliação";

  return (
    <AlertDialog open={confirmation != null} onOpenChange={(open) => !open && onCancel()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        {error != null && (
          <StateBanner tone="danger" title="Não foi possível concluir a ação">
            {error instanceof Error ? error.message : "Tente novamente."}
          </StateBanner>
        )}
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending}>Cancelar</AlertDialogCancel>
          <AlertDialogAction disabled={pending} onClick={onConfirm}>
            {pending ? "Processando..." : actionLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

function ParticipationOwnerCard({ published }: { published: boolean }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="text-lg font-semibold">Convites e acompanhamento</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Esta jornada é configurada aqui. Pessoas participantes, links, validade, reenvios e
        resultados são administrados na Central de Participações.
      </p>
      <div className="mt-4 flex flex-wrap gap-2">
        <Button asChild disabled={!published}>
          <Link to="/participacoes/jornada">Criar convite por jornada</Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/participacoes">Abrir participações</Link>
        </Button>
      </div>
      {!published && (
        <p className="mt-3 text-xs text-muted-foreground">
          Publique a jornada para liberar a criação de convites.
        </p>
      )}
    </section>
  );
}

function JourneyStatusBadge({ status }: { status: AssessmentJourneyStatus }) {
  const label: Record<AssessmentJourneyStatus, string> = {
    draft: "Rascunho",
    published: "Publicada",
    archived: "Arquivada",
  };
  const className =
    status === "published"
      ? "border-success/30 bg-success/10 text-success"
      : status === "archived"
        ? "border-border bg-muted text-muted-foreground"
        : "border-warning/30 bg-warning/10 text-warning-foreground";
  return (
    <span className={cn("rounded-full border px-2 py-0.5 text-[11px]", className)}>
      {label[status]}
    </span>
  );
}

async function invalidateJourney(
  queryClient: ReturnType<typeof useQueryClient>,
  journeyId: string | null,
) {
  await queryClient.invalidateQueries({ queryKey: ["assessment-journeys"] });
  if (journeyId) {
    await queryClient.invalidateQueries({ queryKey: ["assessment-journey", journeyId] });
  }
}
