import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { Termo } from "@/components/glossario";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import {
  createSimulationDraft,
  getSimulationVersion,
  updateSimulationBlueprint,
  updateTenantConfig,
  type TenantConfigOption,
} from "@/lib/api/praxis";
import { useTenantConfig } from "@/lib/tenant-config";

export const Route = createFileRoute("/nova/blueprint")({
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
      { title: "Plano da avaliação — Práxis" },
      {
        name: "description",
        content:
          "Defina cargo, situação crítica, competências avaliadas e uso do resultado.",
      },
    ],
  }),
  component: Page,
});

function Page() {
  const search = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const hasVersionContext = Boolean(search.simulationId && search.versionNumber);
  const {
    config,
    isLoading: tenantConfigLoading,
    isError: tenantConfigError,
    error: tenantConfigQueryError,
  } = useTenantConfig();
  const competencies = config?.competencies ?? [];
  const resultUses = config?.resultUses ?? [];
  const defaultResultUse = getDefaultOption(resultUses)?.value ?? "";
  const [role, setRole] = useState("");
  const [criticalSituation, setCriticalSituation] = useState("");
  const [selectedResultUse, setSelectedResultUse] = useState(defaultResultUse);
  const [selectedCompetencies, setSelectedCompetencies] = useState<string[]>([]);
  const [newCompetency, setNewCompetency] = useState("");
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [hydratedVersionKey, setHydratedVersionKey] = useState<string | null>(null);
  const roleMaxLength = 180;
  const criticalSituationMaxLength = 1200;
  const missingFields = [
    role.trim().length === 0 && "cargo-alvo",
    criticalSituation.trim().length === 0 && "situação crítica",
    selectedCompetencies.length === 0 && "ao menos uma competência",
  ].filter((field): field is string => Boolean(field));
  const canGoNext =
    role.trim().length > 0 &&
    criticalSituation.trim().length > 0 &&
    selectedCompetencies.length > 0;
  const normalizedCompetencies = competencies.map((competency) =>
    competency.value.trim().toLowerCase(),
  );
  const canAddCompetency =
    newCompetency.trim().length > 0 &&
    !normalizedCompetencies.includes(newCompetency.trim().toLowerCase());
  const versionQuery = useQuery({
    queryKey: ["simulation-version", search.simulationId, search.versionNumber],
    queryFn: () => getSimulationVersion(search.simulationId!, search.versionNumber!),
    enabled: hasVersionContext,
  });

  useEffect(() => {
    if (!selectedResultUse || !resultUses.some((use) => use.value === selectedResultUse)) {
      setSelectedResultUse(getDefaultOption(resultUses)?.value ?? "");
    }
  }, [selectedResultUse, resultUses]);

  useEffect(() => {
    if (!versionQuery.data || tenantConfigLoading) {
      return;
    }

    const versionKey = `${versionQuery.data.simulationId}:${versionQuery.data.versionNumber}`;
    if (hydratedVersionKey === versionKey) {
      return;
    }

    const parsedBlueprint = parseBlueprintDescription(versionQuery.data.description);
    setRole(parsedBlueprint.role || versionQuery.data.name);
    setCriticalSituation(parsedBlueprint.criticalSituation);
    setSelectedResultUse(parsedBlueprint.resultUse || defaultResultUse);
    setSelectedCompetencies(
      versionQuery.data.blueprint.competencies.map((competency) => competency.name),
    );
    setHydratedVersionKey(versionKey);
  }, [defaultResultUse, hydratedVersionKey, tenantConfigLoading, versionQuery.data]);

  const createDraftMutation = useMutation({
    mutationFn: () =>
      createSimulationDraft({
        name: role.trim(),
        description: buildBlueprintDescription({
          role,
          criticalSituation,
          competencies: selectedCompetencies,
          resultUse: selectedResultUse,
        }),
        rootNodeId: "turno-1",
        competencies: selectedCompetencies,
        criticalSituation: criticalSituation.trim(),
        resultUse: selectedResultUse,
      }),
    onSuccess: (simulation) => {
      void navigate({
        to: "/nova/personagem",
        search: {
          simulationId: simulation.id,
          versionNumber: simulation.versionNumber,
        },
      });
    },
  });

  const updateExistingMutation = useMutation({
    mutationFn: () =>
      updateSimulationBlueprint(search.simulationId!, search.versionNumber!, {
        rootNodeId: versionQuery.data?.blueprint.rootNodeId ?? "turno-1",
        competencies: buildCompetencyWeights(selectedCompetencies, versionQuery.data?.blueprint.competencies),
      }),
    onSuccess: async (simulation) => {
      await queryClient.invalidateQueries({
        queryKey: ["simulation-version", search.simulationId, search.versionNumber],
      });
      await queryClient.invalidateQueries({ queryKey: ["simulations"] });
      void navigate({
        to: "/nova/personagem",
        search: {
          simulationId: simulation.id,
          versionNumber: simulation.versionNumber,
        },
      });
    },
  });

  const addCompetencyMutation = useMutation({
    mutationFn: async (value: string) => {
      const nextCompetencies = [
        ...competencies,
        { value, label: value, locked: false, selectedByDefault: false },
      ];
      return updateTenantConfig("COMPETENCY", nextCompetencies);
    },
    onSuccess: async (_, value) => {
      setNewCompetency("");
      setSelectedCompetencies((current) =>
        current.includes(value) ? current : [...current, value],
      );
      await queryClient.invalidateQueries({ queryKey: ["tenant-config"] });
    },
  });

  function toggleCompetency(competency: string) {
    setSelectedCompetencies((current) =>
      current.includes(competency)
        ? current.filter((item) => item !== competency)
        : [...current, competency],
    );
  }

  return (
    <AppShell>
      <WizardStepper current="avaliacao" unlockedThrough={canGoNext ? "cenario" : "avaliacao"} />
      <ScreenStateStrip blockedReason="cargo, situação crítica e competência obrigatórios" />

      {tenantConfigLoading && (
        <StateBanner tone="info" title="Carregando configuracao da empresa">
          Buscando competencias e usos de resultado no backend.
        </StateBanner>
      )}

      {tenantConfigError && (
        <StateBanner tone="danger" title="Nao foi possivel carregar a configuracao">
          {tenantConfigQueryError instanceof Error
            ? tenantConfigQueryError.message
            : "Verifique se o backend esta disponivel antes de criar uma simulacao."}
        </StateBanner>
      )}

      {createDraftMutation.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title="Não foi possível criar o rascunho">
            {createDraftMutation.error instanceof Error
              ? createDraftMutation.error.message
              : "Tente novamente quando o sistema estiver disponível."}
          </StateBanner>
        </div>
      )}

      {updateExistingMutation.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title="Nao foi possivel atualizar o plano">
            {updateExistingMutation.error instanceof Error
              ? updateExistingMutation.error.message
              : "Tente novamente quando o sistema estiver disponivel."}
          </StateBanner>
        </div>
      )}

      {versionQuery.isLoading && (
        <div className="mb-5">
          <StateBanner tone="info" title="Carregando plano cadastrado">
            Buscando simulacao {search.simulationId} v{search.versionNumber}.
          </StateBanner>
        </div>
      )}

      {versionQuery.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title="Nao foi possivel carregar o plano cadastrado">
            {versionQuery.error instanceof Error
              ? versionQuery.error.message
              : "Verifique se a simulacao e a versao existem."}
          </StateBanner>
        </div>
      )}

      {config && !versionQuery.isLoading && !versionQuery.isError && (
      <div className="space-y-6">
        <div className="rounded-xl border border-border bg-card p-5">
          <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Por que plano da avaliação?
          </div>
          <p className="mt-2 text-sm text-foreground/80">
            O <Termo id="blueprint">plano da avaliação</Termo> vira referência fixa para o{" "}
            <Termo id="validador">Validador</Termo> checar se a simulação mede o que prometeu.
          </p>
        </div>
        <div className="rounded-xl border border-warning/30 bg-warning/10 p-5">
          <div className="text-xs font-semibold uppercase tracking-wider text-warning-foreground">
            Critérios de pontuação, peso e cálculo
          </div>
          <p className="mt-2 text-sm text-foreground/80">
            A nota sai de regras declaradas. O <Termo id="blueprint">plano da avaliação</Termo> registra o
            porquê comportamental que a auditoria vai pedir.
          </p>
        </div>
        <div className="space-y-6">
          <Header
            kicker="Passo 1"
            title="Plano da avaliação"
            lede="Antes de escrever qualquer diálogo, defina o porquê. O plano da avaliação vira referência fixa para o Validador de Qualidade."
          />

          <Card title="Cargo" required>
            <Field label="Cargo-alvo">
              <input
                className={`input ${submitAttempted && role.trim().length === 0 ? "border-danger" : ""}`}
                placeholder="Ex.: Analista de Atendimento N2"
                maxLength={roleMaxLength}
                required
                aria-required="true"
                value={role}
                onChange={(event) => setRole(event.target.value)}
              />
              <FieldMeta
                error={
                  submitAttempted && role.trim().length === 0 ? "Informe o cargo-alvo." : undefined
                }
                count={role.length}
                max={roleMaxLength}
              />
            </Field>
          </Card>

          <Card title="Situação crítica do cargo" required>
            <textarea
              aria-label="Situação crítica do cargo"
              className={`input min-h-24 ${submitAttempted && criticalSituation.trim().length === 0 ? "border-danger" : ""}`}
              maxLength={criticalSituationMaxLength}
              required
              aria-required="true"
              value={criticalSituation}
              onChange={(event) => setCriticalSituation(event.target.value)}
            />
            <FieldMeta
              error={
                submitAttempted && criticalSituation.trim().length === 0
                  ? "Descreva a situação crítica do cargo."
                  : undefined
              }
              count={criticalSituation.length}
              max={criticalSituationMaxLength}
            />
            <Help>Use situação que de fato acontece nesta empresa, não um caso genérico.</Help>
          </Card>

          <Card title="Competências avaliadas" required>
            <div className="flex flex-wrap gap-2">
              {competencies.map((competency) => (
                <label
                  key={competency.value}
                  className={`cursor-pointer rounded-full border px-3 py-1.5 text-sm ${
                    selectedCompetencies.includes(competency.value)
                      ? "border-primary bg-primary/10 text-foreground"
                      : "border-border bg-card text-foreground/75 hover:bg-accent"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={selectedCompetencies.includes(competency.value)}
                    onChange={() => toggleCompetency(competency.value)}
                    className="sr-only"
                  />
                  {competency.label}
                </label>
              ))}
            </div>
            <div className="mt-4 grid gap-2 md:grid-cols-[1fr_auto]">
              <input
                className="input"
                placeholder="Adicionar competência da empresa"
                value={newCompetency}
                onChange={(event) => setNewCompetency(event.target.value)}
              />
              <button
                type="button"
                onClick={() => addCompetencyMutation.mutate(newCompetency.trim())}
                disabled={!canAddCompetency || addCompetencyMutation.isPending}
                className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50"
              >
                {addCompetencyMutation.isPending ? "Salvando..." : "Adicionar e salvar"}
              </button>
            </div>
            <Help>
              Ao salvar, a competência entra no catálogo da sua empresa e fica disponível só para
              ela.
            </Help>
            {addCompetencyMutation.isError && (
              <p className="mt-2 text-xs text-danger">
                {addCompetencyMutation.error instanceof Error
                  ? addCompetencyMutation.error.message
                  : "Não foi possível salvar a competência."}
              </p>
            )}
            <Help>
              Competências personalizadas podem ser organizadas no catálogo interno de competências.
            </Help>
          </Card>

          <Card title="Uso do resultado">
            <div className="grid gap-2 md:grid-cols-4">
              {resultUses.map((use) => (
                <label
                  key={use.value}
                  className={`flex cursor-pointer items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                    selectedResultUse === use.value
                      ? "border-primary bg-primary/5 text-foreground"
                      : use.locked
                        ? "border-dashed border-border bg-muted/40 text-muted-foreground"
                        : "border-border bg-card hover:bg-accent"
                  }`}
                >
                  <input
                    type="radio"
                    name="uso"
                    checked={selectedResultUse === use.value}
                    onChange={() => setSelectedResultUse(use.value)}
                    disabled={use.locked}
                    className="sr-only"
                  />
                  {use.label}
                </label>
              ))}
            </div>
            <Help>
              O Práxis entrega evidência comportamental estruturada. A decisão do processo continua
              com a empresa, dentro da Gupy.
            </Help>
          </Card>

          <div className="sticky bottom-0 -mx-6 mt-2 flex flex-col gap-3 border-t border-border bg-background/90 px-6 py-4 backdrop-blur sm:flex-row sm:items-center sm:justify-between lg:-mx-10 lg:px-10">
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Cancelar
            </Link>
            <div className="flex flex-col items-start gap-2 sm:items-end">
              {!canGoNext && (
                <p
                  className={`text-xs ${submitAttempted ? "text-danger" : "text-muted-foreground"}`}
                  aria-live="polite"
                >
                  Para avançar, preencha {missingFields.join(", ")}.
                </p>
              )}
              <button
                type="button"
                disabled={createDraftMutation.isPending || updateExistingMutation.isPending}
                title={
                  canGoNext
                    ? "Criar rascunho e avançar"
                    : "Preencha cargo, situação crítica e competências para avançar"
                }
                onClick={() => {
                  if (!canGoNext) {
                    setSubmitAttempted(true);
                    return;
                  }
                  if (hasVersionContext) {
                    updateExistingMutation.mutate();
                    return;
                  }
                  createDraftMutation.mutate();
                }}
                aria-disabled={!canGoNext}
                className={`rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                  !canGoNext || createDraftMutation.isPending || updateExistingMutation.isPending
                    ? "cursor-not-allowed opacity-50"
                    : ""
                }`}
              >
                {createDraftMutation.isPending ? "Criando rascunho..." : "Próximo: Cenário"}
              </button>
            </div>
          </div>
        </div>
      </div>
      )}
    </AppShell>
  );
}

function getDefaultOption(options: TenantConfigOption[]) {
  return (
    options.find((option) => option.selectedByDefault && !option.locked) ??
    options.find((option) => !option.locked) ??
    options[0]
  );
}

function buildBlueprintDescription({
  role,
  criticalSituation,
  competencies,
  resultUse,
}: {
  role: string;
  criticalSituation: string;
  competencies: string[];
  resultUse: string;
}) {
  const lines = [
    `Cargo: ${role.trim()}`,
    `Situação crítica: ${criticalSituation.trim()}`,
    competencies.length > 0 && `Competências: ${competencies.join(", ")}`,
    resultUse.trim() && `Uso do resultado: ${resultUse.trim()}`,
  ].filter((line): line is string => Boolean(line));

  const description = lines.join("\n");
  return description.length > 1000 ? `${description.slice(0, 997).trimEnd()}...` : description;
}

function buildCompetencyWeights(
  competencies: string[],
  existingCompetencies?: { name: string; weight: number }[],
) {
  const uniqueCompetencies = [...new Set(competencies.map((competency) => competency.trim()))].filter(
    Boolean,
  );
  const normalizedUniqueCompetencies = uniqueCompetencies.map(normalizeLabel).sort();
  const normalizedExistingCompetencies = (existingCompetencies ?? [])
    .map((competency) => normalizeLabel(competency.name))
    .sort();

  if (
    existingCompetencies &&
    normalizedUniqueCompetencies.length === normalizedExistingCompetencies.length &&
    normalizedUniqueCompetencies.every(
      (competency, index) => competency === normalizedExistingCompetencies[index],
    )
  ) {
    return uniqueCompetencies.map((name) => {
      const existingCompetency = existingCompetencies.find(
        (competency) => normalizeLabel(competency.name) === normalizeLabel(name),
      );
      return { name, weight: existingCompetency?.weight ?? 1 / uniqueCompetencies.length };
    });
  }

  const weight = uniqueCompetencies.length > 0 ? 1 / uniqueCompetencies.length : 1;
  return uniqueCompetencies.map((name) => ({ name, weight }));
}

function parseBlueprintDescription(description: string) {
  const fields = {
    role: "",
    criticalSituation: "",
    resultUse: "",
  };

  for (const line of description.split("\n")) {
    const separatorIndex = line.indexOf(":");
    if (separatorIndex < 0) {
      continue;
    }

    const label = normalizeLabel(line.slice(0, separatorIndex));
    const value = line.slice(separatorIndex + 1).trim();

    if (label === "cargo") {
      fields.role = value;
    } else if (label.includes("situa") && label.includes("cr")) {
      fields.criticalSituation = value;
    } else if (label === "uso do resultado") {
      fields.resultUse = value;
    }
  }

  return fields;
}

function normalizeLabel(value: string) {
  return value
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .trim()
    .toLowerCase();
}

function Header({ kicker, title, lede }: { kicker: string; title: string; lede: string }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-[0.2em] text-foreground">{kicker}</div>
      <h1 className="mt-1 font-display text-3xl">{title}</h1>
      <p className="mt-2 max-w-2xl text-sm text-muted-foreground">{lede}</p>
    </div>
  );
}

function Card({
  title,
  children,
  tone,
  required,
}: {
  title: string;
  children: React.ReactNode;
  tone?: "ok" | "danger";
  required?: boolean;
}) {
  const accent =
    tone === "ok"
      ? "before:bg-success"
      : tone === "danger"
        ? "before:bg-danger"
        : "before:bg-transparent";
  return (
    <section
      className={`relative overflow-hidden rounded-xl border border-border bg-card p-5 before:absolute before:left-0 before:top-0 before:h-full before:w-1 ${accent}`}
    >
      <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
        {title}
        {required && (
          <span className="rounded-full border border-danger/40 bg-danger/10 px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide text-danger">
            Obrigatório
          </span>
        )}
      </h2>
      {children}
    </section>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium text-muted-foreground">{label}</span>
      {children}
    </label>
  );
}

function FieldMeta({ error, count, max }: { error?: string; count: number; max: number }) {
  return (
    <div className="mt-1 flex items-center justify-between gap-3 text-xs">
      <span className={error ? "text-danger" : "text-muted-foreground"}>{error ?? " "}</span>
      <span className="shrink-0 text-muted-foreground">
        {count}/{max}
      </span>
    </div>
  );
}

function Help({ children }: { children: React.ReactNode }) {
  return <p className="mt-2 text-xs text-muted-foreground">{children}</p>;
}
