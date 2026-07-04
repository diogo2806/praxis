import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import {
  Archive,
  CheckCircle2,
  Copy,
  Edit3,
  ExternalLink,
  Plus,
  RefreshCw,
  Save,
  Send,
  Trash2,
  Workflow,
  X,
} from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, SkeletonRows, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  addAssessmentJourneyStep,
  archiveAssessmentJourney,
  createAssessmentJourney,
  createAssessmentJourneyAttempt,
  deleteAssessmentJourneyStep,
  getAssessmentJourney,
  listAssessmentJourneyAttempts,
  listAssessmentJourneys,
  listSimulations,
  publishAssessmentJourney,
  updateAssessmentJourney,
  updateAssessmentJourneyStep,
  type AssessmentJourneyAttemptResponse,
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
        content:
          "Monte sequências de avaliações publicadas, publique jornadas e acompanhe participantes.",
      },
    ],
  }),
  component: AssessmentJourneysPage,
});

const DEFAULT_SEQUENCE = "principal";

function AssessmentJourneysPage() {
  const queryClient = useQueryClient();
  const [selectedJourneyId, setSelectedJourneyId] = useState<string | null>(null);
  const [journeyName, setJourneyName] = useState("");
  const [journeyDescription, setJourneyDescription] = useState("");
  const [selectedSimulationId, setSelectedSimulationId] = useState("");
  const [required, setRequired] = useState(true);
  const [sequenceKey, setSequenceKey] = useState(DEFAULT_SEQUENCE);
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [candidateSequenceKey, setCandidateSequenceKey] = useState("");
  const [copiedAttemptId, setCopiedAttemptId] = useState<string | null>(null);

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
  const attemptsQuery = useQuery({
    queryKey: ["assessment-journey-attempts", selectedJourneyId],
    queryFn: () => listAssessmentJourneyAttempts(selectedJourneyId!),
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
  const attempts = attemptsQuery.data ?? [];
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
      await invalidateJourney(queryClient, journey.id);
    },
  });
  const archiveMutation = useMutation({
    mutationFn: archiveAssessmentJourney,
    onSuccess: async (journey) => {
      await invalidateJourney(queryClient, journey.id);
    },
  });
  const removeStepMutation = useMutation({
    mutationFn: (stepId: number) => deleteAssessmentJourneyStep(selectedJourneyId!, stepId),
    onSuccess: async () => {
      await invalidateJourney(queryClient, selectedJourneyId);
    },
  });
  const reorderStepMutation = useMutation({
    mutationFn: ({ step, direction }: { step: JourneyStepResponse; direction: -1 | 1 }) =>
      updateAssessmentJourneyStep(selectedJourneyId!, step.id, {
        orderIndex: step.orderIndex + direction,
      }),
    onSuccess: async () => {
      await invalidateJourney(queryClient, selectedJourneyId);
    },
  });
  const attemptMutation = useMutation({
    mutationFn: createAssessmentJourneyAttempt,
    onSuccess: async () => {
      setCandidateName("");
      setCandidateEmail("");
      setCandidateSequenceKey("");
      await queryClient.invalidateQueries({
        queryKey: ["assessment-journey-attempts", selectedJourneyId],
      });
    },
  });

  const mutationError =
    createMutation.error ||
    addStepMutation.error ||
    publishMutation.error ||
    archiveMutation.error ||
    removeStepMutation.error ||
    reorderStepMutation.error ||
    attemptMutation.error;

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

  function createAttempt() {
    if (!selectedJourney || !candidateName.trim() || !candidateEmail.includes("@")) return;
    const sequences = selectedJourney.sequences;
    const chosenSequenceKey = candidateSequenceKey || sequences[0]?.sequenceKey || DEFAULT_SEQUENCE;
    attemptMutation.mutate({
      journeyId: selectedJourney.id,
      candidateName: candidateName.trim(),
      candidateEmail: candidateEmail.trim(),
      sequenceKey: chosenSequenceKey,
    });
  }

  async function copyJourneyLink(attempt: AssessmentJourneyAttemptResponse) {
    const url = journeyAttemptUrl(attempt.id);
    await navigator.clipboard.writeText(url);
    setCopiedAttemptId(attempt.id);
    window.setTimeout(() => setCopiedAttemptId(null), 2000);
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-7xl space-y-6">
        <section className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase text-primary">Jornada de Avaliacao</div>
            <h1 className="mt-1 text-3xl font-semibold text-foreground">
              Sequências de avaliações publicadas
            </h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
              Combine avaliações já publicadas, publique a composição e gere uma tentativa de
              jornada para cada candidato.
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-9 gap-2 bg-card"
            onClick={() => {
              void journeysQuery.refetch();
              void selectedJourneyQuery.refetch();
              void attemptsQuery.refetch();
              void simulationsQuery.refetch();
            }}
          >
            <RefreshCw className="h-4 w-4" />
            Atualizar
          </Button>
        </section>

        {mutationError && (
          <StateBanner tone="danger" title="Nao foi possivel concluir a acao">
            {mutationError instanceof Error ? mutationError.message : "Tente novamente."}
          </StateBanner>
        )}

        <div className="space-y-5">
          <aside className="space-y-5">
            <section className="rounded-md border border-border bg-card p-4">
              <h2 className="text-sm font-semibold">Nova jornada</h2>
              <div className="mt-4 space-y-3">
                <input
                  className="input w-full"
                  placeholder="Ex: Processo Trainee 2026"
                  value={journeyName}
                  onChange={(event) => setJourneyName(event.target.value)}
                />
                <textarea
                  className="input min-h-24 w-full resize-y"
                  placeholder="Descricao opcional"
                  value={journeyDescription}
                  onChange={(event) => setJourneyDescription(event.target.value)}
                />
                <Button
                  type="button"
                  className="h-9 w-full gap-2"
                  disabled={!journeyName.trim() || createMutation.isPending}
                  onClick={createJourney}
                >
                  <Plus className="h-4 w-4" />
                  Criar rascunho
                </Button>
              </div>
            </section>

            <section className="overflow-hidden rounded-md border border-border bg-card">
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
                            {journey.stepCount} avaliações - {journey.sequenceCount} sequencia
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

          <main className="space-y-5">
            {!selectedJourneyId ? (
              <EmptyState
                title="Selecione uma jornada"
                description="Crie ou escolha um rascunho para adicionar avaliações publicadas e gerar convites."
              />
            ) : selectedJourneyQuery.isLoading ? (
              <section className="rounded-md border border-border bg-card p-4">
                <SkeletonRows rows={6} />
              </section>
            ) : selectedJourneyQuery.isError || !selectedJourney ? (
              <StateBanner tone="danger" title="Nao foi possivel carregar a jornada">
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
                  onPublish={() => publishMutation.mutate(selectedJourney.id)}
                  publishPending={publishMutation.isPending}
                  onArchive={() => archiveMutation.mutate(selectedJourney.id)}
                  archivePending={archiveMutation.isPending}
                  onRemoveStep={(stepId) => removeStepMutation.mutate(stepId)}
                  onMoveStep={(step, direction) => reorderStepMutation.mutate({ step, direction })}
                />

                <JourneyAttempts
                  journey={selectedJourney}
                  attempts={attempts}
                  loading={attemptsQuery.isLoading}
                  candidateName={candidateName}
                  candidateEmail={candidateEmail}
                  candidateSequenceKey={candidateSequenceKey}
                  onCandidateNameChange={setCandidateName}
                  onCandidateEmailChange={setCandidateEmail}
                  onCandidateSequenceKeyChange={setCandidateSequenceKey}
                  onCreateAttempt={createAttempt}
                  createPending={attemptMutation.isPending}
                  copiedAttemptId={copiedAttemptId}
                  onCopy={copyJourneyLink}
                />
              </>
            )}
          </main>
        </div>
      </div>
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
  onRemoveStep: (stepId: number) => void;
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

  const updateJourneyMutation = useMutation({
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
  const existingSequenceKeys = sequences.map((sequence) => sequence.sequenceKey);
  const draft = journey.status === "draft";
  const canEditMetadata = journey.status !== "archived";
  const canEditStructure = journey.status !== "archived";
  const canPublish = draft && totalSteps > 0;
  const trimmedSequenceKey = sequenceKey.trim() || DEFAULT_SEQUENCE;
  const simulationsInCurrentSequence = new Set(
    sequences
      .find((sequence) => sequence.sequenceKey === trimmedSequenceKey)
      ?.steps.map((step) => step.simulationId) ?? [],
  );
  const availableSimulations = simulations.filter(
    (simulation) => !simulationsInCurrentSequence.has(simulation.id),
  );
  const selectedIsUnavailable =
    Boolean(selectedSimulationId) && simulationsInCurrentSequence.has(selectedSimulationId);

  function resetEditForm() {
    setEditName(journey.name);
    setEditDescription(journey.description ?? "");
    setEditing(false);
  }

  return (
    <section className="rounded-md border border-border bg-card">
      <div className="flex flex-col gap-3 border-b border-border p-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <Workflow className="h-4 w-4 text-primary" />
            <h2 className="text-xl font-semibold">{journey.name}</h2>
            <JourneyStatusBadge status={journey.status} />
          </div>
          {journey.description && (
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">{journey.description}</p>
          )}
          <p className="mt-2 text-xs text-muted-foreground">
            {totalSteps} {totalSteps === 1 ? "avaliação" : "avaliações"} em {sequences.length}{" "}
            {sequences.length === 1 ? "sequencia" : "sequencias"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {canEditMetadata && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-9 gap-2 bg-card"
              onClick={() => setEditing((current) => !current)}
            >
              <Edit3 className="h-4 w-4" />
              {editing ? "Fechar edição" : "Editar"}
            </Button>
          )}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-9 gap-2 bg-card"
            disabled={!canPublish || publishPending}
            onClick={onPublish}
          >
            <CheckCircle2 className="h-4 w-4" />
            Publicar
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-9 gap-2 bg-card"
            disabled={journey.status === "archived" || archivePending}
            onClick={onArchive}
          >
            <Archive className="h-4 w-4" />
            Arquivar
          </Button>
        </div>
      </div>

      {editing && canEditMetadata && (
        <div className="border-b border-border bg-muted/20 p-5">
          <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto]">
            <input
              className="input w-full"
              placeholder="Nome da jornada"
              value={editName}
              onChange={(event) => setEditName(event.target.value)}
            />
            <textarea
              className="input min-h-10 w-full resize-y"
              placeholder="Descricao opcional"
              value={editDescription}
              onChange={(event) => setEditDescription(event.target.value)}
            />
            <div className="flex gap-2">
              <Button
                type="button"
                className="h-10 gap-2"
                disabled={!editName.trim() || updateJourneyMutation.isPending}
                onClick={() => updateJourneyMutation.mutate()}
              >
                <Save className="h-4 w-4" />
                Salvar
              </Button>
              <Button
                type="button"
                variant="outline"
                className="h-10 gap-2 bg-card"
                disabled={updateJourneyMutation.isPending}
                onClick={resetEditForm}
              >
                <X className="h-4 w-4" />
                Cancelar
              </Button>
            </div>
          </div>
          {updateJourneyMutation.error && (
            <p className="mt-2 text-sm text-danger">
              {updateJourneyMutation.error instanceof Error
                ? updateJourneyMutation.error.message
                : "Nao foi possivel salvar a jornada."}
            </p>
          )}
          <p className="mt-2 text-xs text-muted-foreground">
            Nome, descrição e avaliações podem ser editados em jornadas rascunho ou publicadas.
          </p>
        </div>
      )}

      {canEditStructure && (
        <div className="border-b border-border p-5">
          {simulations.length === 0 ? (
            <EmptyState
              title="Nenhuma avaliação publicada"
              description="Publique uma avaliação para poder vinculá-la a esta jornada."
              actions={
                <Button asChild size="sm" className="h-9 gap-2">
                  <Link to="/avaliacoes">
                    <Plus className="h-4 w-4" />
                    Ir para avaliações
                  </Link>
                </Button>
              }
            />
          ) : (
            <>
              <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_minmax(0,220px)_160px_120px]">
                <select
                  className="input w-full"
                  value={selectedIsUnavailable ? "" : selectedSimulationId}
                  onChange={(event) => onSimulationChange(event.target.value)}
                  disabled={availableSimulations.length === 0}
                >
                  <option value="">
                    {availableSimulations.length === 0
                      ? "Todas as avaliações publicadas já estão nesta sequência"
                      : "Selecione uma avaliação publicada"}
                  </option>
                  {availableSimulations.map((simulation) => (
                    <option key={simulation.id} value={simulation.id}>
                      {simulation.name} - v
                      {simulation.livePublishedVersionNumber ?? simulation.versionNumber}
                    </option>
                  ))}
                </select>
                <input
                  className="input w-full"
                  list="journey-sequence-keys"
                  placeholder="Sequencia (ex: principal)"
                  value={sequenceKey}
                  onChange={(event) => onSequenceKeyChange(event.target.value)}
                />
                <datalist id="journey-sequence-keys">
                  {existingSequenceKeys.map((key) => (
                    <option key={key} value={key} />
                  ))}
                </datalist>
                <label className="inline-flex h-10 items-center gap-2 rounded-md border border-border bg-background px-3 text-sm">
                  <input
                    type="checkbox"
                    checked={required}
                    onChange={(event) => onRequiredChange(event.target.checked)}
                  />
                  Obrigatorio
                </label>
                <Button
                  type="button"
                  className="h-10 gap-2"
                  disabled={
                    !selectedSimulationId || selectedIsUnavailable || !sequenceKey.trim() || addPending
                  }
                  onClick={onAddStep}
                >
                  <Plus className="h-4 w-4" />
                  Adicionar
                </Button>
              </div>
              <p className="mt-2 text-xs text-muted-foreground">
                Combine avaliações diferentes em uma mesma sequência (cada avaliação só pode entrar
                uma vez por sequência) ou crie sequências diferentes digitando um novo nome. Cada
                candidato recebe o link de uma sequencia.
              </p>
            </>
          )}
        </div>
      )}

      {totalSteps === 0 ? (
        <div className="p-5 text-sm text-muted-foreground">
          Adicione pelo menos uma avaliação publicada para publicar a jornada.
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
                  {sequence.steps.length} {sequence.steps.length === 1 ? "avaliação" : "avaliações"}
                </span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3 font-medium">Ordem</th>
                      <th className="px-4 py-3 font-medium">Avaliação</th>
                      <th className="px-4 py-3 font-medium">Versao</th>
                      <th className="px-4 py-3 font-medium">Obrigatorio</th>
                      <th className="px-4 py-3 text-right font-medium">Acoes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sequence.steps.map((step, index) => (
                      <tr key={step.id} className="border-b border-border last:border-0">
                        <td className="px-4 py-3 tabular-nums">{index + 1}</td>
                        <td className="px-4 py-3 font-medium">{step.simulationName}</td>
                        <td className="px-4 py-3 text-muted-foreground">
                          v{step.simulationVersionNumber}
                        </td>
                        <td className="px-4 py-3">{step.required ? "Sim" : "Opcional"}</td>
                        <td className="px-4 py-3 text-right">
                          {canEditStructure && (
                            <div className="flex justify-end gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="h-8 bg-background text-xs"
                                disabled={index === 0}
                                onClick={() => onMoveStep(step, -1)}
                              >
                                Subir
                              </Button>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="h-8 bg-background text-xs"
                                disabled={index === sequence.steps.length - 1}
                                onClick={() => onMoveStep(step, 1)}
                              >
                                Descer
                              </Button>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="h-8 bg-background text-xs text-danger"
                                onClick={() => onRemoveStep(step.id)}
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

function JourneyAttempts({
  journey,
  attempts,
  loading,
  candidateName,
  candidateEmail,
  candidateSequenceKey,
  onCandidateNameChange,
  onCandidateEmailChange,
  onCandidateSequenceKeyChange,
  onCreateAttempt,
  createPending,
  copiedAttemptId,
  onCopy,
}: {
  journey: AssessmentJourneyDetailResponse;
  attempts: AssessmentJourneyAttemptResponse[];
  loading: boolean;
  candidateName: string;
  candidateEmail: string;
  candidateSequenceKey: string;
  onCandidateNameChange: (value: string) => void;
  onCandidateEmailChange: (value: string) => void;
  onCandidateSequenceKeyChange: (value: string) => void;
  onCreateAttempt: () => void;
  createPending: boolean;
  copiedAttemptId: string | null;
  onCopy: (attempt: AssessmentJourneyAttemptResponse) => Promise<void>;
}) {
  const published = journey.status === "published";
  const sequences = journey.sequences;
  const hasMultipleSequences = sequences.length > 1;

  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border p-5">
        <h2 className="text-xl font-semibold">Candidatos da jornada</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Gere um link de jornada; cada etapa interna abre a avaliação individual na ordem definida.
        </p>
      </div>

      <div
        className={cn(
          "grid gap-3 border-b border-border p-5",
          hasMultipleSequences
            ? "lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,180px)_140px]"
            : "lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_140px]",
        )}
      >
        <input
          className="input w-full"
          placeholder="Nome do candidato"
          value={candidateName}
          disabled={!published}
          onChange={(event) => onCandidateNameChange(event.target.value)}
        />
        <input
          className="input w-full"
          type="email"
          placeholder="email@empresa.com"
          value={candidateEmail}
          disabled={!published}
          onChange={(event) => onCandidateEmailChange(event.target.value)}
        />
        {hasMultipleSequences && (
          <select
            className="input w-full"
            value={candidateSequenceKey}
            disabled={!published}
            onChange={(event) => onCandidateSequenceKeyChange(event.target.value)}
          >
            <option value="">{sequences[0]?.sequenceKey} (padrao)</option>
            {sequences.map((sequence) => (
              <option key={sequence.sequenceKey} value={sequence.sequenceKey}>
                {sequence.sequenceKey}
              </option>
            ))}
          </select>
        )}
        <Button
          type="button"
          className="h-10 gap-2"
          disabled={!published || !candidateName.trim() || !candidateEmail.includes("@") || createPending}
          onClick={onCreateAttempt}
        >
          <Send className="h-4 w-4" />
          Gerar link
        </Button>
      </div>

      {!published && (
        <div className="border-b border-border px-5 py-3 text-sm text-muted-foreground">
          Publique a jornada antes de gerar links para candidatos.
        </div>
      )}

      {loading ? (
        <div className="p-5">
          <SkeletonRows rows={4} />
        </div>
      ) : attempts.length === 0 ? (
        <div className="p-5 text-sm text-muted-foreground">Nenhum candidato nesta jornada.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[780px] text-left text-sm">
            <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3 font-medium">Candidato</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Progresso</th>
                <th className="px-4 py-3 font-medium">Link</th>
                <th className="px-4 py-3 text-right font-medium">Acoes</th>
              </tr>
            </thead>
            <tbody>
              {attempts.map((attempt) => {
                const completed = attempt.steps.filter((step) => step.status === "completed").length;
                const link = journeyAttemptUrl(attempt.id);
                const copied = copiedAttemptId === attempt.id;
                return (
                  <tr key={attempt.id} className="border-b border-border last:border-0">
                    <td className="px-4 py-3">
                      <div className="font-medium">{attempt.candidateName}</div>
                      <div className="text-xs text-muted-foreground">{attempt.candidateEmail}</div>
                      {hasMultipleSequences && (
                        <div className="mt-1 text-[11px] text-muted-foreground">
                          Sequencia: {attempt.sequenceKey}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <AttemptStatusBadge status={attempt.status} />
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {completed}/{attempt.steps.length} avaliações
                    </td>
                    <td className="max-w-[300px] px-4 py-3">
                      <code className="block truncate rounded-md border border-border bg-background px-2 py-1.5 text-xs text-muted-foreground">
                        {link}
                      </code>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className={cn(
                            "h-8 gap-1.5 bg-background text-xs",
                            copied && "text-success",
                          )}
                          onClick={() => void onCopy(attempt)}
                        >
                          {copied ? (
                            <CheckCircle2 className="h-3.5 w-3.5" />
                          ) : (
                            <Copy className="h-3.5 w-3.5" />
                          )}
                          {copied ? "Copiado" : "Copiar"}
                        </Button>
                        <Button
                          asChild
                          variant="outline"
                          size="sm"
                          className="h-8 gap-1.5 bg-background text-xs"
                        >
                          <Link to="/jornada/$attemptId" params={{ attemptId: attempt.id }}>
                            <ExternalLink className="h-3.5 w-3.5" />
                            Abrir
                          </Link>
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function JourneyStatusBadge({ status }: { status: AssessmentJourneyStatus }) {
  const map: Record<AssessmentJourneyStatus, string> = {
    draft: "Rascunho",
    published: "Publicada",
    archived: "Arquivada",
  };
  const tone =
    status === "published"
      ? "border-success/30 bg-success/10 text-success"
      : status === "archived"
        ? "border-muted bg-muted text-muted-foreground"
        : "border-warning/30 bg-warning/10 text-warning";
  return (
    <span className={cn("rounded-full border px-2 py-0.5 text-[11px]", tone)}>{map[status]}</span>
  );
}

function AttemptStatusBadge({ status }: { status: AssessmentJourneyAttemptResponse["status"] }) {
  const map: Record<AssessmentJourneyAttemptResponse["status"], string> = {
    created: "Criada",
    inProgress: "Em andamento",
    completed: "Concluida",
    expired: "Expirada",
    abandoned: "Abandonada",
  };
  const tone =
    status === "completed"
      ? "border-success/30 bg-success/10 text-success"
      : status === "inProgress"
        ? "border-primary/30 bg-primary/10 text-primary"
        : "border-border bg-background text-muted-foreground";
  return (
    <span className={cn("rounded-full border px-2 py-0.5 text-[11px]", tone)}>{map[status]}</span>
  );
}

function journeyAttemptUrl(attemptId: string) {
  if (typeof window === "undefined") {
    return `/jornada/${attemptId}`;
  }
  return `${window.location.origin}/jornada/${attemptId}`;
}

async function invalidateJourney(
  queryClient: ReturnType<typeof useQueryClient>,
  journeyId: string | null,
) {
  await queryClient.invalidateQueries({ queryKey: ["assessment-journeys"] });
  if (journeyId) {
    await queryClient.invalidateQueries({ queryKey: ["assessment-journey", journeyId] });
    await queryClient.invalidateQueries({ queryKey: ["assessment-journey-attempts", journeyId] });
  }
}
