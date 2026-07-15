import { Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { z } from "zod";
import { CheckCircle2, Copy, Link2, Mail, MessageCircle, RefreshCw, Send } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { EmptyState, SkeletonRows, StateBanner, StatusBadge } from "@/components/praxis-ui";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  listCandidateLinks,
  listSimulations,
  type CandidateLinkResponse,
  type SimulationSummaryResponse,
} from "@/lib/api/praxis";
import {
  createNewCandidateLink,
  resendExistingCandidateLink,
  type DirectCandidateLinkResponse,
} from "@/lib/api/direct-candidate-links";
import { maturityForStatus } from "@/lib/simulation-meta";
import { cn } from "@/lib/utils";

const candidateSchema = z.object({
  candidateName: z.string().trim().min(1, "Informe o nome do candidato."),
  candidateEmail: z.string().trim().min(1, "Informe o e-mail do candidato.").email("Informe um e-mail válido."),
});

type Step = "select" | "form" | "share";
type ShareMode = "new" | "resend";
type FormErrors = { candidateName?: string; candidateEmail?: string };

export function EnviarLinkPage() {
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>("select");
  const [shareMode, setShareMode] = useState<ShareMode>("new");
  const [selectedSimulation, setSelectedSimulation] = useState<SimulationSummaryResponse | null>(null);
  const [candidateName, setCandidateName] = useState("");
  const [candidateEmail, setCandidateEmail] = useState("");
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [generated, setGenerated] = useState<DirectCandidateLinkResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const simulationsQuery = useQuery({
    queryKey: ["simulations"],
    queryFn: listSimulations,
    retry: false,
  });
  const linksQuery = useQuery({
    queryKey: ["candidate-links", { blind: false }],
    queryFn: () => listCandidateLinks(false),
    retry: false,
  });

  const createMutation = useMutation({
    mutationFn: createNewCandidateLink,
    onSuccess: (data) => {
      setGenerated(data);
      setShareMode("new");
      setStep("share");
      void queryClient.invalidateQueries({ queryKey: ["candidate-links"] });
    },
  });

  const resendMutation = useMutation({
    mutationFn: ({ attemptId }: CandidateLinkResponse) => resendExistingCandidateLink(attemptId),
    onSuccess: (data, link) => {
      setCandidateName(link.candidateName);
      setCandidateEmail(link.candidateEmail ?? "");
      setGenerated(data);
      setShareMode("resend");
      setStep("share");
    },
  });

  const publishedSimulations = (simulationsQuery.data ?? []).filter(
    (simulation) => simulation.status === "published" || simulation.livePublishedVersionNumber != null,
  );
  const mutationError = createMutation.error ?? resendMutation.error;

  function selectSimulation(simulation: SimulationSummaryResponse) {
    setSelectedSimulation(simulation);
    setFormErrors({});
    setStep("form");
  }

  function createAttempt() {
    if (!selectedSimulation) return;
    const parsed = candidateSchema.safeParse({ candidateName, candidateEmail });
    if (!parsed.success) {
      const errors: FormErrors = {};
      for (const issue of parsed.error.issues) {
        if (issue.path[0] === "candidateName") errors.candidateName = issue.message;
        if (issue.path[0] === "candidateEmail") errors.candidateEmail = issue.message;
      }
      setFormErrors(errors);
      return;
    }

    setFormErrors({});
    createMutation.mutate({
      simulationId: selectedSimulation.id,
      candidateName: parsed.data.candidateName,
      candidateEmail: parsed.data.candidateEmail,
      applicationCycleId: newApplicationCycleId(),
    });
  }

  function reset() {
    setStep("select");
    setShareMode("new");
    setSelectedSimulation(null);
    setCandidateName("");
    setCandidateEmail("");
    setFormErrors({});
    setGenerated(null);
    setCopied(false);
    createMutation.reset();
    resendMutation.reset();
  }

  async function copyGeneratedLink() {
    if (!generated) return;
    await navigator.clipboard.writeText(toParticipantPageUrl(generated.candidateUrl));
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2000);
  }

  function shareByEmail() {
    if (!generated) return;
    const link = toParticipantPageUrl(generated.candidateUrl);
    const subject = encodeURIComponent(`Avaliação: ${generated.simulationName}`);
    const body = encodeURIComponent(`Olá, ${candidateName}. Acesse sua avaliação pelo link: ${link}`);
    window.open(`mailto:${candidateEmail}?subject=${subject}&body=${body}`);
  }

  function shareByWhatsApp() {
    if (!generated) return;
    const link = toParticipantPageUrl(generated.candidateUrl);
    const text = encodeURIComponent(`Olá, ${candidateName}. Acesse sua avaliação pelo link: ${link}`);
    window.open(`https://wa.me/?text=${text}`, "_blank");
  }

  return (
    <AppShell>
      <header className="mb-6">
        <div className="text-xs uppercase text-primary">Aplicação de avaliações</div>
        <h1 className="mt-1 text-3xl font-semibold">Enviar link</h1>
        <p className="mt-1 max-w-3xl text-sm text-muted-foreground">
          Crie uma nova tentativa somente para uma nova aplicação. Para enviar novamente uma tentativa já criada,
          use a ação de reenvio na tabela.
        </p>
      </header>

      <div className="mb-6 flex flex-wrap gap-4">
        <StepIndicator number={1} label="Avaliação" active={step === "select"} done={step !== "select"} />
        <StepIndicator number={2} label="Participante" active={step === "form"} done={step === "share"} />
        <StepIndicator number={3} label="Compartilhar" active={step === "share"} done={false} />
      </div>

      {mutationError && (
        <StateBanner tone="danger" title="Não foi possível preparar o link">
          {mutationError instanceof Error ? mutationError.message : "Tente novamente."}
        </StateBanner>
      )}

      {step === "select" && (
        <SimulationSelection
          simulations={publishedSimulations}
          loading={simulationsQuery.isLoading}
          error={simulationsQuery.isError}
          onSelect={selectSimulation}
        />
      )}

      {step === "form" && selectedSimulation && (
        <section className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="rounded-md border border-border bg-card p-6">
            <h2 className="text-lg font-semibold">Criar nova tentativa</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Esta ação cria histórico e resultado independentes. Uma nova tentativa pode consumir crédito quando for concluída.
            </p>
            <StateBanner tone="info" title="Precisa apenas mandar o mesmo link novamente?">
              Volte à lista de links e escolha “Reenviar existente”. Essa ação mantém a tentativa, o progresso e o resultado atuais.
            </StateBanner>
            <div className="mt-5 space-y-4">
              <Field
                id="candidate-name"
                label="Nome do participante"
                value={candidateName}
                error={formErrors.candidateName}
                onChange={(value) => setCandidateName(value)}
              />
              <Field
                id="candidate-email"
                label="E-mail do participante"
                type="email"
                value={candidateEmail}
                error={formErrors.candidateEmail}
                onChange={(value) => setCandidateEmail(value)}
              />
            </div>
            <div className="mt-6 flex gap-3">
              <button type="button" onClick={() => setStep("select")} className="rounded-md border border-border px-4 py-2 text-sm hover:bg-accent">
                Voltar
              </button>
              <button
                type="button"
                onClick={createAttempt}
                disabled={createMutation.isPending}
                className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
              >
                <Link2 className="h-4 w-4" />
                {createMutation.isPending ? "Criando..." : "Criar nova tentativa"}
              </button>
            </div>
          </div>
          <aside className="rounded-md border border-border bg-card p-5">
            <h3 className="text-sm font-semibold">Avaliação selecionada</h3>
            <div className="mt-3 font-medium">{selectedSimulation.name}</div>
            <p className="mt-1 text-xs text-muted-foreground">{selectedSimulation.description}</p>
            <div className="mt-3"><StatusBadge status={selectedSimulation.status} maturity={maturityForStatus(selectedSimulation.status)} /></div>
          </aside>
        </section>
      )}

      {step === "share" && generated && (
        <SharePanel
          response={generated}
          mode={shareMode}
          candidateName={candidateName}
          candidateEmail={candidateEmail}
          copied={copied}
          onCopy={() => void copyGeneratedLink()}
          onEmail={shareByEmail}
          onWhatsApp={shareByWhatsApp}
          onReset={reset}
        />
      )}

      <LinksTable
        links={linksQuery.data ?? []}
        loading={linksQuery.isLoading}
        error={linksQuery.isError}
        resendingAttemptId={resendMutation.isPending ? resendMutation.variables?.attemptId : undefined}
        onResend={(link) => resendMutation.mutate(link)}
      />
    </AppShell>
  );
}

function SimulationSelection({ simulations, loading, error, onSelect }: { simulations: SimulationSummaryResponse[]; loading: boolean; error: boolean; onSelect: (simulation: SimulationSummaryResponse) => void }) {
  if (loading) return <section className="rounded-md border border-border bg-card p-4"><SkeletonRows rows={4} /></section>;
  if (error) return <StateBanner tone="danger" title="Não foi possível carregar as avaliações">Tente novamente.</StateBanner>;
  if (simulations.length === 0) {
    return <EmptyState title="Nenhuma avaliação publicada" description="Publique uma avaliação antes de criar links." actions={<Link to="/nova/avaliacao" className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground">Criar avaliação</Link>} />;
  }
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border p-5"><h2 className="text-lg font-semibold">Selecione a avaliação</h2></div>
      <div className="overflow-x-auto">
        <Table className="min-w-[720px]">
          <TableHeader><TableRow><TableHead>Avaliação</TableHead><TableHead>Status</TableHead><TableHead>Versão</TableHead><TableHead className="text-right">Ação</TableHead></TableRow></TableHeader>
          <TableBody>{simulations.map((simulation) => <TableRow key={simulation.id}><TableCell><div className="font-medium">{simulation.name}</div><div className="mt-1 max-w-xl text-xs text-muted-foreground">{simulation.description}</div></TableCell><TableCell><StatusBadge status={simulation.status} maturity={maturityForStatus(simulation.status)} /></TableCell><TableCell>v{simulation.versionNumber}</TableCell><TableCell className="text-right"><button type="button" onClick={() => onSelect(simulation)} className="rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground">Selecionar</button></TableCell></TableRow>)}</TableBody>
        </Table>
      </div>
    </section>
  );
}

function LinksTable({ links, loading, error, resendingAttemptId, onResend }: { links: CandidateLinkResponse[]; loading: boolean; error: boolean; resendingAttemptId?: string; onResend: (link: CandidateLinkResponse) => void }) {
  return (
    <section className="mt-8 rounded-md border border-border bg-card">
      <div className="border-b border-border p-5">
        <h2 className="text-xl font-semibold">Links já criados</h2>
        <p className="mt-1 text-sm text-muted-foreground">Reenviar existente não cria uma nova tentativa nem altera o progresso.</p>
      </div>
      {loading ? <div className="p-5"><SkeletonRows rows={4} /></div> : error ? <div className="p-5"><StateBanner tone="danger" title="Não foi possível carregar os links">Tente novamente.</StateBanner></div> : links.length === 0 ? <div className="p-5 text-sm text-muted-foreground">Nenhum link criado.</div> : (
        <div className="overflow-x-auto">
          <Table className="min-w-[880px]">
            <TableHeader><TableRow><TableHead>Participante</TableHead><TableHead>Avaliação</TableHead><TableHead>Status</TableHead><TableHead>Link</TableHead><TableHead className="text-right">Ações</TableHead></TableRow></TableHeader>
            <TableBody>{links.map((link) => { const participantLink = toParticipantPageUrl(link.candidateUrl); return <TableRow key={link.attemptId}><TableCell><div className="font-medium">{link.candidateName}</div>{link.candidateEmail && <div className="text-xs text-muted-foreground">{link.candidateEmail}</div>}</TableCell><TableCell>{link.simulationName}</TableCell><TableCell><span className="rounded-md border border-border px-2 py-1 text-xs">{statusLabel(link.status)}</span></TableCell><TableCell className="max-w-[280px]"><code className="block truncate text-xs text-muted-foreground">{participantLink}</code></TableCell><TableCell className="text-right"><div className="flex justify-end gap-2"><button type="button" onClick={() => void navigator.clipboard.writeText(participantLink)} className="inline-flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-xs hover:bg-accent"><Copy className="h-3.5 w-3.5" />Copiar</button><button type="button" onClick={() => onResend(link)} disabled={resendingAttemptId === link.attemptId} className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-xs text-primary-foreground disabled:opacity-60"><RefreshCw className="h-3.5 w-3.5" />{resendingAttemptId === link.attemptId ? "Preparando..." : "Reenviar existente"}</button></div></TableCell></TableRow>; })}</TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}

function SharePanel({ response, mode, candidateName, candidateEmail, copied, onCopy, onEmail, onWhatsApp, onReset }: { response: DirectCandidateLinkResponse; mode: ShareMode; candidateName: string; candidateEmail: string; copied: boolean; onCopy: () => void; onEmail: () => void; onWhatsApp: () => void; onReset: () => void }) {
  const link = toParticipantPageUrl(response.candidateUrl);
  return (
    <div className="space-y-5">
      <StateBanner tone="ok" title={mode === "new" ? "Nova tentativa criada" : "Link existente recuperado"}>
        {mode === "new" ? "Foi criada uma aplicação independente para este participante." : "Nenhuma nova tentativa foi criada. O progresso e o resultado permanecem no mesmo registro."}
      </StateBanner>
      <section className="rounded-md border border-border bg-card p-6">
        <h2 className="text-lg font-semibold">Compartilhar link</h2>
        <p className="mt-1 text-sm text-muted-foreground">{response.simulationName} — {candidateName}</p>
        <div className="mt-4 flex items-center gap-2"><code className="min-w-0 flex-1 truncate rounded-md border border-border bg-background px-3 py-2.5 text-sm">{link}</code><button type="button" onClick={onCopy} className={cn("inline-flex items-center gap-2 rounded-md border px-4 py-2.5 text-sm", copied && "border-success text-success")}><Copy className="h-4 w-4" />{copied ? "Copiado" : "Copiar"}</button></div>
        <div className="mt-4 flex flex-wrap gap-3"><button type="button" onClick={onEmail} disabled={!candidateEmail} className="inline-flex items-center gap-2 rounded-md border border-border px-4 py-2 text-sm disabled:opacity-50"><Mail className="h-4 w-4" />E-mail</button><button type="button" onClick={onWhatsApp} className="inline-flex items-center gap-2 rounded-md border border-border px-4 py-2 text-sm"><MessageCircle className="h-4 w-4" />WhatsApp</button></div>
      </section>
      <div className="flex gap-3"><button type="button" onClick={onReset} className="inline-flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm text-primary-foreground"><Send className="h-4 w-4" />Voltar ao início</button><Link to="/dashboard" className="rounded-md border border-border px-4 py-2 text-sm">Dashboard</Link></div>
    </div>
  );
}

function Field({ id, label, type = "text", value, error, onChange }: { id: string; label: string; type?: string; value: string; error?: string; onChange: (value: string) => void }) {
  return <div><label htmlFor={id} className="mb-1.5 block text-sm font-medium">{label}</label><input id={id} type={type} value={value} onChange={(event) => onChange(event.target.value)} className={cn("input w-full", error && "border-danger")} aria-invalid={Boolean(error)} />{error && <p className="mt-1 text-xs text-danger">{error}</p>}</div>;
}

function StepIndicator({ number, label, active, done }: { number: number; label: string; active: boolean; done: boolean }) {
  return <div className="flex items-center gap-2"><div className={cn("flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold", done ? "bg-success text-success-foreground" : active ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground")}>{done ? <CheckCircle2 className="h-4 w-4" /> : number}</div><span className={cn("text-sm", active ? "font-medium" : "text-muted-foreground")}>{label}</span></div>;
}

function newApplicationCycleId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) return crypto.randomUUID();
  return `application-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function toParticipantPageUrl(candidateUrl: string) {
  if (typeof window === "undefined") return candidateUrl;
  try {
    const token = new URL(candidateUrl).pathname.split("/candidato/")[1];
    if (token) return `${window.location.origin}/candidato/${token}`;
  } catch {
    // Mantém a URL recebida quando ela não é absoluta.
  }
  return candidateUrl;
}

function statusLabel(status: CandidateLinkResponse["status"]) {
  return ({ notStarted: "Não iniciada", inProgress: "Em andamento", completed: "Concluída", abandoned: "Abandonada", expired: "Expirada" } as const)[status] ?? status;
}
