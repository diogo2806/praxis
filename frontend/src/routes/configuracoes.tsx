import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Building2, Key, Link2, Settings, Shield, Users } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { getPrivacyCompliance, getTenantConfig } from "@/lib/api/praxis";
import { useSession } from "@/lib/session";
import { useLanguage } from "@/lib/language-context";

export const Route = createFileRoute("/configuracoes")({
  head: () => ({
    meta: [
      { title: "Configurações - Praxis" },
      {
        name: "description",
        content: "Configurações da empresa: dados, integrações, usuários e retenção.",
      },
    ],
  }),
  component: ConfiguracoesPage,
});

function ConfiguracoesPage() {
  const session = useSession();
  const { t } = useLanguage();

  const tenantConfigQuery = useQuery({
    queryKey: ["tenant-config"],
    queryFn: getTenantConfig,
  });
  const privacyQuery = useQuery({
    queryKey: ["privacy-compliance"],
    queryFn: getPrivacyCompliance,
  });

  const tenantConfig = tenantConfigQuery.data;
  const privacy = privacyQuery.data;

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-8">
          <div className="text-xs uppercase text-primary">Configurações</div>
          <h1 className="mt-1 text-3xl font-semibold">Perfil da empresa</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Dados da organização, integrações ativas, catálogos e políticas de retenção.
          </p>
        </div>

        <div className="space-y-6">
          <section className="rounded-xl border border-border bg-card">
            <div className="border-b border-border px-5 py-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Building2 className="h-4 w-4 text-primary" />
                Dados da empresa
              </div>
            </div>
            <div className="grid gap-4 p-5 md:grid-cols-2">
              <div>
                <div className="text-xs uppercase text-muted-foreground">{t.common.workspace}</div>
                <div className="mt-1 text-sm font-medium">{session.workspaceName}</div>
              </div>
              <div>
                <div className="text-xs uppercase text-muted-foreground">Tenant ID</div>
                <div className="mt-1 font-mono text-sm text-muted-foreground">
                  {session.tenantId ?? "—"}
                </div>
              </div>
              <div>
                <div className="text-xs uppercase text-muted-foreground">Usuário ativo</div>
                <div className="mt-1 text-sm font-medium">{session.userName}</div>
              </div>
              <div>
                <div className="text-xs uppercase text-muted-foreground">Papel</div>
                <div className="mt-1 text-sm">{session.userRole}</div>
              </div>
            </div>
          </section>

          <section className="rounded-xl border border-border bg-card">
            <div className="border-b border-border px-5 py-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Key className="h-4 w-4 text-primary" />
                Integrações
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                Tokens e conexões externas configuradas para este tenant.
              </p>
            </div>
            <div className="p-5">
              <div className="rounded-md border border-border bg-background p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">Gupy</div>
                    <div className="mt-0.5 text-xs text-muted-foreground">
                      Token de integração configurado via{" "}
                      <code className="rounded bg-muted px-1 py-0.5 text-[11px]">
                        tenants.integration_token_hash
                      </code>
                    </div>
                  </div>
                  <span className="rounded-md bg-success/15 px-2 py-0.5 text-xs font-medium text-success">
                    Configurado
                  </span>
                </div>
              </div>
              <div className="mt-3 text-xs text-muted-foreground">
                Para renovar o token, gere um novo hash SHA-256 Base64URL e atualize o banco.
                Consulte o README para instruções.
              </div>
            </div>
          </section>

          <section className="rounded-xl border border-border bg-card">
            <div className="border-b border-border px-5 py-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Settings className="h-4 w-4 text-primary" />
                Catálogos configuráveis
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                Catálogos usados na criação de testes. Gerencie em telas dedicadas.
              </p>
            </div>
            <div className="divide-y divide-border">
              {tenantConfigQuery.isLoading ? (
                <div className="p-5 text-sm text-muted-foreground">Carregando catálogos...</div>
              ) : tenantConfigQuery.isError ? (
                <div className="p-5">
                  <StateBanner tone="danger" title="Erro ao carregar catálogos">
                    {tenantConfigQuery.error instanceof Error
                      ? tenantConfigQuery.error.message
                      : "Tente novamente."}
                  </StateBanner>
                </div>
              ) : tenantConfig ? (
                <>
                  <CatalogRow
                    label="Competências"
                    count={tenantConfig.competencies.length}
                    linkTo="/nova/competencias"
                  />
                  <CatalogRow
                    label="Níveis de senioridade"
                    count={tenantConfig.seniorityLevels.length}
                  />
                  <CatalogRow
                    label="Usos do resultado"
                    count={tenantConfig.resultUses.length}
                  />
                  <CatalogRow
                    label="Tempos de resposta"
                    count={tenantConfig.answerTimeLimits.length}
                  />
                </>
              ) : null}
            </div>
          </section>

          <section className="rounded-xl border border-border bg-card">
            <div className="border-b border-border px-5 py-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Shield className="h-4 w-4 text-primary" />
                Retenção e LGPD
              </div>
            </div>
            <div className="p-5">
              {privacyQuery.isLoading ? (
                <div className="text-sm text-muted-foreground">Carregando...</div>
              ) : privacyQuery.isError ? (
                <StateBanner tone="danger" title="Erro">
                  {privacyQuery.error instanceof Error
                    ? privacyQuery.error.message
                    : "Tente novamente."}
                </StateBanner>
              ) : privacy ? (
                <div className="grid gap-4 md:grid-cols-3">
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">Retenção</div>
                    <div className="mt-1 text-lg font-semibold tabular-nums">
                      {privacy.retentionDays} dias
                    </div>
                  </div>
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">Canal de revisão</div>
                    <div className="mt-1 text-sm">{privacy.reviewChannel}</div>
                  </div>
                  <div>
                    <div className="text-xs uppercase text-muted-foreground">SLA</div>
                    <div className="mt-1 text-sm">{privacy.reviewSla}</div>
                  </div>
                </div>
              ) : null}
              <div className="mt-4">
                <Link
                  to="/lgpd"
                  className="inline-flex items-center gap-1.5 text-xs text-primary hover:underline"
                >
                  <Link2 className="h-3 w-3" />
                  Ver detalhes completos de LGPD
                </Link>
              </div>
            </div>
          </section>

          <section className="rounded-xl border border-border bg-card">
            <div className="border-b border-border px-5 py-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <Users className="h-4 w-4 text-primary" />
                Usuários e permissões
              </div>
            </div>
            <div className="p-5">
              <div className="rounded-md border border-border bg-muted/40 p-4 text-sm text-muted-foreground">
                O gerenciamento de usuários e papéis (quem pode publicar, quem é viewer) ainda não
                está implementado. Hoje a segurança opera com JWT e role{" "}
                <code className="rounded bg-muted px-1 py-0.5 text-[11px]">EMPRESA</code>{" "}
                para todas as rotas internas.
              </div>
            </div>
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function CatalogRow({
  label,
  count,
  linkTo,
}: {
  label: string;
  count: number;
  linkTo?: string;
}) {
  return (
    <div className="flex items-center justify-between px-5 py-3">
      <div>
        <div className="text-sm font-medium">{label}</div>
        <div className="text-xs text-muted-foreground">{count} itens cadastrados</div>
      </div>
      {linkTo ? (
        <Link
          to={linkTo}
          className="rounded-md border border-border bg-background px-3 py-1.5 text-xs hover:bg-accent"
        >
          Gerenciar
        </Link>
      ) : (
        <span className="text-xs text-muted-foreground">Via API</span>
      )}
    </div>
  );
}
