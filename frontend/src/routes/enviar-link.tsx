import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { CheckCircle2, Copy, Link2, Mail, MessageCircle, Send } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EmptyState,
  ScreenStateStrip,
  SkeletonRows,
  StateBanner,
  StatusBadge,
} from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  createCandidateLink,
  listCandidateLinks,
  listSimulations,
  type CandidateLinkResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import { maturityForStatus } from "@/lib/simulation-meta";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/enviar-link")({
  head: () => ({
    meta: [
      { title: "Compartilhar avaliação - Praxis" },
      {
        name: "description",
        content: "Gere um link de avaliação para compartilhar por e-mail ou WhatsApp.",
      },
    ],
  }),
  component: EnviarLinkPage,
});

type Step = "select" | "form" | "share";

function EnviarLinkPage() {
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>("select");
  const [selectedSimulation, setSelectedSimulation] = useState<SimulationSummaryResponse | null>(
    null,
  );
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [generatedLink, setGeneratedLink] = useState("");
  const [simulationName, setSimulationName] = useState("");
  const [copied, setCopied] = useState(false);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const candidateLinksQuery = useQuery({
    queryKey: ["candidate-links", { blind: false }],
    queryFn: () => listCandidateLinks(false),
    retry: false,
  });

  const linkMutation = useMutation({
    mutationFn: createCandidateLink,
    onSuccess: (data) => {
      setGeneratedLink(data.candidateUrl);
      setSimulationName(data.simulationName);
      setStep("share");
      void queryClient.invalidateQueries({ queryKey: ["candidate-links"] });
    },
  });

  const publishedSimulations = (simulationsQuery.data ?? []).filter(
    (s) => s.status === "published",
  );

  function handleSelectSimulation(simulation: SimulationSummaryResponse) {
    setSelectedSimulation(simulation);
    setStep("form");
  }

  function handleGenerateLink() {
    if (!selectedSimulation || !candidateName.trim() || !candidateEmail.trim()) return;
    linkMutation.mutate({
      simulationId: selectedSimulation.id,
      candidateName: candidateName.trim(),
      candidateEmail: candidateEmail.trim(),
    });
  }

  function handleCopy() {
    navigator.clipboard.writeText(generatedLink).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }

  function handleShareEmail() {
    const subject = encodeURIComponent(`Avaliação: ${simulationName}`);
    const body = encodeURIComponent(
      `Olá ${candidateName},\n\nVocê foi convidado(a) para participar de uma avaliação situacional.\n\nAcesse o link abaixo para iniciar:\n${generatedLink}`,
    );
    window.open(`mailto:${candidateEmail}?subject=${subject}&body=${body}`);
  }

  function handleShareWhatsApp() {
    const text = encodeURIComponent(
      `Olá ${candidateName}! Você foi convidado(a) para uma avaliação situacional. Acesse: ${generatedLink}`,
    );
    window.open(`https://wa.me/?text=${text}`, "_blank");
  }

  function handleReset() {
    setStep("select");
    setSelectedSimulation(null);
    setCandidateName("");
    setCandidateEmail("");
    setGeneratedLink("");
    setSimulationName("");
    setCopied(false);
    linkMutation.reset();
  }

  return (
    <AppShell>
      <ScreenStateStrip blockedReason="sem testes no ar para gerar link" />

      <div className="mb-6">
        <div className="text-xs uppercase text-primary">Envio direto</div>
        <h1 className="mt-1 text-3xl font-semibold">Compartilhar acesso</h1>
        <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Selecione uma avaliação no ar, preencha os dados da pessoa participante e compartilhe o
          link por e-mail ou WhatsApp.
        </p>
      </div>

      <div className="mb-6 flex gap-4">
        <StepIndicator
          number={1}
          label="Teste"
          active={step === "select"}
          done={step === "form" || step === "share"}
        />
        <StepIndicator
          number={2}
          label="Participante"
          active={step === "form"}
          done={step === "share"}
        />
        <StepIndicator number={3} label="Compartilhar" active={step === "share"} done={false} />
      </div>

      {linkMutation.isError && (
        <StateBanner tone="danger" title="Não foi possível gerar o link">
          {linkMutation.error instanceof Error
            ? linkMutation.error.message
            : "Verifique se o sistema está disponível e se o teste está no ar."}
        </StateBanner>
      )}

      {step === "select" && (
        <SelectSimulationStep
          simulations={publishedSimulations}
          loading={simulationsQuery.isLoading}
          error={simulationsQuery.isError}
          errorMessage={
            simulationsQuery.error instanceof Error ? simulationsQuery.error.message : undefined
          }
          onSelect={handleSelectSimulation}
        />
      )}

      {step === "form" && selectedSimulation && (
        <CandidateFormStep
          simulation={selectedSimulation}
          candidateName={candidateName}
          candidateEmail={candidateEmail}
          onNameChange={setCandidateName}
          onEmailChange={setCandidateEmail}
          onSubmit={handleGenerateLink}
          onBack={() => setStep("select")}
          loading={linkMutation.isPending}
        />
      )}

      {step === "share" && (
        <ShareStep
          link={generatedLink}
          simulationName={simulationName}
          candidateName={candidateName}
          candidateEmail={candidateEmail}
          copied={copied}
          onCopy={handleCopy}
          onShareEmail={handleShareEmail}
          onShareWhatsApp={handleShareWhatsApp}
          onReset={handleReset}
        />
      )}

      <CandidateLinksTable
        links={candidateLinksQuery.data ?? []}
        loading={candidateLinksQuery.isLoading}
        error={candidateLinksQuery.isError}
        errorMessage={
          candidateLinksQuery.error instanceof Error ? candidateLinksQuery.error.message : undefined
        }
      />
    </AppShell>
  );
}

function StepIndicator({
  number,
  label,
  active,
  done,
}: {
  number: number;
  label: string;
  active: boolean;
  done: boolean;
}) {
  return (
    <div className="flex items-center gap-2">
      <div
        className={cn(
          "flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold",
          done
            ? "bg-success text-success-foreground"
            : active
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground",
        )}
      >
        {done ? <CheckCircle2 className="h-4 w-4" /> : number}
      </div>
      <span
        className={cn("text-sm", active ? "font-medium text-foreground" : "text-muted-foreground")}
      >
        {label}
      </span>
    </div>
  );
}

function SelectSimulationStep({
  simulations,
  loading,
  error,
  errorMessage,
  onSelect,
}: {
  simulations: SimulationSummaryResponse[];
  loading: boolean;
  error: boolean;
  errorMessage?: string;
  onSelect: (simulation: SimulationSummaryResponse) => void;
}) {
  const { t } = useLanguage();

  if (loading) {
    return (
      <section className="rounded-md border border-border bg-card p-4">
        <SkeletonRows rows={4} />
      </section>
    );
  }

  if (error) {
    return (
      <StateBanner tone="danger" title="Não foi possível carregar os testes">
        {errorMessage ?? "Verifique se o sistema está disponível e tente novamente."}
      </StateBanner>
    );
  }

  if (simulations.length === 0) {
    return (
      <EmptyState
        title="Nenhum teste no ar"
        description="Coloque uma avaliação no ar antes de gerar links de participação."
        actions={
          <Link
            to="/nova/blueprint"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            Criar teste
          </Link>
        }
      />
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">Selecione a avaliação no ar</h2>
      <section className="rounded-md border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Teste</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>{t.common.status}</TableHead>
              <TableHead className="text-right">Versão</TableHead>
              <TableHead>Competências</TableHead>
              <TableHead className="text-right">Tentativas</TableHead>
              <TableHead className="text-right">Ação</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {simulations.map((simulation) => (
              <TableRow key={simulation.id}>
                <TableCell className="min-w-[180px] font-medium">{simulation.name}</TableCell>
                <TableCell className="min-w-[220px] max-w-[360px] text-muted-foreground">
                  <span className="line-clamp-2">{simulation.description}</span>
                </TableCell>
                <TableCell>
                  <StatusBadge
                    status={simulation.status}
                    maturity={maturityForStatus(simulation.status)}
                  />
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  v{simulation.versionNumber}
                </TableCell>
                <TableCell className="min-w-[220px]">
                  <div className="flex flex-wrap gap-1">
                    {simulation.competencies.slice(0, 3).map((competency) => (
                      <span
                        key={competency}
                        className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
                      >
                        {competency}
                      </span>
                    ))}
                    {simulation.competencies.length > 3 && (
                      <span className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground">
                        +{simulation.competencies.length - 3}
                      </span>
                    )}
                  </div>
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  {simulation.attemptsCreated.toLocaleString("pt-BR")}
                </TableCell>
                <TableCell className="text-right">
                  <button
                    type="button"
                    onClick={() => onSelect(simulation)}
                    className="inline-flex items-center justify-center rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                  >
                    Selecionar
                  </button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </section>
    </div>
  );
}

function getParticipantLink(link: CandidateLinkResponse) {
  return link.candidateUrl;
}

function candidateStatusLabel(status: CandidateLinkResponse["status"]) {
  const labels: Record<CandidateLinkResponse["status"], string> = {
    created: "Criado",
    started: "Em andamento",
    completed: "Concluído",
    expired: "Expirado",
  };

  return labels[status] ?? status;
}

function CandidateLinksTable({
  links,
  loading,
  error,
  errorMessage,
}: {
  links: CandidateLinkResponse[];
  loading: boolean;
  error: boolean;
  errorMessage?: string;
}) {
  const [copiedAttemptId, setCopiedAttemptId] = useState<string | null>(null);

  async function copyLink(link: CandidateLinkResponse) {
    await navigator.clipboard.writeText(getParticipantLink(link));
    setCopiedAttemptId(link.attemptId);
    window.setTimeout(() => setCopiedAttemptId(null), 2000);
  }

  async function shareLink(link: CandidateLinkResponse) {
    const url = getParticipantLink(link);
    const text = `${link.candidateName}, você foi convidado(a) para participar da avaliação "${link.simulationName}".`;

    if (navigator.share) {
      await navigator.share({
        title: `Avaliação: ${link.simulationName}`,
        text,
        url,
      });
      return;
    }

    await navigator.clipboard.writeText(url);
    setCopiedAttemptId(link.attemptId);
    window.setTimeout(() => setCopiedAttemptId(null), 2000);
  }

  return (
    <section className="mt-8 rounded-md border border-border bg-card">
      <div className="border-b border-border p-5">
        <h2 className="text-xl font-semibold">Links de participação</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Consulte os participantes, avaliações, links gerados e compartilhe o acesso novamente
          quando necessário.
        </p>
      </div>

      {loading ? (
        <div className="p-5">
          <SkeletonRows rows={4} />
        </div>
      ) : error ? (
        <div className="p-5">
          <StateBanner tone="danger" title="Não foi possível carregar os links">
            {errorMessage ?? "Verifique se o sistema está disponível e tente novamente."}
          </StateBanner>
        </div>
      ) : links.length === 0 ? (
        <div className="p-5 text-sm text-muted-foreground">
          Nenhum link de participação foi gerado ainda.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Participante</TableHead>
                <TableHead>Avaliação / fluxo</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Link</TableHead>
                <TableHead className="text-right">Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {links.map((link) => {
                const participantLink = getParticipantLink(link);
                const copied = copiedAttemptId === link.attemptId;

                return (
                  <TableRow key={link.attemptId}>
                    <TableCell className="min-w-[220px]">
                      <div className="font-medium text-foreground">{link.candidateName}</div>
                      {link.candidateEmail && (
                        <div className="mt-0.5 text-xs text-muted-foreground">
                          {link.candidateEmail}
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="min-w-[220px]">
                      <div className="font-medium text-foreground">{link.simulationName}</div>
                      <div className="mt-0.5 text-xs text-muted-foreground">
                        ID: {link.simulationId}
                      </div>
                    </TableCell>
                    <TableCell>
                      <span className="rounded-md border border-border bg-background px-2 py-1 text-xs text-muted-foreground">
                        {candidateStatusLabel(link.status)}
                      </span>
                    </TableCell>
                    <TableCell className="min-w-[280px] max-w-[420px]">
                      <code className="block truncate rounded-md border border-border bg-background px-2 py-1.5 text-xs text-muted-foreground">
                        {participantLink}
                      </code>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => void copyLink(link)}
                          className={cn(
                            "inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-xs font-medium hover:bg-accent",
                            copied
                              ? "border-success bg-success/10 text-success"
                              : "border-border bg-card",
                          )}
                        >
                          {copied ? (
                            <CheckCircle2 className="h-3.5 w-3.5" />
                          ) : (
                            <Copy className="h-3.5 w-3.5" />
                          )}
                          {copied ? "Copiado" : "Copiar"}
                        </button>
                        <button
                          type="button"
                          onClick={() => void shareLink(link)}
                          className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
                        >
                          <Send className="h-3.5 w-3.5" />
                          Compartilhar
                        </button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}

function CandidateFormStep({
  simulation,
  candidateName,
  candidateEmail,
  onNameChange,
  onEmailChange,
  onSubmit,
  onBack,
  loading,
}: {
  simulation: SimulationSummaryResponse;
  candidateName: string;
  candidateEmail: string;
  onNameChange: (value: string) => void;
  onEmailChange: (value: string) => void;
  onSubmit: () => void;
  onBack: () => void;
  loading: boolean;
}) {
  const valid = candidateName.trim().length > 0 && candidateEmail.trim().includes("@");

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
      <div className="rounded-md border border-border bg-card p-6">
        <h2 className="text-lg font-semibold">Dados da pessoa participante</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Preencha o nome e o e-mail da pessoa participante para gerar o link de acesso.
        </p>

        <div className="mt-5 space-y-4">
          <div>
            <label htmlFor="candidate-name" className="mb-1.5 block text-sm font-medium">
              Nome da pessoa participante
            </label>
            <input
              id="candidate-name"
              type="text"
              className="input w-full"
              placeholder="Ex: Maria Silva"
              value={candidateName}
              onChange={(e) => onNameChange(e.target.value)}
            />
          </div>

          <div>
            <label htmlFor="candidate-email" className="mb-1.5 block text-sm font-medium">
              E-mail da pessoa participante
            </label>
            <input
              id="candidate-email"
              type="email"
              className="input w-full"
              placeholder="Ex: maria@example.com"
              value={candidateEmail}
              onChange={(e) => onEmailChange(e.target.value)}
            />
          </div>
        </div>

        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={onBack}
            className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
          >
            Voltar
          </button>
          <button
            type="button"
            onClick={onSubmit}
            disabled={!valid || loading}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Link2 className="h-4 w-4" />
            {loading ? "Gerando..." : "Gerar link"}
          </button>
        </div>
      </div>

      <aside className="rounded-md border border-border bg-card p-5">
        <h3 className="text-sm font-semibold">Avaliação selecionada</h3>
        <div className="mt-3 space-y-2">
          <div className="font-medium text-foreground">{simulation.name}</div>
          <div className="text-xs text-muted-foreground">{simulation.description}</div>
          <StatusBadge status={simulation.status} maturity={maturityForStatus(simulation.status)} />
          <div className="mt-2 flex flex-wrap gap-1">
            {simulation.competencies.map((competency) => (
              <span
                key={competency}
                className="rounded-md border border-border bg-background px-2 py-0.5 text-[10px] text-muted-foreground"
              >
                {competency}
              </span>
            ))}
          </div>
          <div className="mt-3 text-xs text-muted-foreground">
            Versão v{simulation.versionNumber} -{" "}
            {simulation.attemptsCreated.toLocaleString("pt-BR")} tentativas criadas
          </div>
        </div>
      </aside>
    </div>
  );
}

function ShareStep({
  link,
  simulationName,
  candidateName,
  candidateEmail,
  copied,
  onCopy,
  onShareEmail,
  onShareWhatsApp,
  onReset,
}: {
  link: string;
  simulationName: string;
  candidateName: string;
  candidateEmail: string;
  copied: boolean;
  onCopy: () => void;
  onShareEmail: () => void;
  onShareWhatsApp: () => void;
  onReset: () => void;
}) {
  return (
    <div className="space-y-6">
      <StateBanner tone="ok" title="Link gerado com sucesso">
        O link de acesso ao teste "{simulationName}" foi criado para {candidateName} (
        {candidateEmail}).
      </StateBanner>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-5">
          <div className="rounded-md border border-border bg-card p-6">
            <h2 className="text-lg font-semibold">Link de participação</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Copie o link abaixo ou use os botões para compartilhar diretamente.
            </p>
            <div className="mt-4 flex items-center gap-2">
              <div className="min-w-0 flex-1 rounded-md border border-border bg-background px-3 py-2.5">
                <code className="block truncate text-sm text-foreground">{link}</code>
              </div>
              <button
                type="button"
                onClick={onCopy}
                className={cn(
                  "inline-flex shrink-0 items-center gap-2 rounded-md border px-4 py-2.5 text-sm font-medium transition",
                  copied
                    ? "border-success bg-success/10 text-success"
                    : "border-border bg-card hover:bg-accent",
                )}
              >
                {copied ? <CheckCircle2 className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                {copied ? "Copiado" : "Copiar"}
              </button>
            </div>
          </div>

          <div className="rounded-md border border-border bg-card p-6">
            <h2 className="text-lg font-semibold">Compartilhar</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Envie o link diretamente para a pessoa participante pelo canal preferido.
            </p>
            <div className="mt-4 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={onShareEmail}
                className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-5 py-3 text-sm font-medium transition hover:bg-accent"
              >
                <Mail className="h-4 w-4" />
                Enviar por e-mail
              </button>
              <button
                type="button"
                onClick={onShareWhatsApp}
                className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-5 py-3 text-sm font-medium transition hover:bg-accent"
              >
                <MessageCircle className="h-4 w-4" />
                Enviar por WhatsApp
              </button>
            </div>
          </div>
        </div>

        <aside className="space-y-4">
          <div className="rounded-md border border-border bg-card p-5">
            <h3 className="text-sm font-semibold">Resumo do envio</h3>
            <dl className="mt-3 space-y-3">
              <div>
                <dt className="text-xs uppercase text-muted-foreground">Participante</dt>
                <dd className="mt-0.5 text-sm font-medium">{candidateName}</dd>
              </div>
              <div>
                <dt className="text-xs uppercase text-muted-foreground">Email</dt>
                <dd className="mt-0.5 text-sm">{candidateEmail}</dd>
              </div>
              <div>
                <dt className="text-xs uppercase text-muted-foreground">Teste</dt>
                <dd className="mt-0.5 text-sm">{simulationName}</dd>
              </div>
            </dl>
          </div>

          <StateBanner tone="info" title="Um link por participação">
            Se você gerar o link de novo com o mesmo e-mail e a mesma avaliação, o link retornado
            será o mesmo. Pode reenviar sem duplicar.
          </StateBanner>
        </aside>
      </div>

      <div className="flex gap-3">
        <button
          type="button"
          onClick={onReset}
          className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Send className="h-4 w-4" />
          Enviar para outra pessoa
        </button>
        <Link
          to="/"
          className="rounded-md border border-border bg-card px-4 py-2 text-sm hover:bg-accent"
        >
          Voltar ao painel
        </Link>
      </div>
    </div>
  );
}
