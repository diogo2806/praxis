import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { ArrowRight, Check, Copy, KeyRound, RefreshCw, Trash2, Webhook } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { SkeletonRows, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  deleteIntegrationToken,
  listIntegrationTokens,
  rotateIntegrationToken,
  type IntegrationProvider,
  type IntegrationTokenResponse,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/configuracoes/integracoes")({
  head: () => ({
    meta: [
      { title: "Integrações - Práxis" },
      {
        name: "description",
        content: "Configuração de tokens para integrações externas.",
      },
    ],
  }),
  component: IntegrationsPage,
});

const providerLabels: Record<IntegrationProvider, string> = {
  gupy: "Gupy",
  recrutei: "Recrutei",
};

function IntegrationsPage() {
  const queryClient = useQueryClient();
  const [visibleTokens, setVisibleTokens] = useState<Partial<Record<IntegrationProvider, string>>>({});
  const [copiedProvider, setCopiedProvider] = useState<IntegrationProvider | null>(null);
  const copyTimerRef = useRef<ReturnType<typeof setTimeout>>(null);

  useEffect(() => {
    return () => {
      if (copyTimerRef.current) clearTimeout(copyTimerRef.current);
    };
  }, []);

  const tokensQuery = useQuery({
    queryKey: ["integration-tokens"],
    queryFn: listIntegrationTokens,
  });

  const rotateMutation = useMutation({
    mutationFn: rotateIntegrationToken,
    onSuccess: async (response) => {
      setVisibleTokens((current) => ({ ...current, [response.provider]: response.token }));
      await queryClient.invalidateQueries({ queryKey: ["integration-tokens"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteIntegrationToken,
    onSuccess: async (_, provider) => {
      setVisibleTokens((current) => {
        const next = { ...current };
        delete next[provider];
        return next;
      });
      await queryClient.invalidateQueries({ queryKey: ["integration-tokens"] });
    },
  });

  const copyToken = async (provider: IntegrationProvider, token: string) => {
    await navigator.clipboard.writeText(token);
    setCopiedProvider(provider);
    if (copyTimerRef.current) clearTimeout(copyTimerRef.current);
    copyTimerRef.current = setTimeout(() => setCopiedProvider(null), 2000);
  };

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <div className="text-xs uppercase text-primary">Configurações</div>
          <h1 className="mt-1 text-3xl font-semibold">Integrações</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Tokens usados por sistemas externos para listar avaliações, criar participações e receber resultados.
          </p>
        </div>

        <section className="rounded-md border border-border bg-card">
          <div className="border-b border-border px-5 py-4">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <KeyRound className="h-4 w-4 text-primary" />
              Tokens de integração
            </div>
          </div>
          <div className="space-y-4 p-5">
            {tokensQuery.isLoading && <SkeletonRows rows={2} />}
            {tokensQuery.isError && (
              <StateBanner tone="danger" title="Não foi possível carregar integrações">
                {tokensQuery.error instanceof Error ? tokensQuery.error.message : "Tente novamente."}
              </StateBanner>
            )}
            {(rotateMutation.isError || deleteMutation.isError) && (
              <StateBanner tone="danger" title="Não foi possível salvar a integração">
                {(rotateMutation.error ?? deleteMutation.error) instanceof Error
                  ? (rotateMutation.error ?? deleteMutation.error)?.message
                  : "Tente novamente."}
              </StateBanner>
            )}
            {(tokensQuery.data ?? []).map((token) => (
              <IntegrationRow
                key={token.provider}
                token={token}
                visibleToken={visibleTokens[token.provider]}
                copied={copiedProvider === token.provider}
                pending={
                  (rotateMutation.isPending && rotateMutation.variables === token.provider) ||
                  (deleteMutation.isPending && deleteMutation.variables === token.provider)
                }
                onRotate={() => rotateMutation.mutate(token.provider)}
                onDelete={() => deleteMutation.mutate(token.provider)}
                onCopy={(value) => void copyToken(token.provider, value)}
              />
            ))}
          </div>
        </section>

        <section className="mt-6 rounded-md border border-border bg-card">
          <div className="border-b border-border px-5 py-4">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <Webhook className="h-4 w-4 text-primary" />
              API / Webhook personalizado
            </div>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 p-5">
            <p className="max-w-xl text-sm text-muted-foreground">
              Tem outro ATS ou sistema interno? Configure um webhook assinado (HMAC) para receber os
              resultados e gere um token para consultar a nossa API.
            </p>
            <Link
              to="/configuracoes/api"
              className="inline-flex items-center gap-1.5 rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-accent"
            >
              Abrir API e Webhooks
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </section>
      </div>
    </AppShell>
  );
}

function IntegrationRow({
  token,
  visibleToken,
  copied,
  pending,
  onRotate,
  onDelete,
  onCopy,
}: {
  token: IntegrationTokenResponse;
  visibleToken?: string;
  copied: boolean;
  pending: boolean;
  onRotate: () => void;
  onDelete: () => void;
  onCopy: (token: string) => void;
}) {
  return (
    <div className="rounded-md border border-border bg-background p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-semibold">{providerLabels[token.provider]}</h3>
            <span
              className={`rounded-md px-2 py-0.5 text-xs font-medium ${
                token.configured ? "bg-success/15 text-success" : "bg-muted text-muted-foreground"
              }`}
            >
              {token.configured ? "Configurado" : "Não configurado"}
            </span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {token.createdAt ? `Criado em ${formatDateTime(token.createdAt)}` : "Sem token ativo."}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" size="sm" variant="outline" onClick={onRotate} disabled={pending}>
            <RefreshCw className="h-4 w-4" />
            {token.configured ? "Renovar" : "Gerar token"}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={onDelete}
            disabled={pending || !token.configured}
          >
            <Trash2 className="h-4 w-4" />
            Revogar
          </Button>
        </div>
      </div>
      {visibleToken && (
        <div className="mt-4 rounded-md border border-warning/35 bg-warning/10 p-3">
          <div className="text-xs font-semibold text-warning-foreground">
            Copie agora. Este token não será exibido novamente.
          </div>
          <div className="mt-2 grid gap-2 md:grid-cols-[1fr_auto]">
            <Input readOnly value={visibleToken} className="font-mono text-xs" />
            <Button type="button" variant="outline" onClick={() => onCopy(visibleToken)}>
              {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
              {copied ? "Copiado" : "Copiar"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
