import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { AppShell } from "@/components/app-shell";
import { ScreenStateStrip } from "@/components/praxis-ui";
import { WizardStepper } from "@/components/wizard-stepper";

export const Route = createFileRoute("/nova/blueprint")({
  head: () => ({
    meta: [
      { title: "Blueprint da Avaliação · Nova simulação" },
      {
        name: "description",
        content:
          "Defina por que essa avaliação é relevante: cargo, situação crítica, competências, comportamentos de alta performance e erros críticos.",
      },
    ],
  }),
  component: Page,
});

const competencies = [
  "Empatia",
  "Resolução de Conflitos",
  "Aderência à Política",
  "Comunicação",
  "Negociação",
  "Tomada de Decisão",
  "Liderança",
  "Proatividade",
];

function Page() {
  const [role, setRole] = useState("Analista de Atendimento N2");
  const [criticalSituation, setCriticalSituation] = useState(
    "Cliente quer estorno fora da política, com risco de churn de conta grande.",
  );
  const [criticalError, setCriticalError] = useState("Prometer estorno sem validar política.");
  const canGoNext =
    role.trim().length > 0 &&
    criticalSituation.trim().length > 0 &&
    criticalError.trim().length > 0;

  return (
    <AppShell>
      <WizardStepper current="blueprint" />
      <ScreenStateStrip blockedReason="cargo, situacao critica e erro critico obrigatorios" />

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <Header
            kicker="Passo 0"
            title="Blueprint da Avaliação"
            lede="Antes de escrever qualquer diálogo, defina o porquê. Sem isso, você cria uma história interessante que talvez não meça nada útil. O blueprint vira referência fixa para o Validador de Qualidade."
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
                  {["Júnior", "Pleno", "Sênior"].map((s, i) => (
                    <label
                      key={s}
                      className={`flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-md border px-3 py-2 text-sm ${
                        i === 1
                          ? "border-primary bg-primary/5 text-primary"
                          : "border-border bg-card hover:bg-accent"
                      }`}
                    >
                      <input type="radio" name="sen" defaultChecked={i === 1} className="sr-only" />
                      {s}
                    </label>
                  ))}
                </div>
              </Field>
            </div>
          </Card>

          <Card title="Situação crítica REAL do cargo">
            <textarea
              className="input min-h-24"
              value={criticalSituation}
              onChange={(event) => setCriticalSituation(event.target.value)}
            />
            <Help>Use situação que de fato acontece nesta empresa, não um caso genérico.</Help>
          </Card>

          <Card title="Competências avaliadas">
            <div className="flex flex-wrap gap-2">
              {competencies.map((c, i) => (
                <label
                  key={c}
                  className={`cursor-pointer rounded-full border px-3 py-1.5 text-sm ${
                    i < 3
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border bg-card text-foreground/75 hover:bg-accent"
                  }`}
                >
                  <input type="checkbox" defaultChecked={i < 3} className="sr-only" />
                  {c}
                </label>
              ))}
            </div>
            <Help>
              Da taxonomia interna. Competências customizadas serão mapeadas para esta lista.
            </Help>
          </Card>

          <div className="grid gap-6 md:grid-cols-2">
            <Card title="Alta performance faria…" tone="ok">
              <textarea
                className="input min-h-24"
                defaultValue="Acolhe, coleta dados mínimos, explica limite de alçada e oferece alternativa válida."
              />
            </Card>
            <Card title="Erros CRÍTICOS" tone="danger">
              <textarea
                className="input min-h-24"
                value={criticalError}
                onChange={(event) => setCriticalError(event.target.value)}
              />
              <Help>
                Dispara <b>revisão humana obrigatória</b>, bloqueia recomendação automática. Não
                reprova sozinho.
              </Help>
            </Card>
          </div>

          <Card title="Diferença por senioridade">
            <div className="grid gap-3 md:grid-cols-3">
              <Field label="Júnior faria…">
                <textarea className="input min-h-20" defaultValue="Acolhe e escala para o líder." />
              </Field>
              <Field label="Pleno faria…">
                <textarea
                  className="input min-h-20"
                  defaultValue="Acolhe, decide dentro da alçada e registra."
                />
              </Field>
              <Field label="Sênior faria…">
                <textarea
                  className="input min-h-20"
                  defaultValue="Conduz, ajusta política se necessário, fecha com cliente."
                />
              </Field>
            </div>
          </Card>

          <Card title="Uso do resultado">
            <div className="grid gap-2 md:grid-cols-4">
              {[
                { label: "Triagem", on: true },
                { label: "Ranking", on: false },
                { label: "Apoio à entrevista", on: false },
                { label: "🔒 Eliminação", on: false, locked: true },
              ].map((o) => (
                <label
                  key={o.label}
                  className={`flex cursor-pointer items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                    o.on
                      ? "border-primary bg-primary/5 text-primary"
                      : o.locked
                        ? "border-dashed border-border bg-muted/40 text-muted-foreground"
                        : "border-border bg-card hover:bg-accent"
                  }`}
                >
                  <input type="radio" name="uso" defaultChecked={o.on} className="sr-only" />
                  {o.label}
                </label>
              ))}
            </div>
            <Help>
              Eliminação só para simulação <b>validada</b>, com aprovação de gestor/compliance e
              canal de revisão para o candidato (LGPD art. 20).
            </Help>
          </Card>

          <div className="flex items-center justify-between">
            <Link
              to="/"
              className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
            >
              ← Cancelar
            </Link>
            {canGoNext ? (
              <Link
                to="/nova/objetivo"
                className="rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Próximo: Objetivo
              </Link>
            ) : (
              <button
                type="button"
                disabled
                title="Preencha cargo, situação crítica e erro crítico para avançar"
                className="cursor-not-allowed rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground opacity-50"
              >
                Próximo: Objetivo
              </button>
            )}
          </div>
        </div>

        <aside className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-5">
            <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Por que blueprint?
            </div>
            <p className="mt-2 text-sm text-foreground/80">
              O blueprint vira referência fixa para o Validador (Passo 3.5) checar se a simulação
              realmente mede o que prometeu — não só se está bonita.
            </p>
          </div>
          <div className="rounded-xl border border-warning/30 bg-warning/10 p-5">
            <div className="text-xs font-semibold uppercase tracking-wider text-warning-foreground">
              Sem IA, com responsabilidade
            </div>
            <p className="mt-2 text-sm text-foreground/80">
              A nota sai de rubrica + peso + cálculo. O blueprint registra o porquê comportamental —
              e é o que a auditoria vai pedir.
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
