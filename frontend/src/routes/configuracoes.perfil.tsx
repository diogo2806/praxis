import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Building2, Pencil, Save, X } from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { SkeletonRows, StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import {
  getCompanyProfile,
  updateCompanyProfile,
  type CompanyProfileResponse,
  type UpdateCompanyProfileRequest,
} from "@/lib/api/company-profile";

export const Route = createFileRoute("/configuracoes/perfil")({
  head: () => ({
    meta: [
      { title: "Perfil da empresa - Práxis" },
      {
        name: "description",
        content: "Consulta e edição dos dados cadastrais da empresa no Práxis.",
      },
    ],
  }),
  component: CompanyProfilePage,
});

type ProfileForm = Record<keyof UpdateCompanyProfileRequest, string>;

const EMPTY_FORM: ProfileForm = {
  tradeName: "",
  legalName: "",
  taxId: "",
  corporateEmail: "",
  phone: "",
  website: "",
};

function CompanyProfilePage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [saved, setSaved] = useState(false);
  const [form, setForm] = useState<ProfileForm>(EMPTY_FORM);

  const profileQuery = useQuery({
    queryKey: ["company-profile"],
    queryFn: getCompanyProfile,
  });

  useEffect(() => {
    if (profileQuery.data && !editing) {
      setForm(toForm(profileQuery.data));
    }
  }, [editing, profileQuery.data]);

  const updateMutation = useMutation({
    mutationFn: updateCompanyProfile,
    onSuccess: (profile) => {
      queryClient.setQueryData(["company-profile"], profile);
      setForm(toForm(profile));
      setEditing(false);
      setSaved(true);
    },
  });

  function startEditing() {
    if (!profileQuery.data) return;
    setForm(toForm(profileQuery.data));
    setSaved(false);
    setEditing(true);
  }

  function cancelEditing() {
    if (profileQuery.data) {
      setForm(toForm(profileQuery.data));
    }
    updateMutation.reset();
    setEditing(false);
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaved(false);
    updateMutation.mutate({
      tradeName: form.tradeName.trim(),
      legalName: nullable(form.legalName),
      taxId: nullable(form.taxId),
      corporateEmail: nullable(form.corporateEmail),
      phone: nullable(form.phone),
      website: nullable(form.website),
    });
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl space-y-6">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="text-xs uppercase text-primary">Configurações</div>
            <h1 className="mt-1 text-3xl font-semibold">Perfil da empresa</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
              Consulte e mantenha os dados cadastrais usados na identificação da empresa, nas
              comunicações operacionais e nas integrações.
            </p>
          </div>
          {!editing && profileQuery.data && (
            <Button type="button" onClick={startEditing}>
              <Pencil />
              Editar dados
            </Button>
          )}
        </header>

        {saved && (
          <StateBanner tone="ok" title="Perfil atualizado">
            Os dados foram salvos e a alteração foi registrada na auditoria.
          </StateBanner>
        )}

        {updateMutation.isError && (
          <StateBanner tone="danger" title="Não foi possível salvar o perfil">
            {updateMutation.error instanceof Error
              ? updateMutation.error.message
              : "Revise os campos e tente novamente."}
          </StateBanner>
        )}

        <section className="rounded-md border border-border bg-card">
          <div className="border-b border-border px-5 py-4">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <Building2 className="h-4 w-4 text-primary" />
              Cadastro empresarial
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              Permissão necessária: usuário autenticado com perfil EMPRESA da própria organização.
            </p>
          </div>

          <div className="p-5">
            {profileQuery.isLoading && <SkeletonRows rows={4} />}
            {profileQuery.isError && (
              <StateBanner tone="danger" title="Não foi possível carregar o perfil">
                {profileQuery.error instanceof Error ? profileQuery.error.message : "Tente novamente."}
              </StateBanner>
            )}

            {profileQuery.data && !editing && (
              <div className="grid gap-4 md:grid-cols-2">
                <ReadOnlyField label="Nome fantasia" value={profileQuery.data.tradeName} />
                <ReadOnlyField label="Razão social" value={profileQuery.data.legalName} />
                <ReadOnlyField label="CNPJ" value={profileQuery.data.taxId} />
                <ReadOnlyField label="E-mail corporativo" value={profileQuery.data.corporateEmail} />
                <ReadOnlyField label="Telefone" value={profileQuery.data.phone} />
                <ReadOnlyField label="Site" value={profileQuery.data.website} />
              </div>
            )}

            {profileQuery.data && editing && (
              <form onSubmit={submit} className="space-y-5">
                <div className="grid gap-4 md:grid-cols-2">
                  <ProfileField
                    label="Nome fantasia"
                    name="tradeName"
                    value={form.tradeName}
                    onChange={setForm}
                    required
                    maxLength={180}
                  />
                  <ProfileField
                    label="Razão social"
                    name="legalName"
                    value={form.legalName}
                    onChange={setForm}
                    maxLength={180}
                  />
                  <ProfileField
                    label="CNPJ"
                    name="taxId"
                    value={form.taxId}
                    onChange={setForm}
                    maxLength={40}
                  />
                  <ProfileField
                    label="E-mail corporativo"
                    name="corporateEmail"
                    value={form.corporateEmail}
                    onChange={setForm}
                    type="email"
                    maxLength={180}
                  />
                  <ProfileField
                    label="Telefone"
                    name="phone"
                    value={form.phone}
                    onChange={setForm}
                    type="tel"
                    maxLength={40}
                  />
                  <ProfileField
                    label="Site"
                    name="website"
                    value={form.website}
                    onChange={setForm}
                    type="url"
                    placeholder="https://empresa.com.br"
                    maxLength={240}
                  />
                </div>

                <div className="flex flex-wrap justify-end gap-2 border-t border-border pt-5">
                  <Button type="button" variant="outline" onClick={cancelEditing} disabled={updateMutation.isPending}>
                    <X />
                    Cancelar
                  </Button>
                  <Button type="submit" disabled={updateMutation.isPending || !form.tradeName.trim()}>
                    <Save />
                    {updateMutation.isPending ? "Salvando..." : "Salvar alterações"}
                  </Button>
                </div>
              </form>
            )}
          </div>
        </section>
      </div>
    </AppShell>
  );
}

function ProfileField({
  label,
  name,
  value,
  onChange,
  type = "text",
  placeholder,
  required = false,
  maxLength,
}: {
  label: string;
  name: keyof ProfileForm;
  value: string;
  onChange: React.Dispatch<React.SetStateAction<ProfileForm>>;
  type?: "text" | "email" | "tel" | "url";
  placeholder?: string;
  required?: boolean;
  maxLength: number;
}) {
  const id = `company-profile-${name}`;
  return (
    <label htmlFor={id} className="block">
      <span className="mb-1.5 block text-sm font-medium text-foreground">{label}</span>
      <input
        id={id}
        name={name}
        type={type}
        value={value}
        required={required}
        maxLength={maxLength}
        placeholder={placeholder}
        onChange={(event) =>
          onChange((current) => ({ ...current, [name]: event.target.value }))
        }
        className="min-h-11 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground outline-none transition focus-visible:ring-2 focus-visible:ring-ring"
      />
    </label>
  );
}

function ReadOnlyField({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="rounded-md border border-border bg-background p-4">
      <div className="text-xs uppercase text-muted-foreground">{label}</div>
      <div className="mt-1 min-h-5 break-words text-sm font-medium text-foreground">
        {value?.trim() || "-"}
      </div>
    </div>
  );
}

function toForm(profile: CompanyProfileResponse): ProfileForm {
  return {
    tradeName: profile.tradeName ?? "",
    legalName: profile.legalName ?? "",
    taxId: profile.taxId ?? "",
    corporateEmail: profile.corporateEmail ?? "",
    phone: profile.phone ?? "",
    website: profile.website ?? "",
  };
}

function nullable(value: string) {
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : null;
}
