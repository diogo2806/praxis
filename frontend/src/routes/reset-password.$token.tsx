import { useMutation, useQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { KeyRound } from "lucide-react";
import { type ReactNode, useState } from "react";

import { LanguageSelector } from "@/components/language-selector";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PraxisApiError, resetPassword, validatePasswordResetToken } from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";

export const Route = createFileRoute("/reset-password/$token")({
  head: () => ({
    meta: [
      { title: "Redefinir senha - Práxis" },
      {
        name: "description",
        content: "Defina uma nova senha de acesso ao Praxis.",
      },
    ],
  }),
  component: ResetPasswordPage,
});

function ResetPasswordPage() {
  const { t } = useLanguage();
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
      await navigate({ to: "/login" });
    },
  });

  if (tokenQuery.isLoading) {
    return (
      <CenteredCard title={t.auth.resetTitle} subtitle={t.auth.resetValidating}>
        <p className="mt-6 text-sm text-slate-500">{t.auth.resetWaitMoment}</p>
      </CenteredCard>
    );
  }

  if (tokenQuery.isError) {
    const expired = tokenQuery.error instanceof PraxisApiError && tokenQuery.error.status === 410;
    return (
      <CenteredCard
        title={t.auth.resetLinkUnavailable}
        subtitle={expired ? t.auth.resetLinkExpired : t.auth.resetLinkInvalid}
      >
        <div className="mt-6 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {t.auth.resetRequestNewLink}
        </div>
        <p className="mt-5 text-center text-xs text-slate-500">
          <Link to="/recuperar-senha" className="font-medium text-primary hover:underline">
            {t.auth.resetRequestNewLinkCta}
          </Link>
        </p>
      </CenteredCard>
    );
  }

  const errorMessage =
    mutation.error instanceof PraxisApiError
      ? mutation.error.message
      : mutation.isError
        ? t.auth.resetError
        : null;

  return (
    <CenteredCard
      title={t.auth.resetTitle}
      subtitle={
        tokenQuery.data?.userName
          ? t.auth.resetSubtitleWithName.replace("{name}", tokenQuery.data.userName)
          : t.auth.resetSubtitleDefault
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
          {mutation.isPending ? t.auth.resetSubmitting : t.auth.resetSubmit}
        </Button>
      </form>

      <p className="mt-5 text-center text-xs text-slate-500">
        {t.auth.rememberedPassword}{" "}
        <Link to="/login" className="font-medium text-primary hover:underline">
          {t.auth.backToLogin}
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
