import { useMutation } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ArrowRight, Building2, ShieldCheck } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { discoverEnterpriseAccess, startEnterpriseLogin } from "@/lib/api/enterprise-auth";

export const Route = createFileRoute("/sso")({
  validateSearch: (search: Record<string, unknown>) => ({
    email: typeof search.email === "string" ? search.email : "",
    returnUrl: typeof search.returnUrl === "string" ? search.returnUrl : "/dashboard",
  }),
  head: () => ({ meta: [{ title: "Acesso corporativo - Práxis" }] }),
  component: EnterpriseSsoPage,
});

function EnterpriseSsoPage() {
  const search = Route.useSearch();
  const [email, setEmail] = useState(search.email);
  const [message, setMessage] = useState<string>();

  const login = useMutation({
    mutationFn: async () => {
      const discovery = await discoverEnterpriseAccess(email);
      if (!discovery.ssoAvailable || !discovery.providerId) {
        throw new Error(discovery.message);
      }
      const callbackUrl = new URL("/sso/callback", window.location.origin);
      callbackUrl.searchParams.set("returnUrl", safeReturnUrl(search.returnUrl));
      const started = await startEnterpriseLogin(discovery.providerId, {
        email,
        returnUri: callbackUrl.toString(),
      });
      return { discovery, started };
    },
    onSuccess: ({ discovery, started }) => {
      setMessage(discovery.mfaRequired
        ? "Você será direcionado ao provedor corporativo. Conclua a autenticação multifator."
        : "Você será direcionado ao provedor corporativo.");
      window.location.assign(started.authorizationUrl);
    },
  });

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
      <section className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl sm:p-8">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary"><Building2 className="h-6 w-6" /></div>
        <h1 className="mt-5 text-2xl font-semibold">Acesso corporativo</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">Use seu e-mail profissional para localizar o provedor OpenID Connect configurado pela empresa.</p>
        <label className="mt-6 block text-sm font-medium">E-mail corporativo<input className="input mt-1" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
        {message && <p className="mt-4 rounded-lg border border-border bg-background p-3 text-sm">{message}</p>}
        {login.isError && <p role="alert" className="mt-4 rounded-lg border border-danger/30 bg-danger/10 p-3 text-sm">{login.error instanceof Error ? login.error.message : "Não foi possível iniciar o acesso corporativo."}</p>}
        <Button className="mt-5 w-full" disabled={!email || login.isPending} onClick={() => login.mutate()}>{login.isPending ? "Verificando..." : "Continuar com SSO"}<ArrowRight className="ml-2 h-4 w-4" /></Button>
        <div className="mt-5 flex items-start gap-2 text-xs leading-5 text-muted-foreground"><ShieldCheck className="mt-0.5 h-4 w-4 shrink-0" />O Práxis valida assinatura, issuer, audience, nonce, domínio e evidência de MFA antes de criar a sessão.</div>
      </section>
    </main>
  );
}

function safeReturnUrl(value: string) {
  return value.startsWith("/") && !value.startsWith("//") ? value : "/dashboard";
}
