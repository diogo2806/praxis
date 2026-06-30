import { useMutation, useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { KeyRound } from "lucide-react";
import { type ReactNode, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  PraxisApiError,
  resetPassword,
  validatePasswordResetToken,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/reset-password/$token")({
  head: () => ({
    meta: [
      { title: "Redefinir senha - Praxis" },
      {
        name: "description",
        content: "Defina uma nova senha de acesso ao Praxis.",
      },
    ],
  }),
  component: ResetPasswordPage,
});

function ResetPasswordPage() {
  const { token } = Route.useParams();
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const tokenQuery = useQuery({
    queryKey: ["password-reset-token", token],
    queryFn: () => validatePasswordResetToken(token),
    retry: false,
  });

  const passwordsMatch =
    newPassword.length === 0 || confirmPassword.length === 0 || newPassword === confirmPassword;

  const canSubmit =
    token.trim().length > 0 &&
    newPassword.length >= 8 &&
    confirmPassword.length >= 8 &&
    newPassword === confirmPassword;

  const mutation = useMutation({
    mutationFn: () => resetPassword({ token, newPassword, confirmPassword }),
    onSuccess: async () => {
      await navigate({ to: "/" });
    },
  });

  if (tokenQuery.isLoading) {
    return (
      <CenteredCard title="Redefinir senha" subtitle="Validando o link...">
        <p className="mt-6 text-sm text-slate-500">Aguarde um instante.</p>
      </CenteredCard>
    );
  }

  if (tokenQuery.isError) {
    const expired =
      tokenQuery.error instanceof PraxisApiError && tokenQuery.error.status === 410;
    return (
      <CenteredCard
        title="Link indisponível"
        subtitle={
          expired
            ? "Este link de redefinição expirou."
            : "Este link de redefinição é inválido ou já foi utilizado."
        }
      >
        <div className="mt-6 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          Solicite um novo link na página de recuperação de senha.
        </div>
        <p className="mt-5 text-center text-xs text-slate-500">
          <Link to="/recuperar-senha" className="font-medium text-primary hover:underline">
            Solicitar novo link
          </Link>
        </p>
      </CenteredCard>
    );
  }

  const errorMessage =
    mutation.error instanceof PraxisApiError
      ? mutation.error.message
      : mutation.isError
        ? "Não foi possível redefinir a senha."
        : null;

  return (
    <CenteredCard
      title="Redefinir senha"
      subtitle={
        tokenQuery.data?.userName
          ? `Olá, ${tokenQuery.data.userName}. Defina sua nova senha.`
          : "Defina sua nova senha."
      }
    >
      <form
        className="mt-6 space-y-4"
        onSubmit={(event) => {
          event.preventDefault();
          if (canSubmit) mutation.mutate();
        }}
      >
        <div className="space-y-2">
          <Label htmlFor="newPassword">Nova senha</Label>
          <Input
            id="newPassword"
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            disabled={mutation.isPending}
            autoFocus
          />
          <p className="text-xs text-slate-500">Use pelo menos 8 caracteres.</p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Confirmar senha</Label>
          <Input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            disabled={mutation.isPending}
          />
          {!passwordsMatch && <p className="text-xs text-rose-600">As senhas não conferem.</p>}
        </div>

        {errorMessage && (
          <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {errorMessage}
          </div>
        )}

        <Button type="submit" className="w-full" disabled={!canSubmit || mutation.isPending}>
          {mutation.isPending ? "Redefinindo..." : "Redefinir senha"}
        </Button>
      </form>

      <p className="mt-5 text-center text-xs text-slate-500">
        Lembrou a senha?{" "}
        <Link to="/" className="font-medium text-primary hover:underline">
          Voltar para a entrada
        </Link>
      </p>
    </CenteredCard>
  );
}

function CenteredCard({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900">
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] max-w-md items-center">
        <section className="w-full rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-semibold">{title}</h1>
              <p className="text-sm text-slate-500">{subtitle}</p>
            </div>
          </div>
          {children}
        </section>
      </div>
    </main>
  );
}
