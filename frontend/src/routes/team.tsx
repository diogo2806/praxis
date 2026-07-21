import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Ban, CheckCircle2, Clock, Plus, RefreshCw, ShieldOff, Users } from "lucide-react";
import { z } from "zod";

import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ResponsiveTable } from "@/components/ui/responsive-table";
import {
  blockTeamUser,
  inviteTeamUser,
  listTeamUsers,
  resendTeamUserInvite,
  type TeamUser,
  unblockTeamUser,
} from "@/lib/api/praxis";

export const Route = createFileRoute("/team")({
  head: () => ({
    meta: [
      { title: "Minha equipe - Práxis" },
      { name: "description", content: "Gerencie quem pode acessar o Práxis pela sua empresa." },
    ],
  }),
  component: TeamPage,
});

const inviteUserSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome do usuário."),
  email: z.string().trim().min(1, "Informe o e-mail do usuário.").email("Informe um e-mail válido."),
});

type InviteUserErrors = {
  name?: string;
  email?: string;
};

function TeamPage() {
  const [inviteOpen, setInviteOpen] = useState(false);
  const teamQuery = useQuery({ queryKey: ["team"], queryFn: listTeamUsers });

  return (
    <AppShell>
      <main className="mx-auto max-w-5xl space-y-6">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">Minha equipe</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Gerencie os usuários vinculados à sua empresa.
            </p>
          </div>
          <Button onClick={() => setInviteOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Convidar usuário
          </Button>
        </header>

        <InviteUserModal open={inviteOpen} onClose={() => setInviteOpen(false)} />

        {teamQuery.isLoading ? (
          <div className="space-y-2">
            {[1, 2, 3].map((item) => (
              <div key={item} className="h-14 animate-pulse rounded-lg bg-muted" />
            ))}
          </div>
        ) : teamQuery.isError ? (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-6 text-center">
            <p className="text-sm text-destructive">Não foi possível carregar a equipe.</p>
            <Button variant="outline" size="sm" className="mt-4" onClick={() => teamQuery.refetch()}>
              <RefreshCw className="mr-2 h-3.5 w-3.5" />
              Tentar novamente
            </Button>
          </div>
        ) : (
          <section className="space-y-2">
            <h2 className="text-sm font-medium uppercase tracking-wide text-muted-foreground">Usuários</h2>
            <TeamUsersTable users={teamQuery.data ?? []} onInviteClick={() => setInviteOpen(true)} />
          </section>
        )}
      </main>
    </AppShell>
  );
}

function InviteUserModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [fieldErrors, setFieldErrors] = useState<InviteUserErrors>({});
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName("");
    setEmail("");
    setFieldErrors({});
    setError(null);
  }, [open]);

  const mutation = useMutation({
    mutationFn: () => inviteTeamUser({ name: name.trim(), email: email.trim() }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["team"] });
      onClose();
    },
    onError: (mutationError: Error) => {
      setError(mutationError.message || "Não foi possível enviar o convite.");
    },
  });

  function submit() {
    setError(null);
    const parsed = inviteUserSchema.safeParse({ name, email });
    if (!parsed.success) {
      const nextErrors: InviteUserErrors = {};
      for (const issue of parsed.error.issues) {
        if (issue.path[0] === "name") nextErrors.name = issue.message;
        if (issue.path[0] === "email") nextErrors.email = issue.message;
      }
      setFieldErrors(nextErrors);
      return;
    }
    setFieldErrors({});
    mutation.mutate();
  }

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && !mutation.isPending && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Convidar usuário</DialogTitle>
          <DialogDescription>
            O convidado receberá um link para definir a senha e acessará as funções liberadas para o perfil EMPRESA.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label htmlFor="invite-name">Nome</Label>
            <Input
              id="invite-name"
              value={name}
              onChange={(event) => {
                setName(event.target.value);
                setFieldErrors((current) => ({ ...current, name: undefined }));
              }}
              placeholder="Nome do usuário"
              disabled={mutation.isPending}
              aria-invalid={Boolean(fieldErrors.name)}
            />
            {fieldErrors.name && <p className="text-xs text-destructive">{fieldErrors.name}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="invite-email">E-mail</Label>
            <Input
              id="invite-email"
              type="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                setFieldErrors((current) => ({ ...current, email: undefined }));
              }}
              placeholder="usuario@empresa.com"
              disabled={mutation.isPending}
              aria-invalid={Boolean(fieldErrors.email)}
            />
            {fieldErrors.email && <p className="text-xs text-destructive">{fieldErrors.email}</p>}
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>Cancelar</Button>
          <Button onClick={submit} disabled={mutation.isPending}>
            {mutation.isPending ? "Enviando..." : "Enviar convite"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function TeamUsersTable({ users, onInviteClick }: { users: TeamUser[]; onInviteClick: () => void }) {
  const queryClient = useQueryClient();
  const [blockTarget, setBlockTarget] = useState<TeamUser | null>(null);
  const resendMutation = useMutation({
    mutationFn: resendTeamUserInvite,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["team"] }),
  });
  const unblockMutation = useMutation({
    mutationFn: unblockTeamUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["team"] }),
  });

  if (users.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border bg-muted/20 py-16 text-center">
        <Users className="mb-4 h-10 w-10 text-muted-foreground/60" />
        <h3 className="font-semibold">Nenhum usuário convidado ainda.</h3>
        <p className="mt-2 max-w-sm text-sm text-muted-foreground">
          Convide pessoas da sua equipe para acessar o ambiente da empresa.
        </p>
        <Button className="mt-6" onClick={onInviteClick}>
          <Plus className="mr-2 h-4 w-4" />
          Convidar primeiro usuário
        </Button>
      </div>
    );
  }

  return (
    <>
      <BlockUserDialog user={blockTarget} onClose={() => setBlockTarget(null)} />
      <ResponsiveTable minWidth="760px">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Nome</th>
              <th className="px-4 py-3 text-left font-medium">E-mail</th>
              <th className="px-4 py-3 text-left font-medium">Status</th>
              <th className="px-4 py-3 text-left font-medium">Último acesso</th>
              <th className="w-[190px] px-4 py-3 text-right font-medium">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {users.map((user) => (
              <tr key={user.id} className="bg-background hover:bg-muted/30">
                <td className="max-w-[220px] truncate px-4 py-3 font-medium">{user.name}</td>
                <td className="max-w-[260px] truncate px-4 py-3 text-muted-foreground">{user.email}</td>
                <td className="px-4 py-3"><UserStatusBadge status={user.status} /></td>
                <td className="px-4 py-3 text-muted-foreground">{formatLastAccess(user.lastLoginAt)}</td>
                <td className="px-4 py-3 text-right">
                  {user.status === "CONVIDADO" && (
                    <Button variant="outline" size="sm" onClick={() => resendMutation.mutate(user.id)} disabled={resendMutation.isPending}>
                      <RefreshCw className="mr-1.5 h-3.5 w-3.5" />Reenviar
                    </Button>
                  )}
                  {user.status === "ATIVO" && (
                    <Button variant="outline" size="sm" onClick={() => setBlockTarget(user)}>
                      <ShieldOff className="mr-1.5 h-3.5 w-3.5" />Bloquear
                    </Button>
                  )}
                  {user.status === "BLOQUEADO" && (
                    <Button variant="outline" size="sm" onClick={() => unblockMutation.mutate(user.id)} disabled={unblockMutation.isPending}>
                      <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" />Desbloquear
                    </Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </ResponsiveTable>
    </>
  );
}

function BlockUserDialog({ user, onClose }: { user: TeamUser | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => blockTeamUser(user!.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["team"] });
      onClose();
    },
  });

  return (
    <Dialog open={user !== null} onOpenChange={(open) => !open && !mutation.isPending && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Bloquear acesso</DialogTitle>
          <DialogDescription>
            {user?.name} não conseguirá mais acessar o Práxis. O histórico será preservado.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>Cancelar</Button>
          <Button variant="destructive" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            {mutation.isPending ? "Bloqueando..." : "Bloquear"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function UserStatusBadge({ status }: { status: TeamUser["status"] }) {
  if (status === "ATIVO") {
    return <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800 dark:bg-green-900/30 dark:text-green-400"><CheckCircle2 className="h-3 w-3" />Ativo</span>;
  }
  if (status === "CONVIDADO") {
    return <span className="inline-flex items-center gap-1 rounded-full bg-yellow-100 px-2 py-0.5 text-xs font-medium text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400"><Clock className="h-3 w-3" />Convidado</span>;
  }
  return <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800 dark:bg-red-900/30 dark:text-red-400"><Ban className="h-3 w-3" />Bloqueado</span>;
}

function formatLastAccess(value: string | null) {
  if (!value) return "—";
  return new Date(value).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
