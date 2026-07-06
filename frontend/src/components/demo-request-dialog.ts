const dialogId = "praxis-demo-request-dialog";

function openDemoRequestDialog() {
  if (document.getElementById(dialogId)) return;

  const overlay = document.createElement("div");
  overlay.id = dialogId;
  overlay.className = "fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/55 p-4";
  overlay.setAttribute("role", "dialog");
  overlay.setAttribute("aria-modal", "true");
  overlay.setAttribute("aria-labelledby", "demo-request-title");
  overlay.innerHTML = `
    <div class="relative w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl sm:p-8">
      <button type="button" data-close class="absolute right-4 top-4 rounded px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" aria-label="Fechar">Fechar</button>
      <form class="space-y-4" data-form>
        <div class="pr-8">
          <p class="text-xs font-semibold uppercase tracking-[0.16em] text-sky-700">Demonstração da Práxis</p>
          <h2 id="demo-request-title" class="mt-3 text-2xl font-semibold text-slate-950">Veja um cenário aplicado à sua operação.</h2>
          <p class="mt-2 text-sm leading-6 text-slate-600">Deixe seus dados. A equipe entra em contato para mostrar o fluxo de avaliação e integração.</p>
        </div>
        <div class="grid gap-4 sm:grid-cols-2">
          <label class="block text-sm font-medium text-slate-800">Seu nome<span class="mt-1.5 block"><input name="name" required autocomplete="name" class="w-full rounded-lg border border-slate-300 px-3 py-2" /></span></label>
          <label class="block text-sm font-medium text-slate-800">E-mail corporativo<span class="mt-1.5 block"><input name="email" type="email" required autocomplete="email" class="w-full rounded-lg border border-slate-300 px-3 py-2" /></span></label>
          <label class="block text-sm font-medium text-slate-800">Empresa<span class="mt-1.5 block"><input name="company" required autocomplete="organization" class="w-full rounded-lg border border-slate-300 px-3 py-2" /></span></label>
          <label class="block text-sm font-medium text-slate-800">Seu cargo<span class="mt-1.5 block"><input name="role" class="w-full rounded-lg border border-slate-300 px-3 py-2" /></span></label>
        </div>
        <label class="block text-sm font-medium text-slate-800">Volume médio de contratações<span class="mt-1.5 block"><select name="hiringVolume" class="w-full rounded-lg border border-slate-300 px-3 py-2"><option value="">Selecione uma faixa</option><option>Até 20 por mês</option><option>21 a 100 por mês</option><option>Mais de 100 por mês</option></select></span></label>
        <label class="block text-sm font-medium text-slate-800">O que você quer avaliar? (opcional)<span class="mt-1.5 block"><textarea name="message" maxlength="1200" class="min-h-24 w-full rounded-lg border border-slate-300 px-3 py-2"></textarea></span></label>
        <p data-error class="hidden text-sm font-medium text-red-700" role="alert"></p>
        <div class="flex flex-wrap items-center gap-3"><button type="submit" data-submit class="rounded-xl bg-sky-700 px-5 py-3 text-sm font-semibold text-white hover:bg-sky-800 disabled:opacity-60">Solicitar demonstração</button><span class="text-xs text-slate-500">Usaremos seus dados apenas para responder ao pedido.</span></div>
      </form>
    </div>`;

  const close = () => overlay.remove();
  overlay.addEventListener("mousedown", (event) => {
    if (event.target === overlay) close();
  });
  overlay.querySelector<HTMLElement>("[data-close]")?.addEventListener("click", close);

  const form = overlay.querySelector<HTMLFormElement>("[data-form]");
  const submit = overlay.querySelector<HTMLButtonElement>("[data-submit]");
  const error = overlay.querySelector<HTMLElement>("[data-error]");
  form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    submit?.setAttribute("disabled", "true");
    if (submit) submit.textContent = "Enviando...";
    error?.classList.add("hidden");

    const values = new FormData(form);
    const body = Object.fromEntries(values.entries());
    try {
      const response = await fetch("/api/public/demo-requests", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...body, source: "landing-page" }),
      });
      if (!response.ok) throw new Error();
      overlay.querySelector("div.relative")!.innerHTML = `
        <button type="button" data-close class="absolute right-4 top-4 rounded px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" aria-label="Fechar">Fechar</button>
        <div class="pr-8"><p class="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-700">Solicitação recebida</p><h2 id="demo-request-title" class="mt-3 text-2xl font-semibold text-slate-950">Vamos combinar sua demonstração.</h2><p class="mt-3 text-sm leading-6 text-slate-600">Seus dados foram registrados. A equipe entrará em contato pelo e-mail informado.</p></div>`;
      overlay.querySelector<HTMLElement>("[data-close]")?.addEventListener("click", close);
    } catch {
      submit?.removeAttribute("disabled");
      if (submit) submit.textContent = "Solicitar demonstração";
      if (error) {
        error.textContent = "Não foi possível enviar sua solicitação agora. Tente novamente.";
        error.classList.remove("hidden");
      }
    }
  });

  document.body.appendChild(overlay);
  overlay.querySelector<HTMLInputElement>("input[name='name']")?.focus();
}

if (typeof window !== "undefined") {
  document.addEventListener("click", (event) => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    if (!target.closest<HTMLAnchorElement>('a[href^="mailto:"]')) return;
    event.preventDefault();
    openDemoRequestDialog();
  }, true);
}
