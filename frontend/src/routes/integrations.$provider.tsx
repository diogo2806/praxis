import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  CloudOff,
  Copy,
  ExternalLink,
  Key,
  PlugZap,
  RefreshCw,
  RotateCcw,
  Trash2,
  Unplug,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { SkeletonRows, StateBanner } from "@/components/praxis-ui";
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
  generateIntegrationToken,
  getIntegration,
  disconnectIntegration,
  reactivateIntegration,
  rotateIntegrationProviderToken,
  revokeIntegrationProviderToken,
  syncIntegration,
  testIntegrationConnection,
  type GenerateIntegrationTokenResponse,
  type IntegrationCenterItem,
  type IntegrationCenterProvider,
  type IntegrationCenterStatus,
} from "@/lib/api/praxis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/integrations/$provider")({
  head: () => ({
    meta: [{ title: "Detalhes da integração - Práxis" }],
  }),
  component: IntegrationDetailPage,
});

const statusLabel: Record<IntegrationCenterStatus, string> = {
  CONECTADA: "Conectada",
  PENDENTE: "Pendente",
  ERRO: "Erro",
  DESATIVADA: "Desativada",
  NAO_CONFIGURADA: "Não configurada",
};

const PROVIDER_SLUG_MAP: Record<string, IntegrationCenterProvider> = {
  gupy: "GUPY",
  recrutei: "RECRUTEI",
  "custom-api": "CUSTOM_API",
};

const CUSTOM_API_DOCS_URL = "/docs/integracao-api-propria";

function IntegrationDetailPage() {
  const { provider: providerSlug } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const provider = PROVIDER_SLUG_MAP[providerSlug] as IntegrationCenterProvider | undefined;

  const [confirmDisconnect, setConfirmDisconnect] = useState(false);
  const [confirmRevokeToken, setConfirmRevokeToken] = useState(false);
  const [generatedToken, setGeneratedToken] = useState<GenerateIntegrationTokenResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const [connectionTested, setConnectionTested] = useState(false);

  const integrationQuery = useQuery({
    queryKey: ["integration", provider],
    queryFn: () => getIntegration(provider!),
    enabled: Boolean(provider),
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ["integration", provider] });
    await queryClient.invalidateQueries({ queryKey: ["integrations"] });
  };

  const disconnectMutation = useMutation({
    mutationFn: () => disconnectIntegration(provider!),
    onSuccess: async () => {
      setConfirmDisconnect(false);
      await invalidate();
    },
  });

  const reactivateMutation = useMutation({
    mutationFn: () => reactivateIntegration(provider!),
    onSuccess: invalidate,
  });

  const syncMutation = useMutation({
    mutationFn: () => syncIntegration(provider!),
    onSuccess: invalidate,
  });

  const testConnectionMutation = useMutation({
    mutationFn: () => testIntegrationConnection(provider!),
    onSuccess: async () => {
      setConnectionTested(true);
      await invalidate();
      window.setTimeout(() => setConnectionTested(false), 2500);
    },
  });

  const generateTokenMutation = useMutation({
    mutationFn: () => generateIntegrationToken(provider!),
    onSuccess: async (data) => {
      setGeneratedToken(data);
      await invalidate();
    },
  });

  const rotateTokenMutation = useMutation({
    mutationFn: () => rotateIntegrationProviderToken(provider!),
    onSuccess: async (data) => {
      setGeneratedToken(data);
      await invalidate();
    },
  });

  const revokeTokenMutation = useMutation({
    mutationFn: () => revokeIntegrationProviderToken(provider!),
    onSuccess: async () => {
      setConfirmRevokeToken(false);
      setGeneratedToken(null);
      await invalidate();
    },
  });

  if (!provider) {
    return (
      <AppShell>
        <div className="mx-auto max-w-3xl">
          <StateBanner tone="danger" title="Integração não encontrada">
            O provedor informado não existe.
          </StateBanner>
          <Button asChild variant="outline" className="mt-4">
            <Link to="/integrations">
              <ArrowLeft className="h-4 w-4" />
              Voltar para Integrações
            </Link>
          </Button>
        </div>
      </AppShell>
    );
  }

  const integration = integrationQuery.data;
  const anyPending =
    disconnectMutation.isPending ||
    reactivateMutation.isPending ||
    syncMutation.isPending ||
    testConnectionMutation.isPending ||
    generateTokenMutation.isPending ||
    rotateTokenMutation.isPending ||
    revokeTokenMutation.isPending;

  const hasAction = (action: string) => integration?.availableActions.includes(action as never) ?? false;
  const actionError =
    disconnectMutation.error ??
    reactivateMutation.error ??
    syncMutation.error ??
    testConnectionMutation.error ??
    generateTokenMutation.error ??
    rotateTokenMutation.error ??
    revokeTokenMutation.error;

  async function copyToken(token: string) {
    await navigator.clipboard.writeText(token);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-3xl">
        <Button asChild variant="ghost" size="sm" className="mb-4 -ml-2">
          <Link to="/integrations">
            <ArrowLeft className="h-4 w-4" />
            Integrações
          </Link>
        </Button>

        {integrationQuery.isLoading && <SkeletonRows rows={4} />}

        {integrationQuery.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar a integração">
            {integrationQuery.error instanceof Error ? integrationQuery.error.message : "Tente novamente."}
          </StateBanner>
        )}

        {actionError && (
          <StateBanner tone="danger" title="Não foi possível concluir a ação" className="mb-4">
            {actionError instanceof Error ? actionError.message : "Tente novamente."}
          </StateBanner>
        )}

        {integration && (
          <div className="grid gap-6">
            {/* Header */}
            <div className="rounded-md border border-border bg-card p-6">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h1 className="text-2xl font-semibold">{integration.name}</h1>
                    <IntegrationStatusBadge integration={integration} />
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">{integration.description}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  {hasAction("SYNC") && (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => syncMutation.mutate()}
                      disabled={anyPending}
                    >
                      <RefreshCw className={cn("h-4 w-4", syncMutation.isPending && "animate-spin")} />
                      {syncMutation.isPending ? "Sincronizando" : "Sincronizar agora"}
                    </Button>
                  )}
                  {hasAction("TEST_CONNECTION") && (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => testConnectionMutation.mutate()}
                      disabled={anyPending}
                    >
                      <RefreshCw
                        className={cn("h-4 w-4", testConnectionMutation.isPending && "animate-spin")}
                      />
                      {testConnectionMutation.isPending
                        ? "Testando"
                        : connectionTested
                          ? "Conexão ok"
                          : "Testar conexão"}
                    </Button>
                  )}
                  {hasAction("REACTIVATE") && (
                    <Button
                      type="button"
                      size="sm"
                      onClick={() => reactivateMutation.mutate()}
                      disabled={anyPending}
                    >
                      <PlugZap className="h-4 w-4" />
                      {reactivateMutation.isPending ? "Reativando" : "Reativar"}
                    </Button>
                  )}
                  {hasAction("DISCONNECT") && (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => setConfirmDisconnect(true)}
                      disabled={anyPending}
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
                </div>
              </div>

              {/* Meta info */}
              <div className="mt-5 grid gap-3 text-sm md:grid-cols-3">
                <InfoItem label="Tipo" value={typeLabel(integration.type)} />
                <InfoItem label="Última atividade" value={formatDateTime(integration.lastSyncAt)} />
                <InfoItem label="Configurada em" value={formatDateTime(integration.configuredAt)} />
              </div>
            </div>

            {/* Error panel */}
            {integration.errorMessage && (
              <div className="rounded-md border border-danger/25 bg-danger/10 p-4 text-sm">
                <div className="flex items-start gap-2">
                  <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-danger" />
                  <div>
                    <div className="font-medium">Falha na integração</div>
                    <div className="mt-1 text-muted-foreground">{integration.errorMessage}</div>
                  </div>
                </div>
              </div>
            )}

            {/* Token panel */}
            <TokenPanel
              integration={integration}
              generatedToken={generatedToken}
              copied={copied}
              anyPending={anyPending}
              generating={generateTokenMutation.isPending}
              rotating={rotateTokenMutation.isPending}
              onGenerate={() => generateTokenMutation.mutate()}
              onRotate={() => rotateTokenMutation.mutate()}
              onRevoke={() => setConfirmRevokeToken(true)}
              onCopy={copyToken}
            />
          </div>
        )}
      </div>

      {/* Disconnect dialog */}
      <AlertDialog open={confirmDisconnect} onOpenChange={setConfirmDisconnect}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Desconectar integração</AlertDialogTitle>
            <AlertDialogDescription>
              A integração {integration?.name} será desativada para este cliente. O histórico de sincronizações e auditoria será preservado.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={disconnectMutation.isPending}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              disabled={disconnectMutation.isPending}
              onClick={(e) => {
                e.preventDefault();
                disconnectMutation.mutate();
              }}
            >
              {disconnectMutation.isPending ? "Desconectando" : "Desconectar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Revoke token dialog */}
      <AlertDialog open={confirmRevokeToken} onOpenChange={setConfirmRevokeToken}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Revogar token</AlertDialogTitle>
            <AlertDialogDescription>
              O token atual será invalidado imediatamente. A integração ficará desativada até que um novo token seja gerado.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={revokeTokenMutation.isPending}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              disabled={revokeTokenMutation.isPending}
              onClick={(e) => {
                e.preventDefault();
                revokeTokenMutation.mutate();
              }}
            >
              {revokeTokenMutation.isPending ? "Revogando" : "Revogar token"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AppShell>
  );
}

function TokenPanel({
  integration,
  generatedToken,
  copied,
  anyPending,
  generating,
  rotating,
  onGenerate,
  onRotate,
  onRevoke,
  onCopy,
}: {
  integration: IntegrationCenterItem;
  generatedToken: GenerateIntegrationTokenResponse | null;
  copied: boolean;
  anyPending: boolean;
  generating: boolean;
  rotating: boolean;
  onGenerate: () => void;
  onRotate: () => void;
  onRevoke: () => void;
  onCopy: (token: string) => void;
}) {
  const hasAction = (action: string) => integration.availableActions.includes(action as never);
  const showTokenPanel =
    hasAction("GENERATE_TOKEN") ||
    hasAction("VIEW") ||
    integration.tokenPreview != null;

  if (!showTokenPanel) return null;

  const hasToken = integration.tokenPreview != null || generatedToken != null;

  return (
    <div className="rounded-md border border-border bg-card p-6">
      <div className="flex items-center gap-2">
        <Key className="h-4 w-4 text-muted-foreground" />
        <h2 className="font-medium">Token de integração</h2>
      </div>
      <p className="mt-1 text-sm text-muted-foreground">
        Este token é usado pelos sistemas externos para autenticar com o Práxis. Guarde-o com segurança.
      </p>

      {/* Show generated token (one-time) */}
      {generatedToken && (
        <div className="mt-4 rounded-md border border-success/25 bg-success/10 p-4">
          <div className="mb-2 text-sm font-medium text-success">
            Token gerado — copie agora, não será exibido novamente.
          </div>
          <div className="flex items-center gap-2">
            <code className="flex-1 rounded bg-background px-3 py-2 font-mono text-xs break-all select-all">
              {generatedToken.token}
            </code>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => onCopy(generatedToken.token)}
            >
              <Copy className="h-4 w-4" />
              {copied ? "Copiado!" : "Copiar"}
            </Button>
          </div>
        </div>
      )}

      {/* Show masked preview when token exists but not just generated */}
      {!generatedToken && hasToken && integration.tokenPreview && (
        <div className="mt-4 flex items-center gap-2">
          <code className="rounded border border-border bg-muted px-3 py-2 font-mono text-sm text-muted-foreground">
            {integration.tokenPreview}••••••••••••••••••••
          </code>
        </div>
      )}

      {!hasToken && !generatedToken && (
        <p className="mt-3 text-sm text-muted-foreground">Nenhum token ativo. Gere um token para conectar sistemas externos.</p>
      )}

      {/* Actions */}
      <div className="mt-4 flex flex-wrap gap-2">
        {hasAction("GENERATE_TOKEN") && !hasToken && (
          <Button type="button" size="sm" onClick={onGenerate} disabled={anyPending}>
            <Key className="h-4 w-4" />
            {generating ? "Gerando" : "Gerar token"}
          </Button>
        )}
        {hasToken && (
          <Button type="button" size="sm" variant="outline" onClick={onRotate} disabled={anyPending}>
            <RotateCcw className="h-4 w-4" />
            {rotating ? "Rotacionando" : "Rotacionar token"}
          </Button>
        )}
        {hasToken && (
          <Button type="button" size="sm" variant="outline" onClick={onRevoke} disabled={anyPending}>
            <Trash2 className="h-4 w-4" />
            Revogar token
          </Button>
        )}
      </div>
    </div>
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

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 font-medium">{value}</div>
    </div>
  );
}

function typeLabel(type: string) {
  if (type === "ATS") return "ATS";
  if (type === "API") return "API personalizada";
  return type;
}

function formatDateTime(value: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
