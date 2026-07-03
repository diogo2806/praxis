import { Link, Outlet, createFileRoute, useChildMatches } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import {
  AlertCircle,
  CheckCircle2,
  CloudOff,
  ExternalLink,
  Eye,
  Key,
  PlugZap,
  RefreshCw,
  Settings,
  Unplug,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { EmptyState, SkeletonRows, StateBanner } from "@/components/praxis-ui";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  configureIntegration,
  disconnectIntegration,
  generateIntegrationToken,
  listIntegrations,
  reactivateIntegration,
  syncIntegration,
  testIntegrationConnection,
  type ConfigureIntegrationRequest,
  type GenerateIntegrationTokenResponse,
  type IntegrationCenterAction,
  type IntegrationCenterItem,
  type IntegrationCenterProvider,
  type IntegrationCenterStatus,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/integrations")({
  head: () => ({
    meta: [
      { title: "Integrações - Práxis" },
      {
        name: "description",
        content: "Central de integrações externas do Práxis.",
      },
    ],
  }),
  component: IntegrationsRouteLayout,
});

// Esta é a rota-pai das sub-rotas; sem <Outlet /> as filhas nunca renderizam
// e toda navegação interna cai de volta nesta página.
function IntegrationsRouteLayout() {
  const childMatches = useChildMatches();
  if (childMatches.length > 0) {
    return <Outlet />;
  }
  return <IntegrationsPage />;
}

type ConfigureFormState = {
  baseUrl: string;
  tokenLabel: string;
};

const EMPTY_CONFIGURE_FORM: ConfigureFormState = {
  baseUrl: "",
  tokenLabel: "",
};

const PROVIDER_SLUG: Record<IntegrationCenterProvider, string> = {
  GUPY: "gupy",
  RECRUTEI: "recrutei",
  CUSTOM_API: "custom-api",
};

const CUSTOM_API_DOCS_URL = "/docs/integracao-api-propria";

const statusLabel: Record<IntegrationCenterStatus, string> = {
  CONECTADA: "Conectada",
  PENDENTE: "Pendente",
  ERRO: "Erro",
  DESATIVADA: "Desativada",
  NAO_CONFIGURADA: "Não configurada",
};

function IntegrationsPage() {
  const queryClient = useQueryClient();
  const [configuring, setConfiguring] = useState<IntegrationCenterItem | null>(null);
  const [disconnecting, setDisconnecting] = useState<IntegrationCenterItem | null>(null);
  const [generatedToken, setGeneratedToken] = useState<GenerateIntegrationTokenResponse | null>(
    null,
  );
  const [testedProvider, setTestedProvider] = useState<IntegrationCenterProvider | null>(null);

  const integrationsQuery = useQuery({
    queryKey: ["integrations"],
    queryFn: listIntegrations,
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ["integrations"] });
    await queryClient.invalidateQueries({ queryKey: ["integration-tokens"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
  };

  const configureMutation = useMutation({
    mutationFn: ({
      provider,
      body,
    }: {
      provider: IntegrationCenterProvider;
      body: ConfigureIntegrationRequest;
    }) => configureIntegration(provider, body),
    onSuccess: async () => {
      setConfiguring(null);
      await invalidate();
    },
  });

  const disconnectMutation = useMutation({
    mutationFn: disconnectIntegration,
    onSuccess: async () => {
      setDisconnecting(null);
      await invalidate();
    },
  });

  const syncMutation = useMutation({
    mutationFn: syncIntegration,
    onSuccess: invalidate,
  });

  const reactivateMutation = useMutation({
    mutationFn: reactivateIntegration,
    onSuccess: invalidate,
  });

  const testConnectionMutation = useMutation({
    mutationFn: testIntegrationConnection,
    onSuccess: async (_data, provider) => {
      setTestedProvider(provider);
      await invalidate();
      window.setTimeout(() => setTestedProvider(null), 2500);
    },
  });

  const generateTokenMutation = useMutation({
    mutationFn: generateIntegrationToken,
    onSuccess: async (data) => {
      setGeneratedToken(data);
      await invalidate();
    },
  });

  const integrations = integrationsQuery.data ?? [];
  const configuredCount = integrations.filter(
    (integration) => integration.status !== "NAO_CONFIGURADA",
  ).length;
  const hasOnlyUnconfigured = integrations.length > 0 && configuredCount === 0;
  const actionError =
    configureMutation.error ??
    disconnectMutation.error ??
    syncMutation.error ??
    reactivateMutation.error ??
    testConnectionMutation.error ??
    generateTokenMutation.error;

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl">
        <header className="mb-6 flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="text-xs uppercase text-primary">Práxis</div>
            <h1 className="mt-1 text-3xl font-semibold">Integrações</h1>
            <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
              Conecte o Práxis aos sistemas que sua empresa já utiliza.
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            onClick={() => void integrationsQuery.refetch()}
            disabled={integrationsQuery.isFetching}
          >
            <RefreshCw className={cn("h-4 w-4", integrationsQuery.isFetching && "animate-spin")} />
            Atualizar
          </Button>
        </header>

        {integrationsQuery.isLoading && <SkeletonRows rows={3} />}

        {integrationsQuery.isError && (
          <IntegrationErrorState
            message={
              integrationsQuery.error instanceof Error
                ? integrationsQuery.error.message
                : "Tente novamente."
            }
            onRetry={() => void integrationsQuery.refetch()}
          />
        )}

        {actionError && (
          <StateBanner tone="danger" title="Não foi possível concluir a ação">
            {actionError instanceof Error ? actionError.message : "Tente novamente."}
          </StateBanner>
        )}

        {!integrationsQuery.isLoading && !integrationsQuery.isError && hasOnlyUnconfigured && (
          <IntegrationEmptyState firstIntegration={integrations[0]} onConfigure={setConfiguring} />
        )}

        {!integrationsQuery.isLoading && !integrationsQuery.isError && (
          <section className="mt-5 grid gap-4">
            {integrations.map((integration) => (
              <IntegrationCard
                key={integration.provider}
                integration={integration}
                pendingAction={
                  configureMutation.isPending ||
                  disconnectMutation.isPending ||
                  syncMutation.isPending ||
                  reactivateMutation.isPending ||
                  testConnectionMutation.isPending ||
                  generateTokenMutation.isPending
                }
                syncing={syncMutation.isPending && syncMutation.variables === integration.provider}
                testingConnection={
                  testConnectionMutation.isPending &&
                  testConnectionMutation.variables === integration.provider
                }
                connectionTested={testedProvider === integration.provider}
                reactivating={
                  reactivateMutation.isPending &&
                  reactivateMutation.variables === integration.provider
                }
                onConfigure={() => setConfiguring(integration)}
                onDisconnect={() => setDisconnecting(integration)}
                onSync={() => syncMutation.mutate(integration.provider)}
                onTestConnection={() => testConnectionMutation.mutate(integration.provider)}
                onReactivate={() => reactivateMutation.mutate(integration.provider)}
                onGenerateToken={() => generateTokenMutation.mutate(integration.provider)}
              />
            ))}
          </section>
        )}
      </div>

      <IntegrationConfigModal
        integration={configuring}
        pending={configureMutation.isPending}
        error={configureMutation.error instanceof Error ? configureMutation.error.message : null}
        onOpenChange={(open) => !open && setConfiguring(null)}
        onSubmit={(provider, body) => configureMutation.mutate({ provider, body })}
        onGenerateToken={(provider) => {
          setConfiguring(null);
          generateTokenMutation.mutate(provider);
        }}
      />
      <IntegrationDisconnectDialog
        integration={disconnecting}
        pending={disconnectMutation.isPending}
        onOpenChange={(open) => !open && setDisconnecting(null)}
        onConfirm={(provider) => disconnectMutation.mutate(provider)}
      />
      <GeneratedTokenModal tokenResponse={generatedToken} onClose={() => setGeneratedToken(null)} />
    </AppShell>
  );
}

function IntegrationCard({
  integration,
  pendingAction,
  syncing,
  testingConnection,
  connectionTested,
  reactivating,
  onConfigure,
  onDisconnect,
  onSync,
  onTestConnection,
  onReactivate,
  onGenerateToken,
}: {
  integration: IntegrationCenterItem;
  pendingAction: boolean;
  syncing: boolean;
  testingConnection: boolean;
  connectionTested: boolean;
  reactivating: boolean;
  onConfigure: () => void;
  onDisconnect: () => void;
  onSync: () => void;
  onTestConnection: () => void;
  onReactivate: () => void;
  onGenerateToken: () => void;
}) {
  return (
    <article className="rounded-md border border-border bg-card p-5">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-xl font-semibold">{integration.name}</h2>
            <IntegrationStatusBadge integration={integration} />
          </div>
          <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{integration.description}</p>
          <p className="mt-3 text-sm">
            <span className="font-medium">Como funciona:</span>{" "}
            {integration.provider === "CUSTOM_API"
              ? "Sistema interno envia candidatos via API -> Praxis avalia -> webhook recebe o resultado."
              : "ATS envia candidato -> Praxis avalia -> Praxis devolve o resultado."}
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2">
          <CardActions
            integration={integration}
            pendingAction={pendingAction}
            syncing={syncing}
            testingConnection={testingConnection}
            connectionTested={connectionTested}
            reactivating={reactivating}
            onConfigure={onConfigure}
            onDisconnect={onDisconnect}
            onSync={onSync}
            onTestConnection={onTestConnection}
            onReactivate={onReactivate}
            onGenerateToken={onGenerateToken}
          />
        </div>
      </div>

      <div className="mt-4 grid gap-3 text-sm md:grid-cols-3">
        <InfoItem label="Última atividade" value={formatDateTime(integration.lastSyncAt)} />
        <InfoItem
          label="Tipo"
          value={integration.type === "API" ? "API personalizada" : integration.type}
        />
        <InfoItem label="Configurada em" value={formatDateTime(integration.configuredAt)} />
      </div>

      {integration.errorMessage && (
        <div className="mt-4 rounded-md border border-danger/25 bg-danger/10 p-3 text-sm">
          <div className="flex items-start gap-2">
            <AlertCircle className="mt-0.5 h-4 w-4 text-danger" />
            <div>
              <div className="font-medium">Falha na integração</div>
              <div className="mt-1 text-muted-foreground">{integration.errorMessage}</div>
            </div>
          </div>
        </div>
      )}
    </article>
  );
}

function CardActions({
  integration,
  pendingAction,
  syncing,
  testingConnection,
  connectionTested,
  reactivating,
  onConfigure,
  onDisconnect,
  onSync,
  onTestConnection,
  onReactivate,
  onGenerateToken,
}: {
  integration: IntegrationCenterItem;
  pendingAction: boolean;
  syncing: boolean;
  testingConnection: boolean;
  connectionTested: boolean;
  reactivating: boolean;
  onConfigure: () => void;
  onDisconnect: () => void;
  onSync: () => void;
  onTestConnection: () => void;
  onReactivate: () => void;
  onGenerateToken: () => void;
}) {
  const hasAction = (action: IntegrationCenterAction) =>
    integration.availableActions.includes(action);
  const slug = PROVIDER_SLUG[integration.provider];

  return (
    <>
      {hasAction("CONFIGURE") && (
        <Button type="button" size="sm" onClick={onConfigure} disabled={pendingAction}>
          <Settings className="h-4 w-4" />
          {integration.status === "PENDENTE" ? "Continuar configuração" : "Configurar"}
        </Button>
      )}
      {hasAction("EDIT") && (
        <Button type="button" size="sm" onClick={onConfigure} disabled={pendingAction}>
          <Settings className="h-4 w-4" />
          Editar configuração
        </Button>
      )}
      {hasAction("REACTIVATE") && (
        <Button type="button" size="sm" onClick={onReactivate} disabled={pendingAction}>
          <PlugZap className="h-4 w-4" />
          {reactivating ? "Reativando" : "Reativar"}
        </Button>
      )}
      {hasAction("VIEW") && (
        <Button type="button" size="sm" variant="outline" asChild>
          <Link to="/integrations/$provider" params={{ provider: slug }}>
            <Eye className="h-4 w-4" />
            Ver configuração
          </Link>
        </Button>
      )}
      {/* API própria: webhook e token de API pública vivem na página de detalhe.
          Garante acesso a essa configuração em qualquer status, mesmo sem a ação VIEW. */}
      {integration.provider === "CUSTOM_API" && !hasAction("VIEW") && (
        <Button type="button" size="sm" variant="outline" asChild>
          <Link to="/integrations/$provider" params={{ provider: slug }}>
            <Eye className="h-4 w-4" />
            Ver configuração
          </Link>
        </Button>
      )}
      {hasAction("SYNC") && (
        <IntegrationSyncButton
          pending={syncing}
          disabled={pendingAction && !syncing}
          onClick={onSync}
        />
      )}
      {hasAction("TEST_CONNECTION") && (
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={onTestConnection}
          disabled={pendingAction && !testingConnection}
        >
          <RefreshCw className={cn("h-4 w-4", testingConnection && "animate-spin")} />
          {testingConnection ? "Testando" : connectionTested ? "Conexão ok" : "Testar conexão"}
        </Button>
      )}
      {hasAction("RETRY") && (
        <Button type="button" size="sm" variant="outline" onClick={onSync} disabled={pendingAction}>
          <RefreshCw className="h-4 w-4" />
          Tentar novamente
        </Button>
      )}
      {hasAction("VIEW_ERROR") && integration.errorMessage && (
        <Button type="button" size="sm" variant="outline" asChild>
          <Link to="/integrations/$provider" params={{ provider: slug }}>
            <AlertCircle className="h-4 w-4" />
            Ver erro
          </Link>
        </Button>
      )}
      {hasAction("DISCONNECT") && (
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={onDisconnect}
          disabled={pendingAction}
        >
          <Unplug className="h-4 w-4" />
          Desconectar
        </Button>
      )}
      {hasAction("VIEW_DOCS") && (
        <Button type="button" size="sm" variant="outline" asChild>
          <a href={CUSTOM_API_DOCS_URL} target="_blank" rel="noopener noreferrer">
            <ExternalLink className="h-4 w-4" />
            Ver documentação
          </a>
        </Button>
      )}
      {hasAction("GENERATE_TOKEN") && (
        <Button type="button" size="sm" onClick={onGenerateToken} disabled={pendingAction}>
          <Key className="h-4 w-4" />
          Gerar token
        </Button>
      )}
    </>
  );
}

function IntegrationStatusBadge({ integration }: { integration: IntegrationCenterItem }) {
  const { status } = integration;
  const Icon =
    status === "CONECTADA"
      ? CheckCircle2
      : status === "DESATIVADA"
        ? CloudOff
        : status === "ERRO"
          ? AlertCircle
          : PlugZap;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-xs font-medium",
        status === "CONECTADA" && "border-success/25 bg-success/10 text-success",
        status === "PENDENTE" && "border-warning/35 bg-warning/15 text-warning-foreground",
        status === "ERRO" && "border-danger/25 bg-danger/10 text-danger",
        status === "DESATIVADA" && "border-border bg-muted text-muted-foreground",
        status === "NAO_CONFIGURADA" && "border-border bg-background text-muted-foreground",
      )}
    >
      <Icon className="h-3.5 w-3.5" />
      {status === "CONECTADA" && !integration.lastSyncAt
        ? "Conectada · aguardando primeiro evento"
        : statusLabel[status]}
    </span>
  );
}

function IntegrationConfigModal({
  integration,
  pending,
  error,
  onOpenChange,
  onSubmit,
  onGenerateToken,
}: {
  integration: IntegrationCenterItem | null;
  pending: boolean;
  error: string | null;
  onOpenChange: (open: boolean) => void;
  onSubmit: (provider: IntegrationCenterProvider, body: ConfigureIntegrationRequest) => void;
  onGenerateToken: (provider: IntegrationCenterProvider) => void;
}) {
  const [form, setForm] = useState<ConfigureFormState>(EMPTY_CONFIGURE_FORM);
  const [localError, setLocalError] = useState<string | null>(null);
  const provider = integration?.provider ?? null;
  const usesToken = provider === "GUPY" || provider === "RECRUTEI";

  // Limpa o formulário ao abrir para outro provedor ou reabrir o modal.
  useEffect(() => {
    setForm(EMPTY_CONFIGURE_FORM);
    setLocalError(null);
  }, [provider]);

  const title = useMemo(() => {
    if (!integration) return "Configurar integração";
    return integration.status === "CONECTADA"
      ? `Configuração de ${integration.name}`
      : `Configurar ${integration.name}`;
  }, [integration]);

  const open = Boolean(integration);

  function submit() {
    if (!integration) return;
    const baseUrl = form.baseUrl.trim();
    if (!baseUrl) {
      setLocalError("Informe a URL base da sua API.");
      return;
    }
    if (!isValidHttpUrl(baseUrl)) {
      setLocalError("A URL base deve ser um endereço válido iniciando com http:// ou https://.");
      return;
    }
    setLocalError(null);
    const tokenLabel = form.tokenLabel.trim();
    onSubmit(integration.provider, {
      credentials: tokenLabel ? { tokenLabel } : {},
      settings: { baseUrl },
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            {usesToken
              ? `A conexão com ${integration?.name} é feita por token de acesso. Por segurança, o token é exibido uma única vez e o Práxis guarda apenas o hash.`
              : "Informe os dados do seu sistema. Credenciais sensíveis não são exibidas depois de salvas."}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4">
          {usesToken && integration && (
            <ol className="grid gap-3 text-sm">
              <TokenStep number={1}>
                Gere o token de acesso do Práxis clicando no botão abaixo.
              </TokenStep>
              <TokenStep number={2}>
                Copie o token e cole no painel de integrações do {integration.name}.
              </TokenStep>
              <TokenStep number={3}>
                Pronto: o {integration.name} passa a enviar candidatos automaticamente e o Práxis
                devolve o resultado da avaliação.
              </TokenStep>
            </ol>
          )}
          {provider === "CUSTOM_API" && (
            <>
              <div className="grid gap-2">
                <Label htmlFor="custom-api-base-url">
                  URL base da sua API <span className="text-danger">*</span>
                </Label>
                <Input
                  id="custom-api-base-url"
                  type="url"
                  placeholder="https://api.suaempresa.com"
                  value={form.baseUrl}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, baseUrl: event.target.value }))
                  }
                />
                <p className="text-xs text-muted-foreground">
                  Endereço do sistema interno que vai se comunicar com o Práxis.
                </p>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="custom-api-token-label">Identificação da credencial</Label>
                <Input
                  id="custom-api-token-label"
                  placeholder="Ex.: Token do middleware interno"
                  value={form.tokenLabel}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, tokenLabel: event.target.value }))
                  }
                />
                <p className="text-xs text-muted-foreground">
                  Opcional. Um apelido para identificar esta credencial mais tarde.
                </p>
              </div>
            </>
          )}
          {(localError || error) && (
            <StateBanner tone="danger" title="Configuração inválida">
              {localError ?? error}
            </StateBanner>
          )}
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={pending}
          >
            Cancelar
          </Button>
          {usesToken && integration ? (
            <Button
              type="button"
              onClick={() => onGenerateToken(integration.provider)}
              disabled={pending}
            >
              <Key className="h-4 w-4" />
              Gerar token
            </Button>
          ) : (
            <Button type="button" onClick={submit} disabled={pending}>
              <Settings className="h-4 w-4" />
              {pending
                ? "Salvando"
                : integration?.status === "DESATIVADA"
                  ? "Reativar"
                  : "Salvar configuração"}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function TokenStep({ number, children }: { number: number; children: ReactNode }) {
  return (
    <li className="flex items-start gap-3">
      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
        {number}
      </span>
      <span className="pt-0.5">{children}</span>
    </li>
  );
}

function IntegrationDisconnectDialog({
  integration,
  pending,
  onOpenChange,
  onConfirm,
}: {
  integration: IntegrationCenterItem | null;
  pending: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: (provider: IntegrationCenterProvider) => void;
}) {
  return (
    <AlertDialog open={Boolean(integration)} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Desconectar integração</AlertDialogTitle>
          <AlertDialogDescription>
            A integração {integration?.name} será desativada para este cliente. O histórico de
            sincronizações e auditoria será preservado.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending}>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            disabled={pending || !integration}
            onClick={(event) => {
              event.preventDefault();
              if (integration) onConfirm(integration.provider);
            }}
          >
            {pending ? "Desconectando" : "Desconectar"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

function GeneratedTokenModal({
  tokenResponse,
  onClose,
}: {
  tokenResponse: GenerateIntegrationTokenResponse | null;
  onClose: () => void;
}) {
  const [copied, setCopied] = useState(false);

  async function copyToken() {
    if (!tokenResponse) return;
    await navigator.clipboard.writeText(tokenResponse.token);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <Dialog open={Boolean(tokenResponse)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Token gerado</DialogTitle>
          <DialogDescription>
            Copie o token agora. Por segurança, ele não será exibido novamente após fechar esta
            janela.
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-3">
          <code className="rounded-md border border-input bg-muted px-3 py-2 font-mono text-xs break-all select-all">
            {tokenResponse?.token}
          </code>
          <Button type="button" variant="outline" onClick={copyToken}>
            {copied ? "Copiado!" : "Copiar token"}
          </Button>
        </div>
        <DialogFooter>
          <Button type="button" onClick={onClose}>
            Fechar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function IntegrationSyncButton({
  pending,
  disabled,
  onClick,
}: {
  pending: boolean;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <Button
      type="button"
      size="sm"
      variant="outline"
      onClick={onClick}
      disabled={disabled || pending}
    >
      <RefreshCw className={cn("h-4 w-4", pending && "animate-spin")} />
      {pending ? "Sincronizando" : "Sincronizar agora"}
    </Button>
  );
}

function IntegrationEmptyState({
  firstIntegration,
  onConfigure,
}: {
  firstIntegration: IntegrationCenterItem | undefined;
  onConfigure: (integration: IntegrationCenterItem) => void;
}) {
  return (
    <EmptyState
      title="Nenhuma integração configurada ainda."
      description="Conecte o Práxis aos sistemas que sua empresa já utiliza para automatizar o envio de candidatos e resultados."
      actions={
        <Button
          type="button"
          disabled={!firstIntegration}
          onClick={() => firstIntegration && onConfigure(firstIntegration)}
        >
          <Settings className="h-4 w-4" />
          Configurar primeira integração
        </Button>
      }
    />
  );
}

function IntegrationErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <StateBanner
      tone="danger"
      title="Não foi possível carregar integrações"
      action={
        <Button type="button" size="sm" variant="outline" onClick={onRetry}>
          <RefreshCw className="h-4 w-4" />
          Tentar novamente
        </Button>
      }
    >
      {message}
    </StateBanner>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 font-medium">{value}</div>
    </div>
  );
}

function formatDateTime(value: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function isValidHttpUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}
