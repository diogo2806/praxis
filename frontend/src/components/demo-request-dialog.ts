import { createElement, type FormEvent, type ReactNode, useEffect, useState } from "react";

type DemoRequest = {
  name: string;
  email: string;
  company: string;
  role: string;
  hiringVolume: string;
  message: string;
};

const emptyForm: DemoRequest = { name: "", email: "", company: "", role: "", hiringVolume: "", message: "" };

export function DemoRequestDialog() {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<DemoRequest>(emptyForm);
  const [status, setStatus] = useState<"idle" | "sending" | "sent" | "error">("idle");
  const [error, setError] = useState("");

  useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      const target = event.target;
      if (!(target instanceof Element)) return;
      const link = target.closest<HTMLAnchorElement>('a[href^="mailto:"]');
      if (!link) return;
      event.preventDefault();
      setStatus("idle");
      setError("");
      setOpen(true);
    };
    document.addEventListener("click", handleClick, true);
    return () => document.removeEventListener("click", handleClick, true);
  }, []);

  const setValue = (field: keyof DemoRequest, value: string) => setForm((current) => ({ ...current, [field]: value }));

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setStatus("sending");
    setError("");
    try {
      const response = await fetch("/api/public/demo-requests", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...form, source: "landing-page" }),
      });
      if (!response.ok) throw new Error("Não foi possível enviar sua solicitação agora.");
      setForm(emptyForm);
      setStatus("sent");
    } catch (reason) {
      setStatus("error");
      setError(reason instanceof Error ? reason.message : "Não foi possível enviar sua solicitação agora.");
    }
  };

  if (!open) return null;

  const field = (label: string, input: ReactNode) => createElement("label", { className: "block text-sm font-medium text-slate-800" }, label, createElement("span", { className: "mt-1.5 block" }, input));

  return createElement(
    "div",
    { className: "fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/55 p-4", role: "dialog", "aria-modal": true, "aria-labelledby": "demo-request-title", onMouseDown: (event: React.MouseEvent<HTMLDivElement>) => event.target === event.currentTarget && setOpen(false) },
    createElement(
      "div",
      { className: "relative w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-8" },
      createElement("button", { type: "button", className: "absolute right-4 top-4 rounded px-2 py-1 text-sm text-slate-500 hover:bg-slate-100", onClick: () => setOpen(false), "aria-label": "Fechar" }, "Fechar"),
      status === "sent"
        ? createElement("div", { className: "pr-8" }, createElement("p", { className: "text-xs font-semibold uppercase tracking-[0.16em] text-emerald-700" }, "Solicitação recebida"), createElement("h2", { id: "demo-request-title", className: "mt-3 text-2xl font-semibold text-slate-950" }, "Vamos combinar sua demonstração."), createElement("p", { className: "mt-3 text-sm leading-6 text-slate-600" }, "Seus dados foram registrados. A equipe entrará em contato pelo e-mail informado."))
        : createElement(
          "form",
          { className: "space-y-4", onSubmit: submit },
          createElement("div", { className: "pr-8" }, createElement("p", { className: "text-xs font-semibold uppercase tracking-[0.16em] text-sky-700" }, "Demonstração da Práxis"), createElement("h2", { id: "demo-request-title", className: "mt-3 text-2xl font-semibold text-slate-950" }, "Veja um cenário aplicado à sua operação."), createElement("p", { className: "mt-2 text-sm leading-6 text-slate-600" }, "Deixe seus dados. A equipe entra em contato para mostrar o fluxo de avaliação e integração.")),
          createElement("div", { className: "grid gap-4 sm:grid-cols-2" },
            field("Seu nome", createElement("input", { required: true, value: form.name, autoComplete: "name", className: "w-full rounded-lg border border-slate-300 px-3 py-2", onChange: (event: React.ChangeEvent<HTMLInputElement>) => setValue("name", event.target.value) })),
            field("E-mail corporativo", createElement("input", { required: true, type: "email", value: form.email, autoComplete: "email", className: "w-full rounded-lg border border-slate-300 px-3 py-2", onChange: (event: React.ChangeEvent<HTMLInputElement>) => setValue("email", event.target.value) })),
            field("Empresa", createElement("input", { required: true, value: form.company, autoComplete: "organization", className: "w-full rounded-lg border border-slate-300 px-3 py-2", onChange: (event: React.ChangeEvent<HTMLInputElement>) => setValue("company", event.target.value) })),
            field("Seu cargo", createElement("input", { value: form.role, className: "w-full rounded-lg border border-slate-300 px-3 py-2", onChange: (event: React.ChangeEvent<HTMLInputElement>) => setValue("role", event.target.value) })),
          ),
          field("Volume médio de contratações", createElement("select", { value: form.hiringVolume, className: "w-full rounded-lg border border-slate-300 px-3 py-2", onChange: (event: React.ChangeEvent<HTMLSelectElement>) => setValue("hiringVolume", event.target.value) }, createElement("option", { value: "" }, "Selecione uma faixa"), createElement("option", null, "Até 20 por mês"), createElement("option", null, "21 a 100 por mês"), createElement("option", null, "Mais de 100 por mês"))),
          field("O que você quer avaliar? (opcional)", createElement("textarea", { value: form.message, maxLength: 1200, className: "min-h-24 w-full rounded-lg border border-slate-300 px-3 py-2", onChange: (event: React.ChangeEvent<HTMLTextAreaElement>) => setValue("message", event.target.value) })),
          status === "error" ? createElement("p", { className: "text-sm font-medium text-red-700", role: "alert" }, error) : null,
          createElement("div", { className: "flex flex-wrap items-center gap-3" }, createElement("button", { type: "submit", disabled: status === "sending", className: "rounded-xl bg-sky-700 px-5 py-3 text-sm font-semibold text-white hover:bg-sky-800 disabled:opacity-60" }, status === "sending" ? "Enviando..." : "Solicitar demonstração"), createElement("span", { className: "text-xs text-slate-500" }, "Usaremos seus dados apenas para responder ao pedido.")),
        ),
    ),
  );
}
