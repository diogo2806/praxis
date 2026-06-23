import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import {
  Building2,
  Check,
  Copy,
  KeyRound,
  Plus,
  RefreshCw,
  Save,
  Settings2,
  ShieldCheck,
  Trash2,
  UserRound,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { SkeletonRows, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  changePassword,
  deleteIntegrationToken,
  getCurrentAccount,
  getTenantConfig,
  listIntegrationTokens,
  rotateIntegrationToken,
  updateTenantConfig,
  type IntegrationProvider,
  type IntegrationTokenResponse,
  type TenantConfigOption,
  type TenantConfigType,
} from "@/lib/api/praxis";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/configuracoes")({
  head: () => ({
    meta: [
      { title: "Configurações - Praxis" },
      {
        name: "description",
        content: "Configurações administrativas da empresa, acesso, integrações e parâmetros.",
      },
    ],
  }),
  component: ConfiguracoesPage,
});

type SectionId = "empresa" | "acesso" | "integracoes" | "parametros";

type EditableCatalog = {
  type: TenantConfigType;
  key: "resultUses" | "languageChecklist" | "answerTimeLimits";
  title: string;
  description: string;
  valueLabel: string;
  labelLabel: string;
  numericValue?: boolean;
};

const sections: Array<{ id: SectionId; label: string; Icon: typeof Building2 }> = [
  { id: "empresa", label: "Empresa", Icon: Building2 },
  { id: "acesso", label: "Acesso", Icon: UserRound },
  { id: "integracoes", label: "Integrações", Icon: KeyRound },
  { id: "parametros", label: "Parâmetros", Icon: Settings2 },
];

const catalogs: EditableCatalog[] = [
  {
    type: "RESULT_USE",
    key: "resultUses",
    title: "Usos do resultado",
    description: "Opções disponíveis ao definir como o resultado será usado no processo.",
    valueLabel: "Valor",
    labelLabel: "Rótulo",
  },
  {
    type: "LANGUAGE_CHECKLIST",
    key: "languageChecklist",
    title: "Checklist de linguagem",
    description: "Critérios usados para revisar neutralidade e adequação textual.",
    valueLabel: "Critério",
    labelLabel: "Rótulo",
  },
  {
    type: "ANSWER_TIME_LIMIT",
    key: "answerTimeLimits",
    title: "Tempos de resposta",
    description: "Limites oferecidos no editor de diálogo para novas etapas.",
    valueLabel: "Segundos",
    labelLabel: "Rótulo",
    numericValue: true,
  },
];

const providerLabels: Record<IntegrationProvider, string> = {
  gupy: "Gupy",
  recrutei: "Recrutei",
};

function ConfiguracoesPage() {
  const [activeSection, setActiveSection] = useState<SectionId>("empresa");

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl">
        <div className="mb-6">
          <div className="text-xs uppercase text-primary">Configurações</div>
          <h1 className="mt-1 text-3xl font-semibold">Administração da empresa</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
            Ajuste dados de acesso, integrações e parâmetros que ainda não possuem tela própria.
          </p>
        </div>

        <div className="mb-5 flex flex-wrap gap-2 border-b border-border pb-3">
          {sections.map(({ id, label, Icon }) => (
            <button
              key={id}
              type="button"
              onClick={() => setActiveSection(id)}
              className={`inline-flex min-h-9 items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                activeSection === id
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card hover:bg-accent"
              }`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>

        {activeSection === "empresa" && <EmpresaSection />}
        {activeSection === "acesso" && <AcessoSection />}
        {activeSection === "integracoes" && <IntegracoesSection />}
        {activeSection === "parametros" && <ParametrosSection />}
      </div>
    </AppShell>
  );
}

function EmpresaSection() {
  const session = useSession();
  const accountQuery = useQuery({
    queryKey: ["account", "me"],
    queryFn: getCurrentAccount,
  });

  return (
    <Section title="Empresa" icon={<Building2 className="h-4 w-4 text-primary" />}>
      {accountQuery.isError && (
        <StateBanner tone="danger" title="Não foi possível carregar a conta">
          {accountQuery.error instanceof Error ? accountQuery.error.message : "Tente novamente."}
        </StateBanner>
      )}
      <div className="grid gap-4 md:grid-cols-2">
        <ReadOnlyField label="Workspace" value={session.workspaceName} />
        <ReadOnlyField label="ID do tenant" value={session.tenantId ?? "-"} mono />
        <ReadOnlyField label="Usuário ativo" value={accountQuery.data?.name ?? session.userName} />
        <ReadOnlyField label="E-mail" value={accountQuery.data?.email ?? "-"} />
        <ReadOnlyField
          label="Papéis"
          value={(accountQuery.data?.roles ?? [session.userRole]).join(", ")}
        />
      </div>
    </Section>
  );
}

function AcessoSection() {
  const queryClient = useQueryClient();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
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
    <Section title="Acesso" icon={<ShieldCheck className="h-4 w-4 text-primary" />}>
      <div className="max-w-xl space-y-4">
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
    </Section>
  );
}

function IntegracoesSection() {
  const queryClient = useQueryClient();
  const [visibleTokens, setVisibleTokens] = useState<Partial<Record<IntegrationProvider, string>>>(
    {},
  );
  const [copiedProvider, setCopiedProvider] = useState<IntegrationProvider | null>(null);
  const tokensQuery = useQuery({
    queryKey: ["integration-tokens"],
    queryFn: listIntegrationTokens,
  });

  const rotateMutation = useMutation({
    mutationFn: rotateIntegrationToken,
    onSuccess: async (response) => {
      setVisibleTokens((current) => ({ ...current, [response.provider]: response.token }));
      await queryClient.invalidateQueries({ queryKey: ["integration-tokens"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteIntegrationToken,
    onSuccess: async (_, provider) => {
      setVisibleTokens((current) => {
        const next = { ...current };
        delete next[provider];
        return next;
      });
      await queryClient.invalidateQueries({ queryKey: ["integration-tokens"] });
    },
  });

  const copyToken = async (provider: IntegrationProvider, token: string) => {
    await navigator.clipboard.writeText(token);
    setCopiedProvider(provider);
    window.setTimeout(() => setCopiedProvider(null), 2000);
  };

  return (
    <Section title="Integrações" icon={<KeyRound className="h-4 w-4 text-primary" />}>
      <div className="space-y-4">
        {tokensQuery.isLoading && <SkeletonRows rows={2} />}
        {tokensQuery.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar integrações">
            {tokensQuery.error instanceof Error ? tokensQuery.error.message : "Tente novamente."}
          </StateBanner>
        )}
        {(rotateMutation.isError || deleteMutation.isError) && (
          <StateBanner tone="danger" title="Não foi possível salvar a integração">
            {(rotateMutation.error ?? deleteMutation.error) instanceof Error
              ? (rotateMutation.error ?? deleteMutation.error)?.message
              : "Tente novamente."}
          </StateBanner>
        )}
        {(tokensQuery.data ?? []).map((token) => (
          <IntegrationRow
            key={token.provider}
            token={token}
            visibleToken={visibleTokens[token.provider]}
            copied={copiedProvider === token.provider}
            pending={
              (rotateMutation.isPending && rotateMutation.variables === token.provider) ||
              (deleteMutation.isPending && deleteMutation.variables === token.provider)
            }
            onRotate={() => rotateMutation.mutate(token.provider)}
            onDelete={() => deleteMutation.mutate(token.provider)}
            onCopy={(value) => void copyToken(token.provider, value)}
          />
        ))}
      </div>
    </Section>
  );
}

function IntegrationRow({
  token,
  visibleToken,
  copied,
  pending,
  onRotate,
  onDelete,
  onCopy,
}: {
  token: IntegrationTokenResponse;
  visibleToken?: string;
  copied: boolean;
  pending: boolean;
  onRotate: () => void;
  onDelete: () => void;
  onCopy: (token: string) => void;
}) {
  return (
    <div className="rounded-md border border-border bg-background p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-semibold">{providerLabels[token.provider]}</h3>
            <span
              className={`rounded-md px-2 py-0.5 text-xs font-medium ${
                token.configured ? "bg-success/15 text-success" : "bg-muted text-muted-foreground"
              }`}
            >
              {token.configured ? "Configurado" : "Não configurado"}
            </span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {token.createdAt ? `Criado em ${formatDateTime(token.createdAt)}` : "Sem token ativo."}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" size="sm" variant="outline" onClick={onRotate} disabled={pending}>
            <RefreshCw className="h-4 w-4" />
            {token.configured ? "Renovar" : "Gerar token"}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={onDelete}
            disabled={pending || !token.configured}
          >
            <Trash2 className="h-4 w-4" />
            Revogar
          </Button>
        </div>
      </div>
      {visibleToken && (
        <div className="mt-4 rounded-md border border-warning/35 bg-warning/10 p-3">
          <div className="text-xs font-semibold text-warning-foreground">
            Copie agora. Este token não será exibido novamente.
          </div>
          <div className="mt-2 grid gap-2 md:grid-cols-[1fr_auto]">
            <Input readOnly value={visibleToken} className="font-mono text-xs" />
            <Button type="button" variant="outline" onClick={() => onCopy(visibleToken)}>
              {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
              {copied ? "Copiado" : "Copiar"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function ParametrosSection() {
  const tenantConfigQuery = useQuery({
    queryKey: ["tenant-config"],
    queryFn: getTenantConfig,
  });

  return (
    <Section title="Parâmetros" icon={<Settings2 className="h-4 w-4 text-primary" />}>
      <div className="space-y-6">
        {tenantConfigQuery.isLoading && <SkeletonRows rows={4} />}
        {tenantConfigQuery.isError && (
          <StateBanner tone="danger" title="Não foi possível carregar os parâmetros">
            {tenantConfigQuery.error instanceof Error
              ? tenantConfigQuery.error.message
              : "Tente novamente."}
          </StateBanner>
        )}
        {tenantConfigQuery.data &&
          catalogs.map((catalog) => (
            <CatalogEditor
              key={catalog.type}
              catalog={catalog}
              options={tenantConfigQuery.data[catalog.key]}
            />
          ))}
      </div>
    </Section>
  );
}

function CatalogEditor({
  catalog,
  options,
}: {
  catalog: EditableCatalog;
  options: TenantConfigOption[];
}) {
  const queryClient = useQueryClient();
  const [draftOptions, setDraftOptions] = useState<TenantConfigOption[]>(options);
  const [newValue, setNewValue] = useState("");
  const [newLabel, setNewLabel] = useState("");
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const hasChanges = useMemo(
    () => JSON.stringify(draftOptions) !== JSON.stringify(options),
    [draftOptions, options],
  );

  const saveMutation = useMutation({
    mutationFn: () => updateTenantConfig(catalog.type, normalizeOptions(draftOptions)),
    onSuccess: async (savedOptions) => {
      setDraftOptions(savedOptions);
      setSuccessMessage("Parâmetro salvo.");
      await queryClient.invalidateQueries({ queryKey: ["tenant-config"] });
    },
  });

  const addOption = () => {
    const value = newValue.trim();
    const label = (newLabel.trim() || value).trim();
    if (!value || draftOptions.some((option) => sameValue(option.value, value))) return;
    setSuccessMessage(null);
    setDraftOptions((current) => [
      ...current,
      { value, label, locked: false, selectedByDefault: false },
    ]);
    setNewValue("");
    setNewLabel("");
  };

  const updateOption = (index: number, patch: Partial<TenantConfigOption>) => {
    setSuccessMessage(null);
    setDraftOptions((current) =>
      current.map((option, optionIndex) =>
        optionIndex === index && !option.locked ? { ...option, ...patch } : option,
      ),
    );
  };

  const removeOption = (index: number) => {
    setSuccessMessage(null);
    setDraftOptions((current) =>
      current.filter((option, optionIndex) => optionIndex !== index || option.locked),
    );
  };

  const setDefault = (index: number, selected: boolean) => {
    setSuccessMessage(null);
    setDraftOptions((current) =>
      current.map((option, optionIndex) => ({
        ...option,
        selectedByDefault:
          optionIndex === index ? selected : selected ? false : option.selectedByDefault,
      })),
    );
  };

  return (
    <div className="rounded-md border border-border bg-card">
      <div className="border-b border-border px-4 py-3">
        <h3 className="text-sm font-semibold">{catalog.title}</h3>
        <p className="mt-1 text-xs text-muted-foreground">{catalog.description}</p>
      </div>
      <div className="space-y-4 p-4">
        {successMessage && (
          <StateBanner tone="ok" title={successMessage}>
            As telas que usam este parâmetro passam a ler a nova configuração.
          </StateBanner>
        )}
        {saveMutation.isError && (
          <StateBanner tone="danger" title="Não foi possível salvar">
            {saveMutation.error instanceof Error ? saveMutation.error.message : "Tente novamente."}
          </StateBanner>
        )}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] text-sm">
            <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-3 py-2 text-left font-medium">{catalog.valueLabel}</th>
                <th className="px-3 py-2 text-left font-medium">{catalog.labelLabel}</th>
                <th className="px-3 py-2 text-left font-medium">Padrão</th>
                <th className="px-3 py-2 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {draftOptions.map((option, index) => (
                <tr
                  key={`${option.value}-${index}`}
                  className="border-b border-border last:border-0"
                >
                  <td className="px-3 py-2">
                    <Input
                      value={option.value}
                      inputMode={catalog.numericValue ? "numeric" : undefined}
                      disabled={option.locked || saveMutation.isPending}
                      onChange={(event) => updateOption(index, { value: event.target.value })}
                    />
                  </td>
                  <td className="px-3 py-2">
                    <Input
                      value={option.label}
                      disabled={option.locked || saveMutation.isPending}
                      onChange={(event) => updateOption(index, { label: event.target.value })}
                    />
                    {option.locked && (
                      <p className="mt-1 text-xs text-muted-foreground">Item travado.</p>
                    )}
                  </td>
                  <td className="px-3 py-2">
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={option.selectedByDefault}
                        disabled={option.locked || saveMutation.isPending}
                        onCheckedChange={(checked) => setDefault(index, checked)}
                        aria-label={`Marcar ${option.label} como padrão`}
                      />
                      <span className="text-xs text-muted-foreground">
                        {option.selectedByDefault ? "Sim" : "Não"}
                      </span>
                    </div>
                  </td>
                  <td className="px-3 py-2 text-right">
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      onClick={() => removeOption(index)}
                      disabled={option.locked || saveMutation.isPending}
                      title="Remover"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="grid gap-3 rounded-md border border-border bg-background p-3 md:grid-cols-[1fr_1fr_auto]">
          <div>
            <Label>{catalog.valueLabel}</Label>
            <Input
              value={newValue}
              inputMode={catalog.numericValue ? "numeric" : undefined}
              placeholder={catalog.numericValue ? "Ex: 45" : "Novo valor"}
              onChange={(event) => setNewValue(event.target.value)}
              disabled={saveMutation.isPending}
            />
          </div>
          <div>
            <Label>{catalog.labelLabel}</Label>
            <Input
              value={newLabel}
              placeholder={catalog.numericValue ? "Ex: 45 s" : "Opcional"}
              onChange={(event) => setNewLabel(event.target.value)}
              disabled={saveMutation.isPending}
              onKeyDown={(event) => {
                if (event.key === "Enter") addOption();
              }}
            />
          </div>
          <div className="flex items-end">
            <Button type="button" variant="outline" onClick={addOption} disabled={!newValue.trim()}>
              <Plus className="h-4 w-4" />
              Adicionar
            </Button>
          </div>
        </div>

        <div className="flex justify-end">
          <Button
            type="button"
            onClick={() => saveMutation.mutate()}
            disabled={
              !hasChanges || saveMutation.isPending || !isCatalogValid(draftOptions, catalog)
            }
          >
            <Save className="h-4 w-4" />
            {saveMutation.isPending ? "Salvando..." : "Salvar parâmetro"}
          </Button>
        </div>
      </div>
    </div>
  );
}

function Section({
  title,
  icon,
  children,
}: {
  title: string;
  icon: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-md border border-border bg-card">
      <div className="border-b border-border px-5 py-4">
        <div className="flex items-center gap-2 text-sm font-semibold">
          {icon}
          {title}
        </div>
      </div>
      <div className="p-5">{children}</div>
    </section>
  );
}

function ReadOnlyField({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className={`mt-1 text-sm ${mono ? "font-mono text-muted-foreground" : "font-medium"}`}>
        {value}
      </div>
    </div>
  );
}

function normalizeOptions(options: TenantConfigOption[]) {
  return options.map((option) => ({
    value: option.value.trim(),
    label: option.label.trim() || option.value.trim(),
    locked: option.locked,
    selectedByDefault: option.selectedByDefault,
  }));
}

function isCatalogValid(options: TenantConfigOption[], catalog: EditableCatalog) {
  const values = options.map((option) => option.value.trim()).filter(Boolean);
  const valuesAreValid = catalog.numericValue
    ? values.every((value) => Number.isFinite(Number(value)) && Number(value) >= 0)
    : true;
  return (
    valuesAreValid &&
    values.length === options.length &&
    new Set(values.map((value) => value.toLowerCase())).size === values.length
  );
}

function sameValue(left: string, right: string) {
  return left.trim().toLowerCase() === right.trim().toLowerCase();
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
