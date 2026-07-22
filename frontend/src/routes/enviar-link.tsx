import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, Copy, Link2, Mail, MessageCircle, Send } from "lucide-react";
import { useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { createCandidateLink, type CandidateLinkOperation } from "@/lib/api/candidate-links";
import { listSimulations, type SimulationSummaryResponse } from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/enviar-link")({
  head: () => ({
    meta: [
      { title: "Nova participação individual - Práxis" },
      {
        name: "description",
        content:
          "Crie uma participação individual e acompanhe o convite pela central de participações.",
      },
    ],
  }),
  component: EnviarLinkPage,
});

type Step = "assessment" | "participant" | "share";

type FormErrors = {
  candidateName?: string;
  candidateEmail?: string;
};

function EnviarLinkPage() {
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>("assessment");
  const [selectedSimulation, setSelectedSimulation] = useState<SimulationSummaryResponse | null>(
    null,
  );
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [generatedLink, setGeneratedLink] = useState("");
  const [generatedOperation, setGeneratedOperation] = useState<CandidateLinkOperation | null>(null);
  const [copied, setCopied] = useState(false);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });

  const publishedSimulations = useMemo(
    () =>
      (simulationsQuery.data ?? []).filter(
        (simulation) =>
          simulation.status === "published" || simulation.livePublishedVersionNumber != null,
      ),
    [simulationsQuery.data],
  );

  const createMutation = useMutation({
    mutationFn: createCandidateLink,
    onSuccess: async (response) => {
      setGeneratedLink(toAbsoluteUrl(response.candidateUrl));
      setGeneratedOperation(response.operation);
      setStep("share");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["candidate-links"] }),
        queryClient.invalidateQueries({ queryKey: ["participations"] }),
      ]);
    },
  });

  function selectSimulation(simulation: SimulationSummaryResponse) {
    setSelectedSimulation(simulation);
    setFormErrors({});
    setStep("participant");
  }

  function createApplication() {
    if (!selectedSimulation) return;

    const errors: FormErrors = {};
    if (!candidateName.trim()) errors.candidateName = "Informe o nome da pessoa participante.";
    if (!isValidEmail(candidateEmail)) errors.candidateEmail = "Informe um e-mail válido.";
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) return;

    createMutation.mutate({
      simulationId: selectedSimulation.id,
      candidateName: candidateName.trim(),
      candidateEmail: candidateEmail.trim(),
      applicationCycleId: newApplicationCycleId(),
      applicationContext: `Participação individual - ${selectedSimulation.name}`,
    });
  }

  async function copyGeneratedLink() {
    await navigator.clipboard.writeText(generatedLink);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2000);
  }

  function shareByEmail() {
    const subject = encodeURIComponent(
      `Avaliação Práxis - ${selectedSimulation?.name ?? "avaliação"}`,
    );
    const body = encodeURIComponent(
      `Olá, ${candidateName}.\n\nAcesse sua avaliação pelo link abaixo:\n${generatedLink}`,
    );
    window.open(`mailto:${candidateEmail}?subject=${subject}&body=${body}`);
  }

  function shareByWhatsApp() {
    const text = encodeURIComponent(
      `Olá, ${candidateName}. Acesse sua avaliação Práxis: ${generatedLink}`,
    );
    window.open(`https://wa.me/?text=${text}`, "_blank", "noopener,noreferrer");
  }

  function reset() {
    setStep("assessment");
    setSelectedSimulation(null);
    setCandidateName("");
    setCandidateEmail("");
    setFormErrors({});
    setGeneratedLink("");
    setGeneratedOperation(null);
    setCopied(false);
    createMutation.reset();
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-6xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Participação individual
            </div>
            <h1 className="mt-1 font-display text-3xl">Aplicar avaliação individual</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Use este fluxo quando a avaliação não fizer parte de uma jornada. Depois da criação,
              reenvio, validade e acompanhamento ficam centralizados em Participações.
            </p>
          </div>
          <Button asChild variant="outline" className="bg-card">
            <Link to="/participacoes">Abrir participações</Link>
          </Button>
        </header>

        <StepIndicator step={step} />

        {createMutation.isError && (
          <StateBanner tone="danger" title="Não foi possível criar a participação">
            {createMutation.error instanceof Error
              ? createMutation.error.message
              : "Verifique os dados e tente novamente."}
          </StateBanner>
        )}

        {step === "assessment" && (
          <AssessmentStep
            simulations={publishedSimulations}
            loading={simulationsQuery.isLoading}
            error={simulationsQuery.error}
            onSelect={selectSimulation}
          />
        )}

        {step === "participant" && selectedSimulation && (
          <ParticipantStep
            simulation={selectedSimulation}
            candidateName={candidateName}
            candidateEmail={candidateEmail}
            errors={formErrors}
            loading={createMutation.isPending}
            onNameChange={(value) => {
              setCandidateName(value);
              setFormErrors((current) => ({ ...current, candidateName: undefined }));
            }}
            onEmailChange={(value) => {
              setCandidateEmail(value);
              setFormErrors((current) => ({ ...current, candidateEmail: undefined }));
            }}
            onBack={() => setStep("assessment")}
            onSubmit={createApplication}
          />
        )}

        {step === "share" && selectedSimulation && (
          <ShareStep
            simulation={selectedSimulation}
            candidateName={candidateName}
            candidateEmail={candidateEmail}
            link={generatedLink}
            operation={generatedOperation}
            copied={copied}
            onCopy={copyGeneratedLink}
            onEmail={shareByEmail}
            onWhatsApp={shareByWhatsApp}
            onReset={reset}
          />
        )}
      </main>
    </AppShell>
  );
}

function StepIndicator({ step }: { step: Step }) {
  const items: Array<{ value: Step; label: string }> = [
    { value: "assessment", label: "1. Avaliação" },
    { value: "participant", label: "2. Participante" },
    { value: "share", label: "3. Compartilhar" },
  ];
  const currentIndex = items.findIndex((item) => item.value === step);

  return (
    <ol className="grid gap-2 sm:grid-cols-3" aria-label="Etapas da participação individual">
      {items.map((item, index) => (
        <li
          key={item.value}
          className={cn(
            "rounded-md border px-4 py-3 text-sm font-medium",
            index === currentIndex
              ? "border-primary/40 bg-primary/10 text-primary"
              : index < currentIndex
                ? "border-success/30 bg-success/10 text-success"
                : "border-border bg-card text-muted-foreground",
          )}
        >
          {item.label}
        </li>
      ))}
    </ol>
  );
}

function AssessmentStep({
  simulations,
  loading,
  error,
  onSelect,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
  error: unknown;
  onSelect: (simulation: SimulationSummaryResponse) => void;
}) {
  if (loading) {
    return (
      <StateBanner tone="info" title="Carregando avaliações">
        Aguarde a consulta das versões publicadas.
      </StateBanner>
    );
  }
  if (error) {
    return (
      <StateBanner tone="danger" title="Não foi possível carregar as avaliações">
        {error instanceof Error ? error.message : "Tente novamente."}
      </StateBanner>
    );
  }
  if (simulations.length === 0) {
    return (
      <EmptyState
        title="Nenhuma avaliação publicada"
        description="Publique uma avaliação antes de criar uma participação individual."
        actions={
          <Button asChild>
            <Link
              to="/nova/avaliacao"
              search={{ simulationId: undefined, versionNumber: undefined }}
            >
              Criar avaliação
            </Link>
          </Button>
        }
      />
    );
  }

  return (
    <section className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="border-b border-border p-5">
        <h2 className="text-lg font-semibold">Selecione a avaliação publicada</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          A participação será independente de jornadas e processos compostos.
        </p>
      </div>
      <div className="divide-y divide-border">
        {simulations.map((simulation) => (
          <button
            key={simulation.id}
            type="button"
            onClick={() => onSelect(simulation)}
            className="flex w-full items-center justify-between gap-4 p-5 text-left hover:bg-accent"
          >
            <span className="min-w-0">
              <span className="block truncate font-medium">{simulation.name}</span>
              <span className="mt-1 block text-sm text-muted-foreground">
                {simulation.description || "Sem descrição."}
              </span>
            </span>
            <span className="shrink-0 text-sm font-medium text-primary">Selecionar</span>
          </button>
        ))}
      </div>
    </section>
  );
}

function ParticipantStep({
  simulation,
  candidateName,
  candidateEmail,
  errors,
  loading,
  onNameChange,
  onEmailChange,
  onBack,
  onSubmit,
}: {
  simulation: SimulationSummaryResponse;
  candidateName: string;
  candidateEmail: string;
  errors: FormErrors;
  loading: boolean;
  onNameChange: (value: string) => void;
  onEmailChange: (value: string) => void;
  onBack: () => void;
  onSubmit: () => void;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-6">
      <div className="rounded-lg border border-primary/20 bg-primary/5 p-4">
        <div className="text-xs font-semibold uppercase tracking-wide text-primary">
          Avaliação selecionada
        </div>
        <div className="mt-1 font-medium">{simulation.name}</div>
      </div>
      <div className="mt-6 grid gap-4 md:grid-cols-2">
        <label className="space-y-1.5 text-sm font-medium">
          Nome da pessoa participante
          <input
            value={candidateName}
            onChange={(event) => onNameChange(event.target.value)}
            className={cn("input w-full", errors.candidateName && "border-danger")}
            placeholder="Nome completo"
            aria-invalid={Boolean(errors.candidateName)}
          />
          {errors.candidateName && (
            <span className="block text-xs text-danger">{errors.candidateName}</span>
          )}
        </label>
        <label className="space-y-1.5 text-sm font-medium">
          E-mail
          <input
            type="email"
            value={candidateEmail}
            onChange={(event) => onEmailChange(event.target.value)}
            className={cn("input w-full", errors.candidateEmail && "border-danger")}
            placeholder="pessoa@empresa.com"
            aria-invalid={Boolean(errors.candidateEmail)}
          />
          {errors.candidateEmail && (
            <span className="block text-xs text-danger">{errors.candidateEmail}</span>
          )}
        </label>
      </div>
      <div className="mt-6 flex flex-wrap gap-3">
        <Button type="button" variant="outline" onClick={onBack} disabled={loading}>
          Voltar
        </Button>
        <Button type="button" onClick={onSubmit} disabled={loading} className="gap-2">
          <Link2 className="h-4 w-4" />
          {loading ? "Criando..." : "Criar participação"}
        </Button>
      </div>
    </section>
  );
}

function ShareStep({
  simulation,
  candidateName,
  candidateEmail,
  link,
  operation,
  copied,
  onCopy,
  onEmail,
  onWhatsApp,
  onReset,
}: {
  simulation: SimulationSummaryResponse;
  candidateName: string;
  candidateEmail: string;
  link: string;
  operation: CandidateLinkOperation | null;
  copied: boolean;
  onCopy: () => Promise<void>;
  onEmail: () => void;
  onWhatsApp: () => void;
  onReset: () => void;
}) {
  return (
    <div className="space-y-6">
      <StateBanner tone="ok" title="Participação criada">
        {operationLabel(operation)} O acompanhamento e as próximas ações estão disponíveis em
        Participações.
      </StateBanner>
      <section className="rounded-xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold">Compartilhar acesso</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {candidateName} · {candidateEmail} · {simulation.name}
        </p>
        <div className="mt-5 flex flex-col gap-3 rounded-lg border border-border bg-background p-4 sm:flex-row sm:items-center">
          <code className="min-w-0 flex-1 overflow-hidden text-ellipsis whitespace-nowrap text-sm">
            {link}
          </code>
          <Button type="button" variant="outline" onClick={() => void onCopy()} className="gap-2">
            {copied ? (
              <CheckCircle2 className="h-4 w-4 text-success" />
            ) : (
              <Copy className="h-4 w-4" />
            )}
            {copied ? "Copiado" : "Copiar link"}
          </Button>
        </div>
        <div className="mt-4 flex flex-wrap gap-3">
          <Button type="button" variant="outline" onClick={onEmail} className="gap-2">
            <Mail className="h-4 w-4" />
            Enviar por e-mail
          </Button>
          <Button type="button" variant="outline" onClick={onWhatsApp} className="gap-2">
            <MessageCircle className="h-4 w-4" />
            Compartilhar no WhatsApp
          </Button>
        </div>
      </section>
      <div className="flex flex-wrap gap-3">
        <Button type="button" onClick={onReset} className="gap-2">
          <Send className="h-4 w-4" />
          Criar outra participação
        </Button>
        <Button asChild variant="outline">
          <Link to="/participacoes">Gerenciar em Participações</Link>
        </Button>
      </div>
    </div>
  );
}

function isValidEmail(value: string) {
  const normalized = value.trim();
  return normalized.includes("@") && normalized.lastIndexOf(".") > normalized.indexOf("@") + 1;
}

function newApplicationCycleId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `manual-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}

function toAbsoluteUrl(value: string) {
  if (typeof window === "undefined") return value;
  try {
    return new URL(value, window.location.origin).toString();
  } catch {
    return value;
  }
}

function operationLabel(operation: CandidateLinkOperation | null) {
  if (operation === "REUSED_IDEMPOTENT_REQUEST")
    return "Uma solicitação idêntica já existia e foi reutilizada.";
  if (operation === "RESENT_EXISTING_LINK") return "O acesso existente foi reenviado.";
  if (operation === "EXTENDED_LINK_VALIDITY") return "A validade do acesso existente foi ampliada.";
  return "Uma nova tentativa independente foi criada.";
}
