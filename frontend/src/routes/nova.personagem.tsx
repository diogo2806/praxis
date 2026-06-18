import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  createSimulationNode,
  getSimulationVersion,
  listSimulations,
  updateSimulationNode,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { defaultAnswerTimeLimitSeconds, useTenantConfig } from "@/lib/tenant-config";

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
    isLoading: tenantConfigLoading,
    isError: tenantConfigError,
    error: tenantConfigQueryError,
  } = useTenantConfig();
  const checklist = config?.languageChecklist.map((item) => item.value) ?? [];
  const hasDraftContext = Boolean(search.simulationId && search.versionNumber);
  const [name, setName] = useState("");
  const [emotion, setEmotion] = useState("");
  const [context, setContext] = useState("");
  const [checkedItems, setCheckedItems] = useState<string[]>([]);
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
  const canGoNext = checkedItems.length === checklist.length && context.trim().length > 0;
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
      if (rootNode) {
        await updateSimulationNode(search.simulationId!, search.versionNumber!, rootNode.id, {
          clientMessage,
          timeLimitSeconds: rootNode.timeLimitSeconds,
        });
        return rootNode.id;
      }
      if (!config) {
        throw new Error("Configuracao da empresa ainda nao foi carregada pelo backend.");
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
        to: "/nova/dialogo",
        search: { simulationId: search.simulationId, versionNumber: search.versionNumber },
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

  function toggleChecklist(item: string) {
    setCheckedItems((current) =>
      current.includes(item) ? current.filter((value) => value !== item) : [...current, item],
    );
  }

  return (
    <AppShell>
      <WizardStepper current="cenario" />
      <ScreenStateStrip blockedReason="contexto e checklist de linguagem precisam ser confirmados" />
      <div className="mb-6">
        <div className="text-xs uppercase tracking-[0.2em] text-primary">Passo 2</div>
        <h1 className="mt-1 font-display text-3xl">Personagem do cliente fictício</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          O contexto informado aqui é salvo na primeira etapa da versão selecionada.
        </p>
      </div>

      {!hasDraftContext ? (
        <EmptyState
          title="Escolha uma simulação real"
          description="Esta etapa não usa personagem de exemplo."
          actions={
            <SimulationLinks
              loading={simulationsQuery.isLoading}
              simulations={simulationsQuery.data ?? []}
            />
          }
        />
      ) : tenantConfigLoading || versionQuery.isLoading ? (
        <StateBanner tone="info" title="Carregando fluxo da conversa">
          Buscando a primeira etapa da simulação {search.simulationId} v{search.versionNumber}.
        </StateBanner>
      ) : tenantConfigError ? (
        <StateBanner tone="danger" title="Nao foi possivel carregar a configuracao">
          {tenantConfigQueryError instanceof Error
            ? tenantConfigQueryError.message
            : "Verifique se o backend esta disponivel antes de continuar."}
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
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                  Nome ou identificador
                </span>
                <input
                  className="input"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                />
              </label>
              <label className="block">
                <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                  Estado emocional inicial
                </span>
                <input
                  className="input"
                  value={emotion}
                  onChange={(event) => setEmotion(event.target.value)}
                />
              </label>
            </div>
            <label className="mt-4 block">
              <span className="mb-1.5 block text-xs font-medium text-muted-foreground">
                Contexto do cliente
              </span>
              <textarea
                className="input min-h-24"
                value={context}
                onChange={(event) => setContext(event.target.value)}
              />
            </label>
            <div className="mt-6 rounded-md border border-warning/30 bg-warning/10 p-4">
              <div className="text-sm font-semibold text-warning-foreground">
                Checklist de linguagem
              </div>
              <ul className="mt-3 space-y-2">
                {checklist.map((item) => (
                  <li key={item} className="flex items-start gap-3 text-sm">
                    <input
                      type="checkbox"
                      checked={checkedItems.includes(item)}
                      onChange={() => toggleChecklist(item)}
                      className="mt-0.5 h-4 w-4 accent-primary"
                    />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
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
              onClick={() => saveCharacterMutation.mutate()}
              disabled={!canGoNext || saveCharacterMutation.isPending}
              className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saveCharacterMutation.isPending ? "Salvando..." : "Montar diálogo →"}
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
