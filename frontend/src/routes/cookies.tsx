import { createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, Cookie } from "lucide-react";

export const Route = createFileRoute("/cookies")({ component: CookiesPage });

function CookiesPage() {
  return (
    <main className="min-h-screen bg-background px-4 py-10 text-foreground">
      <article className="mx-auto max-w-3xl rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-10">
        <a href="/" className="inline-flex items-center gap-2 text-sm text-primary hover:underline">
          <ArrowLeft className="h-4 w-4" aria-hidden /> Voltar
        </a>
        <div className="mt-8 flex items-center gap-3">
          <Cookie className="h-6 w-6 text-primary" aria-hidden />
          <h1 className="text-3xl font-semibold">Cookies e armazenamento no navegador</h1>
        </div>
        <p className="mt-3 text-sm text-muted-foreground">Versão 1.0 · 20 de julho de 2026</p>

        <div className="mt-8 space-y-7 text-sm leading-7 text-muted-foreground">
          <section>
            <h2 className="text-lg font-semibold text-foreground">Uso atual</h2>
            <p>O Práxis utiliza somente tecnologias necessárias para autenticação, segurança, idioma e continuidade da navegação. O token da área administrativa fica limitado à sessão do navegador e é removido ao encerrar a sessão ou sair da conta.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Categorias</h2>
            <ul className="list-disc space-y-2 pl-5">
              <li><strong className="text-foreground">Essenciais:</strong> sessão, autenticação, prevenção de fraude e funcionamento das telas.</li>
              <li><strong className="text-foreground">Preferências:</strong> idioma e escolhas de interface.</li>
              <li><strong className="text-foreground">Analíticos:</strong> não são ativados por padrão. Caso sejam adotados, dependerão de informação prévia e mecanismo de escolha adequado.</li>
              <li><strong className="text-foreground">Marketing:</strong> não utilizados por padrão.</li>
            </ul>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Terceiros</h2>
            <p>Ao abrir páginas externas, como o checkout do Mercado Pago, o serviço de destino pode utilizar suas próprias tecnologias, regidas por seus documentos de privacidade.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Alterações</h2>
            <p>Novos rastreadores não essenciais não devem ser ativados sem atualização deste inventário e, quando necessário, sem coleta prévia de consentimento por categoria, com opção equivalente de rejeição.</p>
          </section>
        </div>
      </article>
    </main>
  );
}
