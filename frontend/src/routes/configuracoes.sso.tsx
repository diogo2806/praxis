import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { KeyRound, ShieldCheck, TestTube2 } from "lucide-react";
import { useState, type ReactNode } from "react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  createIdentityProvider,
  listEnterpriseAuthAuditEvents,
  listIdentityProviders,
  testIdentityProvider,
  updateIdentityProvider,
  type IdentityProvider,
  type IdentityProviderInput,
} from "@/lib/api/enterprise-auth";

export const Route = createFileRoute("/configuracoes/sso")({
  head: () => ({ meta: [{ title: "SSO corporativo e MFA - Práxis" }] }),
  component: EnterpriseAuthSettingsPage,
});

const EMPTY_PROVIDER: IdentityProviderInput = {
  displayName: "",
  protocol: "OIDC",
  issuerUri: "",
  clientId: "",
  clientSecretEnvVar: "",
  redirectUri: "",
  frontendSuccessUri: "",
  allowedEmailDomains: [],
  scopes: "openid profile email",
  defaultRole: "RESULTS_ANALYST",
  jitProvisioningEnabled: false,
  enforceSso: false,
  requireMfa: true,
  acceptedMfaAmrValues: ["mfa", "otp", "hwk", "sms"],
  active: false,
};

function EnterpriseAuthSettingsPage() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string>();
  const [form, setForm] = useState<IdentityProviderInput>({ ...EMPTY_PROVIDER });
  const [domains, setDomains] = useState("");
  const [amrValues, setAmrValues] = useState("mfa,otp,hwk,sms");
  const [testMessage, setTestMessage] = useState<string>();

  const providers = useQuery({ queryKey: ["enterprise-identity-providers"], queryFn: listIdentityProviders });
  const audit = useQuery({ queryKey: ["enterprise-auth-audit"], queryFn: () => listEnterpriseAuthAuditEvents(50) });

  const save = useMutation({
    mutationFn: () => {
      const input: IdentityProviderInput = {
        ...form,
        allowedEmailDomains: domains.split(",").map((item) => item.trim()).filter(Boolean),
        acceptedMfaAmrValues: amrValues.split(",").map((item) => item.trim()).filter(Boolean),
      };
      return selectedId ? updateIdentityProvider(selectedId, input) : createIdentityProvider(input);
    },
    onSuccess: (provider) => {
      selectProvider(provider);
      void queryClient.invalidateQueries({ queryKey: ["enterprise-identity-providers"] });
      void queryClient.invalidateQueries({ queryKey: ["enterprise-auth-audit"] });
    },
  });

  const test = useMutation({
    mutationFn: () => testIdentityProvider(selectedId!),
    onSuccess: (response) => {
      setTestMessage(response.message);
      void queryClient.invalidateQueries({ queryKey: ["enterprise-identity-providers"] });
      void queryClient.invalidateQueries({ queryKey: ["enterprise-auth-audit"] });
    },
  });

  const error = providers.error ?? audit.error ?? save.error ?? test.error;

  function selectProvider(provider: IdentityProvider) {
    setSelectedId(provider.id);
    setForm({
      displayName: provider.displayName,
      protocol: provider.protocol,
      issuerUri: provider.issuerUri,
      clientId: provider.clientId,
      clientSecretEnvVar: provider.clientSecretEnvVar,
      redirectUri: provider.redirectUri,
      frontendSuccessUri: provider.frontendSuccessUri,
      allowedEmailDomains: provider.allowedEmailDomains,
      scopes: provider.scopes,
      defaultRole: provider.defaultRole,
      jitProvisioningEnabled: provider.jitProvisioningEnabled,
      enforceSso: provider.enforceSso,
      requireMfa: provider.requireMfa,
      acceptedMfaAmrValues: provider.acceptedMfaAmrValues,
      active: provider.active,
    });
    setDomains(provider.allowedEmailDomains.join(","));
    setAmrValues(provider.acceptedMfaAmrValues.join(","));
    setTestMessage(undefined);
  }

  function newProvider() {
    setSelectedId(undefined);
    setForm({ ...EMPTY_PROVIDER });
    setDomains("");
    setAmrValues("mfa,otp,hwk,sms");
    setTestMessage(undefined);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header>
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Identidade corporativa</div>
          <h1 className="mt-1 font-display text-3xl">SSO corporativo e MFA</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
            Configure OpenID Connect por empresa, exija MFA no provedor e bloqueie senha local para domínios corporativos. Segredos permanecem em variáveis de ambiente.
          </p>
        </header>

        {error && <StateBanner tone="danger" title="Não foi possível concluir a operação">{error instanceof Error ? error.message : String(error)}</StateBanner>}
        {testMessage && <StateBanner tone={test.data?.success ? "success" : "danger"} title="Teste do provedor">{testMessage}</StateBanner>}

        <section className="grid gap-6 xl:grid-cols-[20rem_minmax(0,1fr)]">
          <Card title="Provedores" icon={<KeyRound className="h-5 w-5 text-primary" />}>
            <Button className="mb-4 w-full" variant="outline" onClick={newProvider}>Novo provedor</Button>
            <div className="space-y-2">
              {(providers.data ?? []).map((provider) => (
                <button key={provider.id} type="button" onClick={() => selectProvider(provider)} className={`w-full rounded-lg border p-3 text-left ${selectedId === provider.id ? "border-primary bg-primary/10" : "border-border bg-background hover:bg-accent"}`}>
                  <div className="font-semibold">{provider.displayName}</div>
                  <div className="mt-1 text-xs text-muted-foreground">{provider.active ? "Ativo" : "Inativo"} · {provider.enforceSso ? "SSO obrigatório" : "SSO opcional"}</div>
                  {provider.lastTestStatus && <div className={`mt-2 text-xs ${provider.lastTestStatus === "SUCCESS" ? "text-emerald-700" : "text-red-700"}`}>{provider.lastTestStatus}: {provider.lastTestMessage}</div>}
                </button>
              ))}
            </div>
          </Card>

          <Card title={selectedId ? "Editar provedor" : "Novo provedor"} icon={<ShieldCheck className="h-5 w-5 text-primary" />}>
            <div className="grid gap-3 md:grid-cols-2">
              <Field label="Nome"><input className="input" value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></Field>
              <Field label="Protocolo"><select className="input" value={form.protocol} disabled><option value="OIDC">OpenID Connect</option></select></Field>
              <Field label="Issuer HTTPS"><input className="input" value={form.issuerUri} onChange={(event) => setForm({ ...form, issuerUri: event.target.value })} /></Field>
              <Field label="Client ID"><input className="input" value={form.clientId} onChange={(event) => setForm({ ...form, clientId: event.target.value })} /></Field>
              <Field label="Variável do client secret"><input className="input" value={form.clientSecretEnvVar} onChange={(event) => setForm({ ...form, clientSecretEnvVar: event.target.value.toUpperCase() })} placeholder="AZURE_PRAXIS_CLIENT_SECRET" /></Field>
              <Field label="Redirect URI"><input className="input" value={form.redirectUri} onChange={(event) => setForm({ ...form, redirectUri: event.target.value })} /></Field>
              <Field label="Destino após autenticação"><input className="input" value={form.frontendSuccessUri} onChange={(event) => setForm({ ...form, frontendSuccessUri: event.target.value })} /></Field>
              <Field label="Domínios autorizados"><input className="input" value={domains} onChange={(event) => setDomains(event.target.value)} placeholder="empresa.com.br,grupo.com" /></Field>
              <Field label="Escopos"><input className="input" value={form.scopes} onChange={(event) => setForm({ ...form, scopes: event.target.value })} /></Field>
              <Field label="Perfil padrão"><select className="input" value={form.defaultRole} onChange={(event) => setForm({ ...form, defaultRole: event.target.value })}><option value="TEAM_MANAGER">Gestor da empresa</option><option value="PARTNER_MANAGER">Gestor de parceiros</option><option value="ASSESSMENT_EDITOR">Editor de avaliações</option><option value="RESULTS_ANALYST">Analista de resultados</option><option value="OPERATIONS_MANAGER">Gestor operacional</option></select></Field>
              <Field label="Valores AMR aceitos"><input className="input" value={amrValues} onChange={(event) => setAmrValues(event.target.value)} /></Field>
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <Check label="Provisionamento automático no primeiro acesso" checked={form.jitProvisioningEnabled} onChange={(checked) => setForm({ ...form, jitProvisioningEnabled: checked })} />
              <Check label="Bloquear senha local para os domínios" checked={form.enforceSso} onChange={(checked) => setForm({ ...form, enforceSso: checked })} />
              <Check label="Exigir MFA comprovada pelo claim AMR" checked={form.requireMfa} onChange={(checked) => setForm({ ...form, requireMfa: checked })} />
              <Check label="Provedor ativo" checked={form.active} onChange={(checked) => setForm({ ...form, active: checked })} />
            </div>
            <div className="mt-5 flex flex-wrap gap-2">
              <Button disabled={save.isPending || !form.displayName || !form.issuerUri || !form.clientId || !form.clientSecretEnvVar || !domains} onClick={() => save.mutate()}>Salvar configuração</Button>
              <Button variant="outline" disabled={!selectedId || test.isPending} onClick={() => test.mutate()}><TestTube2 className="mr-2 h-4 w-4" />Testar discovery e segredo</Button>
            </div>
          </Card>
        </section>

        <Card title="Auditoria de autenticação" icon={<ShieldCheck className="h-5 w-5 text-primary" />}>
          <div className="overflow-auto rounded-lg border border-border">
            <table className="w-full min-w-[760px] text-left text-sm"><thead className="bg-muted/50"><tr><th className="p-3">Data</th><th className="p-3">Evento</th><th className="p-3">Resultado</th><th className="p-3">Identidade</th><th className="p-3">Detalhe</th></tr></thead><tbody>{(audit.data ?? []).map((event) => <tr key={event.id} className="border-t border-border"><td className="p-3">{new Date(event.occurredAt).toLocaleString("pt-BR")}</td><td className="p-3">{event.eventType}</td><td className="p-3">{event.outcome}</td><td className="p-3">{event.actorIdentifier ?? "—"}</td><td className="p-3">{event.detail ?? "—"}</td></tr>)}</tbody></table>
          </div>
        </Card>
      </main>
    </AppShell>
  );
}

function Card({ title, icon, children }: { title: string; icon: ReactNode; children: ReactNode }) { return <section className="rounded-xl border border-border bg-card p-5"><div className="mb-4 flex items-center gap-2">{icon}<h2 className="font-semibold">{title}</h2></div>{children}</section>; }
function Field({ label, children }: { label: string; children: ReactNode }) { return <label className="block text-sm font-medium"><span className="mb-1 block">{label}</span>{children}</label>; }
function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) { return <label className="flex items-start gap-2 rounded-lg border border-border p-3 text-sm"><input className="mt-1" type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} /><span>{label}</span></label>; }
