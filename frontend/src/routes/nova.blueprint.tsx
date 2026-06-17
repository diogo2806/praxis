import { useMutation } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip, StateBanner } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";
import { createSimulationDraft } from "@/lib/api/praxis";
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
  const { config } = useTenantConfig();
  const competencies = config.competencies;
  const seniorityLevels = config.seniorityLevels;
  const resultUses = config.resultUses;
  const [role, setRole] = useState("");
  const [criticalSituation, setCriticalSituation] = useState("");
  const [criticalError, setCriticalError] = useState("");
  const [selectedCompetencies, setSelectedCompetencies] = useState<string[]>([]);
  const canGoNext =
    role.trim().length > 0 &&
    criticalSituation.trim().length > 0 &&
    criticalError.trim().length > 0 &&
    selectedCompetencies.length > 0;
  const createDraftMutation = useMutation({
    mutationFn: () =>
      createSimulationDraft({
        name: role.trim(),
        description: `${criticalSituation.trim()}\n\nErro critico: ${criticalError.trim()}`,
        rootNodeId: "turno-1",
        competencies: selectedCompetencies,
      }),
    onSuccess: (simulation) => {
      void navigate({
        to: "/nova/objetivo",
        search: {
          simulationId: simulation.id,
          versionNumber: simulation.versionNumber,
        },
      });
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
      <WizardStepper current="blueprint" />
      <ScreenStateStrip blockedReason="cargo, situação crítica e erro crítico obrigatórios" />

      {createDraftMutation.isError && (
        <div className="mb-5">
          <StateBanner tone="danger" title="Nao foi possivel criar o rascunho">
            {createDraftMutation.error instanceof Error
              ? createDraftMutation.error.message
              : "Tente novamente quando a API estiver disponivel."}
          </StateBanner>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <Header
            kicker="Passo 0"
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
                        level.selectedByDefault
                          ? "border-primary bg-primary/5 text-primary"
                          : "border-border bg-card hover:bg-accent"
                      }`}
                    >
                      <input
                        type="radio"
                        name="sen"
                        defaultChecked={level.selectedByDefault}
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
                      ? "border-primary bg-primary/10 text-primary"
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
            <Help>Competências customizadas podem ser mapeadas para a taxonomia interna.</Help>
          </Card>

          <div className="grid gap-6 md:grid-cols-2">
            <Card title="Alta performance faria" tone="ok">
              <textarea
                className="input min-h-24"
                placeholder="Descreva o comportamento esperado para alta performance."
              />
            </Card>
            <Card title="Erros críticos" tone="danger">
              <textarea
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
                    use.selectedByDefault
                      ? "border-primary bg-primary/5 text-primary"
                      : use.locked
                        ? "border-dashed border-border bg-muted/40 text-muted-foreground"
                        : "border-border bg-card hover:bg-accent"
                  }`}
                >
                  <input
                    type="radio"
                    name="uso"
                    defaultChecked={use.selectedByDefault}
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

          <div className="flex items-center justify-between">
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              Cancelar
            </Link>
            <button
              type="button"
              disabled={!canGoNext || createDraftMutation.isPending}
              title={
                canGoNext
                  ? "Criar rascunho e avancar"
                  : "Preencha cargo, situacao critica, erro critico e competencias para avancar"
              }
              onClick={() => createDraftMutation.mutate()}
              className={`rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 ${
                !canGoNext || createDraftMutation.isPending ? "cursor-not-allowed opacity-50" : ""
              }`}
            >
              {createDraftMutation.isPending ? "Criando rascunho..." : "Proximo: Objetivo"}
            </button>
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

function Header({ kicker, title, lede }: { kicker: string; title: string; lede: string }) {
  return (
    <div>
      <div className="text-xs uppercase tracking-[0.2em] text-primary">{kicker}</div>
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
      <h3 className="mb-3 text-sm font-semibold text-foreground">{title}</h3>
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
