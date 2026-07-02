import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner, StatusBadge } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  cloneSimulationVersionToDraft,
  createSimulationNode,
  getSimulationVersion,
  listSimulations,
  updateSimulationNode,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { canEditSimulationVersion, statusMeta } from "@/lib/simulation-meta";
import { defaultAnswerTimeLimitSeconds, useEmpresaConfig } from "@/lib/empresa-config";

export const Route = createFileRoute("/nova/personagem")({
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
      { title: "Personagem Fictício - Práxis" },
      { name: "description", content: "Configura o contexto inicial salvo no fluxo da conversa." },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const {
    config,
    isLoading: empresaConfigLoading,
    isError: empresaConfigError,
    error: empresaConfigQueryError,
  } = useEmpresaConfig();
  const hasDraftContext = Boolean(search.simulationId && search.versionNumber);
  const [name, setName] = useState("");
  const [emotion, setEmotion] = useState("");
  const [context, setContext] = useState("");
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    enabled: !hasDraftContext,
  });
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasDraftContext,
  });
  const rootNodeId = versionQuery.data?.blueprint.rootNodeId;
  const orderedNodes = useMemo(
    () => [...(versionQuery.data?.nodes ?? [])].sort((a, b) => a.turnIndex - b.turnIndex),
    [versionQuery.data?.nodes],
  );
  const rootNode = orderedNodes.find((node) => node.id === rootNodeId) ?? orderedNodes[0];
  const existingMessage = rootNode?.clientMessage;
  const versionStatus = versionQuery.data?.status;
  const isEditable = versionStatus ? canEditSimulationVersion(versionStatus) : true;
  const contextMaxLength = 1200;
  const canGoNext = context.trim().length > 0 && emotion.trim().length > 0;
  const clientMessage = useMemo(() => {
    const parts = [
      emotion.trim(),
      context.trim(),
      name.trim() ? `Personagem: ${name.trim()}` : "",
    ].filter(Boolean);
    return parts.join("\n\n");
  }, [context, emotion, name]);
  const saveCharacterMutation = useMutation({
    mutationFn: async () => {
      if (!isEditable) {
        throw new Error("Esta versão não pode ser editada. Crie um rascunho antes de alterar.");
      }
      if (rootNode) {
        await updateSimulationNode(search.simulationId!, search.versionNumber!, rootNode.id, {
          clientMessage,
          timeLimitSeconds: rootNode.timeLimitSeconds,
        });
        return rootNode.id;
      }
      if (!config) {
        throw new Error("A configuração da empresa ainda não foi carregada pelo sistema.");
      }
      return createSimulationNode(search.simulationId!, search.versionNumber!, {
        clientMessage,
        timeLimitSeconds: defaultAnswerTimeLimitSeconds(config),
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      });
      void navigate({
        to: "/nova/validador",
        search: { simulationId: search.simulationId, versionNumber: search.versionNumber },
      });
    },
  });
  const cloneDraftMutation = useMutation({
    mutationFn: () => cloneSimulationVersionToDraft(search.simulationId!, search.versionNumber!),
    onSuccess: async (draft) => {
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/personagem",
        search: { simulationId: draft.simulationId, versionNumber: draft.newVersionNumber },
      });
    },
  });

  useEffect(() => {
    if (!existingMessage) {
      setName("");
      setEmotion("");
      setContext("");
      return;
    }

    const parsed = parseCharacterMessage(existingMessage);
    setName(parsed.name);
    setEmotion(parsed.emotion);
    setContext(parsed.context);
  }, [existingMessage]);

  return (
    <AppShell>
      <WizardStepper current="cenario" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 2</div>
        <h1 className="mt-1 font-display text-3xl">Personagem do cliente fictício</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          O contexto informado aqui é salvo na primeira etapa da versão selecionada.
        </p>
      </div>

      {!hasDraftContext ? (
        <EmptyState
          title="Escolha uma avaliação real"
          description="Esta etapa não usa personagem de exemplo."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : empresaConfigLoading || versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando fluxo da conversa">
          Buscando a primeira etapa da avaliação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : empresaConfigError ? (
        <StateBanner tone="danger" title="Não foi possível carregar a configuração">
          {empresaConfigQueryError instanceof Error
            ? empresaConfigQueryError.message
            : "Verifique se o sistema está disponível antes de continuar."}
        </StateBanner>
      ) : versionQuery.isError ? (
        <StateBanner tone="danger" title="Não foi possível carregar a versão">
          {versionQuery.error instanceof Error
            ? versionQuery.error.message
            : "Não foi possível carregar agora. Verifique sua conexão e tente novamente."}
        </StateBanner>
      ) : (
        <>
          {existingMessage && (
            <div className="mb-5">
              <StateBanner tone="info" title={`Primeira etapa atual: ${rootNodeId}`}>
                {existingMessage}
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
                {versionStatus === "published"
                  ? "A versão no ar fica protegida. Crie um rascunho para editar sem afetar candidatos em andamento."
                  : "Atualize a etapa atual da versão antes de alterar o personagem."}
              </StateBanner>
            </div>
          )}
          {cloneDraftMutation.isError && (
            <div className="mb-5">
              <StateBanner tone="danger" title="Não foi possível criar o rascunho">
                {cloneDraftMutation.error instanceof Error
                  ? cloneDraftMutation.error.message
                  : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          {saveCharacterMutation.isError && (
            <div className="mb-5">
              <StateBanner tone="danger" title="Não foi possível salvar o personagem">
                {saveCharacterMutation.error instanceof Error
                  ? saveCharacterMutation.error.message
                  : "Tente novamente."}
              </StateBanner>
            </div>
          )}
          <div className="rounded-md border border-border bg-card p-6">
            <div className="grid gap-4 md:grid-cols-2">
              {versionStatus && (
                <div className="md:col-span-2">
                  <StatusBadge status={versionStatus} />
                </div>
              )}
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                  Nome ou identificador
                  <span className="ml-2 inline-flex h-4 items-center rounded-full border border-muted px-2 py-0.5 text-[10px] text-muted-foreground">
                    Opcional
                  </span>
                </span>
                <input
                  className="input"
                  value={name}
                  disabled={!isEditable}
                  onChange={(event) => setName(event.target.value)}
                />
              </label>
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                  Estado emocional inicial
                  <span className="ml-2 inline-flex h-4 items-center rounded-full border border-danger/40 bg-danger/10 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-danger">
                    Obrigatório
                  </span>
                </span>
                <input
                  className={`input ${submitAttempted && emotion.trim().length === 0 ? "border-danger" : ""}`}
                  value={emotion}
                  required
                  disabled={!isEditable}
                  onChange={(event) => setEmotion(event.target.value)}
                />
              </label>
            </div>
            <label className="mt-4 block">
              <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                Contexto do cliente
                <span className="ml-2 inline-flex h-4 items-center rounded-full border border-danger/40 bg-danger/10 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-danger">
                  Obrigatório
                </span>
              </span>
              <textarea
                className={`input min-h-24 ${submitAttempted && context.trim().length === 0 ? "border-danger" : ""}`}
                maxLength={contextMaxLength}
                required
                value={context}
                disabled={!isEditable}
                onChange={(event) => setContext(event.target.value)}
              />
              <div className="mt-1 flex justify-end text-xs text-muted-foreground">
                <span
                  className={submitAttempted && context.trim().length === 0 ? "text-danger" : ""}
                >
                  {context.length}/{contextMaxLength}
                </span>
              </div>
            </label>
            <div className="mt-6 rounded-md border border-warning/30 bg-warning/10 p-4 text-sm">
              <div className="text-sm font-semibold text-warning-foreground">
                Como preencher este card
              </div>
              <p className="mt-2 text-muted-foreground">
                Use o nome ou identificador apenas para reconhecer o personagem no roteiro. No
                estado emocional inicial, descreva como o cliente chega ao atendimento, por exemplo
                calmo, frustrado, confuso ou com pressa. No contexto, informe a situação que ele
                vive, o objetivo da conversa, o que já tentou fazer e qualquer limite importante
                para orientar as respostas do participante.
              </p>
            </div>
          </div>
          <div className="mt-8 flex justify-between">
            <Link
              to="/nova/objetivo"
              search={{ simulationId: search.simulationId, versionNumber: search.versionNumber }}
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Voltar: Objetivo
            </Link>
            <button
              type="button"
              onClick={() => {
                setSubmitAttempted(true);
                if (!canGoNext) return;
                saveCharacterMutation.mutate();
              }}
              disabled={!isEditable || !canGoNext || saveCharacterMutation.isPending}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saveCharacterMutation.isPending ? "Salvando..." : "Ir para revisão →"}
            </button>
          </div>
        </>
      )}
    </AppShell>
  );
}

function parseCharacterMessage(message: string) {
  const parts = message
    .split(/\n{2,}/)
    .map((part) => part.trim())
    .filter(Boolean);
  const namePrefix = "Personagem:";
  const namePart = parts.at(-1)?.startsWith(namePrefix) ? parts.pop() : undefined;

  if (namePart) {
    return {
      emotion: parts.length > 1 ? parts[0] : "",
      context: parts.length > 1 ? parts.slice(1).join("\n\n") : (parts[0] ?? ""),
      name: namePart.slice(namePrefix.length).trim(),
    };
  }

  return {
    emotion: "",
    context: message.trim(),
    name: "",
  };
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
          to="/nova/personagem"
          search={{ simulationId: simulation.id, versionNumber: simulation.versionNumber }}
          className="rounded-md border border-border bg-card px-3 py-2 text-sm hover:bg-accent"
        >
          {simulation.name} v{simulation.versionNumber}
        </Link>
      ))}
    </div>
  );
}
