import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  Ban,
  CheckCircle2,
  Clock,
  Pencil,
  Plus,
  RefreshCw,
  ShieldCheck,
  ShieldOff,
  Users,
} from "lucide-react";
import { z } from "zod";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
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
  type AssignableTeamProfile,
  type TeamProfile,
  type TeamUser,
  unblockTeamUser,
  updateTeamUserAccess,
} from "@/lib/api/team";
import { isLegacyCompanyManager } from "@/lib/feature-flags";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/team")({
  head: () => ({
    meta: [
      { title: "Minha equipe - Práxis" },
      {
        name: "description",
        content: "Gerencie usuários, perfis, permissões e situação de acesso da empresa.",
      },
    ],
  }),
  component: TeamPage,
});

const PROFILE_OPTIONS: Array<{
  value: AssignableTeamProfile;
  label: string;
  description: string;
}> = [
  {
    value: "ADMINISTRADOR",
    label: "Administrador",
    description: "Equipe, parceiros, autoria, operação, resultados, integrações e cobrança.",
  },
  {
    value: "AUTOR",
    label: "Autor de avaliações",
    description: "Cria e revisa avaliações e consulta o catálogo de competências.",
  },
  {
    value: "ANALISTA",
    label: "Analista de resultados",
    description: "Consulta evidências, compara participantes e registra decisões humanas.",
  },
  {
    value: "OPERADOR",
    label: "Operador",
    description: "Cria participações e acompanha jornadas, integrações e ocorrências.",
  },
];

const inviteUserSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome do usuário."),
  email: z.string().trim().min(1, "Informe o e-mail do usuário.").email("Informe um e-mail válido."),
  profile: z.enum(["ADMINISTRADOR", "AUTOR", "ANALISTA", "OPERADOR"]),
});

type InviteUserErrors = Partial<Record<"name" | "email" | "profile", string>>;

function TeamPage() {
  const session = useSession();
  const [inviteOpen, setInviteOpen] = useState(false);
  const teamQuery = useQuery({ queryKey: ["team"], queryFn: listTeamUsers, retry: false });
  const canManage =
    session.roles.includes("TEAM_MANAGER") || isLegacyCompanyManager(session.roles);

  return (
    <AppShell>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              Acesso da empresa
            </div>
            <h1 className="mt-1 text-3xl font-semibold tracking-tight text-foreground">
              Minha equipe
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
              Consulte quem está vinculado à empresa, qual perfil cada pessoa possui, o que pode
              fazer e se o acesso está ativo.
            </p>
          </div>
          {canManage && (
            <Button onClick={() => setInviteOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Convidar usuário
            </Button>
          )}
        </header>

        {!canManage && (
          <StateBanner tone="info" title="Consulta sem permissão administrativa">
            Seu perfil pode consultar a equipe, mas somente administradores podem convidar,
            alterar perfis ou bloquear acessos.
          </StateBanner>
        )}

        <InviteUserModal open={inviteOpen} onClose={() => setInviteOpen(false)} />

        {teamQuery.isLoading ? (
          <div className="space-y-2">
            {[1, 2, 3].map((item) => (
              <div key={item} className="h-20 animate-pulse rounded-lg bg-muted" />
            ))}
          </div>
        ) : teamQuery.isError ? (
          <StateBanner
            tone="danger"
            title="Não foi possível carregar a equipe"
            action={
              <Button variant="outline" size="sm" onClick={() => teamQuery.refetch()}>
                <RefreshCw className="mr-2 h-3.5 w-3.5" />
                Tentar novamente
              </Button>
            }
          >
            {teamQuery.error instanceof Error ? teamQuery.error.message : "Tente novamente."}
          </StateBanner>
        ) : (
          <TeamUsersTable
            users={teamQuery.data ?? []}
            canManage={canManage}
            onInviteClick={() => setInviteOpen(true)}
          />
        )}
      </main>
    </AppShell>
  );
}

function InviteUserModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [profile, setProfile] = useState<AssignableTeamProfile>("OPERADOR");
  const [fieldErrors, setFieldErrors] = useState<InviteUserErrors>({});
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName("");
    setEmail("");
    setProfile("OPERADOR");
    setFieldErrors({});
    setError(null);
  }, [open]);

  const mutation = useMutation({
    mutationFn: () => inviteTeamUser({ name: name.trim(), email: email.trim(), profile }),
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
    const parsed = inviteUserSchema.safeParse({ name, email, profile });
    if (!parsed.success) {
      const nextErrors: InviteUserErrors = {};
      for (const issue of parsed.error.issues) {
        const field = issue.path[0];
        if (field === "name" || field === "email" || field === "profile") {
          nextErrors[field] = issue.message;
        }
      }
      setFieldErrors(nextErrors);
      return;
    }
    setFieldErrors({});
    mutation.mutate();
  }

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && !mutation.isPending && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Convidar usuário</DialogTitle>
          <DialogDescription>
            Defina o perfil antes do envio. As permissões serão aplicadas quando a pessoa aceitar o
            convite.
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
          <ProfileSelect value={profile} onChange={setProfile} disabled={mutation.isPending} />
          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>
            Cancelar
          </Button>
          <Button onClick={submit} disabled={mutation.isPending}>
            {mutation.isPending ? "Enviando..." : "Enviar convite"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ProfileSelect({
  value,
  onChange,
  disabled,
}: {
  value: AssignableTeamProfile;
  onChange: (profile: AssignableTeamProfile) => void;
  disabled: boolean;
}) {
  const selected = PROFILE_OPTIONS.find((option) => option.value === value);
  return (
    <div className="space-y-1.5">
      <Label htmlFor="team-profile">Perfil</Label>
      <select
        id="team-profile"
        value={value}
        onChange={(event) => onChange(event.target.value as AssignableTeamProfile)}
        disabled={disabled}
        className="input w-full"
      >
        {PROFILE_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <p className="text-xs leading-5 text-muted-foreground">{selected?.description}</p>
    </div>
  );
}

function TeamUsersTable({
  users,
  canManage,
  onInviteClick,
}: {
  users: TeamUser[];
  canManage: boolean;
  onInviteClick: () => void;
}) {
  const queryClient = useQueryClient();
  const [blockTarget, setBlockTarget] = useState<TeamUser | null>(null);
  const [profileTarget, setProfileTarget] = useState<TeamUser | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["team"] });
  const resendMutation = useMutation({
    mutationFn: resendTeamUserInvite,
    onSuccess: invalidate,
    onError: (error: Error) => setActionError(error.message),
  });
  const unblockMutation = useMutation({
    mutationFn: unblockTeamUser,
    onSuccess: invalidate,
    onError: (error: Error) => setActionError(error.message),
  });

  if (users.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border bg-muted/20 py-16 text-center">
        <Users className="mb-4 h-10 w-10 text-muted-foreground/60" />
        <h2 className="font-semibold">Nenhum usuário convidado ainda</h2>
        <p className="mt-2 max-w-sm text-sm text-muted-foreground">
          Convide pessoas e conceda somente as permissões necessárias para cada função.
        </p>
        {canManage && (
          <Button className="mt-6" onClick={onInviteClick}>
            <Plus className="mr-2 h-4 w-4" />
            Convidar primeiro usuário
          </Button>
        )}
      </div>
    );
  }

  return (
    <section className="space-y-3">
      {actionError && (
        <StateBanner tone="danger" title="A ação não foi concluída">
          {actionError}
        </StateBanner>
      )}
      <BlockUserDialog user={blockTarget} onClose={() => setBlockTarget(null)} />
      <EditProfileDialog user={profileTarget} onClose={() => setProfileTarget(null)} />
      <ResponsiveTable minWidth="1080px">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Usuário</th>
              <th className="px-4 py-3 text-left font-medium">Perfil</th>
              <th className="px-4 py-3 text-left font-medium">Permissões</th>
              <th className="px-4 py-3 text-left font-medium">Acesso</th>
              <th className="w-[240px] px-4 py-3 text-right font-medium">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {users.map((user) => (
              <tr key={user.id} className="bg-background align-top hover:bg-muted/30">
                <td className="px-4 py-4">
                  <div className="font-medium">{user.name}</div>
                  <div className="mt-1 text-xs text-muted-foreground">{user.email}</div>
                </td>
                <td className="px-4 py-4">
                  <ProfileBadge profile={user.profile} />
                </td>
                <td className="px-4 py-4">
                  <ul className="max-w-md space-y-1 text-xs leading-5 text-muted-foreground">
                    {user.permissions.map((permission) => (
                      <li key={permission}>• {permission}</li>
                    ))}
                  </ul>
                </td>
                <td className="px-4 py-4">
                  <UserStatusBadge status={user.status} />
                  <div className="mt-2 text-xs text-muted-foreground">
                    Último acesso: {formatLastAccess(user.lastLoginAt)}
                  </div>
                </td>
                <td className="px-4 py-4 text-right">
                  {canManage ? (
                    <div className="flex flex-wrap justify-end gap-2">
                      {user.profile !== "ESPECIALISTA" && (
                        <Button variant="outline" size="sm" onClick={() => setProfileTarget(user)}>
                          <Pencil className="mr-1.5 h-3.5 w-3.5" />Perfil
                        </Button>
                      )}
                      {user.status === "CONVIDADO" && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => resendMutation.mutate(user.id)}
                          disabled={resendMutation.isPending}
                        >
                          <RefreshCw className="mr-1.5 h-3.5 w-3.5" />Reenviar
                        </Button>
                      )}
                      {user.status === "ATIVO" && (
                        <Button variant="outline" size="sm" onClick={() => setBlockTarget(user)}>
                          <ShieldOff className="mr-1.5 h-3.5 w-3.5" />Bloquear
                        </Button>
                      )}
                      {user.status === "BLOQUEADO" && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => unblockMutation.mutate(user.id)}
                          disabled={unblockMutation.isPending}
                        >
                          <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" />Desbloquear
                        </Button>
                      )}
                    </div>
                  ) : (
                    <span className="text-xs text-muted-foreground">Somente consulta</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </ResponsiveTable>
    </section>
  );
}

function EditProfileDialog({ user, onClose }: { user: TeamUser | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [profile, setProfile] = useState<AssignableTeamProfile>("OPERADOR");

  useEffect(() => {
    if (user && user.profile !== "ESPECIALISTA") {
      setProfile(user.profile);
    }
  }, [user]);

  const mutation = useMutation({
    mutationFn: () => updateTeamUserAccess(user!.id, profile),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["team"] });
      onClose();
    },
  });

  return (
    <Dialog open={user !== null} onOpenChange={(open) => !open && !mutation.isPending && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Alterar perfil</DialogTitle>
          <DialogDescription>
            O novo conjunto de permissões será usado no próximo acesso de {user?.name ?? "usuário"}.
          </DialogDescription>
        </DialogHeader>
        <ProfileSelect value={profile} onChange={setProfile} disabled={mutation.isPending} />
        {mutation.isError && (
          <p className="text-sm text-destructive" role="alert">
            {mutation.error instanceof Error ? mutation.error.message : "Não foi possível alterar o perfil."}
          </p>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>Cancelar</Button>
          <Button onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            {mutation.isPending ? "Salvando..." : "Salvar perfil"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
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
        {mutation.isError && (
          <p className="text-sm text-destructive" role="alert">
            {mutation.error instanceof Error ? mutation.error.message : "Não foi possível bloquear."}
          </p>
        )}
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

function ProfileBadge({ profile }: { profile: TeamProfile }) {
  const option = PROFILE_OPTIONS.find((item) => item.value === profile);
  const label = profile === "ESPECIALISTA" ? "Especialista parceiro" : option?.label ?? profile;
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
      <ShieldCheck className="h-3.5 w-3.5" />
      {label}
    </span>
  );
}

function UserStatusBadge({ status }: { status: TeamUser["status"] }) {
  if (status === "ATIVO") {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-success/10 px-2 py-1 text-xs font-medium text-success">
        <CheckCircle2 className="h-3 w-3" />Ativo
      </span>
    );
  }
  if (status === "CONVIDADO") {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-warning/10 px-2 py-1 text-xs font-medium text-warning-foreground">
        <Clock className="h-3 w-3" />Convidado
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-danger/10 px-2 py-1 text-xs font-medium text-danger">
      <Ban className="h-3 w-3" />Bloqueado
    </span>
  );
}

function formatLastAccess(value: string | null) {
  if (!value) return "Nunca acessou";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
