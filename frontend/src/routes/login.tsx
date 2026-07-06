import { useEffect, useMemo, useState, type FormEvent } from "react";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { ArrowRight, CheckCircle2, LockKeyhole, ShieldCheck } from "lucide-react";

import { LanguageSelector } from "@/components/language-selector";
import { login } from "@/lib/api/auth";
import { useLanguage } from "@/lib/language-context";
import { PraxisApiError } from "@/lib/api/praxis";
import { defaultAuthenticatedRoute, getSession, saveAuthenticatedSession } from "@/lib/session";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Entrar - Práxis" },
      {
        name: "description",
        content:
          "Acesse as avaliações e os resultados da sua empresa no Práxis, com critérios claros e histórico auditável.",
      },
    ],
  }),
  component: LoginPage,
});

type LoginFormState = {
  empresaId: string;
  email: string;
  password: string;
};

const initialForm: LoginFormState = {
  empresaId: "",
  email: "",
  password: "",
};

const loginCopy = {
  "pt-BR": {
    headTitle: "Entrar - Práxis",
    secureAccess: "Acesso da empresa",
    heroTitle: "Acesse as avaliações e os resultados da sua empresa.",
    heroDescription:
      "Crie avaliações situacionais, acompanhe candidatos e revise resultados por competência, com critérios claros e histórico auditável.",
    securityItems: [
      "Seus dados ficam separados dos dados de outras empresas.",
      "Critérios e resultados preservam o histórico da avaliação.",
      "Decisões de contratação continuam sob responsabilidade humana.",
    ],
    panelTitle: "Entrar no painel",
    panelDescription: "Informe o código da empresa, seu e-mail e senha para continuar.",
    companyLabel: "Código da empresa",
    companyPlaceholder: "ex.: minha-empresa",
    emailLabel: "E-mail",
    emailPlaceholder: "voce@empresa.com",
    passwordLabel: "Senha",
    passwordPlaceholder: "Digite sua senha",
    submitting: "Entrando...",
    submit: "Entrar",
    backToSite: "Voltar ao site",
    companyIdHint: "Use o código recebido no convite ou informado pela pessoa administradora da sua empresa.",
    forgotPassword: "Esqueci minha senha",
    firstAccess: "Primeiro acesso?",
    inviteInstruction: "Use o link de convite enviado para seu e-mail para definir sua senha.",
    invalidCredentials: "Credenciais inválidas ou usuário sem permissão para esta empresa.",
    invalidFields: "Confira o código da empresa, e-mail e senha antes de tentar novamente.",
    genericError: "Não foi possível entrar agora. Verifique sua conexão e tente novamente.",
  },
  en: {
    headTitle: "Sign in - Práxis",
    secureAccess: "Company access",
    heroTitle: "Access your company’s assessments and results.",
    heroDescription:
      "Create situational assessments, follow candidates, and review competency results with clear criteria and an auditable history.",
    securityItems: [
      "Your data is kept separate from other companies’ data.",
      "Criteria and results preserve each assessment’s history.",
      "Hiring decisions remain under human responsibility.",
    ],
    panelTitle: "Sign in to the dashboard",
    panelDescription: "Enter your company code, email, and password to continue.",
    companyLabel: "Company code",
    companyPlaceholder: "e.g. my-company",
    emailLabel: "Email",
    emailPlaceholder: "you@company.com",
    passwordLabel: "Password",
    passwordPlaceholder: "Enter your password",
    submitting: "Signing in...",
    submit: "Sign in",
    backToSite: "Back to site",
    companyIdHint: "Use the code from your invitation or provided by your company administrator.",
    forgotPassword: "Forgot your password?",
    firstAccess: "First access?",
    inviteInstruction: "Use the invitation link sent to your email to set your password.",
    invalidCredentials: "Invalid credentials or user without permission for this company.",
    invalidFields: "Check the company code, email, and password before trying again.",
    genericError: "Could not sign in right now. Check your connection and try again.",
  },
  "es-MX": {
    headTitle: "Iniciar sesión - Práxis",
    secureAccess: "Acceso de la empresa",
    heroTitle: "Accede a las evaluaciones y resultados de tu empresa.",
    heroDescription:
      "Crea evaluaciones situacionales, acompaña candidatos y revisa resultados por competencia, con criterios claros e historial auditable.",
    securityItems: [
      "Tus datos permanecen separados de los datos de otras empresas.",
      "Los criterios y resultados conservan el historial de cada evaluación.",
      "Las decisiones de contratación siguen bajo responsabilidad humana.",
    ],
    panelTitle: "Iniciar sesión en el panel",
    panelDescription: "Ingresa el código de la empresa, tu correo y contraseña para continuar.",
    companyLabel: "Código de la empresa",
    companyPlaceholder: "ej.: mi-empresa",
    emailLabel: "Correo",
    emailPlaceholder: "tu@empresa.com",
    passwordLabel: "Contraseña",
    passwordPlaceholder: "Ingresa tu contraseña",
    submitting: "Ingresando...",
    submit: "Iniciar sesión",
    backToSite: "Volver al sitio",
    companyIdHint: "Usa el código recibido en la invitación o informado por la persona administradora de tu empresa.",
    forgotPassword: "¿Olvidaste tu contraseña?",
    firstAccess: "¿Primer acceso?",
    inviteInstruction: "Usa el enlace de invitación enviado a tu correo para definir tu contraseña.",
    invalidCredentials: "Credenciales inválidas o usuario sin permiso para esta empresa.",
    invalidFields: "Revisa el código de la empresa, correo y contraseña antes de intentarlo nuevamente.",
    genericError: "No fue posible iniciar sesión ahora. Revisa tu conexión e inténtalo nuevamente.",
  },
} as const;

type LoginCopy = (typeof loginCopy)[keyof typeof loginCopy];

function LoginPage() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const copy = loginCopy[language];
  const [form, setForm] = useState<LoginFormState>(initialForm);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    document.title = copy.headTitle;
  }, [copy.headTitle]);

  useEffect(() => {
    if (getSession().token) {
      navigate({ to: defaultAuthenticatedRoute(), replace: true });
    }
  }, [navigate]);

  const canSubmit = useMemo(
    () => form.empresaId.trim() !== "" && form.email.trim() !== "" && form.password.trim() !== "",
    [form],
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit || isSubmitting) return;

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const session = await login({
        empresaId: form.empresaId.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      saveAuthenticatedSession(session);
      await navigate({ to: defaultAuthenticatedRoute(), replace: true });
    } catch (error) {
      setErrorMessage(resolveLoginError(error, copy));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,hsl(var(--primary)/0.12),transparent_34%),linear-gradient(135deg,hsl(var(--background)),hsl(var(--muted)/0.35))] px-4 py-8 text-foreground sm:px-6 lg:px-8">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-6xl items-center">
        <div className="grid w-full gap-8 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
          <section className="hidden rounded-[2rem] border border-border/70 bg-card/80 p-8 shadow-2xl shadow-primary/10 backdrop-blur lg:block">
            <Link to="/" className="inline-flex items-center gap-2 text-lg font-semibold tracking-tight">
              <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary text-primary-foreground">
                P
              </span>
              Práxis
            </Link>

            <div className="mt-16 max-w-xl">
              <span className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-[0.22em] text-primary">
                <ShieldCheck className="h-3.5 w-3.5" /> {copy.secureAccess}
              </span>
              <h1 className="mt-6 text-4xl font-semibold tracking-tight text-foreground xl:text-5xl">
                {copy.heroTitle}
              </h1>
              <p className="mt-5 text-base leading-7 text-muted-foreground">
                {copy.heroDescription}
              </p>
            </div>

            <div className="mt-12 grid gap-3 text-sm text-muted-foreground">
              {copy.securityItems.map((item) => (
                <div
                  key={item}
                  className="flex items-center gap-3 rounded-2xl border border-border/70 bg-background/70 p-4"
                >
                  <CheckCircle2 className="h-4 w-4 text-primary" />
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </section>

          <section className="mx-auto w-full max-w-md rounded-[2rem] border border-border bg-card p-6 shadow-xl sm:p-8">
            <div className="mb-8 lg:hidden">
              <span className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-medium uppercase tracking-[0.22em] text-primary">
                <ShieldCheck className="h-3.5 w-3.5" /> {copy.secureAccess}
              </span>
              <h1 className="mt-4 text-2xl font-semibold tracking-tight text-foreground">
                {copy.heroTitle}
              </h1>
              <p className="mt-3 text-sm leading-6 text-muted-foreground">{copy.heroDescription}</p>
            </div>

            <div className="mb-8 flex items-start justify-between gap-4">
              <div>
                <Link to="/" className="text-sm font-semibold text-primary hover:underline">
                  Práxis
                </Link>
                <h2 className="mt-4 text-2xl font-semibold tracking-tight text-foreground">
                  {copy.panelTitle}
                </h2>
                <p className="mt-2 text-sm text-muted-foreground">
                  {copy.panelDescription}
                </p>
              </div>
              <div className="flex shrink-0 flex-col items-end gap-3">
                <LanguageSelector className="bg-background" />
                <div className="grid h-11 w-11 place-items-center rounded-2xl bg-primary/10 text-primary">
                  <LockKeyhole className="h-5 w-5" />
                </div>
              </div>
            </div>

            <form className="space-y-4" onSubmit={handleSubmit}>
              <Field
                label={copy.companyLabel}
                value={form.empresaId}
                autoComplete="organization"
                placeholder={copy.companyPlaceholder}
                onChange={(empresaId) => setForm((current) => ({ ...current, empresaId }))}
              />
              <Field
                label={copy.emailLabel}
                type="email"
                value={form.email}
                autoComplete="email"
                placeholder={copy.emailPlaceholder}
                onChange={(email) => setForm((current) => ({ ...current, email }))}
              />
              <Field
                label={copy.passwordLabel}
                type="password"
                value={form.password}
                autoComplete="current-password"
                placeholder={copy.passwordPlaceholder}
                onChange={(password) => setForm((current) => ({ ...current, password }))}
              />

              {errorMessage ? (
                <div className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
                  {errorMessage}
                </div>
              ) : null}

              <button
                type="submit"
                disabled={!canSubmit || isSubmitting}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting ? copy.submitting : copy.submit}
                {!isSubmitting ? <ArrowRight className="h-4 w-4" /> : null}
              </button>
            </form>

            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
              <Link to="/recuperar-senha" className="font-medium text-primary hover:underline">
                {copy.forgotPassword}
              </Link>
              <Link to="/" className="text-muted-foreground hover:text-foreground hover:underline">
                {copy.backToSite}
              </Link>
            </div>

            <div className="mt-6 rounded-xl border border-border/70 bg-muted/30 p-4 text-sm text-muted-foreground">
              <p>{copy.companyIdHint}</p>
              <p className="mt-3">
                <span className="font-medium text-foreground">{copy.firstAccess}</span> {copy.inviteInstruction}
              </p>
            </div>
          </section>
        </div>
      </div>
    </main>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  autoComplete,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: "text" | "email" | "password";
  autoComplete?: string;
  placeholder?: string;
}) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-medium text-foreground">{label}</span>
      <input
        value={value}
        type={type}
        autoComplete={autoComplete}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-xl border border-input bg-background px-4 py-3 text-sm text-foreground outline-none transition placeholder:text-muted-foreground/70 focus:border-primary focus:ring-4 focus:ring-primary/10"
      />
    </label>
  );
}

function resolveLoginError(error: unknown, copy: LoginCopy) {
  if (error instanceof PraxisApiError) {
    if (error.status === 401 || error.status === 403) {
      return copy.invalidCredentials;
    }
    if (error.status === 400) {
      return error.message || copy.invalidFields;
    }
    return error.message;
  }

  return copy.genericError;
}
