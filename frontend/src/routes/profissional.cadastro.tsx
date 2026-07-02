import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, Loader2, UserPlus } from "lucide-react";
import { useState } from "react";

import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { registerMarketplaceProfessional } from "@/lib/api/praxis";
import { splitList } from "@/lib/marketplace";

export const Route = createFileRoute("/profissional/cadastro")({
  head: () => ({
    meta: [{ title: "Cadastro profissional - Marketplace Práxis" }],
  }),
  component: ProfessionalSignupPage,
});

function ProfessionalSignupPage() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    document: "",
    professionalRegistration: "",
    bio: "",
    specialties: "",
    linkedinUrl: "",
    lattesUrl: "",
    pixKey: "",
  });

  const register = useMutation({
    mutationFn: () =>
      registerMarketplaceProfessional({
        ...form,
        specialties: splitList(form.specialties),
      }),
  });

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-5 py-6">
        <Button asChild variant="ghost" size="sm" className="mb-4">
          <Link to="/marketplace">
            <ArrowLeft className="h-4 w-4" />
            Marketplace
          </Link>
        </Button>

        <section className="rounded-md border border-border bg-card p-5">
          <div className="flex items-center gap-2 text-xs uppercase text-primary">
            <UserPlus className="h-4 w-4" />
            Profissionais
          </div>
          <h1 className="mt-2 text-2xl font-semibold">Cadastro para vender avaliações</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            O perfil entra em verificação manual antes da publicação de anúncios.
          </p>

          {register.isSuccess && (
            <div className="mt-5">
              <StateBanner tone="ok" title="Cadastro enviado para verificação">
                Conta criada para {register.data.email}. A moderação precisa aprovar o perfil antes
                da venda.
              </StateBanner>
            </div>
          )}
          {register.isError && (
            <div className="mt-5">
              <StateBanner tone="danger" title="Não foi possível concluir o cadastro">
                {register.error instanceof Error ? register.error.message : "Tente novamente."}
              </StateBanner>
            </div>
          )}

          <form
            className="mt-5 grid gap-4"
            onSubmit={(event) => {
              event.preventDefault();
              register.mutate();
            }}
          >
            <div className="grid gap-4 md:grid-cols-2">
              <Field
                label="Nome"
                value={form.name}
                onChange={(value) => setForm({ ...form, name: value })}
                required
              />
              <Field
                label="E-mail"
                type="email"
                value={form.email}
                onChange={(value) => setForm({ ...form, email: value })}
                required
              />
              <Field
                label="Senha"
                type="password"
                value={form.password}
                onChange={(value) => setForm({ ...form, password: value })}
                required
              />
              <Field
                label="CPF/CNPJ"
                value={form.document}
                onChange={(value) => setForm({ ...form, document: value })}
                required
              />
              <Field
                label="Registro profissional"
                value={form.professionalRegistration}
                onChange={(value) => setForm({ ...form, professionalRegistration: value })}
              />
              <Field
                label="Chave Pix"
                value={form.pixKey}
                onChange={(value) => setForm({ ...form, pixKey: value })}
              />
            </div>
            <Field
              label="Especialidades"
              value={form.specialties}
              onChange={(value) => setForm({ ...form, specialties: value })}
              placeholder="Seleção, liderança, atendimento"
            />
            <Field
              label="LinkedIn"
              value={form.linkedinUrl}
              onChange={(value) => setForm({ ...form, linkedinUrl: value })}
            />
            <Field
              label="Curriculo Lattes (URL)"
              value={form.lattesUrl}
              onChange={(value) => setForm({ ...form, lattesUrl: value })}
              placeholder="https://lattes.cnpq.br/0000000000000000"
              required
            />
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Bio</span>
              <textarea
                value={form.bio}
                onChange={(event) => setForm({ ...form, bio: event.target.value })}
                rows={5}
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </label>
            <div className="flex justify-end">
              <Button type="submit" disabled={register.isPending}>
                {register.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                Enviar cadastro
              </Button>
            </div>
          </form>
        </section>
      </div>
    </main>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  required,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
  placeholder?: string;
}) {
  return (
    <label className="grid gap-1 text-sm">
      <span className="font-medium">{label}</span>
      <Input
        type={type}
        value={value}
        required={required}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}
