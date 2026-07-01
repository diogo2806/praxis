import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Check, Copy, ExternalLink, KeyRound, Send, Webhook } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { SkeletonRows, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  configureGenericWebhook,
  generatePublicApiToken,
  getGenericWebhook,
  revokePublicApiToken,
  rotateGenericWebhookSecret,
  testGenericWebhook,
  type WebhookEvent,
  type WebhookTestResponse,
} from "@/lib/api/praxis";
import { getApiBaseUrl } from "@/lib/runtime-config";

export const Route = createFileRoute("/configuracoes/api")({
  head: () => ({
    meta: [
      { title: "API e Webhooks - Praxis" },
      {
        name: "description",
        content: "Webhook assinado e token de API pública para integrar qualquer sistema.",
      },
    ],
  }),
  component: ApiSettingsPage,
});

const EVENT_OPTIONS: { value: WebhookEvent; label: string }[] = [
  { value: "RESULT_READY", label: "Resultado pronto" },
  { value: "ATTEMPT_STARTED", label: "Tentativa iniciada" },
];

function ApiSettingsPage() {
  const queryClient = useQueryClient();
  const webhookQuery = useQuery({
    queryKey: ["generic-webhook"],
    queryFn: getGenericWebhook,
  });

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6">
          <div className="text-xs uppercase text-primary">Configurações</div>
          <h1 className="mt-1 text-3xl font-semibold">API e Webhooks</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Conecte qualquer ATS ou sistema interno: receba resultados via webhook assinado (HMAC) e
            consulte a Práxis com um token de API.
          </p>
        </div>

        {webhookQuery.isLoading ? (
          <SkeletonRows rows={3} />
        ) : webhookQuery.isError ? (
          <StateBanner tone="danger" title="Não foi possível carregar a configuração">
            {webhookQuery.error instanceof Error ? webhookQuery.error.message : "Tente novamente."}
          </StateBanner>
        ) : (
          <WebhookSection
            initialUrl={webhookQuery.data?.webhookUrl ?? ""}
            initialEvents={(webhookQuery.data?.events as WebhookEvent[]) ?? ["RESULT_READY"]}
            secretPreview={webhookQuery.data?.secretPreview ?? null}
            lastError={webhookQuery.data?.lastError ?? null}
            onChanged={() => queryClient.invalidateQueries({ queryKey: ["generic-webhook"] })}
          />
        )}

        <PublicTokenSection />
      </div>
    </AppShell>
  );
}

function WebhookSection({
  initialUrl,
  initialEvents,
  secretPreview,
  lastError,
  onChanged,
}: {
  initialUrl: string;
  initialEvents: WebhookEvent[];
  secretPreview: string | null;
  lastError: string | null;
  onChanged: () => void;
}) {
  const [url, setUrl] = useState(initialUrl);
  const [events, setEvents] = useState<WebhookEvent[]>(
    initialEvents.length > 0 ? initialEvents : ["RESULT_READY"],
  );
  const [revealedSecret, setRevealedSecret] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<WebhookTestResponse | null>(null);

  const saveMutation = useMutation({
    mutationFn: () => configureGenericWebhook({ webhookUrl: url.trim(), events }),
    onSuccess: onChanged,
  });
  const rotateMutation = useMutation({
    mutationFn: rotateGenericWebhookSecret,
    onSuccess: (response) => {
      setRevealedSecret(response.secret);
      onChanged();
    },
  });
  const testMutation = useMutation({
    mutationFn: testGenericWebhook,
    onSuccess: (response) => setTestResult(response),
  });

  const toggleEvent = (value: WebhookEvent) => {
    setEvents((current) =>
      current.includes(value) ? current.filter((event) => event !== value) : [...current, value],
    );
  };

  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border px-5 py-4">
        <div className="flex items-center gap-2 text-sm font-semibold">
          <Webhook className="h-4 w-4 text-primary" />
          Webhook de resultados
        </div>
      </div>
      <div className="space-y-4 p-5">
        {saveMutation.isError && (
          <StateBanner tone="danger" title="Não foi possível salvar o webhook">
            {saveMutation.error instanceof Error ? saveMutation.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {lastError && (
          <StateBanner tone="warn" title="Última entrega falhou">
            {lastError}
          </StateBanner>
        )}

        <label className="block">
          <span className="mb-1 block text-xs text-muted-foreground">URL de destino</span>
          <Input
            value={url}
            onChange={(event) => setUrl(event.target.value)}
            placeholder="https://meu-ats.com/webhooks/praxis"
            className="font-mono text-xs"
          />
        </label>

        <div>
          <span className="mb-1 block text-xs text-muted-foreground">Eventos</span>
          <div className="flex flex-wrap gap-4">
            {EVENT_OPTIONS.map((option) => (
              <label key={option.value} className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={events.includes(option.value)}
                  onChange={() => toggleEvent(option.value)}
                />
                {option.label}
              </label>
            ))}
          </div>
        </div>

        <div className="rounded-md border border-border bg-background p-3">
          <div className="text-xs text-muted-foreground">Segredo (HMAC)</div>
          <div className="mt-1 font-mono text-sm">{secretPreview ?? "ainda não gerado"}</div>
          {revealedSecret && (
            <div className="mt-3 rounded-md border border-warning/35 bg-warning/10 p-3">
              <div className="text-xs font-semibold text-warning-foreground">
                Copie agora. Este segredo não será exibido novamente.
              </div>
              <CopyableValue value={revealedSecret} />
            </div>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            onClick={() => saveMutation.mutate()}
            disabled={saveMutation.isPending || url.trim().length === 0}
          >
            Salvar
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => rotateMutation.mutate()}
            disabled={rotateMutation.isPending}
          >
            <KeyRound className="h-4 w-4" />
            {secretPreview ? "Rotacionar e revelar segredo" : "Gerar segredo"}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => testMutation.mutate()}
            disabled={testMutation.isPending || !secretPreview}
          >
            <Send className="h-4 w-4" />
            Enviar evento de teste
          </Button>
        </div>

        {testResult && (
          <StateBanner
            tone={testResult.delivered ? "ok" : "danger"}
            title={testResult.delivered ? "Evento entregue" : "Falha na entrega"}
          >
            HTTP {testResult.httpStatus ?? "—"}
            {testResult.responseSnippet ? ` · ${testResult.responseSnippet}` : ""}
          </StateBanner>
        )}
      </div>
    </section>
  );
}

function PublicTokenSection() {
  const [token, setToken] = useState<string | null>(null);
  const [preview, setPreview] = useState<string | null>(null);

  const generateMutation = useMutation({
    mutationFn: generatePublicApiToken,
    onSuccess: (response) => {
      setToken(response.token);
      setPreview(response.tokenPreview);
    },
  });
  const revokeMutation = useMutation({
    mutationFn: revokePublicApiToken,
    onSuccess: () => {
      setToken(null);
      setPreview(null);
    },
  });

  const docsUrl = `${getApiBaseUrl()}/swagger-ui/index.html`;

  return (
    <section className="mt-6 rounded-md border border-border bg-card">
      <div className="border-b border-border px-5 py-4">
        <div className="flex items-center gap-2 text-sm font-semibold">
          <KeyRound className="h-4 w-4 text-primary" />
          Token de API pública
        </div>
      </div>
      <div className="space-y-4 p-5">
        {(generateMutation.isError || revokeMutation.isError) && (
          <StateBanner tone="danger" title="Não foi possível atualizar o token">
            {(generateMutation.error ?? revokeMutation.error) instanceof Error
              ? (generateMutation.error ?? revokeMutation.error)?.message
              : "Tente novamente."}
          </StateBanner>
        )}
        <p className="text-sm text-muted-foreground">
          Use este token para consultar resultados via nossa API. {preview ? `Atual: ${preview}` : ""}
        </p>

        {token && (
          <div className="rounded-md border border-warning/35 bg-warning/10 p-3">
            <div className="text-xs font-semibold text-warning-foreground">
              Copie agora. Este token não será exibido novamente.
            </div>
            <CopyableValue value={token} />
          </div>
        )}

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            onClick={() => generateMutation.mutate()}
            disabled={generateMutation.isPending}
          >
            {preview ? "Revogar e gerar novo" : "Gerar token"}
          </Button>
          {preview && (
            <Button
              type="button"
              variant="outline"
              onClick={() => revokeMutation.mutate()}
              disabled={revokeMutation.isPending}
            >
              Revogar
            </Button>
          )}
          <a
            href={docsUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-accent"
          >
            Ver documentação da API
            <ExternalLink className="h-4 w-4" />
          </a>
        </div>
      </div>
    </section>
  );
}

function CopyableValue({ value }: { value: string }) {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <div className="mt-2 grid gap-2 md:grid-cols-[1fr_auto]">
      <Input readOnly value={value} className="font-mono text-xs" />
      <Button type="button" variant="outline" onClick={() => void copy()}>
        {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
        {copied ? "Copiado" : "Copiar"}
      </Button>
    </div>
  );
}
