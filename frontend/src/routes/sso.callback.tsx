import { createFileRoute } from "@tanstack/react-router";
import { CheckCircle2, CircleAlert } from "lucide-react";
import { useEffect, useState } from "react";

import * as sessionModule from "@/lib/session";

export const Route = createFileRoute("/sso/callback")({
  validateSearch: (search: Record<string, unknown>) => ({
    returnUrl: typeof search.returnUrl === "string" ? search.returnUrl : "/dashboard",
  }),
  head: () => ({ meta: [{ title: "Concluindo acesso corporativo - Práxis" }] }),
  component: EnterpriseSsoCallbackPage,
});

function EnterpriseSsoCallbackPage() {
  const { returnUrl } = Route.useSearch();
  const [error, setError] = useState<string>();

  useEffect(() => {
    try {
      const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ""));
      const token = fragment.get("enterprise_token");
      const email = fragment.get("email") ?? "";
      const name = fragment.get("name") ?? email;
      const empresaId = fragment.get("empresa_id") ?? "";
      const roles = (fragment.get("roles") ?? "").split(",").filter(Boolean);
      const mfaVerified = fragment.get("mfa") === "true";
      if (!token || !email || !empresaId || roles.length === 0) {
        throw new Error("O retorno do provedor não contém os dados necessários para criar a sessão.");
      }
      persistSession({ token, email, name, empresaId, roles, mfaVerified });
      window.history.replaceState(null, "", window.location.pathname + window.location.search);
      window.location.replace(safeReturnUrl(returnUrl));
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Não foi possível criar a sessão corporativa.");
    }
  }, [returnUrl]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 text-foreground">
      <section className="w-full max-w-md rounded-2xl border border-border bg-card p-8 text-center shadow-xl">
        {error ? <><CircleAlert className="mx-auto h-10 w-10 text-danger" /><h1 className="mt-4 text-xl font-semibold">Falha no acesso corporativo</h1><p role="alert" className="mt-3 text-sm text-muted-foreground">{error}</p><a className="mt-5 inline-flex rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground" href="/sso">Tentar novamente</a></> : <><CheckCircle2 className="mx-auto h-10 w-10 text-emerald-600" /><h1 className="mt-4 text-xl font-semibold">Autenticação validada</h1><p className="mt-3 text-sm text-muted-foreground">A sessão está sendo preparada.</p></>}
      </section>
    </main>
  );
}

function persistSession(input: {
  token: string;
  email: string;
  name: string;
  empresaId: string;
  roles: string[];
  mfaVerified: boolean;
}) {
  const profile = input.roles.includes("ADMIN")
    ? "ADMIN"
    : input.roles.includes("PARTNER_SPECIALIST")
      ? "PARTNER_SPECIALIST"
      : "CLIENT_MANAGER";
  const payload = {
    token: input.token,
    profile,
    roles: input.roles,
    email: input.email,
    name: input.name,
    displayName: input.name,
    empresaId: input.empresaId,
    mfaVerified: input.mfaVerified,
  };
  const dynamicModule = sessionModule as unknown as Record<string, unknown>;
  for (const name of ["setSession", "saveSession", "persistSession", "storeSession"]) {
    const candidate = dynamicModule[name];
    if (typeof candidate === "function") {
      try {
        const callable = candidate as (...args: unknown[]) => unknown;
        if (callable.length >= 2) callable(input.token, payload);
        else callable(payload);
        return;
      } catch {
        // Tenta o próximo contrato de compatibilidade.
      }
    }
  }
  for (const key of ["praxis-session", "praxis.auth.session", "praxis.session"]) {
    window.localStorage.setItem(key, JSON.stringify(payload));
  }
}

function safeReturnUrl(value: string) {
  return value.startsWith("/") && !value.startsWith("//") ? value : "/dashboard";
}
