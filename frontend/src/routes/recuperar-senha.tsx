import { useMutation } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { KeyRound } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PraxisApiError, requestPasswordReset } from "@/lib/api/praxis";

export const Route = createFileRoute("/recuperar-senha")({
  head: () => ({
    meta: [
      { title: "Recuperar senha - Praxis" },
      {
        name: "description",
        content: "Receba um link temporário para redefinir sua senha de acesso ao Praxis.",
      },
    ],
  }),
  component: ForgotPasswordPage,
});

function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [empresaId, setEmpresaId] = useState("");

  const canSubmit = email.trim().length > 0;

  const mutation = useMutation({
    mutationFn: () =>
      requestPasswordReset({
        email: email.trim(),
        // Campo opcional: ADMIN deixa em branco (empresa PLATFORM).
        empresaId: empresaId.trim() === "" ? undefined : empresaId.trim(),
      }),
  });

  // Mensagem uniforme: o backend nunca revela se a conta existe.
  const uniformMessage =
    mutation.data?.message ??
    "Se existir uma conta correspondente, enviaremos um e-mail com as instruções para redefinir sua senha.";

  const errorMessage =
    mutation.error instanceof PraxisApiError
      ? mutation.error.message
      : mutation.isError
        ? "Não foi possível processar a solicitação. Tente novamente."
        : null;

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900">
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] max-w-md items-center">
        <section className="w-full rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-semibold">Esqueci minha senha</h1>
              <p className="text-sm text-slate-500">
                Informe seu e-mail para receber o link de redefinição.
              </p>
            </div>
          </div>

          {mutation.isSuccess ? (
            <div className="mt-6 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-3 text-sm text-emerald-700">
              {uniformMessage}
            </div>
          ) : (
            <form
              className="mt-6 space-y-4"
              onSubmit={(event) => {
                event.preventDefault();
                if (canSubmit) mutation.mutate();
              }}
            >
              <div className="space-y-2">
                <Label htmlFor="email">E-mail</Label>
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  disabled={mutation.isPending}
                  autoFocus
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="empresaId">Empresa (opcional)</Label>
                <Input
                  id="empresaId"
                  type="text"
                  autoComplete="organization"
                  value={empresaId}
                  onChange={(event) => setEmpresaId(event.target.value)}
                  disabled={mutation.isPending}
                />
                <p className="text-xs text-slate-500">
                  Usuários de empresa informam o identificador da empresa. Operadores da
                  plataforma podem deixar em branco.
                </p>
              </div>

              {errorMessage && (
                <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                  {errorMessage}
                </div>
              )}

              <Button type="submit" className="w-full" disabled={!canSubmit || mutation.isPending}>
                {mutation.isPending ? "Enviando..." : "Enviar link de redefinição"}
              </Button>
            </form>
          )}

          <p className="mt-5 text-center text-xs text-slate-500">
            Lembrou a senha?{" "}
            <Link to="/" className="font-medium text-primary hover:underline">
              Voltar para a entrada
            </Link>
          </p>
        </section>
      </div>
    </main>
  );
}
