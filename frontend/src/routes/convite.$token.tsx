import { useMutation } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { KeyRound } from "lucide-react";
import { useState } from "react";

import { LanguageSelector } from "@/components/language-selector";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { acceptInvite, PraxisApiError } from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";
import { defaultAuthenticatedRoute, saveAuthenticatedSession } from "@/lib/session";

export const Route = createFileRoute("/convite/$token")({
  head: () => ({
    meta: [
      { title: "Ativar acesso - Práxis" },
      {
        name: "description",
        content: "Defina sua senha para ativar o acesso ao Praxis.",
      },
    ],
  }),
  component: InviteAcceptPage,
});

function InviteAcceptPage() {
  const { t } = useLanguage();
  const { token } = Route.useParams();
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const passwordsMatch =
    newPassword.length === 0 || confirmPassword.length === 0 || newPassword === confirmPassword;

  const canSubmit =
    token.trim().length > 0 &&
    newPassword.length >= 8 &&
    confirmPassword.length >= 8 &&
    newPassword === confirmPassword;

  const mutation = useMutation({
    mutationFn: () =>
      acceptInvite({
        token,
        newPassword,
        confirmPassword,
      }),
    onSuccess: async (response) => {
      saveAuthenticatedSession(response);
      await navigate({ to: defaultAuthenticatedRoute() });
    },
  });

  const errorMessage =
    mutation.error instanceof PraxisApiError
      ? mutation.error.message
      : mutation.isError
        ? t.auth.inviteError
        : null;

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900">
      <div className="mx-auto flex max-w-md justify-end pb-4">
        <LanguageSelector />
      </div>
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] max-w-md items-center">
        <section className="w-full rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-semibold">{t.auth.inviteTitle}</h1>
              <p className="text-sm text-slate-500">{t.auth.inviteSubtitle}</p>
            </div>
          </div>

          <form
            className="mt-6 space-y-4"
            onSubmit={(event) => {
              event.preventDefault();
              if (canSubmit) mutation.mutate();
            }}
          >
            <div className="space-y-2">
              <Label htmlFor="newPassword">{t.auth.resetNewPasswordLabel}</Label>
              <Input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                disabled={mutation.isPending}
                autoFocus
              />
              <p className="text-xs text-slate-500">{t.auth.resetPasswordHelp}</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="confirmPassword">{t.auth.resetConfirmPasswordLabel}</Label>
              <Input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                disabled={mutation.isPending}
              />
              {!passwordsMatch && (
                <p className="text-xs text-rose-600">{t.auth.resetPasswordsDoNotMatch}</p>
              )}
            </div>

            {errorMessage && (
              <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                {errorMessage}
              </div>
            )}

            <Button type="submit" className="w-full" disabled={!canSubmit || mutation.isPending}>
              {mutation.isPending ? t.auth.inviteSubmitting : t.auth.inviteSubmit}
            </Button>
          </form>

          <p className="mt-5 text-center text-xs text-slate-500">
            {t.auth.inviteAlreadyActivated}{" "}
            <Link to="/" className="font-medium text-primary hover:underline">
              {t.auth.inviteGoToLogin}
            </Link>
          </p>
        </section>
      </div>
    </main>
  );
}
