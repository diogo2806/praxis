import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Flag, FlaskConical, Pencil, Plus, ShieldOff } from "lucide-react";
import { useMemo, useState } from "react";

import { AdminShell } from "@/components/admin-shell";
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
import {
  createFeatureFlag,
  evaluateFeatureFlag,
  getFeatureFlagGovernance,
  toggleFeatureFlag,
  toggleFeatureFlagKillSwitch,
  updateFeatureFlag,
  type FeatureFlagResponse,
  type FeatureFlagUpsertRequest,
} from "@/lib/api/feature-flags";

export const Route = createFileRoute("/admin/feature-flags")({
  head: () => ({ meta: [{ title: "Feature flags · Admin Praxis" }] }),
  component: AdminFeatureFlagsPage,
});

type FormState = {
  key: string;
  description: string;
  owner: string;
  defaultEnabled: boolean;
  globalOverride: "default" | "on" | "off";
  active: boolean;
  frontendExposed: boolean;
  temporary: boolean;
  expiresAt: string;
  removalPlan: string;
  environments: string;
  companyIds: string;
  plans: string;
  userIds: string;
  roles: string;
  rolloutPercentage: string;
};

const EMPTY_FORM: FormState = {
  key: "",
  description: "",
  owner: "",
  defaultEnabled: false,
  globalOverride: "default",
  active: false,
  frontendExposed: false,
  temporary: true,
  expiresAt: "",
  removalPlan: "",
  environments: "",
  companyIds: "",
  plans: "",
  userIds: "",
  roles: "",
  rolloutPercentage: "0",
};

function AdminFeatureFlagsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [activeFilter, setActiveFilter] = useState<"all" | "active" | "inactive">("all");
  const [editing, setEditing] = useState<FeatureFlagResponse | null | undefined>(undefined);
  const [evaluation, setEvaluation] = useState<FeatureFlagResponse | null>(null);

  const governanceQuery = useQuery({
    queryKey: ["admin-feature-flags", search, activeFilter],
    queryFn: () =>
      getFeatureFlagGovernance(
        search,
        activeFilter === "all" ? undefined : activeFilter === "active",
      ),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      toggleFeatureFlag(id, enabled),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-feature-flags"] }),
  });

  const killMutation = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      toggleFeatureFlagKillSwitch(id, enabled),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-feature-flags"] }),
  });

  const summary = governanceQuery.data;
  const flags = summary?.flags ?? [];

  return (
    <AdminShell>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-semibold">
            <Flag className="size-6 text-primary" /> Feature flags
          </h1>
          <p className="mt-1 max-w-3xl text-sm text-slate-500">
            Libere recursos por empresa, ambiente, plano, usuário, papel ou percentual estável,
            com expiração, auditoria e desligamento imediato.
          </p>
        </div>
        <Button onClick={() => setEditing(null)}>
          <Plus className="size-4" /> Nova flag
        </Button>
      </div>

      <div className="mt-6 grid gap-3 sm:grid-cols-3">
        <SummaryCard label="Ativas" value={summary?.activeCount ?? 0} />
        <SummaryCard label="Kill switches" value={summary?.killSwitchCount ?? 0} />
        <SummaryCard label="Expiradas" value={summary?.expiredFlags.length ?? 0} />
      </div>

      <div className="mt-6 flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="min-w-[240px] flex-1">
          <Label htmlFor="flag-search" className="text-xs text-slate-500">
            Busca
          </Label>
          <Input
            id="flag-search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Chave, descrição ou proprietário"
          />
        </div>
        <div>
          <Label htmlFor="flag-status" className="text-xs text-slate-500">
            Ativação
          </Label>
          <select
            id="flag-status"
            value={activeFilter}
            onChange={(event) =>
              setActiveFilter(event.target.value as "all" | "active" | "inactive")
            }
            className="block h-9 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="all">Todas</option>
            <option value="active">Ativas</option>
            <option value="inactive">Inativas</option>
          </select>
        </div>
      </div>

      <div className="mt-4 overflow-hidden rounded-xl border border-slate-200 bg-white">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[980px] text-sm">
            <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Flag</th>
                <th className="px-4 py-3">Escopo</th>
                <th className="px-4 py-3">Rollout</th>
                <th className="px-4 py-3">Situação</th>
                <th className="px-4 py-3">Expiração</th>
                <th className="px-4 py-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {governanceQuery.isLoading ? (
                <TableMessage message="Carregando flags…" />
              ) : governanceQuery.isError ? (
                <TableMessage message="Não foi possível carregar as flags." error />
              ) : flags.length === 0 ? (
                <TableMessage message="Nenhuma flag encontrada." />
              ) : (
                flags.map((flag) => (
                  <tr key={flag.id} className="align-top hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="font-mono text-xs font-semibold text-slate-900">{flag.key}</div>
                      <div className="mt-1 max-w-md text-xs leading-5 text-slate-500">
                        {flag.description}
                      </div>
                      <div className="mt-1 text-xs text-slate-400">Responsável: {flag.owner}</div>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600">
                      <ScopeSummary flag={flag} />
                    </td>
                    <td className="px-4 py-3 font-medium tabular-nums">
                      {flag.rolloutPercentage}%
                    </td>
                    <td className="px-4 py-3">
                      <FlagStatus status={flag.status} />
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600">
                      {flag.temporary && flag.expiresAt
                        ? new Date(flag.expiresAt).toLocaleString("pt-BR")
                        : "Permanente"}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1.5">
                        <Button variant="ghost" size="sm" onClick={() => setEditing(flag)}>
                          <Pencil className="size-4" /> Editar
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={toggleMutation.isPending}
                          onClick={() =>
                            toggleMutation.mutate({ id: flag.id, enabled: !flag.active })
                          }
                        >
                          {flag.active ? "Desativar" : "Ativar"}
                        </Button>
                        <Button
                          variant={flag.killSwitch ? "default" : "outline"}
                          size="sm"
                          disabled={killMutation.isPending}
                          onClick={() =>
                            killMutation.mutate({ id: flag.id, enabled: !flag.killSwitch })
                          }
                        >
                          <ShieldOff className="size-4" />
                          {flag.killSwitch ? "Liberar" : "Bloquear"}
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => setEvaluation(flag)}>
                          <FlaskConical className="size-4" /> Simular
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <FeatureFlagDialog
        flag={editing}
        open={editing !== undefined}
        onClose={() => setEditing(undefined)}
        onSaved={() => queryClient.invalidateQueries({ queryKey: ["admin-feature-flags"] })}
      />
      <EvaluationDialog flag={evaluation} onClose={() => setEvaluation(null)} />
    </AdminShell>
  );
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 text-2xl font-semibold tabular-nums text-slate-900">{value}</div>
    </div>
  );
}

function TableMessage({ message, error = false }: { message: string; error?: boolean }) {
  return (
    <tr>
      <td colSpan={6} className={`px-4 py-10 text-center ${error ? "text-rose-600" : "text-slate-500"}`}>
        {message}
      </td>
    </tr>
  );
}

function ScopeSummary({ flag }: { flag: FeatureFlagResponse }) {
  const scopes = [
    flag.companyIds.length > 0 && `${flag.companyIds.length} empresa(s)`,
    flag.environments.length > 0 && `${flag.environments.length} ambiente(s)`,
    flag.plans.length > 0 && `${flag.plans.length} plano(s)`,
    flag.userIds.length > 0 && `${flag.userIds.length} usuário(s)`,
    flag.roles.length > 0 && `${flag.roles.length} papel(is)`,
    flag.globalOverride != null && "global",
  ].filter(Boolean);
  return <span>{scopes.length > 0 ? scopes.join(" · ") : "Padrão seguro"}</span>;
}

function FlagStatus({ status }: { status: FeatureFlagResponse["status"] }) {
  const styles: Record<FeatureFlagResponse["status"], string> = {
    ACTIVE: "bg-emerald-100 text-emerald-700",
    INACTIVE: "bg-slate-100 text-slate-600",
    KILLED: "bg-rose-100 text-rose-700",
    EXPIRED: "bg-amber-100 text-amber-700",
  };
  return <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${styles[status]}`}>{status}</span>;
}

function FeatureFlagDialog({
  flag,
  open,
  onClose,
  onSaved,
}: {
  flag: FeatureFlagResponse | null | undefined;
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState<FormState>(() => toForm(flag));
  const [error, setError] = useState("");
  const isEditing = Boolean(flag);

  const mutation = useMutation({
    mutationFn: (request: FeatureFlagUpsertRequest) =>
      flag ? updateFeatureFlag(flag.id, request) : createFeatureFlag(request),
    onSuccess: () => {
      onSaved();
      onClose();
    },
    onError: (mutationError: Error) => setError(mutationError.message),
  });

  useMemo(() => {
    setForm(toForm(flag));
    setError("");
    return undefined;
  }, [flag]);

  function patch<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function submit() {
    setError("");
    const rolloutPercentage = Number(form.rolloutPercentage);
    if (!form.key.trim() || !form.description.trim() || !form.owner.trim()) {
      setError("Chave, descrição e responsável são obrigatórios.");
      return;
    }
    if (!Number.isInteger(rolloutPercentage) || rolloutPercentage < 0 || rolloutPercentage > 100) {
      setError("O rollout deve ser um inteiro entre 0 e 100.");
      return;
    }
    if (form.temporary && (!form.expiresAt || !form.removalPlan.trim())) {
      setError("Flags temporárias exigem expiração e plano de remoção.");
      return;
    }
    mutation.mutate({
      key: form.key.trim(),
      description: form.description.trim(),
      owner: form.owner.trim(),
      defaultEnabled: form.defaultEnabled,
      globalOverride:
        form.globalOverride === "default" ? null : form.globalOverride === "on",
      active: form.active,
      killSwitch: flag?.killSwitch ?? false,
      frontendExposed: form.frontendExposed,
      temporary: form.temporary,
      expiresAt: form.temporary && form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
      removalPlan: form.temporary ? form.removalPlan.trim() : null,
      environments: splitValues(form.environments),
      companyIds: splitValues(form.companyIds),
      plans: splitValues(form.plans),
      userIds: splitValues(form.userIds),
      roles: splitValues(form.roles),
      rolloutPercentage,
      affectsScoring: false,
    });
  }

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !nextOpen && onClose()}>
      <DialogContent className="max-h-[92vh] max-w-3xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar feature flag" : "Nova feature flag"}</DialogTitle>
          <DialogDescription>
            A precedência é kill switch, usuário, empresa, papel, plano, ambiente, rollout,
            override global e padrão seguro.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Chave" value={form.key} onChange={(value) => patch("key", value)} placeholder="recurso.novo-fluxo" />
          <Field label="Responsável" value={form.owner} onChange={(value) => patch("owner", value)} placeholder="produto@empresa.com" />
          <div className="sm:col-span-2">
            <Label>Descrição</Label>
            <textarea value={form.description} onChange={(event) => patch("description", event.target.value)} className="mt-1 min-h-24 w-full rounded-md border border-input bg-background px-3 py-2 text-sm" />
          </div>
          <Field label="Rollout percentual" value={form.rolloutPercentage} onChange={(value) => patch("rolloutPercentage", value)} type="number" />
          <div>
            <Label>Override global</Label>
            <select value={form.globalOverride} onChange={(event) => patch("globalOverride", event.target.value as FormState["globalOverride"])} className="mt-1 block h-9 w-full rounded-md border border-input bg-background px-3 text-sm">
              <option value="default">Usar padrão</option>
              <option value="on">Ligado</option>
              <option value="off">Desligado</option>
            </select>
          </div>
          <Field label="Empresas" value={form.companyIds} onChange={(value) => patch("companyIds", value)} placeholder="empresa-a, empresa-b" />
          <Field label="Ambientes" value={form.environments} onChange={(value) => patch("environments", value)} placeholder="production, staging" />
          <Field label="Planos" value={form.plans} onChange={(value) => patch("plans", value)} placeholder="enterprise" />
          <Field label="Papéis" value={form.roles} onChange={(value) => patch("roles", value)} placeholder="team_manager" />
          <Field label="Usuários" value={form.userIds} onChange={(value) => patch("userIds", value)} placeholder="user-1, user-2" />
          <div />
          <Checkbox label="Ativa" checked={form.active} onChange={(checked) => patch("active", checked)} />
          <Checkbox label="Exposta ao frontend" checked={form.frontendExposed} onChange={(checked) => patch("frontendExposed", checked)} />
          <Checkbox label="Padrão ligado" checked={form.defaultEnabled} onChange={(checked) => patch("defaultEnabled", checked)} />
          <Checkbox label="Temporária" checked={form.temporary} onChange={(checked) => patch("temporary", checked)} />
          {form.temporary && (
            <>
              <Field label="Expira em" value={form.expiresAt} onChange={(value) => patch("expiresAt", value)} type="datetime-local" />
              <Field label="Plano de remoção" value={form.removalPlan} onChange={(value) => patch("removalPlan", value)} placeholder="Remover após validar o piloto" />
            </>
          )}
        </div>

        {error && <p className="rounded-md bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancelar</Button>
          <Button disabled={mutation.isPending} onClick={submit}>{mutation.isPending ? "Salvando…" : "Salvar"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function EvaluationDialog({ flag, onClose }: { flag: FeatureFlagResponse | null; onClose: () => void }) {
  const [identifier, setIdentifier] = useState("");
  const [companyId, setCompanyId] = useState("");
  const evaluationMutation = useMutation({
    mutationFn: () => evaluateFeatureFlag(flag?.id ?? "", identifier, companyId || undefined),
  });

  return (
    <Dialog open={Boolean(flag)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Simular rollout</DialogTitle>
          <DialogDescription>{flag?.key}</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <Field label="Identificador estável" value={identifier} onChange={setIdentifier} placeholder="candidate-42" />
          <Field label="Empresa" value={companyId} onChange={setCompanyId} placeholder="empresa-piloto" />
          {evaluationMutation.data && (
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm">
              <div className="font-semibold">Variante {evaluationMutation.data.variant}</div>
              <div className="mt-1 text-slate-600">Motivo: {evaluationMutation.data.reason}</div>
              {evaluationMutation.data.rolloutBucket >= 0 && <div className="text-slate-600">Bucket estável: {evaluationMutation.data.rolloutBucket}</div>}
            </div>
          )}
          {evaluationMutation.isError && <p className="text-sm text-rose-600">{evaluationMutation.error.message}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Fechar</Button>
          <Button disabled={!identifier.trim() || evaluationMutation.isPending} onClick={() => evaluationMutation.mutate()}>
            {evaluationMutation.isPending ? "Simulando…" : "Simular"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function Field({ label, value, onChange, placeholder, type = "text" }: { label: string; value: string; onChange: (value: string) => void; placeholder?: string; type?: string }) {
  return (
    <div>
      <Label>{label}</Label>
      <Input className="mt-1" type={type} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
    </div>
  );
}

function Checkbox({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="flex min-h-10 items-center gap-2 rounded-md border border-slate-200 px-3 text-sm">
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      {label}
    </label>
  );
}

function splitValues(value: string) {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function toForm(flag: FeatureFlagResponse | null | undefined): FormState {
  if (!flag) return EMPTY_FORM;
  return {
    key: flag.key,
    description: flag.description,
    owner: flag.owner,
    defaultEnabled: flag.defaultEnabled,
    globalOverride: flag.globalOverride == null ? "default" : flag.globalOverride ? "on" : "off",
    active: flag.active,
    frontendExposed: flag.frontendExposed,
    temporary: flag.temporary,
    expiresAt: flag.expiresAt ? new Date(flag.expiresAt).toISOString().slice(0, 16) : "",
    removalPlan: flag.removalPlan ?? "",
    environments: flag.environments.join(", "),
    companyIds: flag.companyIds.join(", "),
    plans: flag.plans.join(", "),
    userIds: flag.userIds.join(", "),
    roles: flag.roles.join(", "),
    rolloutPercentage: String(flag.rolloutPercentage),
  };
}
