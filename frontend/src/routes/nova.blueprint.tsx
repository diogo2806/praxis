import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import { createSimulationDraft, updateTenantConfig, type TenantConfigOption } from "@/lib/api/praxis";
import { useTenantConfig } from "@/lib/tenant-config";

export const Route = createFileRoute("/nova/blueprint")({
  head: () => ({
    meta: [
      { title: "Blueprint — Práxis" },
      {
        name: "description",
        content:
          "Defina cargo, situação crítica, competências, comportamentos esperados e erros críticos.",
      },
    ],
  }),
  component: Page,
});

function Page() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { config } = useTenantConfig();
  const competencies = config.competencies;
  const seniorityLevels = config.seniorityLevels;
  const resultUses = config.resultUses;
  const defaultSeniority = getDefaultOption(seniorityLevels)?.value ?? "";
  const defaultResultUse = getDefaultOption(resultUses)?.value ?? "";
  const [role, setRole] = useState("");
  const [selectedSeniority, setSelectedSeniority] = useState(defaultSeniority);
  const [criticalSituation, setCriticalSituation] = useState("");
  const [highPerformance, setHighPerformance] = useState("");
  const [criticalError, setCriticalError] = useState("");
  const [seniorityExpectations, setSeniorityExpectations] = useState<Record<string, string>>({});
  const [selectedResultUse, setSelectedResultUse] = useState(defaultResultUse);
  const [selectedCompetencies, setSelectedCompetencies] = useState<string[]>([]);
  const [newCompetency, setNewCompetency] = useState("");
  const missingFields = [
    role.trim().length === 0 && "cargo-alvo",
    criticalSituation.trim().length === 0 && "situação crítica",
    criticalError.trim().length === 0 && "erros críticos",
    selectedCompetencies.length === 0 && "ao menos uma competência",
  ].filter((field): field is string => Boolean(field));
  const canGoNext =
    role.trim().length > 0 &&
    criticalSituation.trim().length > 0 &&
    criticalError.trim().length > 0 &&
    selectedCompetencies.length > 0;
  const normalizedCompetencies = competencies.map((competency) => competency.value.trim().toLowerCase());
  const canAddCompetency =
    newCompetency.trim().length > 0 &&
    !normalizedCompetencies.includes(newCompetency.trim().toLowerCase());

  useEffect(() => {
    if (!selectedSeniority || !seniorityLevels.some((level) => level.value === selectedSeniority)) {
      setSelectedSeniority(getDefaultOption(seniorityLevels)?.value ?? "");
    }
  }, [selectedSeniority, seniorityLevels]);

  useEffect(() => {
    if (!selectedResultUse || !resultUses.some((use) => use.value === selectedResultUse)) {
      setSelectedResultUse(getDefaultOption(resultUses)?.value ?? "");
    }
  }, [selectedResultUse, resultUses]);

  const createDraftMutation = useMutation({
    mutationFn: () =>
      createSimulationDraft({
        name: role.trim(),
        description: buildBlueprintDescription({
          role,
          seniority: selectedSeniority,
          criticalSituation,
          competencies: selectedCompetencies,
          highPerformance,
          criticalError,
          seniorityExpectations,
          resultUse: selectedResultUse,
        }),
        rootNodeId: "turno-1",
        competencies: selectedCompetencies,
        seniority: selectedSeniority,
        criticalSituation: criticalSituation.trim(),
        highPerformance: highPerformance.trim(),
        criticalError: criticalError.trim(),
        seniorityExpectations: trimRecordValues(seniorityExpectations),
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
      <ScreenStateStrip blockedReason="cargo, situação crítica e erro crítico obrigatórios" />

      {createDraftMutation.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title="Não foi possível criar o rascunho">
            {createDraftMutation.error instanceof Error
              ? createDraftMutation.error.message
              : "Tente novamente quando a API estiver disponível."}
          </StateBanner>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <Header
            kicker="Passo 1"
            title="Blueprint da avaliação"
            lede="Antes de escrever qualquer diálogo, defina o porquê. O blueprint vira referência fixa para o Validador de Qualidade."
          />

          <Card title="Cargo e senioridade">
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Cargo-alvo">
                <input
                  className="input"
                  placeholder="Ex.: Analista de Atendimento N2"
                  value={role}
                  onChange={(event) => setRole(event.target.value)}
                />
              </Field>
              <Field label="Senioridade">
                <div className="flex gap-2">
                  {seniorityLevels.map((level) => (
                    <label
                      key={level.value}
                      className={`flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-md border px-3 py-2 text-sm ${
                        selectedSeniority === level.value
                          ? "border-primary bg-primary/5 text-foreground"
                          : "border-border bg-card hover:bg-accent"
                      }`}
                    >
                      <input
                        type="radio"
                        name="sen"
                        checked={selectedSeniority === level.value}
                        onChange={() => setSelectedSeniority(level.value)}
                        className="sr-only"
                      />
                      {level.label}
                    </label>
                  ))}
                </div>
              </Field>
            </div>
          </Card>

          <Card title="Situação crítica do cargo">
            <textarea
              aria-label="Situação crítica do cargo"
              className="input min-h-24"
              value={criticalSituation}
              onChange={(event) => setCriticalSituation(event.target.value)}
            />
            <Help>Use situação que de fato acontece nesta empresa, não um caso genérico.</Help>
          </Card>

          <Card title="Competências avaliadas">
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
            <Help>Ao salvar, a competência entra no catálogo deste tenant e fica disponível só para esta empresa.</Help>
            {addCompetencyMutation.isError && (
              <p className="mt-2 text-xs text-danger">
                {addCompetencyMutation.error instanceof Error
                  ? addCompetencyMutation.error.message
                  : "Nao foi possivel salvar a competencia."}
              </p>
            )}
            <Help>Competências customizadas podem ser mapeadas para a taxonomia interna.</Help>
          </Card>

          <div className="grid gap-6 md:grid-cols-2">
            <Card title="Alta performance faria" tone="ok">
              <textarea
                aria-label="Alta performance faria"
                className="input min-h-24"
                placeholder="Descreva o comportamento esperado para alta performance."
                value={highPerformance}
                onChange={(event) => setHighPerformance(event.target.value)}
              />
            </Card>
            <Card title="Erros críticos" tone="danger">
              <textarea
                aria-label="Erros críticos"
                className="input min-h-24"
                value={criticalError}
                onChange={(event) => setCriticalError(event.target.value)}
              />
              <Help>Dispara revisão humana obrigatória e bloqueia recomendação sem validação.</Help>
            </Card>
          </div>

          <Card title="Diferença por senioridade">
            <div className="grid gap-3 md:grid-cols-3">
              {seniorityLevels.map((level) => (
                <Field key={level.value} label={`${level.label} faria`}>
                  <textarea
                    className="input min-h-20"
                    placeholder={`Comportamento esperado para ${level.label.toLowerCase()}.`}
                    value={seniorityExpectations[level.value] ?? ""}
                    onChange={(event) =>
                      setSeniorityExpectations((current) => ({
                        ...current,
                        [level.value]: event.target.value,
                      }))
                    }
                  />
                </Field>
              ))}
            </div>
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

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Cancelar
            </Link>
            <div className="flex flex-col items-start gap-2 sm:items-end">
              {!canGoNext && (
                <p className="text-xs text-muted-foreground" aria-live="polite">
                  Para avançar, preencha {missingFields.join(", ")}.
                </p>
              )}
              <button
                type="button"
                disabled={!canGoNext || createDraftMutation.isPending}
                title={
                  canGoNext
                    ? "Criar rascunho e avançar"
                    : "Preencha cargo, situação crítica, erro crítico e competências para avançar"
                }
                onClick={() => createDraftMutation.mutate()}
                className={`rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                  !canGoNext || createDraftMutation.isPending ? "cursor-not-allowed opacity-50" : ""
                }`}
              >
                {createDraftMutation.isPending ? "Criando rascunho..." : "Próximo: Cenário"}
              </button>
            </div>
          </div>
        </div>

        <aside className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-5">
            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Por que blueprint?
            </div>
            <p className="mt-2 text-sm text-foreground/80">
              O blueprint vira referência fixa para o Validador checar se a simulação mede o que
              prometeu.
            </p>
          </div>
          <div className="rounded-xl border border-warning/30 bg-warning/10 p-5">
            <div className="text-xs font-semibold uppercase tracking-wider text-warning-foreground">
              Rubrica, peso e cálculo
            </div>
            <p className="mt-2 text-sm text-foreground/80">
              A nota sai de regras declaradas. O blueprint registra o porquê comportamental que a
              auditoria vai pedir.
            </p>
          </div>
        </aside>
      </div>
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
  seniority,
  criticalSituation,
  competencies,
  highPerformance,
  criticalError,
  seniorityExpectations,
  resultUse,
}: {
  role: string;
  seniority: string;
  criticalSituation: string;
  competencies: string[];
  highPerformance: string;
  criticalError: string;
  seniorityExpectations: Record<string, string>;
  resultUse: string;
}) {
  const seniorityLines = Object.entries(seniorityExpectations)
    .map(([level, expected]) => [level.trim(), expected.trim()] as const)
    .filter(([, expected]) => expected.length > 0)
    .map(([level, expected]) => `${level}: ${expected}`);

  const lines = [
    `Cargo: ${role.trim()}`,
    seniority.trim() && `Senioridade: ${seniority.trim()}`,
    `Situação crítica: ${criticalSituation.trim()}`,
    competencies.length > 0 && `Competências: ${competencies.join(", ")}`,
    `Erro crítico: ${criticalError.trim()}`,
    highPerformance.trim() && `Alta performance faria: ${highPerformance.trim()}`,
    seniorityLines.length > 0 && `Diferença por senioridade: ${seniorityLines.join(" | ")}`,
    resultUse.trim() && `Uso do resultado: ${resultUse.trim()}`,
  ].filter((line): line is string => Boolean(line));

  const description = lines.join("\n");
  return description.length > 1000 ? `${description.slice(0, 997).trimEnd()}...` : description;
}

function trimRecordValues(values: Record<string, string>) {
  return Object.fromEntries(
    Object.entries(values)
      .map(([key, value]) => [key, value.trim()] as const)
      .filter(([, value]) => value.length > 0),
  );
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
}: {
  title: string;
  children: React.ReactNode;
  tone?: "ok" | "danger";
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
      <h2 className="mb-3 text-sm font-semibold text-foreground">{title}</h2>
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

function Help({ children }: { children: React.ReactNode }) {
  return <p className="mt-2 text-xs text-muted-foreground">{children}</p>;
}
