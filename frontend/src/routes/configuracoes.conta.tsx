import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Save, ShieldCheck, UserRound } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { changePassword, getCurrentAccount } from "@/lib/api/praxis";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/configuracoes/conta")({
  head: () => ({
    meta: [
      { title: "Minha conta - Práxis" },
      {
        name: "description",
        content: "Dados de acesso e troca de senha da conta.",
      },
    ],
  }),
  component: AccountSettingsPage,
});

function AccountSettingsPage() {
  const session = useSession();
  const queryClient = useQueryClient();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const accountQuery = useQuery({
    queryKey: ["account", "me"],
    queryFn: getCurrentAccount,
  });

  const passwordsMatch =
    newPassword.length === 0 || confirmPassword.length === 0 || newPassword === confirmPassword;
  const canSubmit =
    currentPassword.trim().length > 0 &&
    newPassword.length >= 8 &&
    confirmPassword.length >= 8 &&
    newPassword === confirmPassword;

  const passwordMutation = useMutation({
    mutationFn: () => changePassword({ currentPassword, newPassword }),
    onSuccess: async () => {
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSuccessMessage("Senha alterada.");
      await queryClient.invalidateQueries({ queryKey: ["account", "me"] });
    },
  });

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <div className="text-xs uppercase text-primary">Configurações</div>
          <h1 className="mt-1 text-3xl font-semibold">Minha conta</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Dados do usuário logado e credenciais de acesso.
          </p>
        </div>

        <div className="space-y-6">
          <section className="rounded-md border border-border bg-card">
            <div className="border-b border-border px-5 py-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <UserRound className="h-4 w-4 text-primary" />
                Usuário
              </div>
            </div>
            <div className="grid gap-4 p-5 md:grid-cols-2">
              {accountQuery.isError && (
                <div className="md:col-span-2">
                  <StateBanner tone="danger" title="Não foi possível carregar a conta">
                    {accountQuery.error instanceof Error ? accountQuery.error.message : "Tente novamente."}
                  </StateBanner>
                </div>
              )}
              <ReadOnlyField label="Nome" value={accountQuery.data?.name ?? session.userName} />
              <ReadOnlyField label="E-mail" value={accountQuery.data?.email ?? "-"} />
            </div>
          </section>

          <section className="rounded-md border border-border bg-card">
            <div className="border-b border-border px-5 py-4">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <ShieldCheck className="h-4 w-4 text-primary" />
                Senha
              </div>
            </div>
            <div className="max-w-xl space-y-4 p-5">
              {successMessage && (
                <StateBanner tone="ok" title={successMessage}>
                  Use a nova senha no próximo login.
                </StateBanner>
              )}
              {passwordMutation.isError && (
                <StateBanner tone="danger" title="Não foi possível alterar a senha">
                  {passwordMutation.error instanceof Error
                    ? passwordMutation.error.message
                    : "Tente novamente."}
                </StateBanner>
              )}
              <div>
                <Label htmlFor="current-password">Senha atual</Label>
                <Input
                  id="current-password"
                  type="password"
                  value={currentPassword}
                  onChange={(event) => {
                    setSuccessMessage(null);
                    setCurrentPassword(event.target.value);
                  }}
                  disabled={passwordMutation.isPending}
                />
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <Label htmlFor="new-password">Nova senha</Label>
                  <Input
                    id="new-password"
                    type="password"
                    value={newPassword}
                    onChange={(event) => {
                      setSuccessMessage(null);
                      setNewPassword(event.target.value);
                    }}
                    disabled={passwordMutation.isPending}
                  />
                  <p className="mt-1 text-xs text-muted-foreground">Mínimo de 8 caracteres.</p>
                </div>
                <div>
                  <Label htmlFor="confirm-password">Confirmar nova senha</Label>
                  <Input
                    id="confirm-password"
                    type="password"
                    value={confirmPassword}
                    onChange={(event) => {
                      setSuccessMessage(null);
                      setConfirmPassword(event.target.value);
                    }}
                    disabled={passwordMutation.isPending}
                  />
                  {!passwordsMatch && <p className="mt-1 text-xs text-danger">As senhas não conferem.</p>}
                </div>
              </div>
              <Button
                type="button"
                onClick={() => passwordMutation.mutate()}
                disabled={!canSubmit || passwordMutation.isPending}
              >
                <Save className="h-4 w-4" />
                {passwordMutation.isPending ? "Salvando..." : "Salvar senha"}
              </Button>
            </div>
          </section>
        </div>
      </div>
    </AppShell>
  );
}

function ReadOnlyField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 text-sm font-medium">{value}</div>
    </div>
  );
}
