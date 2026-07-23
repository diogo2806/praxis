import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  CalendarClock,
  Download,
  MailCheck,
  Pause,
  Play,
  Send,
  Upload,
  Users,
  XCircle,
} from "lucide-react";
import { type ChangeEvent, useMemo, useState } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  campaignAction,
  createParticipationCampaign,
  downloadCampaignCsv,
  listParticipationCampaigns,
  previewCampaignCsv,
  previewCampaignMessage,
  type CsvPreviewResponse,
  type ParticipationCampaign,
  type ReminderRuleInput,
} from "@/lib/api/participation-campaigns";

export const Route = createFileRoute("/participacoes/campanhas")({
  head: () => ({
    meta: [
      { title: "Campanhas de participação - Práxis" },
      {
        name: "description",
        content:
          "Importe participantes, valide o lote, agende convites, configure lembretes e acompanhe o funil operacional.",
      },
    ],
  }),
  component: ParticipationCampaignsPage,
});

const DEFAULT_BODY = `Olá, {{name}}.

Você foi convidado(a) para participar da campanha {{campaign}}.

Acesse sua avaliação pelo link seguro: {{link}}

O link é válido até {{expiresAt}}.`;

function ParticipationCampaignsPage() {
  const queryClient = useQueryClient();
  const [csvContent, setCsvContent] = useState("name,email,consent,accommodation_multiplier\n");
  const [simulationId, setSimulationId] = useState("");
  const [applicationCycleId, setApplicationCycleId] = useState("");
  const [campaignName, setCampaignName] = useState("");
  const [applicationContext, setApplicationContext] = useState("");
  const [consentRequired, setConsentRequired] = useState(true);
  const [allowExistingActive, setAllowExistingActive] = useState(false);
  const [initialSendAt, setInitialSendAt] = useState(() => localDateTimeValue(new Date()));
  const [linkValidityDays, setLinkValidityDays] = useState(7);
  const [retentionDays, setRetentionDays] = useState(180);
  const [subjectTemplate, setSubjectTemplate] = useState("Convite para {{campaign}}");
  const [bodyTemplate, setBodyTemplate] = useState(DEFAULT_BODY);
  const [preview, setPreview] = useState<CsvPreviewResponse | null>(null);
  const [messagePreview, setMessagePreview] = useState<{ subject: string; body: string } | null>(null);
  const [reminders, setReminders] = useState<ReminderRuleInput[]>([
    {
      reminderIndex: 1,
      sendAfterHours: 24,
      targetState: "NOT_STARTED",
      subjectTemplate: "Lembrete: {{campaign}}",
      bodyTemplate: DEFAULT_BODY,
    },
  ]);

  const campaignsQuery = useQuery({
    queryKey: ["participation-campaigns"],
    queryFn: listParticipationCampaigns,
    refetchInterval: 60_000,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["participation-campaigns"] });
  const previewMutation = useMutation({
    mutationFn: () =>
      previewCampaignCsv({
        simulationId,
        applicationCycleId,
        consentRequired,
        allowExistingActive,
        csvContent,
      }),
    onSuccess: setPreview,
  });
  const messageMutation = useMutation({
    mutationFn: () =>
      previewCampaignMessage({
        subjectTemplate,
        bodyTemplate,
        sampleName: "Pessoa candidata",
        campaignName: campaignName || "Campanha de exemplo",
      }),
    onSuccess: setMessagePreview,
  });
  const createMutation = useMutation({
    mutationFn: () =>
      createParticipationCampaign({
        name: campaignName,
        simulationId,
        applicationCycleId,
        applicationContext,
        initialSendAt: new Date(initialSendAt).toISOString(),
        linkValidityDays,
        consentRequired,
        allowExistingActive,
        subjectTemplate,
        bodyTemplate,
        retentionDays,
        participants: preview!.validParticipants,
        reminders,
        idempotencyKey: crypto.randomUUID(),
      }),
    onSuccess: () => {
      setPreview(null);
      void refresh();
    },
  });
  const actionMutation = useMutation({
    mutationFn: (input: { id: string; action: "pause" | "resume" | "cancel" }) =>
      campaignAction(input.id, input.action),
    onSuccess: refresh,
  });

  const operationError =
    previewMutation.error ?? messageMutation.error ?? createMutation.error ?? actionMutation.error;
  const canCreate = Boolean(
    preview?.validParticipants.length &&
      !preview.planLimitExceeded &&
      preview.invalidRows === 0 &&
      campaignName.trim() &&
      simulationId.trim() &&
      applicationCycleId.trim(),
  );

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setCsvContent(await file.text());
    setPreview(null);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Convites em lote com rastreabilidade
            </div>
            <h1 className="mt-1 font-display text-3xl">Campanhas de participação</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Valide o lote antes de consumir créditos, personalize mensagens, agende até três lembretes
              e acompanhe entrega, abertura, início, conclusão, expiração e cancelamento.
            </p>
          </div>
          <Button asChild variant="outline">
            <Link to="/participacoes">Participações</Link>
          </Button>
        </header>

        {operationError && <ErrorBanner error={operationError} />}

        <section className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
          <div className="space-y-6">
            <Card icon={<Upload className="h-5 w-5 text-primary" />} title="1. Importar e pré-validar">
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="ID da avaliação publicada">
                  <input value={simulationId} onChange={(event) => { setSimulationId(event.target.value); setPreview(null); }} className="input" />
                </Field>
                <Field label="ID do ciclo da aplicação">
                  <input value={applicationCycleId} onChange={(event) => { setApplicationCycleId(event.target.value); setPreview(null); }} className="input" />
                </Field>
              </div>
              <label className="block text-sm font-medium">
                Arquivo CSV
                <input type="file" accept=".csv,text/csv" onChange={(event) => void handleFile(event)} className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2" />
              </label>
              <Field label="Conteúdo CSV para revisão">
                <textarea value={csvContent} onChange={(event) => { setCsvContent(event.target.value); setPreview(null); }} className="min-h-40 w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-xs" />
              </Field>
              <div className="flex flex-wrap gap-5 text-sm">
                <label className="flex items-center gap-2"><input type="checkbox" checked={consentRequired} onChange={(event) => { setConsentRequired(event.target.checked); setPreview(null); }} />Exigir consentimento no CSV</label>
                <label className="flex items-center gap-2"><input type="checkbox" checked={allowExistingActive} onChange={(event) => { setAllowExistingActive(event.target.checked); setPreview(null); }} />Reaproveitar aplicação existente no ciclo</label>
              </div>
              <Button onClick={() => previewMutation.mutate()} disabled={previewMutation.isPending || !simulationId.trim() || !applicationCycleId.trim() || !csvContent.trim()}>
                Pré-validar sem gravar
              </Button>
            </Card>

            {preview && <CsvPreviewPanel preview={preview} />}

            <Card icon={<MailCheck className="h-5 w-5 text-primary" />} title="2. Mensagem e lembretes">
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Nome da campanha"><input value={campaignName} onChange={(event) => setCampaignName(event.target.value)} className="input" /></Field>
                <Field label="Contexto da aplicação"><input value={applicationContext} onChange={(event) => setApplicationContext(event.target.value)} className="input" /></Field>
              </div>
              <Field label="Assunto"><input value={subjectTemplate} onChange={(event) => setSubjectTemplate(event.target.value)} className="input" /></Field>
              <Field label="Corpo da mensagem"><textarea value={bodyTemplate} onChange={(event) => setBodyTemplate(event.target.value)} className="min-h-44 w-full rounded-md border border-input bg-background px-3 py-2" /></Field>
              <p className="text-xs text-muted-foreground">Variáveis permitidas: {`{{name}}, {{email}}, {{link}}, {{campaign}}, {{simulation}}, {{expiresAt}}`}.</p>
              <Button variant="outline" onClick={() => messageMutation.mutate()} disabled={messageMutation.isPending}>Pré-visualizar mensagem</Button>
              {messagePreview && <div className="rounded-lg border border-border bg-background p-4 text-sm"><div className="font-semibold">{messagePreview.subject}</div><pre className="mt-3 whitespace-pre-wrap font-sans text-muted-foreground">{messagePreview.body}</pre></div>}
              <ReminderEditor reminders={reminders} setReminders={setReminders} />
            </Card>
          </div>

          <div className="space-y-6">
            <Card icon={<CalendarClock className="h-5 w-5 text-primary" />} title="3. Agendamento e confirmação">
              <Field label="Primeiro envio"><input type="datetime-local" value={initialSendAt} onChange={(event) => setInitialSendAt(event.target.value)} className="input" /></Field>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Validade do link (dias)"><input type="number" min={1} max={90} value={linkValidityDays} onChange={(event) => setLinkValidityDays(Number(event.target.value))} className="input" /></Field>
                <Field label="Retenção de dados (dias)"><input type="number" min={1} max={3650} value={retentionDays} onChange={(event) => setRetentionDays(Number(event.target.value))} className="input" /></Field>
              </div>
              <div className="rounded-lg border border-border bg-background p-4 text-sm">
                <div className="font-semibold">Resumo antes de confirmar</div>
                <dl className="mt-3 grid grid-cols-2 gap-3">
                  <Metric label="Participantes válidos" value={String(preview?.validParticipants.length ?? 0)} />
                  <Metric label="Lembretes" value={String(reminders.length)} />
                  <Metric label="Capacidade disponível" value={preview?.availableCapacity === -1 ? "Ilimitada" : String(preview?.availableCapacity ?? "-")} />
                  <Metric label="Consentimento" value={consentRequired ? "Obrigatório" : "Opcional"} />
                </dl>
              </div>
              <Button onClick={() => createMutation.mutate()} disabled={!canCreate || createMutation.isPending}>
                <Send className="mr-2 h-4 w-4" />Confirmar e agendar campanha
              </Button>
            </Card>

            <StateBanner tone="warning" title="Antes da confirmação">
              Nenhum crédito é consumido na pré-validação. A campanha cria os links apenas quando o
              primeiro envio entra na outbox. Cancelar não remove resultados já concluídos.
            </StateBanner>
          </div>
        </section>

        <CampaignList campaigns={campaignsQuery.data ?? []} loading={campaignsQuery.isLoading} action={(id, action) => actionMutation.mutate({ id, action })} />
      </main>
    </AppShell>
  );
}

function CsvPreviewPanel({ preview }: { preview: CsvPreviewResponse }) {
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-semibold">Diagnóstico do arquivo</h2>
      <dl className="mt-4 grid gap-3 sm:grid-cols-4"><Metric label="Linhas" value={String(preview.totalRows)} /><Metric label="Válidas" value={String(preview.validRows)} /><Metric label="Inválidas" value={String(preview.invalidRows)} /><Metric label="Capacidade" value={preview.availableCapacity === -1 ? "Ilimitada" : String(preview.availableCapacity)} /></dl>
      {preview.planLimitExceeded && <div className="mt-4"><StateBanner tone="danger" title="Limite do plano excedido">Ajuste o lote ou adquira capacidade antes de confirmar.</StateBanner></div>}
      <div className="mt-4 max-h-96 overflow-auto rounded-lg border border-border">
        <table className="w-full min-w-[760px] text-left text-sm"><thead className="sticky top-0 bg-card"><tr><th className="p-3">Linha</th><th className="p-3">Nome</th><th className="p-3">E-mail</th><th className="p-3">Situação</th><th className="p-3">Diagnóstico</th></tr></thead><tbody>{preview.rows.map((row, index) => <tr key={`${row.rowNumber}-${row.email}-${index}`} className="border-t border-border"><td className="p-3">{row.rowNumber || "Arquivo"}</td><td className="p-3">{row.name}</td><td className="p-3">{row.email}</td><td className="p-3 font-semibold">{row.valid ? "Válida" : "Inválida"}</td><td className="p-3"><ul className="space-y-1">{row.errors.map((error) => <li key={error} className="text-destructive">{error}</li>)}{row.warnings.map((warning) => <li key={warning} className="text-amber-700">{warning}</li>)}</ul></td></tr>)}</tbody></table>
      </div>
    </section>
  );
}

function ReminderEditor({ reminders, setReminders }: { reminders: ReminderRuleInput[]; setReminders: (items: ReminderRuleInput[]) => void }) {
  function update(index: number, patch: Partial<ReminderRuleInput>) { setReminders(reminders.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item)); }
  function add() { if (reminders.length >= 3) return; const reminderIndex = reminders.length + 1; setReminders([...reminders, { reminderIndex, sendAfterHours: reminderIndex * 24, targetState: "NOT_STARTED", subjectTemplate: "Lembrete: {{campaign}}", bodyTemplate: DEFAULT_BODY }]); }
  return <div className="space-y-3 border-t border-border pt-4"><div className="flex items-center justify-between"><h3 className="font-semibold">Lembretes automáticos</h3><Button type="button" variant="outline" size="sm" onClick={add} disabled={reminders.length >= 3}>Adicionar</Button></div>{reminders.map((reminder, index) => <div key={reminder.reminderIndex} className="rounded-lg border border-border bg-background p-3"><div className="grid gap-3 sm:grid-cols-2"><Field label={`Lembrete ${reminder.reminderIndex} após horas`}><input type="number" min={1} value={reminder.sendAfterHours} onChange={(event) => update(index, { sendAfterHours: Number(event.target.value) })} className="input" /></Field><Field label="Enviar quando"><select value={reminder.targetState} onChange={(event) => update(index, { targetState: event.target.value as ReminderRuleInput["targetState"] })} className="input"><option value="NOT_OPENED">Não abriu</option><option value="NOT_STARTED">Não iniciou</option><option value="IN_PROGRESS">Está em andamento</option></select></Field></div><Field label="Assunto"><input value={reminder.subjectTemplate} onChange={(event) => update(index, { subjectTemplate: event.target.value })} className="input" /></Field><Field label="Mensagem"><textarea value={reminder.bodyTemplate} onChange={(event) => update(index, { bodyTemplate: event.target.value })} className="min-h-24 w-full rounded-md border border-input bg-background px-3 py-2" /></Field><Button type="button" variant="ghost" size="sm" onClick={() => setReminders(reminders.filter((_, itemIndex) => itemIndex !== index).map((item, itemIndex) => ({ ...item, reminderIndex: itemIndex + 1 })))}>Remover</Button></div>)}</div>;
}

function CampaignList({ campaigns, loading, action }: { campaigns: ParticipationCampaign[]; loading: boolean; action: (id: string, action: "pause" | "resume" | "cancel") => void }) {
  if (loading) return <section className="rounded-xl border border-border bg-card py-12 text-center text-sm text-muted-foreground">Carregando campanhas...</section>;
  return <section className="space-y-4"><div className="flex items-center gap-2"><Users className="h-5 w-5 text-primary" /><h2 className="text-xl font-semibold">Campanhas da empresa</h2></div>{campaigns.length === 0 ? <StateBanner tone="warning" title="Nenhuma campanha criada">Use o formulário acima para importar o primeiro lote.</StateBanner> : campaigns.map((campaign) => <CampaignCard key={campaign.id} campaign={campaign} action={action} />)}</section>;
}

function CampaignCard({ campaign, action }: { campaign: ParticipationCampaign; action: (id: string, action: "pause" | "resume" | "cancel") => void }) {
  const totals = campaign.totals;
  return <article className="rounded-xl border border-border bg-card p-5"><div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between"><div><div className="text-xs font-semibold uppercase tracking-wide text-primary">{campaign.status}</div><h3 className="mt-1 text-lg font-semibold">{campaign.name}</h3><p className="mt-1 text-sm text-muted-foreground">{campaign.simulationId} · ciclo {campaign.applicationCycleId} · envio {new Date(campaign.initialSendAt).toLocaleString("pt-BR")}</p></div><div className="flex flex-wrap gap-2">{(campaign.status === "RUNNING" || campaign.status === "SCHEDULED") && <Button variant="outline" size="sm" onClick={() => action(campaign.id, "pause")}><Pause className="mr-2 h-4 w-4" />Pausar</Button>}{campaign.status === "PAUSED" && <Button variant="outline" size="sm" onClick={() => action(campaign.id, "resume")}><Play className="mr-2 h-4 w-4" />Retomar</Button>}{!(["COMPLETED", "CANCELLED"].includes(campaign.status)) && <Button variant="outline" size="sm" onClick={() => action(campaign.id, "cancel")}><XCircle className="mr-2 h-4 w-4" />Cancelar</Button>}<Button variant="outline" size="sm" onClick={() => void downloadCampaignCsv(campaign.id)}><Download className="mr-2 h-4 w-4" />Exportar CSV</Button></div></div><dl className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6"><Metric label="Total" value={String(totals.total)} /><Metric label="Entregues" value={String(totals.delivered)} /><Metric label="Abertos" value={String(totals.opened)} /><Metric label="Em andamento" value={String(totals.inProgress)} /><Metric label="Concluídos" value={String(totals.completed)} /><Metric label="Falhas/bounces" value={String(totals.failed + totals.bounced)} /></dl><div className="mt-4 overflow-x-auto"><table className="w-full min-w-[780px] text-left text-sm"><thead><tr><th className="pb-2">Pessoa</th><th className="pb-2">Comunicação</th><th className="pb-2">Participação</th><th className="pb-2">Expiração</th><th className="pb-2">Erro</th></tr></thead><tbody>{campaign.participants.map((participant) => <tr key={participant.id} className="border-t border-border"><td className="py-3"><div className="font-medium">{participant.candidateName || "Dados removidos"}</div><div className="text-xs text-muted-foreground">{participant.maskedEmail}</div></td><td className="py-3">{participant.communicationStatus}</td><td className="py-3">{participant.participationStatus}</td><td className="py-3">{participant.linkExpiresAt ? new Date(participant.linkExpiresAt).toLocaleString("pt-BR") : "-"}</td><td className="max-w-xs py-3 text-destructive">{participant.lastError || "-"}</td></tr>)}</tbody></table></div></article>;
}

function Card({ icon, title, children }: { icon: React.ReactNode; title: string; children: React.ReactNode }) { return <section className="rounded-xl border border-border bg-card p-5"><div className="flex items-center gap-2">{icon}<h2 className="font-semibold">{title}</h2></div><div className="mt-4 space-y-4">{children}</div></section>; }
function Field({ label, children }: { label: string; children: React.ReactNode }) { return <label className="block space-y-1.5 text-sm"><span className="font-medium">{label}</span>{children}</label>; }
function Metric({ label, value }: { label: string; value: string }) { return <div><dt className="text-xs uppercase tracking-wide text-muted-foreground">{label}</dt><dd className="mt-1 font-semibold">{value}</dd></div>; }
function ErrorBanner({ error }: { error: unknown }) { return <StateBanner tone="danger" title="Não foi possível concluir a operação">{error instanceof Error ? error.message : "Tente novamente."}</StateBanner>; }
function localDateTimeValue(date: Date) { const offset = date.getTimezoneOffset() * 60_000; return new Date(date.getTime() - offset).toISOString().slice(0, 16); }
